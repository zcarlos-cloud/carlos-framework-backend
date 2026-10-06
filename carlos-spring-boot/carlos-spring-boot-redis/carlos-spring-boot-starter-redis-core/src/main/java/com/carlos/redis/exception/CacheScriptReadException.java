package com.carlos.redis.exception;

import com.carlos.core.exception.ComponentException;
import com.carlos.core.response.ErrorCode;

/**
 * 缓存异常父类
 *
 * @author carlos
 * @date 2020-09-20 00:06:26
 */
public class CacheScriptReadException extends ComponentException {

    private static final long serialVersionUID = -2303357122330162359L;

    public CacheScriptReadException() {
        super("redis lua script read error!");
    }

    public CacheScriptReadException(String message) {
        super(message);
    }

    public CacheScriptReadException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public CacheScriptReadException(ErrorCode errorCode) {
        super(errorCode);
    }

    public CacheScriptReadException(Throwable cause) {
        super(cause);
    }

    public CacheScriptReadException(String message, Throwable cause) {
        super(message, cause);
    }
}
