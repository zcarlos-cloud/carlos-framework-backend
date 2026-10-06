package com.carlos.boot.translation.annotation;

import java.lang.annotation.*;

/**
 * <p>
 * 翻译用户ID字段
 * </p>
 *
 * <p>将用户ID翻译为用户名或完整用户信息</p>
 *
 * @author carlos
 * @date 2025/01/20
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TransUser {

    /**
     * 用户ID来源字段（默认当前字段）
     */
    String source() default "";

    /**
     * 输出类型
     */
    Type type() default Type.NAME;

    /**
     * 输出到指定字段（为空则替换原值）
     */
    String target() default "";

    /**
     * 用户翻译输出类型
     */
    enum Type {
        /**
         * 完整对象
         */
        FULL,
        /**
         * 用户名
         */
        NAME,
        /**
         * 昵称
         */
        NICKNAME,
        /**
         * 真实姓名
         */
        REALNAME,
        /**
         * 手机号
         */
        PHONE,
        /**
         * 邮箱
         */
        EMAIL
    }
}
