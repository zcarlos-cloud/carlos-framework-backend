package com.carlos.boot.translation.annotation;

import java.lang.annotation.*;

/**
 * <p>
 * 标记响应需要翻译处理
 * </p>
 *
 * <p>标记在方法或类上，启用字段翻译功能</p>
 *
 * @author carlos
 * @date 2025/01/20
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Translated {

    /**
     * 是否启用缓存
     */
    boolean cacheEnabled() default true;

    /**
     * 缓存时间（分钟）
     */
    long cacheMinutes() default 30;
}
