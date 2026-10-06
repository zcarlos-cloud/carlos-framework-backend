package com.carlos.redis.exception;

import com.carlos.core.response.ErrorCode;

/**
 * 缓存删除异常
 *
 * @author carlos
 * @date 2020-09-20 00:06:26
 */
public class CacheDelException extends CacheException {

    private static final long serialVersionUID = -2303357122330162359L;

    public CacheDelException() {
        super("cache del exception!");
    }

    public CacheDelException(String message) {
        super(message);
    }

    public CacheDelException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public CacheDelException(ErrorCode errorCode) {
        super(errorCode);
    }

    public CacheDelException(Throwable cause) {
        super(cause);
    }

    public CacheDelException(String message, Throwable cause) {
        super(message, cause);
    }
}
