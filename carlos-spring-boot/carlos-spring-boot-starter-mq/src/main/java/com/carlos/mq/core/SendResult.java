package com.carlos.mq.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 消息发送结果
 * <p>
 * 相比旧实现： {@code String code} 换成类型安全的 {@link SendStatus}；
 * 移除 RocketMQ 形状字段（{@code tag} / {@code transactionId} / {@code queueId} /
 * {@code queueOffset} / {@code regionId}），以传输中立的 {@code recordId} 取代。
 * </p>
 *
 * @author Carlos
 * @date 2026/3/14
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 发送状态
     */
    private SendStatus status;

    /**
     * 消息 ID
     */
    private String messageId;

    /**
     * 主题
     */
    private String topic;

    /**
     * 业务键
     */
    private String key;

    /**
     * 底层传输的物理 id（如 Stream 的 recordId）；发送失败时为 null
     */
    private String recordId;

    /**
     * 发送时刻（epoch millis）
     */
    private Long sendTime;

    /**
     * 发送耗时（毫秒）
     */
    private Long sendCostTime;

    /**
     * 附加消息（失败原因等）
     */
    private String message;

    /**
     * 异常（发送失败时）
     */
    private Throwable exception;

    /**
     * 成功结果
     *
     * @param messageId 消息 ID
     * @param topic     主题
     * @param recordId  物理 id
     * @return 结果
     */
    public static SendResult success(String messageId, String topic, String recordId) {
        return SendResult.builder()
            .status(SendStatus.SUCCESS)
            .messageId(messageId)
            .topic(topic)
            .recordId(recordId)
            .sendTime(System.currentTimeMillis())
            .build();
    }

    /**
     * 失败结果
     *
     * @param status    状态
     * @param messageId 消息 ID
     * @param topic     主题
     * @return 结果
     */
    public static SendResult failure(SendStatus status, String messageId, String topic) {
        return SendResult.builder()
            .status(status)
            .messageId(messageId)
            .topic(topic)
            .sendTime(System.currentTimeMillis())
            .message(status == null ? null : status.getMessage())
            .build();
    }

    /**
     * 失败结果（带异常）
     *
     * @param status    状态
     * @param messageId 消息 ID
     * @param topic     主题
     * @param cause     异常
     * @return 结果
     */
    public static SendResult failure(SendStatus status, String messageId, String topic, Throwable cause) {
        SendResult result = failure(status, messageId, topic);
        result.setException(cause);
        return result;
    }

    /**
     * 是否视为成功
     * <p>{@link SendStatus#DROPPED} 计入成功：它是「按策略主动丢弃」，
     * 调用方无需重试（重试也不会改变结果）。</p>
     *
     * @return 成功或主动丢弃时返回 true
     */
    public boolean isSuccess() {
        return status == SendStatus.SUCCESS || status == SendStatus.DROPPED;
    }

    /**
     * 是否应当重试
     * <p>仅「传输层临时不可用」类状态值得重试；能力不支持、参数不合法、超限重试无意义。</p>
     *
     * @return 可重试时返回 true
     */
    public boolean isRetryable() {
        return status == SendStatus.REJECTED
            || status == SendStatus.SEND_FAILED
            || status == SendStatus.SEND_TIMEOUT
            || status == SendStatus.TRANSPORT_UNAVAILABLE;
    }

    /**
     * 错误码（便于日志与监控按码聚合）
     *
     * @return 5 位错误码；状态为空时返回 null
     */
    public String getCode() {
        return status == null ? null : status.getCode();
    }
}
