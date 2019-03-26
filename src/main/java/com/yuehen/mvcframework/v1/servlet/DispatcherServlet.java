package com.yuehen.mvcframework.v1.servlet;

import com.yuehen.demo.mvc.action.DemoAction;
import com.yuehen.mvcframework.annotation.Autowired;
import com.yuehen.mvcframework.annotation.Controller;
import com.yuehen.mvcframework.annotation.RequestMapping;
import com.yuehen.mvcframework.annotation.Service;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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

    private List<String> classNames = new ArrayList<String>();
    //存放Bean
    private Map<String, Object> mapping = new HashMap<String, Object>();

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
        if (!mapping.containsKey(url)) {resp.getWriter().write("404 Not Found!!!"); return;}
        Method method = (Method) mapping.get(url);
        Map<String, String[]> params = req.getParameterMap();
        //投机取巧，参数写死了
        method.invoke(mapping.get(method.getDeclaringClass().getName()), new Object[]{req, resp, params.get("name")[0]});
    }

    //初始化所有相关的类，IOC容器，servletBean
    @Override
    public void init(ServletConfig config) throws ServletException {
        //1、加载配置文件中的目录，然后扫描包下所有文件，创建Bean放入IOC容器
        InputStream is = null;
        try {
            Properties configContext = new Properties();
            is = this.getClass().getClassLoader().getResourceAsStream(config.getInitParameter("contextConfigLocation"));
            configContext.load(is);
            String scanPackage = configContext.getProperty("scanPackage");
            //2、扫描所有目录下的类名，放到map中
            doScanner(scanPackage);
            //3、对不同注解做处理，url和method的映射
            for (String className : classNames) {
                if (!className.contains(".")) {continue;}
                Class<?> clazz = Class.forName(className);
                if (clazz.isAnnotationPresent(Controller.class)) {
                    //如果有Controller注解，则是一个Servlet，继续解析RequestMapping注解出url
                    mapping.put(className, clazz.newInstance());
                    String baseUrl = "";
                    if (clazz.isAnnotationPresent(RequestMapping.class)) {
                        RequestMapping requestMapping = clazz.getAnnotation(RequestMapping.class);
                        baseUrl = requestMapping.value();
                    }
                    Method[] methods = clazz.getMethods();
                    for (Method method : methods) {
                        if (!method.isAnnotationPresent(RequestMapping.class)) {continue;}
                        //url和method映射
                        RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
                        String url = (baseUrl + "/" + requestMapping.value()).replaceAll("/+", "/");
                        mapping.put(url, method);
                        System.out.println("Mapped " + url + " , " + method);
                    }
                } else if (clazz.isAnnotationPresent(Service.class)) {
                    //装入bean
                    Service service = clazz.getAnnotation(Service.class);
                    String beanName = service.value();
                    if ("".equals(beanName)) {beanName = clazz.getName();}
                    Object instance = clazz.newInstance();
                    mapping.put(beanName, instance);
                    //类的接口也是对应这个实例
                    for (Class<?> aClass : clazz.getInterfaces()) {
                        mapping.put(aClass.getName(), instance);
                    }
                } else {
                    continue;
                }
            }
            //DI赋值，对所有Controller注解的类中有Autowired注解的实例进行注入
            for (Object object : mapping.values()) {
                if (object == null) {continue;}
                Class clazz = object.getClass();
                if (clazz.isAnnotationPresent(Controller.class)){
                    Field[] fields = clazz.getDeclaredFields();
                    for (Field field : fields) {
                        if (!field.isAnnotationPresent(Autowired.class)) {continue;}
                        Autowired autowired = field.getAnnotation(Autowired.class);
                        String beanName = autowired.value();
                        if ("".equals(beanName)) {
                            beanName = field.getType().getName();
                        }
                        field.setAccessible(true);
                        field.set(mapping.get(clazz.getName()), mapping.get(beanName));
                    }
                }
            }
            System.out.println(mapping);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
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
        System.out.println("Yuehen mvcframework is inited ..");
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
