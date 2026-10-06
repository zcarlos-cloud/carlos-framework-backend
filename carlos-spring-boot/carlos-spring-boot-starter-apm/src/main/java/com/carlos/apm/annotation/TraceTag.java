package com.carlos.apm.annotation;

import java.lang.annotation.*;

/**
 * 追踪标签注解
 * <p>
 * 用于在方法上添加自定义追踪标签，支持 SpEL 表达式解析参数值
 * <p>
 * 示例：
 * <pre>
 *     {@literal @}TraceTag(key = "user.id", value = "#userId")
 *     public User getUser(Long userId) {
 *         // ...
 *     }
 *
 *     {@literal @}TraceTag(key = "order.status", value = "'CREATED'")
 *     public Order createOrder(OrderParam param) {
 *         // ...
 *     }
 * </pre>
 *
 * @author Carlos
 * @date 2024-12-09
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(TraceTags.class)
public @interface TraceTag {

    /**
     * 标签键
     */
    String key();

    /**
     * 标签值，支持 SpEL 表达式
     * <p>
     * 使用 #paramName 引用方法参数
     * 使用 #result 引用返回值（仅在 afterReturning 时有效）
     * 使用单引号包裹字符串常量，如 "'constant'"
     */
    String value();

    /**
     * 条件表达式，满足条件时才添加标签
     * <p>
     * 支持 SpEL 表达式，返回 boolean 类型
     */
    String condition() default "";
}


