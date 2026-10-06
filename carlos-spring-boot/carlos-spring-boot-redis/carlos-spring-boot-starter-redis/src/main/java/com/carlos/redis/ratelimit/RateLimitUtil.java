package com.carlos.redis.ratelimit;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * 分布式限流工具类
 * <p>
 * 基于 Redisson RRateLimiter 实现令牌桶算法的分布式限流。
 * 支持编程式限流，提供更灵活的控制能力。
 * <p>
 * 使用示例：
 * <pre>{@code
 * // 1. 简单限流检查
 * boolean allowed = RateLimitUtil.tryAcquire("order:submit", 10, 1, TimeUnit.SECONDS);
 *
 * // 2. 带突发容量的限流
 * boolean allowed = RateLimitUtil.tryAcquire("api:call", 100, 200, 1, TimeUnit.MINUTES);
 *
 * // 3. 阻塞等待获取许可
 * boolean acquired = RateLimitUtil.tryAcquire("important:task", 5, 10, TimeUnit.SECONDS, 3000);
 *
 * // 4. 在限流保护下执行操作
 * String result = RateLimitUtil.executeWithRateLimit("report:generate", 1, 1, TimeUnit.MINUTES,
 *     () -> reportService.generate());
 *
 * // 5. 带降级策略的执行
 * String result = RateLimitUtil.executeWithFallback("api:external", 10, 1, TimeUnit.SECONDS,
 *     () -> externalApi.call(),
 *     () -> "降级数据");
 * }</pre>
 *
 * @author carlos
 * @date 2026-04-08
 */
@Slf4j
public class RateLimitUtil {

    private static RedissonClient staticRedissonClient;
    private static RateLimitProperties staticProperties;
    private static final ReentrantLock RATE_LIMITER_LOCK = new ReentrantLock();

    /**
     * 初始化 RateLimitUtil 静态字段
     */
    public static void init(RedissonClient redissonClient, RateLimitProperties properties) {
        staticRedissonClient = redissonClient;
        staticProperties = properties;
        log.info("[RateLimitUtil] Initialized with prefix: {}", properties.getPrefix());
    }

    // ========================= 基础限流操作 =========================

    /**
     * 尝试获取许可（非阻塞）
     *
     * @param key       限流键
     * @param rate      速率（单位时间内的许可数）
     * @param rateInterval 速率间隔
     * @param timeUnit  时间单位
     * @return true-获取成功, false-触发限流
     */
    public static boolean tryAcquire(String key, long rate, long rateInterval, TimeUnit timeUnit) {
        return tryAcquire(key, rate, rate, rateInterval, timeUnit);
    }

    /**
     * 尝试获取许可（带容量，非阻塞）
     *
     * @param key          限流键
     * @param rate         速率（单位时间内的许可数）
     * @param capacity     令牌桶容量（突发流量）
     * @param rateInterval 速率间隔
     * @param timeUnit     时间单位
     * @return true-获取成功, false-触发限流
     */
    public static boolean tryAcquire(String key, long rate, long capacity, long rateInterval, TimeUnit timeUnit) {
        checkRedissonClient();
        try {
            RRateLimiter rateLimiter = getOrCreateRateLimiter(key, rate, capacity, rateInterval, timeUnit);
            boolean acquired = rateLimiter.tryAcquire();
            log.debug("[RateLimit] Key: {}, Acquired: {}", key, acquired);
            return acquired;
        } catch (Exception e) {
            log.error("[RateLimit] Error acquiring permit for key: {}", key, e);
            // 异常时默认允许通过，避免误杀
            return true;
        }
    }

    /**
     * 尝试获取许可（带等待时间）
     *
     * @param key          限流键
     * @param rate         速率
     * @param rateInterval 速率间隔
     * @param timeUnit     时间单位
     * @param waitTime     最大等待时间（毫秒）
     * @return true-获取成功, false-等待超时
     */
    public static boolean tryAcquire(String key, long rate, long rateInterval, TimeUnit timeUnit, long waitTime) {
        return tryAcquire(key, rate, rate, rateInterval, timeUnit, waitTime);
    }

    /**
     * 尝试获取许可（带容量和等待时间）
     *
     * @param key          限流键
     * @param rate         速率
     * @param capacity     令牌桶容量
     * @param rateInterval 速率间隔
     * @param timeUnit     时间单位
     * @param waitTime     最大等待时间（毫秒）
     * @return true-获取成功, false-等待超时
     */
    public static boolean tryAcquire(String key, long rate, long capacity, long rateInterval,
                                     TimeUnit timeUnit, long waitTime) {
        checkRedissonClient();
        try {
            RRateLimiter rateLimiter = getOrCreateRateLimiter(key, rate, capacity, rateInterval, timeUnit);
            boolean acquired = rateLimiter.tryAcquire(1, waitTime, TimeUnit.MILLISECONDS);
            log.debug("[RateLimit] Key: {}, WaitTime: {}ms, Acquired: {}", key, waitTime, acquired);
            return acquired;
        } catch (Exception e) {
            log.error("[RateLimit] Error acquiring permit for key: {}", key, e);
            return true;
        }
    }

    /**
     * 获取指定数量的许可（非阻塞）
     *
     * @param key          限流键
     * @param permits      需要的许可数量
     * @param rate         速率
     * @param rateInterval 速率间隔
     * @param timeUnit     时间单位
     * @return true-获取成功, false-触发限流
     */
    public static boolean tryAcquire(String key, int permits, long rate, long rateInterval, TimeUnit timeUnit) {
        checkRedissonClient();
        try {
            RRateLimiter rateLimiter = getOrCreateRateLimiter(key, rate, rate, rateInterval, timeUnit);
            boolean acquired = rateLimiter.tryAcquire(permits);
            log.debug("[RateLimit] Key: {}, Permits: {}, Acquired: {}", key, permits, acquired);
            return acquired;
        } catch (Exception e) {
            log.error("[RateLimit] Error acquiring {} permits for key: {}", permits, key, e);
            return true;
        }
    }

    // ========================= 模板方法 =========================

    /**
     * 在限流保护下执行操作（快速失败）
     *
     * @param key          限流键
     * @param rate         速率
     * @param rateInterval 速率间隔
     * @param timeUnit     时间单位
     * @param supplier     业务逻辑
     * @param <T>          返回类型
     * @return 业务逻辑返回值
     * @throws RateLimitException 触发限流时抛出
     */
    public static <T> T executeWithRateLimit(String key, long rate, long rateInterval,
                                             TimeUnit timeUnit, Supplier<T> supplier) {
        return executeWithRateLimit(key, rate, rate, rateInterval, timeUnit, supplier);
    }

    /**
     * 在限流保护下执行操作（带容量）
     *
     * @param key          限流键
     * @param rate         速率
     * @param capacity     令牌桶容量
     * @param rateInterval 速率间隔
     * @param timeUnit     时间单位
     * @param supplier     业务逻辑
     * @param <T>          返回类型
     * @return 业务逻辑返回值
     * @throws RateLimitException 触发限流时抛出
     */
    public static <T> T executeWithRateLimit(String key, long rate, long capacity, long rateInterval,
                                             TimeUnit timeUnit, Supplier<T> supplier) {
        if (!tryAcquire(key, rate, capacity, rateInterval, timeUnit)) {
            throw new RateLimitException(key);
        }
        return supplier.get();
    }

    /**
     * 在限流保护下执行操作（带降级）
     *
     * @param key            限流键
     * @param rate           速率
     * @param rateInterval   速率间隔
     * @param timeUnit       时间单位
     * @param supplier       主业务逻辑
     * @param fallback       降级逻辑
     * @param <T>            返回类型
     * @return 业务逻辑返回值或降级值
     */
    public static <T> T executeWithFallback(String key, long rate, long rateInterval,
                                            TimeUnit timeUnit, Supplier<T> supplier, Supplier<T> fallback) {
        return executeWithFallback(key, rate, rate, rateInterval, timeUnit, supplier, fallback);
    }

    /**
     * 在限流保护下执行操作（带容量和降级）
     *
     * @param key            限流键
     * @param rate           速率
     * @param capacity       令牌桶容量
     * @param rateInterval   速率间隔
     * @param timeUnit       时间单位
     * @param supplier       主业务逻辑
     * @param fallback       降级逻辑
     * @param <T>            返回类型
     * @return 业务逻辑返回值或降级值
     */
    public static <T> T executeWithFallback(String key, long rate, long capacity, long rateInterval,
                                            TimeUnit timeUnit, Supplier<T> supplier, Supplier<T> fallback) {
        if (!tryAcquire(key, rate, capacity, rateInterval, timeUnit)) {
            log.warn("[RateLimit] Rate limit triggered for key: {}, executing fallback", key);
            return fallback.get();
        }
        return supplier.get();
    }

    /**
     * 在限流保护下执行操作（阻塞等待）
     *
     * @param key          限流键
     * @param rate         速率
     * @param rateInterval 速率间隔
     * @param timeUnit     时间单位
     * @param waitTime     最大等待时间（毫秒）
     * @param supplier     业务逻辑
     * @param <T>          返回类型
     * @return 业务逻辑返回值
     * @throws RateLimitException 等待超时抛出
     */
    public static <T> T executeWithBlock(String key, long rate, long rateInterval,
                                         TimeUnit timeUnit, long waitTime, Supplier<T> supplier) {
        return executeWithBlock(key, rate, rate, rateInterval, timeUnit, waitTime, supplier);
    }

    /**
     * 在限流保护下执行操作（带容量，阻塞等待）
     *
     * @param key          限流键
     * @param rate         速率
     * @param capacity     令牌桶容量
     * @param rateInterval 速率间隔
     * @param timeUnit     时间单位
     * @param waitTime     最大等待时间（毫秒）
     * @param supplier     业务逻辑
     * @param <T>          返回类型
     * @return 业务逻辑返回值
     * @throws RateLimitException 等待超时抛出
     */
    public static <T> T executeWithBlock(String key, long rate, long capacity, long rateInterval,
                                         TimeUnit timeUnit, long waitTime, Supplier<T> supplier) {
        if (!tryAcquire(key, rate, capacity, rateInterval, timeUnit, waitTime)) {
            throw new RateLimitException(key, waitTime);
        }
        return supplier.get();
    }

    // ========================= 限流器管理 =========================

    /**
     * 删除限流器
     *
     * @param key 限流键
     * @return true-删除成功
     */
    public static boolean deleteRateLimiter(String key) {
        checkRedissonClient();
        try {
            String fullKey = buildKey(key);
            RRateLimiter rateLimiter = staticRedissonClient.getRateLimiter(fullKey);
            boolean deleted = rateLimiter.delete();
            log.info("[RateLimit] Deleted rate limiter: {}, result: {}", key, deleted);
            return deleted;
        } catch (Exception e) {
            log.error("[RateLimit] Error deleting rate limiter: {}", key, e);
            return false;
        }
    }

    /**
     * 检查限流器是否存在
     *
     * @param key 限流键
     * @return true-存在
     */
    public static boolean isExists(String key) {
        checkRedissonClient();
        try {
            String fullKey = buildKey(key);
            RRateLimiter rateLimiter = staticRedissonClient.getRateLimiter(fullKey);
            return rateLimiter.isExists();
        } catch (Exception e) {
            log.error("[RateLimit] Error checking existence: {}", key, e);
            return false;
        }
    }

    /**
     * 获取限流器可用许可数
     *
     * @param key 限流键
     * @return 可用许可数，-1表示限流器不存在
     */
    public static long availablePermits(String key) {
        checkRedissonClient();
        try {
            String fullKey = buildKey(key);
            RRateLimiter rateLimiter = staticRedissonClient.getRateLimiter(fullKey);
            if (!rateLimiter.isExists()) {
                return -1;
            }
            return rateLimiter.availablePermits();
        } catch (Exception e) {
            log.error("[RateLimit] Error getting available permits: {}", key, e);
            return -1;
        }
    }

    // ========================= 私有方法 =========================

    /**
     * 获取或创建限流器
     */
    private static RRateLimiter getOrCreateRateLimiter(String key, long rate, long capacity,
                                                       long rateInterval, TimeUnit timeUnit) {
        String fullKey = buildKey(key);
        RRateLimiter rateLimiter = staticRedissonClient.getRateLimiter(fullKey);

        // 如果限流器不存在，则初始化
        if (!rateLimiter.isExists()) {
            RATE_LIMITER_LOCK.lock();
            try {
                if (!rateLimiter.isExists()) {
                    long actualCapacity = capacity > 0 ? capacity : rate;
                    RateIntervalUnit intervalUnit = convertToRateIntervalUnit(timeUnit);
                    rateLimiter.trySetRate(RateType.OVERALL, rate, rateInterval, intervalUnit);
                    log.info("[RateLimit] Created rate limiter: {}, rate: {}/{}, capacity: {}",
                        key, rate, timeUnit, actualCapacity);
                }
            } finally {
                RATE_LIMITER_LOCK.unlock();
            }
        }

        return rateLimiter;
    }

    /**
     * 构建完整的限流键
     */
    private static String buildKey(String key) {
        String prefix = staticProperties != null ? staticProperties.getPrefix() : RateLimitProperties.DEFAULT_PREFIX;
        return prefix + key;
    }

    /**
     * 转换时间单位
     */
    private static RateIntervalUnit convertToRateIntervalUnit(TimeUnit timeUnit) {
        return switch (timeUnit) {
            case NANOSECONDS, MICROSECONDS, MILLISECONDS -> RateIntervalUnit.MILLISECONDS;
            case SECONDS -> RateIntervalUnit.SECONDS;
            case MINUTES -> RateIntervalUnit.MINUTES;
            case HOURS -> RateIntervalUnit.HOURS;
            case DAYS -> RateIntervalUnit.HOURS; // Redisson 不支持 DAYS，转为小时
        };
    }

    private static void checkRedissonClient() {
        if (staticRedissonClient == null) {
            throw new IllegalStateException("RedissonClient is not initialized, RateLimitUtil not ready");
        }
    }
}
