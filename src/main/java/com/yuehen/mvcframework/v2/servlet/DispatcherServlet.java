package com.yuehen.mvcframework.v2.servlet;

import com.yuehen.mvcframework.annotation.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

/**
 * @author 吃土的飞鱼
 * @date 2019/3/26
 */
public class DispatcherServlet extends HttpServlet {
    //application.properties中的配置项
    private Properties contextConfig = new Properties();

    //存放所有扫描到的类
    private List<String> classNames = new ArrayList<String>();
    //ioc容器，注册式单例
    private Map<String, Object> ioc = new HashMap<String, Object>();
    //handerMapping，url和method的映射关系
    private Map<String, Method> handlerMapping = new HashMap<String, Method>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //处理请求
        // 调用/demo/query    执行 DemoAction的query方法：   method.invoke(xx,xx);
        //1、解析出url和method的映射
        //2、参数赋值
        //3、根据url调用相应method执行
        try {
            //委派模式
            doDispatcher(req, resp);
        } catch (Exception e) {
            resp.getWriter().write("500 Exception ： " + Arrays.toString(e.getStackTrace()));
        }

    }

    //http://localhost:8080/demo/query?name=Tom
    private void doDispatcher(HttpServletRequest req, HttpServletResponse resp) throws IOException, InvocationTargetException, IllegalAccessException {
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replace(contextPath, "").replaceAll("/+", "/");
        if (!handlerMapping.containsKey(url)) {resp.getWriter().write("404 Not Found!!!"); return;}
        Method method = handlerMapping.get(url);
        Map<String, String[]> parameterMap = req.getParameterMap();
        //获取方法对应的参数
        Class<?>[] parameterTypes = method.getParameterTypes();
        Annotation[][] annotations = method.getParameterAnnotations();
        Object[] paramValues = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> parameterType = parameterTypes[i];
            if (parameterType == HttpServletRequest.class) {
                paramValues[i] = req;
                continue;
            } else if (parameterType == HttpServletResponse.class) {
                paramValues[i] = resp;
                continue;
            } else if (parameterType == String.class) {
                //这里只处理了String类型，需要扩展
                Annotation ano = annotations[i][0];
                if (ano instanceof RequestParm) {
                    RequestParm requestParm = (RequestParm) ano;
                    if (parameterMap.containsKey(requestParm.value())) {
                        for (Map.Entry<String, String[]> param : parameterMap.entrySet()) {
                            String value = Arrays.toString(param.getValue())
                                    .replaceAll("\\[|\\]","");
                            paramValues[i] = value;
                        }
                    }
                }
            }
        }

        String beanName = toLowerFirstCase(method.getDeclaringClass().getSimpleName());
        method.invoke(ioc.get(beanName), paramValues);
    }

    //提取公共流程，使用模板方法模式
    @Override
    public void init(ServletConfig config) throws ServletException {
        //1、加载配置文件
        doLoadConfig(config.getInitParameter("contextConfigLocation"));

        //2、扫描相关类
        doScanner(contextConfig.getProperty("scanPackage"));

        //3、初始化所有相关类的实例，并放入IOC容器中
        doInstance();

        //4、完成依赖注入
        doAutowried();

        //5、初始化HanderMapping
        initHandlerMapping();

        System.out.println("Yuehen mvcframework is inited ..");
    }

    private void initHandlerMapping() {
        if (ioc.isEmpty()) {return;}
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();
            if (!clazz.isAnnotationPresent(Controller.class)) {continue;}
            String baseUrl = "";
            if (clazz.isAnnotationPresent(RequestMapping.class)) {
                RequestMapping requestMapping = clazz.getAnnotation(RequestMapping.class);
                baseUrl = requestMapping.value();
            }
            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                if (!method.isAnnotationPresent(RequestMapping.class)) {continue;}
                RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
                String url = (baseUrl + "/" + requestMapping.value()).replaceAll("/+", "/");
                handlerMapping.put(url, method);
                System.out.println("Mapped " + url + " , " + method);
            }
        }
    }

    private void doAutowried() {
        if (ioc.isEmpty()) {return;}
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field field : fields) {
                if (!field.isAnnotationPresent(Autowired.class)) {continue;}
                Autowired autowired = field.getAnnotation(Autowired.class);
                String beanName = autowired.value().trim();
                if ("".equals(beanName)) {
                    beanName = field.getType().getName();
                }
                field.setAccessible(true);
                try {
                    field.set(entry.getValue(), ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    continue;
                }
            }
        }
    }

    private void doInstance() {
        if (classNames.isEmpty()) {return;}
        try {
            for (String className : classNames) {
                if (!className.contains(".")) {continue;}
                Class<?> clazz = Class.forName(className);
                if (clazz.isAnnotationPresent(Controller.class)) {
                    Object instance = clazz.newInstance();
                    String beanName = toLowerFirstCase(clazz.getSimpleName());
                    ioc.put(beanName, instance);
                } else if (clazz.isAnnotationPresent(Service.class)) {
                    String beanName = toLowerFirstCase(clazz.getSimpleName());
                    Service service = clazz.getAnnotation(Service.class);
                    if (!"".equals(service.value())) {beanName = service.value();}
                    Object instance = clazz.newInstance();
                    ioc.put(beanName, instance);
                    //类的接口也是对应这个实例
                    for (Class<?> a : clazz.getInterfaces()) {
                        if (ioc.containsKey(a.getName()))
                            throw new Exception("The bean “" + a.getName() + "”is Exsited.");
                        ioc.put(a.getName(), instance);
                    }
                } else {
                    continue;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private String toLowerFirstCase(String simpleName) {
        char[] chars = simpleName.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }

    private void doLoadConfig(String contextConfigLocation) {
        InputStream is = null;
        try {
            is = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
            contextConfig.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void doScanner(String scanPackage) {
        URL url = this.getClass().getClassLoader().getResource("/" + scanPackage.replaceAll("\\.", "/"));
        File classDir = new File(url.getFile());
        for (File file : classDir.listFiles()) {
            if (file.isDirectory()) {
                doScanner(scanPackage + "." + file.getName());
                continue;
            } else {
                if (!file.getName().endsWith(".class")) {continue;}
                String className = scanPackage + "." + file.getName().replace(".class", "");
                classNames.add(className);
            } 
        }
        
    }
}
