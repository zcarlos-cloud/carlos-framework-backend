package com.carlos.redis.stream;

import com.carlos.core.exception.BusinessException;
import com.carlos.core.exception.GlobalException;
import com.carlos.core.response.ErrorCode;

/**
 * Redis Stream 原语层异常
 * <p>
 * 与 {@code MqException} 同理：字符串构造器显式使用 500（系统级故障），
 * 需要精确状态码时使用 {@link ErrorCode} 构造器。
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
public class RedisStreamException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public RedisStreamException(String message) {
        super(message, GlobalException.DEFAULT_HTTP_STATUS);
    }

    public RedisStreamException(String message, Throwable cause) {
        super(message, GlobalException.DEFAULT_HTTP_STATUS, cause);
    }

    public RedisStreamException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public RedisStreamException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
