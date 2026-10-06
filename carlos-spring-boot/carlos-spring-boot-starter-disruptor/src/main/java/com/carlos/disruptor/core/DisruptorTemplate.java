package com.carlos.disruptor.core;

import com.carlos.disruptor.metrics.DisruptorMetrics;
import com.lmax.disruptor.RingBuffer;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * Disruptor 事件发布模板
 * <p>
 * 提供便捷的事件发布 API，是业务代码与 Disruptor 交互的主要入口
 * </p>
 *
 * @author Carlos
 * @date 2026-03-14
 */
@Slf4j
public class DisruptorTemplate<T> {

    /**
     * RingBuffer
     */
    private final RingBuffer<DisruptorEvent<T>> ringBuffer;

    /**
     * 指标收集器
     */
    private final DisruptorMetrics metrics;

    /**
     * 模板名称
     */
    private final String name;

    public DisruptorTemplate(String name, RingBuffer<DisruptorEvent<T>> ringBuffer) {
        this(name, ringBuffer, null);
    }

    public DisruptorTemplate(String name, RingBuffer<DisruptorEvent<T>> ringBuffer, DisruptorMetrics metrics) {
        this.name = name;
        this.ringBuffer = ringBuffer;
        this.metrics = metrics;
    }

    /**
     * 发布事件（同步方式，如果 RingBuffer 满则等待）
     *
     * @param data 业务数据
     */
    public void publishEvent(T data) {
        long startTime = System.currentTimeMillis();
        try {
            if (data instanceof DisruptorEvent<?> rawEvent) {
                @SuppressWarnings("unchecked")
                DisruptorEvent<T> srcEvent = (DisruptorEvent<T>) rawEvent;
                ringBuffer.publishEvent((event, sequence) -> {
                    event.setEventId(srcEvent.getEventId());
                    event.setEventType(srcEvent.getEventType());
                    event.setEventTime(srcEvent.getEventTime());
                    event.setData(srcEvent.getData());
                    event.setAttributes(srcEvent.getAttributes());
                    event.setCreateTimestamp(srcEvent.getCreateTimestamp());
                });
            } else {
                ringBuffer.publishEvent((event, sequence) -> event.setData(data));
            }
            recordMetrics(startTime, true);
        } catch (Exception e) {
            recordMetrics(startTime, false);
            log.error("[{}] Publish event failed", name, e);
            throw new com.carlos.disruptor.exception.DisruptorException("Publish event failed", e);
        }
    }

    /**
     * 发布带类型的事件
     *
     * @param eventType 事件类型
     * @param data      业务数据
     */
    public void publishEvent(String eventType, T data) {
        long startTime = System.currentTimeMillis();
        try {
            DisruptorEvent<T> event = new DisruptorEvent<>(eventType, data);
            ringBuffer.publishEvent((e, sequence) -> {
                e.setEventId(event.getEventId());
                e.setEventType(event.getEventType());
                e.setEventTime(event.getEventTime());
                e.setData(event.getData());
                e.setAttributes(event.getAttributes());
                e.setCreateTimestamp(event.getCreateTimestamp());
            });
            recordMetrics(startTime, true);
        } catch (Exception e) {
            recordMetrics(startTime, false);
            log.error("[{}] Publish event failed", name, e);
            throw new com.carlos.disruptor.exception.DisruptorException("Publish event failed", e);
        }
    }

    /**
     * 尝试发布事件（如果 RingBuffer 满则返回 false）
     *
     * @param data 业务数据
     * @return true-发布成功，false-RingBuffer 满
     */
    public boolean tryPublishEvent(T data) {
        long startTime = System.currentTimeMillis();
        try {
            boolean success = ringBuffer.tryPublishEvent((event, sequence) -> event.setData(data));
            recordMetrics(startTime, success);
            return success;
        } catch (Exception e) {
            recordMetrics(startTime, false);
            log.error("[{}] Try publish event failed", name, e);
            return false;
        }
    }

    /**
     * 带超时时间发布事件（基于忙等待实现）
     *
     * @param data    业务数据
     * @param timeout 超时时间
     * @param unit    时间单位
     * @return true-发布成功，false-超时
     */
    public boolean tryPublishEvent(T data, long timeout, TimeUnit unit) {
        long startTime = System.currentTimeMillis();
        long timeoutMillis = unit.toMillis(timeout);
        long deadline = startTime + timeoutMillis;
        long waitNanos = 10_000L;        // 初始等待 10μs
        long maxWaitNanos = 1_000_000L;  // 最大等待 1ms

        while (System.currentTimeMillis() < deadline) {
            try {
                // 尝试获取序号
                long sequence = ringBuffer.tryNext();
                try {
                    DisruptorEvent<T> event = ringBuffer.get(sequence);
                    event.setData(data);
                } finally {
                    ringBuffer.publish(sequence);
                }
                recordMetrics(startTime, true);
                return true;
            } catch (com.lmax.disruptor.InsufficientCapacityException e) {
                // 容量不足，使用指数退避策略避免忙等待导致 CPU 飙升
                LockSupport.parkNanos(waitNanos);
                waitNanos = Math.min(waitNanos * 2, maxWaitNanos);
            }
        }

        // 超时
        recordMetrics(startTime, false);
        return false;
    }

    /**
     * 批量发布事件
     *
     * @param events 事件列表
     */
    @SafeVarargs
    public final void publishEvents(T... events) {
        for (T event : events) {
            publishEvent(event);
        }
    }

    /**
     * 批量发布事件（List 版本）
     *
     * @param events 事件列表
     */
    public void publishEventList(List<T> events) {
        for (T event : events) {
            publishEvent(event);
        }
    }

    /**
     * 获取 RingBuffer 剩余容量
     *
     * @return 剩余容量
     */
    public long remainingCapacity() {
        return ringBuffer.remainingCapacity();
    }

    /**
     * 获取 RingBuffer 容量
     *
     * @return 容量
     */
    public long getBufferSize() {
        return ringBuffer.getBufferSize();
    }

    /**
     * 获取已发布的事件序号
     *
     * @return 当前序号
     */
    public long getCursor() {
        return ringBuffer.getCursor();
    }

    /**
     * 检查是否有可用空间
     *
     * @param requiredCapacity 需要的空间
     * @return true-有可用空间
     */
    public boolean hasAvailableCapacity(int requiredCapacity) {
        return ringBuffer.hasAvailableCapacity(requiredCapacity);
    }

    /**
     * 记录指标
     *
     * @param startTime 开始时间
     * @param success   是否成功
     */
    private void recordMetrics(long startTime, boolean success) {
        if (metrics != null) {
            long latency = System.currentTimeMillis() - startTime;
            if (success) {
                metrics.recordPublishSuccess(name, latency);
            } else {
                metrics.recordPublishFailure(name);
            }
        }
    }
}
