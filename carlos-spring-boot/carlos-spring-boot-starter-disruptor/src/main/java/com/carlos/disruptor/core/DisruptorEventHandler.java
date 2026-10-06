package com.carlos.disruptor.core;

/**
 * Disruptor 业务事件处理器接口
 * <p>
 * 业务代码实现此接口来处理特定类型的事件
 * </p>
 *
 * @author Carlos
 * @date 2026-03-14
 */
public interface DisruptorEventHandler<T> {

    /**
     * 处理事件
     *
     * @param event      事件对象
     * @param sequence   事件序号
     * @param endOfBatch 是否是批次最后一个事件
     * @throws Exception 处理异常
     */
    void onEvent(DisruptorEvent<T> event, long sequence, boolean endOfBatch) throws Exception;

    /**
     * 获取处理器名称
     *
     * @return 处理器名称
     */
    default String getHandlerName() {
        return this.getClass().getSimpleName();
    }

    /**
     * 是否忽略异常继续处理
     *
     * @return true-忽略异常继续处理，false-抛出异常
     */
    default boolean ignoreException() {
        return false;
    }

    /**
     * 异常处理回调
     *
     * @param event     事件对象
     * @param sequence  事件序号
     * @param exception 异常对象
     */
    default void onException(DisruptorEvent<T> event, long sequence, Throwable exception) {
        // 默认空实现，子类可覆盖
    }
}
