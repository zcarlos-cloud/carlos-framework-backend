package com.carlos.redis.exception;

import com.carlos.core.response.ErrorCode;

/**
 * 缓存保存异常
 *
 * @author carlos
 * @date 2020-09-20 00:06:26
 */
public class CachePutException extends CacheException {

    private static final long serialVersionUID = -2303357122330162359L;

    public CachePutException() {
        super("cache put exception!");
    }

    public CachePutException(String message) {
        super(message);
    }

    public CachePutException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public CachePutException(ErrorCode errorCode) {
        super(errorCode);
    }

    public CachePutException(Throwable cause) {
        super(cause);
    }

    public CachePutException(String message, Throwable cause) {
        super(message, cause);
    }
}
