package com.carlos.mq.error;

import com.carlos.core.response.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * MQ 模块错误码
 * <p>
 * 采用框架统一的 5 位错误码规范 {@code A-BB-CC}，模块编码取 {@code 00} 通用段。
 * </p>
 * <p>
 * 段位占用说明（依据 {@link com.carlos.core.response.CommonErrorCode}）：
 * <ul>
 *   <li>{@code 1-00-xx}：已占 {@code 10001}、{@code 10004}–{@code 10008}，本模块用 {@code 10002}/{@code 10003}</li>
 *   <li>{@code 2-00-xx}：仅占 {@code 20001}，本模块自 {@code 20002} 起</li>
 *   <li>{@code 3-00-xx}：已占 {@code 30001}/{@code 30002}，本模块自 {@code 30003} 起</li>
 *   <li>{@code 5-00-xx}：{@code 50001}–{@code 50009} 已满，本模块自 {@code 50010} 起（{@code 50012} 留空）</li>
 * </ul>
 * </p>
 * <p>
 * 本枚举是 MQ 错误码的<b>唯一真源</b>：任何发送/消费状态枚举中的码值都必须与本表一致，
 * 由单元测试断言防漂移。
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@Getter
@RequiredArgsConstructor
public enum MqErrorCode implements ErrorCode {

    // ==================== 客户端错误 (1-00-xx) ====================
    /** 主题不合法（空、含非法字符、未被允许等） */
    MQ_TOPIC_INVALID("10002", "MQ 主题不合法", 400),
    /** 消息体超过配置上限。注意：{@code 10003} 不用于 carlos-job 的测试桩字面量 */
    MQ_PAYLOAD_TOO_LARGE("10003", "消息体超过上限", 413),

    // ==================== 业务错误 (2-00-xx) ====================
    /** 当前传输实现未声明该能力。用于「不支持即显式报错」，禁止静默降级 */
    MQ_CAPABILITY_NOT_SUPPORTED("20002", "当前 MQ 实现不支持该能力", 501),
    /** 延迟时长超过配置上限。不做截断，直接报错 */
    MQ_DELAY_EXCEEDS_MAX("20003", "延迟时长超出上限", 400),
    /** 延迟队列达到总量上限 */
    MQ_DELAY_QUEUE_FULL("20004", "延迟队列已达总量上限", 429),
    /** 背压拒绝投递 */
    MQ_BACKPRESSURE_REJECTED("20005", "消息积压超阈值，已拒绝投递", 429),
    /** {@code @MqListener} 配置不合法（启动期校验） */
    MQ_LISTENER_CONFIG_INVALID("20006", "@MqListener 配置不合法", 500),
    /** 消息反序列化失败 */
    MQ_MESSAGE_DESERIALIZE_FAILED("20007", "消息反序列化失败", 500),
    /** 消息被降级丢弃（可容忍丢失流量的背压策略） */
    MQ_MESSAGE_DROPPED("20009", "消息被降级丢弃", 200),
    /** 主题显式配置的延时桶不存在于 {@code carlos.redis.delay.buckets}（启动期校验） */
    MQ_DELAY_BUCKET_NOT_FOUND("20010", "延时桶未在 carlos.redis.delay.buckets 中声明", 500),

    // ==================== 传输层错误 (3-00-xx) ====================
    /** 发送失败 */
    MQ_SEND_FAILED("30003", "消息发送失败", 500),
    /** 发送超时 */
    MQ_SEND_TIMEOUT("30004", "消息发送超时", 504),
    /** 消费失败 */
    MQ_CONSUME_FAILED("30005", "消息消费失败", 500),
    /** 传输不可用（连接失败、写满被拒等） */
    MQ_TRANSPORT_UNAVAILABLE("30006", "消息传输不可用", 503),

    // ==================== 系统错误 (5-00-xx) ====================
    /** MQ 未初始化 */
    MQ_NOT_INITIALIZED("50010", "MQ 未初始化", 500),
    /** 未找到可用的传输实现 */
    MQ_TRANSPORT_NOT_FOUND("50011", "未找到可用的 MQ 实现", 500),
    ;

    /** 5 位错误码 */
    private final String code;
    /** 人类可读描述 */
    private final String message;
    /** 对应 HTTP 状态码 */
    private final int httpStatus;
}
