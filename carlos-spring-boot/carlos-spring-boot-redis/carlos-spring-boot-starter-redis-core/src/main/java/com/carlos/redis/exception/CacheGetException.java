package com.carlos.redis.exception;

import com.carlos.core.response.ErrorCode;

/**
 * 缓存读取异常
 *
 * @author carlos
 * @date 2020-09-20 00:06:26
 */
public class CacheGetException extends CacheException {

    private static final long serialVersionUID = -2303357122330162359L;

    public CacheGetException() {
        super("cache get exception!");
    }

    public CacheGetException(String message) {
        super(message);
    }

    public CacheGetException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public CacheGetException(ErrorCode errorCode) {
        super(errorCode);
    }

    public CacheGetException(Throwable cause) {
        super(cause);
    }

    public CacheGetException(String message, Throwable cause) {
        super(message, cause);
    }
}
