package com.carlos.redis.delay;

import com.carlos.core.exception.BusinessException;
import com.carlos.core.exception.GlobalException;
import com.carlos.core.response.ErrorCode;

/**
 * 延时队列原语层异常
 * <p>
 * 字符串构造器显式使用 500（系统级故障）；需要精确状态码时使用 {@link ErrorCode} 构造器。
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
public class RedisDelayException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public RedisDelayException(String message) {
        super(message, GlobalException.DEFAULT_HTTP_STATUS);
    }

    public RedisDelayException(String message, Throwable cause) {
        super(message, GlobalException.DEFAULT_HTTP_STATUS, cause);
    }

    public RedisDelayException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public RedisDelayException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
