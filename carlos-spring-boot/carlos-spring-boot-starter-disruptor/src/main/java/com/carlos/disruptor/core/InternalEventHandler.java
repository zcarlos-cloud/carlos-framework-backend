package com.carlos.disruptor.core;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.LifecycleAware;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.LongAdder;

/**
 * 内部事件处理器
 * <p>
 * 适配 Disruptor 的 EventHandler 接口，封装业务处理器
 * </p>
 *
 * @author Carlos
 * @date 2026-03-14
 */
@Slf4j
public class InternalEventHandler<T> implements EventHandler<DisruptorEvent<T>>, LifecycleAware {

    /**
     * 业务处理器列表
     */
    private final List<DisruptorEventHandler<T>> handlers;

    /**
     * 处理器名称
     */
    private final String name;

    /**
     * 已处理事件计数
     */
    private final LongAdder processedCount = new LongAdder();

    /**
     * 异常计数
     */
    private final LongAdder exceptionCount = new LongAdder();

    public InternalEventHandler(String name) {
        this.name = name;
        this.handlers = new CopyOnWriteArrayList<>();
    }

    public InternalEventHandler(String name, List<DisruptorEventHandler<T>> handlers) {
        this.name = name;
        this.handlers = new CopyOnWriteArrayList<>(handlers);
    }

    /**
     * 添加业务处理器
     *
     * @param handler 业务处理器
     */
    public void addHandler(DisruptorEventHandler<T> handler) {
        this.handlers.add(handler);
    }

    @Override
    public void onEvent(DisruptorEvent<T> event, long sequence, boolean endOfBatch) throws Exception {
        for (DisruptorEventHandler<T> handler : handlers) {
            try {
                handler.onEvent(event, sequence, endOfBatch);
            } catch (Exception e) {
                exceptionCount.increment();
                if (handler.ignoreException()) {
                    log.warn("[{}] Handler ignored exception for event [{}]: {}",
                        name, event.getEventId(), e.getMessage());
                    handler.onException(event, sequence, e);
                } else {
                    log.error("[{}] Handler exception for event [{}]", name, event.getEventId(), e);
                    throw e;
                }
            }
        }
        processedCount.increment();

        // 清理事件数据（对象池复用）
        if (endOfBatch) {
            event.clear();
        }
    }

    @Override
    public void onStart() {
        log.debug("[{}] Disruptor handler started", name);
    }

    @Override
    public void onShutdown() {
        log.debug("[{}] Disruptor handler shutdown. Processed: {}, Exceptions: {}",
            name, processedCount.sum(), exceptionCount.sum());
    }

    /**
     * 获取已处理事件数
     *
     * @return 处理数量
     */
    public long getProcessedCount() {
        return processedCount.sum();
    }

    /**
     * 获取异常数
     *
     * @return 异常数量
     */
    public long getExceptionCount() {
        return exceptionCount.sum();
    }

    /**
     * 获取处理器名称
     *
     * @return 名称
     */
    public String getName() {
        return name;
    }
}
