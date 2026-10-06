package com.carlos.boot.translation.core;

import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 翻译上下文 - 用于在翻译过程中传递配置信息
 * </p>
 *
 * @author carlos
 * @date 2025/01/20
 */
@Getter
@Setter
public class TranslationContext {

    /**
     * 是否启用缓存
     */
    private boolean cacheEnabled = true;

    /**
     * 缓存时间（分钟）
     */
    private long cacheMinutes = 30;

    /**
     * ThreadLocal 存储当前线程的上下文
     */
    private static final ThreadLocal<TranslationContext> CONTEXT = new ThreadLocal<>();

    /**
     * 获取当前上下文
     *
     * @return 上下文
     */
    public static TranslationContext get() {
        TranslationContext context = CONTEXT.get();
        if (context == null) {
            context = new TranslationContext();
            CONTEXT.set(context);
        }
        return context;
    }

    /**
     * 设置上下文
     *
     * @param context 上下文
     */
    public static void set(TranslationContext context) {
        CONTEXT.set(context);
    }

    /**
     * 清除上下文
     */
    public static void clear() {
        CONTEXT.remove();
    }

    /**
     * 使用默认配置
     */
    public static void useDefault() {
        clear();
    }

    /**
     * 禁用缓存
     */
    public static void disableCache() {
        TranslationContext context = new TranslationContext();
        context.setCacheEnabled(false);
        CONTEXT.set(context);
    }

    /**
     * 设置缓存配置
     *
     * @param enabled 是否启用
     * @param minutes 缓存时间（分钟）
     */
    public static void setCacheConfig(boolean enabled, long minutes) {
        TranslationContext context = new TranslationContext();
        context.setCacheEnabled(enabled);
        context.setCacheMinutes(minutes);
        CONTEXT.set(context);
    }
}
