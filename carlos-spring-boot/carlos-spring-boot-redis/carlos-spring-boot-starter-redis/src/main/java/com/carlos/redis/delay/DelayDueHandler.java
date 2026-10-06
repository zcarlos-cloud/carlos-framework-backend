package com.carlos.redis.delay;

/**
 * 延时到期处理器 SPI
 * <p>
 * 延时原语层只负责「到期」，<b>不负责知道到期条目该去哪里</b>。本接口就是这个接缝：
 * 由上层（{@code com.carlos.mq}）提供实现，把到期条目路由到目标（例如对应的 Stream）。
 * </p>
 * <p>
 * <b>没有提供实现时的行为</b>：延时队列不会被启动，任何入队都会显式报「未启动」，
 * 而不是静默丢弃——符合「不静默降级」的项目约束。
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@FunctionalInterface
public interface DelayDueHandler {

    /**
     * 处理一条到期条目
     *
     * @param bucketName 桶名（上层据此路由）
     * @param payload    载荷（JSON 字符串）
     */
    void onDue(String bucketName, String payload);
}
