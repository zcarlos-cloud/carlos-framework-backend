package com.carlos.core.exception;

import com.carlos.core.response.ErrorCode;

/**
 * 组件异常
 * <p>
 * 用于表示组件层面的异常，如依赖注入失败、组件初始化异常等
 *
 * @author carlos
 * @since 1.0.0
 */
public class ComponentException extends GlobalException {

    private static final long serialVersionUID = -2303357122330162359L;

    /**
     * 默认 HTTP 状态码（500 Internal Server Error）
     */
    public static final int DEFAULT_HTTP_STATUS = 500;

    public ComponentException() {
        super("组件异常！");
        setHttpStatus(DEFAULT_HTTP_STATUS);
    }

    public ComponentException(String message) {
        super(message);
        setHttpStatus(DEFAULT_HTTP_STATUS);
    }

    public ComponentException(Throwable cause) {
        super(cause);
        setHttpStatus(DEFAULT_HTTP_STATUS);
    }

    public ComponentException(String message, Throwable cause) {
        super(message, cause);
        setHttpStatus(DEFAULT_HTTP_STATUS);
    }

    public ComponentException(String message, int httpStatus) {
        super(message, httpStatus);
    }

    public ComponentException(String message, int httpStatus, Throwable cause) {
        super(message, httpStatus, cause);
    }

    public ComponentException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ComponentException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public ComponentException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public ComponentException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    public ComponentException(String errorCode, String message) {
        super(errorCode, message);
        setHttpStatus(DEFAULT_HTTP_STATUS);
    }

    public ComponentException(String errorCode, String message, int httpStatus) {
        super(errorCode, message, httpStatus);
    }

    public ComponentException(String errorCode, String message, int httpStatus, Throwable cause) {
        super(errorCode, message, httpStatus, cause);
    }

    public ComponentException(Throwable cause, int httpStatus) {
        super(cause, httpStatus);
    }
}
