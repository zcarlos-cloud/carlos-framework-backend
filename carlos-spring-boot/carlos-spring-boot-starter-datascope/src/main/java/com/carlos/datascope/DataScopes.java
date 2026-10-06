package com.carlos.datascope;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * <p>
 * 注解权限配置
 * </p>
 *
 * @author Carlos
 * @date 2022/11/21 13:56
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
public @interface DataScopes {

    /**
     * 配置多层级的数据权限
     */
    DataScope[] value();
}