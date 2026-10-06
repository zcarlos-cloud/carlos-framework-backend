package com.carlos.core.enums;


import java.lang.annotation.*;

/**
 * 自定义应用枚举标记注解（用于反射扫描） 该注解可以被子类继承
 *
 * @author carlos
 * @date 2020/3/19
 **/
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface AppEnum {

    /**
     * 枚举code
     */
    String code();

}
