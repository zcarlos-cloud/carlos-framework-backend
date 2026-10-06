package com.carlos.redis.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 缓存监控指标
 * <p>
 * 提供缓存命中率、操作耗时等指标收集
 * </p>
 *
 * @author carlos
 * @date 2026-03-14
 */
@Slf4j
public class CacheMetrics {

    private static final String METRIC_PREFIX = "carlos.cache";

    private MeterRegistry meterRegistry;

    // 命中/未命中计数器
    private Counter hitCounter;
    private Counter missCounter;

    // 操作计时器
    private Timer getTimer;
    private Timer setTimer;
    private Timer deleteTimer;

    // 错误计数器
    private Counter errorCounter;

    // 本地统计（当 Micrometer 不可用时降级使用）
    private final AtomicLong localHits = new AtomicLong(0);
    private final AtomicLong localMisses = new AtomicLong(0);
    private final AtomicLong localErrors = new AtomicLong(0);

    public CacheMetrics() {
    }

    public CacheMetrics(MeterRegistry meterRegistry) {
        init(meterRegistry);
    }

    /**
     * 初始化 CacheMetrics
     */
    public void init(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        if (meterRegistry != null) {
            hitCounter = Counter.builder(METRIC_PREFIX + ".hits")
                .description("Cache hit count")
                .register(meterRegistry);

            missCounter = Counter.builder(METRIC_PREFIX + ".misses")
                .description("Cache miss count")
                .register(meterRegistry);

            errorCounter = Counter.builder(METRIC_PREFIX + ".errors")
                .description("Cache error count")
                .register(meterRegistry);

            getTimer = Timer.builder(METRIC_PREFIX + ".get")
                .description("Cache get operation time")
                .register(meterRegistry);

            setTimer = Timer.builder(METRIC_PREFIX + ".set")
                .description("Cache set operation time")
                .register(meterRegistry);

            deleteTimer = Timer.builder(METRIC_PREFIX + ".delete")
                .description("Cache delete operation time")
                .register(meterRegistry);

            log.info("CacheMetrics initialized with Micrometer");
        } else {
            log.warn("MeterRegistry not available, using local statistics only");
        }
    }

    /**
     * 记录缓存命中
     */
    public void recordHit() {
        if (hitCounter != null) {
            hitCounter.increment();
        } else {
            localHits.incrementAndGet();
        }
    }

    /**
     * 记录缓存未命中
     */
    public void recordMiss() {
        if (missCounter != null) {
            missCounter.increment();
        } else {
            localMisses.incrementAndGet();
        }
    }

    /**
     * 记录缓存错误
     */
    public void recordError() {
        if (errorCounter != null) {
            errorCounter.increment();
        } else {
            localErrors.incrementAndGet();
        }
    }

    /**
     * 记录 GET 操作耗时
     *
     * @param nanos 耗时（纳秒）
     */
    public void recordGetTime(long nanos) {
        if (getTimer != null) {
            getTimer.record(nanos, TimeUnit.NANOSECONDS);
        }
    }

    /**
     * 记录 SET 操作耗时
     *
     * @param nanos 耗时（纳秒）
     */
    public void recordSetTime(long nanos) {
        if (setTimer != null) {
            setTimer.record(nanos, TimeUnit.NANOSECONDS);
        }
    }

    /**
     * 记录 DELETE 操作耗时
     *
     * @param nanos 耗时（纳秒）
     */
    public void recordDeleteTime(long nanos) {
        if (deleteTimer != null) {
            deleteTimer.record(nanos, TimeUnit.NANOSECONDS);
        }
    }

    /**
     * 获取命中率
     *
     * @return 命中率（0-1）
     */
    public double getHitRate() {
        long hits = getTotalHits();
        long misses = getTotalMisses();
        long total = hits + misses;
        return total == 0 ? 0 : (double) hits / total;
    }

    /**
     * 获取总命中次数
     */
    public long getTotalHits() {
        if (hitCounter != null) {
            return (long) hitCounter.count();
        }
        return localHits.get();
    }

    /**
     * 获取总未命中次数
     */
    public long getTotalMisses() {
        if (missCounter != null) {
            return (long) missCounter.count();
        }
        return localMisses.get();
    }

    /**
     * 获取总错误次数
     */
    public long getTotalErrors() {
        if (errorCounter != null) {
            return (long) errorCounter.count();
        }
        return localErrors.get();
    }

    /**
     * 获取统计信息字符串
     */
    public String getStats() {
        return "CacheStats{hits=%d, misses=%d, errors=%d, hitRate=%.2f%%}".formatted(
            getTotalHits(), getTotalMisses(), getTotalErrors(), getHitRate() * 100);
    }
}
