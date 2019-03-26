package com.yuehen.demo.mvc.action;

import com.yuehen.demo.service.IDemoService;
import com.yuehen.mvcframework.annotation.Autowired;
import com.yuehen.mvcframework.annotation.Controller;
import com.yuehen.mvcframework.annotation.RequestMapping;
import com.yuehen.mvcframework.annotation.RequestParm;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author 吃土的飞鱼
 * @date 2019/3/26
 */
@Controller
@RequestMapping("/demo")
public class DemoAction {
    
    @Autowired
    private IDemoService demoService;

    @RequestMapping("/query")
    public void query(HttpServletRequest request, HttpServletResponse response, @RequestParm("name") String name) {
        String result = demoService.get(name);
        try {
            response.getWriter().write(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @RequestMapping("/add")
    public void add(HttpServletRequest req, HttpServletResponse resp,
                    @RequestParm("a") Integer a, @RequestParm("b") Integer b){
        try {
            resp.getWriter().write(a + "+" + b + "=" + (a + b));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
