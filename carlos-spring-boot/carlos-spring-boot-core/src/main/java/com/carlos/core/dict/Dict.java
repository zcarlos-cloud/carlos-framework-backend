package com.carlos.core.dict;


import java.lang.annotation.*;

/**
 * <p>
 * 标记枚举属于字典枚举
 * </p>
 *
 * @author carlos
 * @date 2021/12/9 14:40
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Dict {

    /**
     * 字典code
     */
    String code();

    /**
     * 字典名称
     */
    String name();

}
