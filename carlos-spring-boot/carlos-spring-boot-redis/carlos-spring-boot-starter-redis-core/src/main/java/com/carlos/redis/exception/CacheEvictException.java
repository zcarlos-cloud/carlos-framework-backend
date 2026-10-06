package com.carlos.redis.exception;

import com.carlos.core.response.ErrorCode;

/**
 * 缓存更新异常
 *
 * @author carlos
 * @date 2020-09-20 00:06:26
 */
public class CacheEvictException extends CacheException {

    private static final long serialVersionUID = -2303357122330162359L;

    public CacheEvictException() {
        super("cache put exception!");
    }

    public CacheEvictException(String message) {
        super(message);
    }

    public CacheEvictException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public CacheEvictException(ErrorCode errorCode) {
        super(errorCode);
    }

    public CacheEvictException(Throwable cause) {
        super(cause);
    }

    public CacheEvictException(String message, Throwable cause) {
        super(message, cause);
    }
}
