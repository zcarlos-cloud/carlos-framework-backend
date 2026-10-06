package com.carlos.boot.translation.annotation;

import java.lang.annotation.*;

/**
 * <p>
 * 翻译区域编码字段
 * </p>
 *
 * <p>将区域编码翻译为区域名称或完整路径</p>
 *
 * @author carlos
 * @date 2025/01/20
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TransRegion {

    /**
     * 区域编码来源字段（默认当前字段）
     */
    String source() default "";

    /**
     * 输出类型
     */
    OutputType type() default OutputType.FULLNAME;

    /**
     * 输出到指定字段
     */
    String target() default "";

    /**
     * 层级分隔符
     */
    String separator() default "/";

    /**
     * 向上层级限制
     */
    int limit() default 3;

    /**
     * 区域翻译输出类型
     */
    enum OutputType {
        /**
         * 完整对象
         */
        FULL,
        /**
         * 区域名称
         */
        NAME,
        /**
         * 完整路径，如：中国/广东省/深圳市
         */
        FULLNAME
    }
}
