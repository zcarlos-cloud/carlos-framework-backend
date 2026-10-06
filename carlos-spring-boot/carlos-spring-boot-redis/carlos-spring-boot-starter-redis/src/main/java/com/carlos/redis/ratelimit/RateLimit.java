package com.carlos.redis.ratelimit;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 分布式限流注解
 * <p>
 * 基于 Redisson RRateLimiter 实现令牌桶算法，支持多种限流维度和策略
 * <p>
 * 使用示例：
 * <pre>{@code
 * // 全局限流：每秒最多100个请求
 * @RateLimit(rate = 100, rateInterval = 1, rateIntervalUnit = TimeUnit.SECONDS)
 * public String globalLimit() { ... }
 *
 * // 用户级限流：每个用户每分钟最多10次
 * @RateLimit(type = RateLimitType.USER, rate = 10, rateInterval = 1, rateIntervalUnit = TimeUnit.MINUTES)
 * public String userLimit() { ... }
 *
 * // IP限流：每个IP每小时最多1000次，使用自定义key
 * @RateLimit(type = RateLimitType.IP, key = "api:order:", rate = 1000, rateInterval = 1, rateIntervalUnit = TimeUnit.HOURS)
 * public String ipLimit() { ... }
 *
 * // 自定义限流：基于方法参数
 * @RateLimit(type = RateLimitType.CUSTOM, key = "#userId", rate = 5, rateInterval = 10)
 * public String customLimit(String userId) { ... }
 *
 * // 阻塞等待策略：等待令牌可用
 * @RateLimit(rate = 10, strategy = RateLimitStrategy.BLOCK, maxWaitTime = 5000)
 * public String blockStrategy() { ... }
 * }</pre>
 *
 * @author carlos
 * @date 2026-04-08
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 限流类型
     * <p>
     * - GLOBAL: 全局限流，所有请求共享一个限流器<br>
     * - USER: 用户级限流，每个用户有独立的限流器（需要从请求上下文获取用户ID）<br>
     * - IP: IP限流，每个IP有独立的限流器<br>
     * - CUSTOM: 自定义限流，通过key表达式定义限流维度
     * </p>
     */
    RateLimitType type() default RateLimitType.GLOBAL;

    /**
     * 限流键前缀
     * <p>用于区分不同业务的限流器，默认为方法全限定名</p>
     */
    String key() default "";

    /**
     * 令牌桶速率（单位时间内的请求数）
     * <p>表示每秒/分钟/小时等时间单位内允许通过的请求数量</p>
     *
     * @return 速率值，必须大于0
     */
    long rate();

    /**
     * 速率间隔时间
     * <p>与 rateIntervalUnit 配合使用，定义速率的时间窗口</p>
     *
     * @return 间隔时间值，默认为1
     */
    long rateInterval() default 1;

    /**
     * 速率间隔时间单位
     *
     * @return 时间单位，默认为秒
     */
    TimeUnit rateIntervalUnit() default TimeUnit.SECONDS;

    /**
     * 令牌桶容量（突发流量限制）
     * <p>
     * 表示桶的最大容量，用于应对突发流量。<br>
     * 默认值为0，表示与 rate 相同（无突发能力）。<br>
     * 例如：rate=10, capacity=20 表示每秒产生10个令牌，但最多可累积20个令牌应对突发
     * </p>
     */
    long capacity() default 0;

    /**
     * 限流策略
     * <p>
     * - FAIL_FAST: 快速失败，立即抛出 RateLimitException<br>
     * - BLOCK: 阻塞等待，直到获取到令牌或超过 maxWaitTime<br>
     * - FALLBACK: 执行降级逻辑（需配合 fallbackMethod 使用）
     * </p>
     */
    RateLimitStrategy strategy() default RateLimitStrategy.FAIL_FAST;

    /**
     * 最大等待时间（毫秒）
     * <p>仅在 strategy = BLOCK 时生效，默认5000毫秒</p>
     */
    long maxWaitTime() default 5000;

    /**
     * 降级方法名
     * <p>
     * 仅在 strategy = FALLBACK 时生效。<br>
     * 方法签名必须与原方法相同，返回值类型兼容。<br>
     * 支持在同一类中定义或使用 SpEL 表达式指定 bean 方法。
     * </p>
     */
    String fallbackMethod() default "";

    /**
     * 自定义限流键 SpEL 表达式
     * <p>
     * 仅在 type = CUSTOM 时生效。<br>
     * 支持以下表达式：
     * <ul>
     *   <li>{@code #userId} - 使用参数名为 userId 的值</li>
     *   <li>{@code #p0} - 使用第一个参数</li>
     *   <li>{@code #user.id} - 使用 user 参数的 id 属性</li>
     *   <li>{@code #ip} - 使用请求IP（框架自动注入）</li>
     *   <li>{@code #userId + ':' + #p1} - 组合多个参数</li>
     * </ul>
     */
    String customKeyExpression() default "";

    /**
     * 是否预热
     * <p>
     * 如果设置为 true，令牌桶会以较慢的速率预热到稳定状态。<br>
     * 适用于冷启动场景，避免突发流量对系统造成冲击。
     * </p>
     */
    boolean warmup() default false;

    /**
     * 预热时间（秒）
     * <p>仅在 warmup = true 时生效</p>
     */
    long warmupPeriod() default 60;
}
