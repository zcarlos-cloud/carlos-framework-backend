package com.carlos.redis.stream;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Redis Stream 原语层指标
 * <p>
 * 指标前缀 {@code carlos.redis.mq.}（对齐本模块的 {@code carlos.cache.} /
 * {@code carlos.caffeine.*} 命名族）。本层只暴露它<b>确实知道</b>的物理量：
 * </p>
 * <ul>
 *   <li>{@code carlos.redis.mq.stream.length} —— 每流长度（{@code XLEN}，O(1)）</li>
 *   <li>{@code carlos.redis.mq.memory.used} —— 实例已用内存（{@code INFO memory}）</li>
 * </ul>
 * <p>
 * <b>不在此层暴露「待处理条数」</b>：{@code XPENDING} 需要消费组名，而原语层只认识
 * {@code key}、不认识消费组语义。积压指标由 MQ 层按消费组注册（{@code carlos.mq.*}）。
 * 宁可少一个指标，也不注册一个永远无值的 gauge。
 * </p>
 * <p>
 * <b>刻意不用 {@code @Scheduled}</b>：整个 {@code carlos-spring-boot} 未开启
 * {@code @EnableScheduling}（既有 {@code CacheMetricsCollector} 因此实际不生效）。
 * 改用 Micrometer 惰性 Gauge——取值函数在采集时调用，无需任何调度器。
 * </p>
 * <p>
 * 指标失败必须降级：取值异常时返回 {@code NaN}，绝不因指标问题影响采集或主流程。
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@Slf4j
public class RedisStreamMetrics {

    /** 指标前缀 */
    public static final String PREFIX = "carlos.redis.mq.";

    /** MeterRegistry；为 null 时本类完全降级为空操作 */
    private final MeterRegistry registry;

    private final RedisStreamTemplate streamTemplate;

    /** 已注册的流 key，保证幂等 */
    private final Set<String> registeredKeys = ConcurrentHashMap.newKeySet();

    /** 内存指标是否已注册（幂等） */
    private volatile boolean memoryRegistered;

    public RedisStreamMetrics(MeterRegistry registry, RedisStreamTemplate streamTemplate) {
        this.registry = registry;
        this.streamTemplate = streamTemplate;
    }

    /**
     * 是否可用（无 MeterRegistry 时为 false）
     *
     * @return 可用返回 true
     */
    public boolean isEnabled() {
        return registry != null;
    }

    /**
     * 为指定流注册长度指标（幂等）
     *
     * @param key 流 key
     */
    public void registerStream(String key) {
        if (registry == null || key == null || !registeredKeys.add(key)) {
            return;
        }
        Gauge.builder(PREFIX + "stream.length", streamTemplate, template -> safeLength(key))
            .description("Redis Stream 当前长度（XLEN）")
            .tag("stream", key)
            .register(registry);
    }

    /**
     * 注册实例已用内存指标（幂等）
     */
    public void registerMemory() {
        if (registry == null || memoryRegistered) {
            return;
        }
        memoryRegistered = true;
        Gauge.builder(PREFIX + "memory.used", streamTemplate, RedisStreamTemplate::usedMemoryBytes)
            .description("Redis 实例已用内存字节数（支撑共用实例的容量与拆分决策）")
            .baseUnit("bytes")
            .register(registry);
    }

    private double safeLength(String key) {
        try {
            return streamTemplate.length(key);
        } catch (Exception e) {
            log.debug("采集 Stream 长度指标失败：key={}", key, e);
            return Double.NaN;
        }
    }
}
