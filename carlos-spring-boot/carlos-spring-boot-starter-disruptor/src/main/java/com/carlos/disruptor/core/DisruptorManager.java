package com.carlos.disruptor.core;

import com.carlos.disruptor.config.DisruptorProperties;
import com.carlos.disruptor.exception.DisruptorException;
import com.carlos.disruptor.metrics.DisruptorMetrics;
import com.lmax.disruptor.ExceptionHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.TimeoutException;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Disruptor 生命周期管理器
 * <p>
 * 管理 Disruptor 实例的创建、启动、停止，支持多实例管理
 * </p>
 *
 * @author Carlos
 * @date 2026-03-14
 */
@Slf4j
public class DisruptorManager {

    /**
     * Disruptor 实例缓存
     */
    private final Map<String, DisruptorHolder<?>> disruptors = new ConcurrentHashMap<>();

    /**
     * 全局配置
     */
    private final DisruptorProperties properties;

    /**
     * 指标收集器
     */
    private final DisruptorMetrics metrics;

    public DisruptorManager(DisruptorProperties properties) {
        this(properties, null);
    }

    public DisruptorManager(DisruptorProperties properties, DisruptorMetrics metrics) {
        this.properties = properties;
        this.metrics = metrics;
    }

    /**
     * 创建并启动 Disruptor
     *
     * @param name     实例名称
     * @param handlers 事件处理器
     * @param <T>      业务数据类型
     * @return DisruptorTemplate
     */
    @SafeVarargs
    public final <T> DisruptorTemplate<T> create(String name, DisruptorEventHandler<T>... handlers) {
        return create(name, properties.getBufferSize(), properties.getWaitStrategy().toStrategy(), handlers);
    }

    /**
     * 创建并启动 Disruptor
     *
     * @param name          实例名称
     * @param bufferSize    RingBuffer 大小
     * @param waitStrategy  等待策略
     * @param handlers      事件处理器
     * @param <T>           业务数据类型
     * @return DisruptorTemplate
     */
    @SafeVarargs
    public final <T> DisruptorTemplate<T> create(String name, int bufferSize,
                                                 WaitStrategy waitStrategy,
                                                 DisruptorEventHandler<T>... handlers) {
        if (disruptors.containsKey(name)) {
            throw new DisruptorException(
                "Disruptor instance already exists: " + name);
        }

        // 创建线程工厂
        ThreadFactory threadFactory = new CustomizableThreadFactory(
            properties.getThreadPool().getThreadNamePrefix() + name + "-");

        // 创建 Disruptor
        Disruptor<DisruptorEvent<T>> disruptor = new Disruptor<>(
            DisruptorEventFactory.getInstance(),
            bufferSize,
            threadFactory,
            ProducerType.MULTI,
            waitStrategy
        );

        // 设置默认异常处理器
        disruptor.setDefaultExceptionHandler(new DefaultExceptionHandler<T>());

        // 组装事件处理器
        InternalEventHandler<T> internalHandler = new InternalEventHandler<>(name);
        for (DisruptorEventHandler<T> handler : handlers) {
            internalHandler.addHandler(handler);
        }

        // 配置事件处理器
        disruptor.handleEventsWith(internalHandler);

        // 启动 Disruptor
        RingBuffer<DisruptorEvent<T>> ringBuffer = disruptor.start();

        // 缓存实例
        DisruptorHolder<T> holder = new DisruptorHolder<>(name, disruptor, ringBuffer, internalHandler);
        disruptors.put(name, holder);

        if (log.isInfoEnabled()) {
            log.info("[{}] Disruptor started. bufferSize={}, waitStrategy={}",
                name, bufferSize, waitStrategy.getClass().getSimpleName());
        }

        return new DisruptorTemplate<>(name, ringBuffer, metrics);
    }

    /**
     * 获取已创建的 DisruptorTemplate
     *
     * @param name 实例名称
     * @param <T>  业务数据类型
     * @return DisruptorTemplate
     */
    @SuppressWarnings("unchecked")
    public <T> DisruptorTemplate<T> getTemplate(String name) {
        DisruptorHolder<T> holder = (DisruptorHolder<T>) disruptors.get(name);
        if (holder == null) {
            throw new DisruptorException(
                "Disruptor instance not found: " + name);
        }
        return new DisruptorTemplate<>(name, holder.getRingBuffer(), metrics);
    }

    /**
     * 停止指定 Disruptor
     *
     * @param name 实例名称
     */
    public void shutdown(String name) {
        @SuppressWarnings("rawtypes")
        DisruptorHolder holder = disruptors.remove(name);
        if (holder != null) {
            shutdownHolder(holder);
        }
    }

    /**
     * 停止所有 Disruptor
     */
    public void shutdownAll() {
        disruptors.forEach((name, holder) -> shutdownHolder(holder));
        disruptors.clear();
    }

    /**
     * 优雅关闭（带超时）
     *
     * @param name    实例名称
     * @param timeout 超时时间
     * @param unit    时间单位
     * @return true-成功关闭，false-超时
     */
    public boolean shutdown(String name, long timeout, TimeUnit unit) {
        @SuppressWarnings("rawtypes")
        DisruptorHolder holder = disruptors.remove(name);
        if (holder != null) {
            return shutdownHolder(holder, timeout, unit);
        }
        return true;
    }

    /**
     * 获取处理器统计信息
     *
     * @param name 实例名称
     * @return 统计信息
     */
    public HandlerStats getStats(String name) {
        @SuppressWarnings("rawtypes")
        DisruptorHolder holder = disruptors.get(name);
        if (holder == null) {
            return null;
        }
        InternalEventHandler<?> handler = holder.getHandler();
        return new HandlerStats(
            name,
            handler.getProcessedCount(),
            handler.getExceptionCount()
        );
    }

    /**
     * 检查实例是否存在
     *
     * @param name 实例名称
     * @return true-存在
     */
    public boolean exists(String name) {
        return disruptors.containsKey(name);
    }

    /**
     * 关闭 Holder
     */
    private void shutdownHolder(@SuppressWarnings("rawtypes") DisruptorHolder holder) {
        try {
            holder.getDisruptor().shutdown();
            if (log.isInfoEnabled()) {
                log.info("[{}] Disruptor shutdown. Processed: {}, Exceptions: {}",
                    holder.getName(),
                    holder.getHandler().getProcessedCount(),
                    holder.getHandler().getExceptionCount());
            }
        } catch (Exception e) {
            log.error("[{}] Disruptor shutdown failed", holder.getName(), e);
        }
    }

    /**
     * 关闭 Holder（带超时）
     */
    private boolean shutdownHolder(@SuppressWarnings("rawtypes") DisruptorHolder holder,
                                   long timeout, TimeUnit unit) {
        try {
            holder.getDisruptor().shutdown(timeout, unit);
            if (log.isInfoEnabled()) {
                log.info("[{}] Disruptor shutdown. Processed: {}, Exceptions: {}",
                    holder.getName(),
                    holder.getHandler().getProcessedCount(),
                    holder.getHandler().getExceptionCount());
            }
            return true;
        } catch (TimeoutException e) {
            log.warn("[{}] Disruptor shutdown timeout", holder.getName());
            return false;
        } catch (Exception e) {
            log.error("[{}] Disruptor shutdown failed", holder.getName(), e);
            return false;
        }
    }

    /**
     * Disruptor 持有者
     */
    private static class DisruptorHolder<T> {
        private final String name;
        private final Disruptor<DisruptorEvent<T>> disruptor;
        private final RingBuffer<DisruptorEvent<T>> ringBuffer;
        private final InternalEventHandler<T> handler;

        DisruptorHolder(String name, Disruptor<DisruptorEvent<T>> disruptor,
                        RingBuffer<DisruptorEvent<T>> ringBuffer,
                        InternalEventHandler<T> handler) {
            this.name = name;
            this.disruptor = disruptor;
            this.ringBuffer = ringBuffer;
            this.handler = handler;
        }

        String getName() {
            return name;
        }

        Disruptor<DisruptorEvent<T>> getDisruptor() {
            return disruptor;
        }

        RingBuffer<DisruptorEvent<T>> getRingBuffer() {
            return ringBuffer;
        }

        InternalEventHandler<T> getHandler() {
            return handler;
        }
    }

    /**
     * 处理器统计信息
     */
    public record HandlerStats(String name, long processedCount, long exceptionCount) {
    }

    /**
     * 默认异常处理器
     */
    private static class DefaultExceptionHandler<T> implements ExceptionHandler<DisruptorEvent<T>> {
        @Override
        public void handleEventException(Throwable ex, long sequence, DisruptorEvent<T> event) {
            log.error("Exception processing sequence [{}] for event [{}]: {}",
                sequence, event != null ? event.getEventId() : "null", ex.getMessage(), ex);
        }

        @Override
        public void handleOnStartException(Throwable ex) {
            log.error("Exception during Disruptor startup: {}", ex.getMessage(), ex);
        }

        @Override
        public void handleOnShutdownException(Throwable ex) {
            log.error("Exception during Disruptor shutdown: {}", ex.getMessage(), ex);
        }
    }
}
