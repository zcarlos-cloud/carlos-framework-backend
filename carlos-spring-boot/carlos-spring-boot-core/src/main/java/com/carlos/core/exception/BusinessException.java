package com.carlos.core.exception;

import com.carlos.core.response.ErrorCode;

/**
 * 业务异常
 * <p>
 * 用于表示可预期的业务错误，如用户不存在、权限不足、业务规则限制等
 *
 * @author carlos
 * @since 1.0.0
 */
public class BusinessException extends GlobalException {

    private static final long serialVersionUID = 1L;

    /**
     * 默认 HTTP 状态码（400 Bad Request）
     */
    public static final int DEFAULT_HTTP_STATUS = 400;

    public BusinessException() {
        super("业务处理异常！");
        setHttpStatus(DEFAULT_HTTP_STATUS);
    }

    public BusinessException(String message) {
        super(message);
        setHttpStatus(DEFAULT_HTTP_STATUS);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        setHttpStatus(DEFAULT_HTTP_STATUS);
    }

    public BusinessException(String message, int httpStatus) {
        super(message, httpStatus);
    }

    public BusinessException(String message, int httpStatus, Throwable cause) {
        super(message, httpStatus, cause);
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode);
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    public BusinessException(String errorCode, String message) {
        super(errorCode, message);
        setHttpStatus(DEFAULT_HTTP_STATUS);
    }

    public BusinessException(String errorCode, String message, int httpStatus) {
        super(errorCode, message, httpStatus);
    }

    public BusinessException(String errorCode, String message, int httpStatus, Throwable cause) {
        super(errorCode, message, httpStatus, cause);
    }

    public BusinessException(Throwable cause) {
        super(cause);
        setHttpStatus(DEFAULT_HTTP_STATUS);
    }

    public BusinessException(Throwable cause, int httpStatus) {
        super(cause, httpStatus);
    }
}
