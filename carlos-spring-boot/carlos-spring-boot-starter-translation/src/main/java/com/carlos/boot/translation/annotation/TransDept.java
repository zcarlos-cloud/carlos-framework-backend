package com.carlos.boot.translation.annotation;

import java.lang.annotation.*;

/**
 * <p>
 * 翻译部门ID字段
 * </p>
 *
 * <p>将部门ID翻译为部门名称或完整路径</p>
 *
 * @author carlos
 * @date 2025/01/20
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TransDept {

    /**
     * 部门ID来源字段（默认当前字段）
     */
    String source() default "";

    /**
     * 输出类型
     */
    OutputType type() default OutputType.NAME;

    /**
     * 输出到指定字段
     */
    String target() default "";

    /**
     * 层级分隔符（用于 FULLNAME）
     */
    String separator() default "/";

    /**
     * 向上层级限制
     */
    int limit() default Integer.MAX_VALUE;

    /**
     * 部门翻译输出类型
     */
    enum OutputType {
        /**
         * 完整对象
         */
        FULL,
        /**
         * 部门名称
         */
        NAME,
        /**
         * 完整路径，如：总公司/技术部/研发组
         */
        FULLNAME
    }
}
