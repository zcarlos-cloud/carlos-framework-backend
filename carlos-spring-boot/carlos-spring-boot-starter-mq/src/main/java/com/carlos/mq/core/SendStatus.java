package com.carlos.mq.core;

import com.carlos.core.response.CommonErrorCode;
import com.carlos.core.response.ErrorCode;
import com.carlos.core.response.SimpleErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 发送状态
 * <p>
 * 取代旧实现里自由的 {@code String code}（9 个硬编码字面量、无枚举、无能力概念）。
 * </p>
 * <p>
 * <b>与 {@link com.carlos.mq.error.MqErrorCode} 的关系</b>：MQ 专属状态码以
 * {@code MqErrorCode} 为唯一真源，本枚举的 {@link #getCode()} 必须与之逐一一致
 * （由单元测试断言防漂移）；通用状态（如序列化）复用 {@link CommonErrorCode}。
 * </p>
 * <p>
 * <b>语义约定</b>：发送族的失败通过<b>结果</b>表达（本枚举），异常只用于启动期配置错误
 * （例如 {@code @MqListener} 声明了传输不支持的能力）。避免同一 API「有时抛异常、
 * 有时返回失败」。
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@Getter
@RequiredArgsConstructor
public enum SendStatus {

    /** 发送成功 */
    SUCCESS("00000", "发送成功", 200),

    /** 按背压策略降级丢弃（仅用于可容忍丢失的流量） */
    DROPPED("20009", "消息被降级丢弃", 200),

    /** 背压拒绝投递（可重试） */
    REJECTED("20005", "消息积压超阈值，已拒绝投递", 429),

    /** 当前传输不支持所请求的能力 */
    CAPABILITY_NOT_SUPPORTED("20002", "当前 MQ 实现不支持该能力", 501),

    /** 主题或负载不合法 */
    INVALID_ARGUMENT("10002", "MQ 主题不合法", 400),

    /** 消息体超过上限 */
    PAYLOAD_TOO_LARGE("10003", "消息体超过上限", 413),

    /** 延迟时长超过上限 */
    DELAY_EXCEEDS_MAX("20003", "延迟时长超出上限", 400),

    /** 延迟队列容量已满 */
    DELAY_QUEUE_FULL("20004", "延迟队列已达总量上限", 429),

    /** 传输层发送失败 */
    SEND_FAILED("30003", "消息发送失败", 500),

    /** 发送超时 */
    SEND_TIMEOUT("30004", "消息发送超时", 504),

    /** 传输不可用（连接失败、写满被拒等） */
    TRANSPORT_UNAVAILABLE("30006", "消息传输不可用", 503),

    /** 序列化失败（复用通用错误码，不再新增同义码） */
    SERIALIZATION_ERROR("50008", "序列化异常", 500),

    /** 传输未初始化或未启用 */
    NOT_INITIALIZED("50010", "MQ 未初始化", 500),
    ;

    /** 5 位错误码 */
    private final String code;
    /** 人类可读描述 */
    private final String message;
    /** 对应 HTTP 状态码 */
    private final int httpStatus;

    /**
     * 转换为框架错误码对象，便于交给全局异常处理
     *
     * @return 错误码
     */
    public ErrorCode asErrorCode() {
        return new SimpleErrorCode(code, message, httpStatus);
    }
}
