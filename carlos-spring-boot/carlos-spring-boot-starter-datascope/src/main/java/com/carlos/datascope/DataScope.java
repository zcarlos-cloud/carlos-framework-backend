package com.carlos.datascope;

import java.lang.annotation.*;


/**
 * <p>
 * 数据权限元注解
 * </p>
 *
 * @author Carlos
 * @date 2022/11/21 14:15
 */
@Repeatable(DataScopes.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
public @interface DataScope {

    /**
     * 权限控制字段
     */
    String field() default "create_by";

    /**
     * mapper方法名称
     */
    String methodPoint() default "";

    /**
     * 数据权限类型
     */
    DataScopeType type() default DataScopeType.ALL;

    /**
     * 调用的类
     */
    Class<?> caller();

    /**
     * 自定义数据权限
     */
    Class<? extends CustomScope> handler() default CustomScope.class;

}