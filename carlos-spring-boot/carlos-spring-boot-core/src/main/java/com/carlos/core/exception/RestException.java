package com.carlos.core.exception;

import com.carlos.core.response.ErrorCode;

/**
 * 接口层异常
 * <p>
 * 用于表示 REST API 接口层的异常，如参数解析失败、请求格式错误等
 *
 * @author carlos
 * @since 1.0.0
 */
public class RestException extends GlobalException {

    private static final long serialVersionUID = -2303357122330162359L;

    /**
     * 默认 HTTP 状态码（400 Bad Request）
     */
    public static final int DEFAULT_HTTP_STATUS = 400;

    public RestException() {
        super("接口层处理异常");
        setHttpStatus(DEFAULT_HTTP_STATUS);
    }

    public RestException(String message) {
        super(message);
        setHttpStatus(DEFAULT_HTTP_STATUS);
    }

    public RestException(String message, Throwable cause) {
        super(message, cause);
        setHttpStatus(DEFAULT_HTTP_STATUS);
    }

    public RestException(String message, int httpStatus) {
        super(message, httpStatus);
    }

    public RestException(String message, int httpStatus, Throwable cause) {
        super(message, httpStatus, cause);
    }

    public RestException(ErrorCode errorCode) {
        super(errorCode);
    }

    public RestException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public RestException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public RestException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    public RestException(String errorCode, String message) {
        super(errorCode, message);
        setHttpStatus(DEFAULT_HTTP_STATUS);
    }

    public RestException(String errorCode, String message, int httpStatus) {
        super(errorCode, message, httpStatus);
    }

    public RestException(String errorCode, String message, int httpStatus, Throwable cause) {
        super(errorCode, message, httpStatus, cause);
    }

    public RestException(Throwable cause) {
        super(cause);
        setHttpStatus(DEFAULT_HTTP_STATUS);
    }

    public RestException(Throwable cause, int httpStatus) {
        super(cause, httpStatus);
    }
}
