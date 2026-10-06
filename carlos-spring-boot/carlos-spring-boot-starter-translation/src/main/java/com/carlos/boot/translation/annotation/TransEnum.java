package com.carlos.boot.translation.annotation;

import com.carlos.core.enums.BaseEnum;

import java.lang.annotation.*;

/**
 * <p>
 * 翻译枚举字段
 * </p>
 *
 * <p>将枚举编码翻译为描述或完整信息</p>
 *
 * @author carlos
 * @date 2025/01/20
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TransEnum {

    /**
     * 枚举类
     */
    Class<? extends BaseEnum<?>> enumClass();

    /**
     * 输出类型
     */
    OutputType type() default OutputType.DESC;

    /**
     * 输出到指定字段
     */
    String target() default "";

    /**
     * 枚举翻译输出类型
     */
    enum OutputType {
        /**
         * 完整对象 {code, desc}
         */
        FULL,
        /**
         * 编码
         */
        CODE,
        /**
         * 描述
         */
        DESC
    }
}
