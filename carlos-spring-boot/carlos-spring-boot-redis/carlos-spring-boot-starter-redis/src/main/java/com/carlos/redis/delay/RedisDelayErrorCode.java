package com.carlos.redis.delay;

import com.carlos.core.response.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 延时队列原语层错误码（00 通用段）
 * <p>
 * 段位占用：{@code 50020}/{@code 50021} 按计划分配；{@code 50022} 归
 * {@code com.carlos.redis.stream.RedisStreamErrorCode}；本层另用 {@code 50023} 表示容量超限。
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@Getter
@RequiredArgsConstructor
public enum RedisDelayErrorCode implements ErrorCode {

    /** 延时队列入队失败 */
    DELAY_OFFER_FAILED("50020", "延时队列入队失败", 500),
    /** 延迟时长超过原语层物理上限 */
    DELAY_EXCEEDS_MAX("50021", "延迟时长超过原语层上限", 500),
    /** 延时队列已达容量上限 */
    DELAY_QUEUE_FULL("50023", "延时队列已达容量上限", 429),
    ;

    /** 5 位错误码 */
    private final String code;
    /** 人类可读描述 */
    private final String message;
    /** 对应 HTTP 状态码 */
    private final int httpStatus;
}
