package com.yuehen.servlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Properties;

/**
 * @author 吃土的飞鱼
 * @date 2019/3/26
 */
public class DemoServlet extends HttpServlet {
    Properties properties = new Properties();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    public void init() throws ServletException {
        String initParam = this.getInitParameter("scanPackage");
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(initParam);
        try {
            properties.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("init." + properties.getProperty("scanPackage"));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        PrintWriter out = resp.getWriter();
        out.println("<html>" + "<body>" + "<h1 align=center>HF</h1>" + "<br>" + "</body>" + "</html>");
    }
}