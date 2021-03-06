package com.yuehen.mvcframework.annotation;

import java.lang.annotation.*;

/**
 * @author 吃土的飞鱼
 * @date 2019/3/26
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Controller {
    String value() default "";
}
