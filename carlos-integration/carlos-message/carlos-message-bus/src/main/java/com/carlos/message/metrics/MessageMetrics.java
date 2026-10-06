package com.carlos.message.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>
 * 消息中心指标采集
 * </p>
 *
 * <p>
 * 基于 Micrometer 封装，暴露以下指标：
 * - message.send.total (Counter): 发送总量，按渠道和结果标记
 * - message.send.duration (Timer): 发送耗时分布
 * - message.send.active (Gauge): 当前正在发送的消息数
 * - message.queue.size (Gauge): 各优先级队列长度
 * - message.retry.total (Counter): 重试次数
 * </p>
 *
 * @author Carlos
 * @date 2026/5/30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageMetrics {

    private final MeterRegistry meterRegistry;

    private static final String METRIC_SEND_TOTAL = "message.send.total";
    private static final String METRIC_SEND_DURATION = "message.send.duration";
    private static final String METRIC_SEND_ACTIVE = "message.send.active";
    private static final String METRIC_RETRY_TOTAL = "message.retry.total";
    private static final String METRIC_CHANNEL_HEALTH = "message.channel.health";

    private final AtomicInteger activeSends = new AtomicInteger(0);
    private final ConcurrentHashMap<String, AtomicInteger> queueSizeMap = new ConcurrentHashMap<>();

    /**
     * 记录发送开始
     */
    public void sendStart() {
        activeSends.incrementAndGet();
    }

    /**
     * 记录发送完成
     *
     * @param channelCode 渠道编码
     * @param success     是否成功
     * @param durationMs  耗时(ms)
     */
    public void sendEnd(String channelCode, boolean success, long durationMs) {
        activeSends.decrementAndGet();

        String result = success ? "success" : "failure";
        Counter.builder(METRIC_SEND_TOTAL)
            .tag("channel", channelCode != null ? channelCode : "unknown")
            .tag("result", result)
            .description("消息发送总量")
            .register(meterRegistry)
            .increment();

        Timer.builder(METRIC_SEND_DURATION)
            .tag("channel", channelCode != null ? channelCode : "unknown")
            .tag("result", result)
            .description("消息发送耗时")
            .register(meterRegistry)
            .record(Duration.ofMillis(durationMs));
    }

    /**
     * 记录重试
     *
     * @param channelCode 渠道编码
     */
    public void retry(String channelCode) {
        Counter.builder(METRIC_RETRY_TOTAL)
            .tag("channel", channelCode != null ? channelCode : "unknown")
            .description("消息重试次数")
            .register(meterRegistry)
            .increment();
    }

    /**
     * 记录渠道健康状态
     *
     * @param channelCode 渠道编码
     * @param healthy     是否健康
     */
    public void channelHealth(String channelCode, boolean healthy) {
        Gauge.builder(METRIC_CHANNEL_HEALTH, () -> healthy ? 1.0 : 0.0)
            .tag("channel", channelCode)
            .description("渠道健康状态 1=健康 0=故障")
            .register(meterRegistry);
    }

    /**
     * 获取当前活跃发送数
     */
    public int getActiveSends() {
        return activeSends.get();
    }

    /**
     * 更新队列大小指标
     *
     * @param priority 优先级
     * @param size     队列大小
     */
    public void updateQueueSize(int priority, long size) {
        String key = "priority-" + priority;
        queueSizeMap.computeIfAbsent(key, k -> new AtomicInteger(0)).set((int) size);

        Gauge.builder("message.queue.size", () -> queueSizeMap.getOrDefault(key, new AtomicInteger(0)).get())
            .tag("priority", key)
            .description("消息队列长度")
            .register(meterRegistry);
    }
}
