package com.carlos.datascope.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据权限组合注解
 * <p>
 * 用于在同一方法上配置多个数据权限规则
 *
 * <p>使用示例：</p>
 * <pre>
 * &#64;DataScopes({
 *     &#64;DataScope(dimension = ScopeDimension.DEPT_AND_CHILDREN, tables = "sys_user"),
 *     &#64;DataScope(dimension = ScopeDimension.CURRENT_USER, tables = "sys_order"),
 *     &#64;DataScope(dimension = ScopeDimension.ALL, tables = "sys_dict")
 * })
 * public ComplexData getComplexData() { ... }
 * </pre>
 *
 * @author Carlos
 * @version 2.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface DataScopes {

    /**
     * 数据权限规则数组
     */
    DataScope[] value();
}
