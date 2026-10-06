package com.carlos.disruptor.chain;

import com.carlos.disruptor.core.DisruptorEvent;
import com.carlos.disruptor.core.DisruptorEventHandler;
import com.lmax.disruptor.EventHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 事件处理器组
 * <p>
 * 用于构建消费者链中的一个处理组，支持并行处理
 * </p>
 *
 * @author Carlos
 * @date 2026-03-14
 */
public class EventHandlerGroup<T> {

    /**
     * 处理器列表
     */
    private final List<DisruptorEventHandler<T>> handlers;

    @SafeVarargs
    public EventHandlerGroup(DisruptorEventHandler<T>... handlers) {
        this.handlers = new ArrayList<>(Arrays.asList(handlers));
    }

    public EventHandlerGroup(List<DisruptorEventHandler<T>> handlers) {
        this.handlers = new ArrayList<>(handlers);
    }

    /**
     * 添加处理器到当前组
     *
     * @param handler 处理器
     * @return 当前组
     */
    public EventHandlerGroup<T> andThen(DisruptorEventHandler<T> handler) {
        this.handlers.add(handler);
        return this;
    }

    /**
     * 添加多个处理器到当前组（并行）
     *
     * @param handlers 处理器数组
     * @return 当前组
     */
    @SafeVarargs
    public final EventHandlerGroup<T> andThen(DisruptorEventHandler<T>... handlers) {
        this.handlers.addAll(Arrays.asList(handlers));
        return this;
    }

    /**
     * 获取处理器列表
     *
     * @return 处理器列表
     */
    public List<DisruptorEventHandler<T>> getHandlers() {
        return new ArrayList<>(handlers);
    }

    /**
     * 转换为 Disruptor EventHandler 数组
     *
     * @return EventHandler 数组
     */
    @SuppressWarnings("unchecked")
    public EventHandler<DisruptorEvent<T>>[] toEventHandlers() {
        // 每个处理器独立成一个 EventHandler，实现并行处理
        return handlers.stream()
            .map(this::wrapHandler)
            .toArray(EventHandler[]::new);
    }

    /**
     * 包装业务处理器为 Disruptor EventHandler
     */
    private EventHandler<DisruptorEvent<T>> wrapHandler(DisruptorEventHandler<T> handler) {
        return (event, sequence, endOfBatch) -> {
            handler.onEvent(event, sequence, endOfBatch);
        };
    }
}
