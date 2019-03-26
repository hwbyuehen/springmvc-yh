package com.yuehen.mvcframework.v0.servlet;

import com.yuehen.demo.mvc.action.DemoAction;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;

/**
 * @author 吃土的飞鱼
 * @date 2019/3/26
 */
public class DispatcherServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //投机取巧，直接写死哪个方法
        //后续通过注解来实现url和method的映射，参数的设置
        try {
            Class clazz = DemoAction.class;
            Object obj = clazz.newInstance();
            Method method = clazz.getMethod("query", HttpServletRequest.class, HttpServletResponse.class);
            method.invoke(obj, req, resp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
