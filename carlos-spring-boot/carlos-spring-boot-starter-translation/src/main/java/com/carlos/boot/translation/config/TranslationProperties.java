package com.carlos.boot.translation.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>
 * 翻译模块配置属性
 * </p>
 *
 * @author carlos
 * @date 2025/01/20
 */
@Data
@ConfigurationProperties(prefix = "carlos.translation")
public class TranslationProperties {

    /**
     * 是否启用翻译
     */
    private boolean enabled = true;

    /**
     * 是否启用缓存
     */
    private boolean cacheEnabled = true;

    /**
     * 缓存配置
     */
    private CacheProperties cache = new CacheProperties();

    /**
     * 缓存配置
     */
    @Data
    public static class CacheProperties {
        /**
         * 本地缓存最大条目数
         */
        private long localSize = 10000;

        /**
         * 本地缓存过期时间（分钟）
         */
        private long localExpireMinutes = 10;

        /**
         * Redis 缓存过期时间（分钟）
         */
        private long redisExpireMinutes = 30;
    }
}
