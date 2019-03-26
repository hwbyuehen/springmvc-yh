package com.yuehen.demo.service.impl;

import com.yuehen.demo.service.IDemoService;
import com.yuehen.mvcframework.annotation.Service;

/**
 * @author 吃土的飞鱼
 * @date 2019/3/26
 */
@Service
public class DemoService implements IDemoService {
    @Override
    public String get(String name) {
        return "My name is " + name;
    }
}
