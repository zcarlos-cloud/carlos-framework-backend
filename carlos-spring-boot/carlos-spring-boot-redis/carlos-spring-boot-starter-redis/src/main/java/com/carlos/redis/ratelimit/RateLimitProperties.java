package com.carlos.redis.ratelimit;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.concurrent.TimeUnit;

/**
 * 限流配置属性
 * <p>
 * 配置前缀: carlos.redis.rate-limit
 *
 * @author carlos
 * @date 2026-04-08
 */
@Data
@ConfigurationProperties(prefix = "carlos.redis.rate-limit")
public class RateLimitProperties {

    /**
     * 是否启用限流功能
     */
    private boolean enabled = true;

    /**
     * 默认限流键前缀
     */
    public static final String DEFAULT_PREFIX = "framework:ratelimit:";

    /**
     * 限流键前缀
     */
    private String prefix = DEFAULT_PREFIX;

    /**
     * 默认速率（单位时间内的请求数）
     */
    private long defaultRate = 100;

    /**
     * 默认速率间隔
     */
    private long defaultRateInterval = 1;

    /**
     * 默认速率间隔单位
     */
    private TimeUnit defaultRateIntervalUnit = TimeUnit.SECONDS;

    /**
     * 默认令牌桶容量
     */
    private long defaultCapacity = 100;

    /**
     * 默认最大等待时间（毫秒）
     */
    private long defaultMaxWaitTime = 5000;

    /**
     * 是否启用监控指标
     */
    private boolean metricsEnabled = true;

    /**
     * 限流器过期时间（分钟）
     * <p>
     * 限流器在 Redis 中的过期时间，避免长期不使用的限流器占用内存。<br>
     * 设置为 -1 表示永不过期。
     * </p>
     */
    private long rateLimiterExpireMinutes = 60;
}
