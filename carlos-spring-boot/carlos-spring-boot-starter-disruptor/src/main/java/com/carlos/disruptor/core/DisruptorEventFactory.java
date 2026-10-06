package com.carlos.disruptor.core;

import com.lmax.disruptor.EventFactory;

/**
 * Disruptor 事件工厂
 * <p>
 * 用于预分配 RingBuffer 中的事件对象，避免运行时GC
 * </p>
 *
 * @author Carlos
 * @date 2026-03-14
 */
public class DisruptorEventFactory<T> implements EventFactory<DisruptorEvent<T>> {

    /**
     * 单例实例
     */
    @SuppressWarnings("rawtypes")
    private static final DisruptorEventFactory INSTANCE = new DisruptorEventFactory<>();

    /**
     * 获取单例实例
     *
     * @param <T> 业务数据类型
     * @return 事件工厂实例
     */
    @SuppressWarnings("unchecked")
    public static <T> DisruptorEventFactory<T> getInstance() {
        return INSTANCE;
    }

    /**
     * 创建新的事件实例
     *
     * @return 新的事件对象
     */
    @Override
    public DisruptorEvent<T> newInstance() {
        return new DisruptorEvent<>();
    }
}
