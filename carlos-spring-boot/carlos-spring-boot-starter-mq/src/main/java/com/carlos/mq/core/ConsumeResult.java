package com.carlos.mq.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 消息消费结果
 * <p>
 * {@link ConsumeStatus} 的四个取值直接映射到框架的四个可靠性出口：
 * </p>
 * <ul>
 *   <li>{@link ConsumeStatus#SUCCESS} → 确认（ACK）</li>
 *   <li>{@link ConsumeStatus#DROP} → 确认 + 计数（可选写入死信）</li>
 *   <li>{@link ConsumeStatus#RETRY} → <b>不确认</b>，留在待处理队列等认领重投</li>
 *   <li>{@link ConsumeStatus#RECONSUME_LATER} → 不确认 + 立即投递一条延迟消息</li>
 * </ul>
 * <p>
 * 相比旧实现：新增 {@code errorCode}（原先只有自由文本 {@code errorMessage}，无法按错误分类统计）；
 * 移除 {@code Throwable exception}（异常不应跨线程/跨进程传递，只日志记录）。
 * </p>
 *
 * @author Carlos
 * @date 2026/3/14
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsumeResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消费状态
     */
    private ConsumeStatus status;

    /**
     * 消息 ID
     */
    private String messageId;

    /**
     * 主题
     */
    private String topic;

    /**
     * 错误码（失败时）
     */
    private String errorCode;

    /**
     * 错误消息（失败时）
     */
    private String errorMessage;

    /**
     * 本次消费的累计投递次数
     */
    private Integer deliveryCount;

    /**
     * 消费完成时刻（epoch millis）
     */
    private Long consumeTime;

    /**
     * 消费耗时（毫秒）
     */
    private Long consumeCostTime;

    /**
     * 消费状态
     */
    public enum ConsumeStatus {

        /**
         * 处理成功
         */
        SUCCESS,

        /**
         * 需要重试（不确认，等待认领重投）
         */
        RETRY,

        /**
         * 放弃本条（确认并计数，可选写死信）
         */
        DROP,

        /**
         * 稍后重投（不确认，并立即投递一条延迟消息）
         */
        RECONSUME_LATER
    }

    /**
     * 成功
     *
     * @param messageId 消息 ID
     * @param topic     主题
     * @return 结果
     */
    public static ConsumeResult success(String messageId, String topic) {
        return ConsumeResult.builder()
            .status(ConsumeStatus.SUCCESS)
            .messageId(messageId)
            .topic(topic)
            .consumeTime(System.currentTimeMillis())
            .build();
    }

    /**
     * 需要重试
     *
     * @param messageId 消息 ID
     * @param topic     主题
     * @param message   原因
     * @return 结果
     */
    public static ConsumeResult retry(String messageId, String topic, String message) {
        return ConsumeResult.builder()
            .status(ConsumeStatus.RETRY)
            .messageId(messageId)
            .topic(topic)
            .errorMessage(message)
            .consumeTime(System.currentTimeMillis())
            .build();
    }

    /**
     * 放弃本条
     *
     * @param messageId 消息 ID
     * @param topic     主题
     * @param message   原因
     * @return 结果
     */
    public static ConsumeResult drop(String messageId, String topic, String message) {
        return ConsumeResult.builder()
            .status(ConsumeStatus.DROP)
            .messageId(messageId)
            .topic(topic)
            .errorMessage(message)
            .consumeTime(System.currentTimeMillis())
            .build();
    }

    /**
     * 稍后重投
     *
     * @param messageId 消息 ID
     * @param topic     主题
     * @return 结果
     */
    public static ConsumeResult reconsumedLater(String messageId, String topic) {
        return ConsumeResult.builder()
            .status(ConsumeStatus.RECONSUME_LATER)
            .messageId(messageId)
            .topic(topic)
            .consumeTime(System.currentTimeMillis())
            .build();
    }

    /**
     * 是否应确认（ACK）
     *
     * @return 成功或主动放弃时返回 true
     */
    public boolean shouldAck() {
        return status == ConsumeStatus.SUCCESS || status == ConsumeStatus.DROP;
    }
}
