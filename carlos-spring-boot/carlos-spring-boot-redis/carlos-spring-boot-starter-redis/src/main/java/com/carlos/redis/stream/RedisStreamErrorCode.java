package com.carlos.redis.stream;

import com.carlos.core.response.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Redis Stream 原语层错误码（00 通用段）
 * <p>
 * 段位占用：{@code 5-00-xx} 的 {@code 50001}–{@code 50009} 已被
 * {@link com.carlos.core.response.CommonErrorCode} 占满，故本层自 {@code 50017} 起
 * （{@code 50010}–{@code 50016} 归 MQ 抽象层，{@code 50020}/{@code 50021} 归延时原语层）。
 * </p>
 * <p>
 * <b>与 MQ 层错误码的分工</b>：本层的 {@code DELAY_EXCEEDS_MAX}（物理上限，属框架/配置问题）
 * 与 MQ 层的 {@code MQ_DELAY_EXCEEDS_MAX}（业务传入超限）触发路径不同，勿混用。
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@Getter
@RequiredArgsConstructor
public enum RedisStreamErrorCode implements ErrorCode {

    /** Stream 修剪失败 */
    STREAM_TRIM_FAILED("50017", "Stream 修剪失败", 500),
    /** 消费组操作失败 */
    STREAM_GROUP_OPERATION_FAILED("50018", "Stream 消费组操作失败", 500),
    /** 条目序列化失败 */
    STREAM_SERIALIZATION_FAILED("50019", "Stream 条目序列化失败", 500),
    /** Redis Stream 不可用 */
    REDIS_STREAM_UNAVAILABLE("50022", "Redis Stream 不可用", 503),
    ;

    /** 5 位错误码 */
    private final String code;
    /** 人类可读描述 */
    private final String message;
    /** 对应 HTTP 状态码 */
    private final int httpStatus;
}
