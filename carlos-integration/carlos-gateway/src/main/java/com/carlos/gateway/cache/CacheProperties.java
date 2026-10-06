package com.carlos.gateway.cache;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * <p>
 * 缓存配置属性
 * </p>
 *
 * @author carlos
 * @date 2026/3/16
 */
@Data
@ConfigurationProperties(prefix = "carlos.gateway.cache")
public class CacheProperties {

    /**
     * 是否启用缓存
     */
    private boolean enabled = true;

    /**
     * 本地缓存大小
     */
    private long localSize = 10000;

    /**
     * 本地缓存过期时间
     */
    private Duration localExpire = Duration.ofMinutes(5);

    /**
     * 是否启用 Redis 缓存
     */
    private boolean redisEnabled = true;

    /**
     * Redis 缓存过期时间
     */
    private Duration redisExpire = Duration.ofMinutes(10);

    /**
     * 缓存路径前缀
     */
    private String pathPrefix = "/api/cache";
}
