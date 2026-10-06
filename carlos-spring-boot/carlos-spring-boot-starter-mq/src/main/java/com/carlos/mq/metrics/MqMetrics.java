package com.carlos.mq.metrics;

import com.carlos.mq.core.ConsumeResult;
import com.carlos.redis.stream.RedisStreamTemplate;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MQ 语义层指标
 * <p>
 * 前缀 {@code carlos.mq.}（Redis 物理层指标在 {@code carlos.redis.mq.} 下，两者不重叠）。
 * </p>
 * <ul>
 *   <li>{@code carlos.mq.consume.pending} —— 每（流, 消费组）的待处理条目数。
 *       <b>这是本项目硬约束要求的「PENDING 数」指标</b>：它同时反映积压与死消费者遗留，
 *       是拆分触发线的关键输入。放在 MQ 层而非 Redis 层，是因为 {@code XPENDING}
 *       需要消费组名，而原语层只认识 key。</li>
 *   <li>{@code carlos.mq.consume.total} —— 消费计数（按状态）</li>
 *   <li>{@code carlos.mq.consume.error} / {@code dlq} / {@code reclaim} —— 异常与自愈计数</li>
 * </ul>
 * <p>
 * 沿用 Micrometer 惰性 Gauge（采集时取值，<b>不需要 {@code @EnableScheduling}</b>，
 * 本框架未开启且既有定时指标组件因此实际不生效）。指标失败一律降级，不影响主流程。
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@Slf4j
public class MqMetrics {

    /** 指标前缀 */
    public static final String PREFIX = "carlos.mq.";

    private static final String TAG_TOPIC = "topic";
    private static final String TAG_STATUS = "status";
    private static final String TAG_GROUP = "group";
    private static final String TAG_STREAM = "stream";

    private final MeterRegistry registry;
    private final RedisStreamTemplate streamTemplate;

    /** 已注册待处理指标的（流, 组）组合，保证幂等 */
    private final Set<String> registeredPending = ConcurrentHashMap.newKeySet();

    /** 消费计数缓存（避免每次取 Counter） */
    private final Map<String, Counter> consumeCounters = new ConcurrentHashMap<>();

    public MqMetrics(MeterRegistry registry, RedisStreamTemplate streamTemplate) {
        this.registry = registry;
        this.streamTemplate = streamTemplate;
    }

    /**
     * 是否可用
     *
     * @return 有 MeterRegistry 时返回 true
     */
    public boolean isEnabled() {
        return registry != null;
    }

    /**
     * 注册某（流, 消费组）的待处理条目数指标（幂等）
     *
     * @param streamKey 流 key
     * @param group     消费组
     */
    public void registerPending(String streamKey, String group) {
        if (registry == null || streamKey == null || group == null) {
            return;
        }
        String key = streamKey + "@" + group;
        if (!registeredPending.add(key)) {
            return;
        }
        Gauge.builder(PREFIX + "consume.pending", streamTemplate,
                template -> safePending(streamKey, group))
            .description("待处理条目数（XPENDING）—— 反映积压与死消费者遗留")
            .tag(TAG_STREAM, streamKey)
            .tag(TAG_GROUP, group)
            .register(registry);
    }

    /**
     * 记录一次消费结果
     *
     * @param topic  主题
     * @param result 结果
     */
    public void recordConsume(String topic, ConsumeResult result) {
        if (registry == null || result == null || result.getStatus() == null) {
            return;
        }
        String status = result.getStatus().name().toLowerCase();
        consumeCounters.computeIfAbsent(topic + "@" + status, ignored ->
            Counter.builder(PREFIX + "consume.total")
                .description("消息消费计数（按状态）")
                .tag(TAG_TOPIC, topic)
                .tag(TAG_STATUS, status)
                .register(registry))
            .increment();
    }

    /**
     * 记录一次处理链自身异常
     *
     * @param topic 主题
     */
    public void recordConsumeError(String topic) {
        increment("consume.error", topic);
    }

    /**
     * 记录一次转入死信
     *
     * @param topic 主题
     */
    public void recordDlq(String topic) {
        increment("dlq", topic);
    }

    /**
     * 记录一次认领重投
     *
     * @param topic 主题
     */
    public void recordReclaim(String topic) {
        increment("reclaim", topic);
    }

    private void increment(String metric, String topic) {
        if (registry == null) {
            return;
        }
        Counter.builder(PREFIX + metric)
            .description("MQ " + metric)
            .tag(TAG_TOPIC, topic)
            .register(registry)
            .increment();
    }

    private double safePending(String streamKey, String group) {
        try {
            return streamTemplate.pendingSummary(streamKey, group).total();
        } catch (Exception e) {
            log.debug("采集待处理条目指标失败：stream={}, group={}", streamKey, group, e);
            return Double.NaN;
        }
    }
}
