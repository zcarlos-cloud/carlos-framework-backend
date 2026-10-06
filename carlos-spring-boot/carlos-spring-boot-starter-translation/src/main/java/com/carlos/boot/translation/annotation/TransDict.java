package com.carlos.boot.translation.annotation;

import java.lang.annotation.*;

/**
 * <p>
 * 翻译字典字段
 * </p>
 *
 * <p>将字典编码或ID翻译为字典名称或完整信息</p>
 *
 * @author carlos
 * @date 2025/01/20
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TransDict {

    /**
     * 字典类型
     */
    String type();

    /**
     * 输入值类型
     */
    InputType inputType() default InputType.CODE;

    /**
     * 输出类型
     */
    OutputType outputType() default OutputType.NAME;

    /**
     * 输出到指定字段
     */
    String target() default "";

    /**
     * 输入类型
     */
    enum InputType {
        /**
         * 字典编码
         */
        CODE,
        /**
         * 字典ID
         */
        ID
    }

    /**
     * 输出类型
     */
    enum OutputType {
        /**
         * 完整对象
         */
        FULL,
        /**
         * 字典名称
         */
        NAME,
        /**
         * 字典编码
         */
        CODE
    }
}
