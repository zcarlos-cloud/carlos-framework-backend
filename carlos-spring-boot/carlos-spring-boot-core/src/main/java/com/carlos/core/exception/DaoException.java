package com.carlos.core.exception;

import com.carlos.core.response.ErrorCode;

/**
 * 数据库操作异常
 * <p>
 * 用于表示数据库操作相关的异常，如连接失败、SQL 错误、数据约束违反等
 *
 * @author carlos
 * @since 1.0.0
 */
public class DaoException extends GlobalException {

    private static final long serialVersionUID = -6912618737345878854L;

    /**
     * 默认 HTTP 状态码（500 Internal Server Error）
     */
    public static final int DEFAULT_HTTP_STATUS = 500;

    public DaoException() {
        super("数据操作异常");
        setHttpStatus(DEFAULT_HTTP_STATUS);
    }

    public DaoException(String message) {
        super(message);
        setHttpStatus(DEFAULT_HTTP_STATUS);
    }

    public DaoException(String message, Throwable cause) {
        super(message, cause);
        setHttpStatus(DEFAULT_HTTP_STATUS);
    }

    public DaoException(String message, int httpStatus) {
        super(message, httpStatus);
    }

    public DaoException(String message, int httpStatus, Throwable cause) {
        super(message, httpStatus, cause);
    }

    public DaoException(ErrorCode errorCode) {
        super(errorCode);
    }

    public DaoException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public DaoException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public DaoException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    public DaoException(String errorCode, String message) {
        super(errorCode, message);
        setHttpStatus(DEFAULT_HTTP_STATUS);
    }

    public DaoException(String errorCode, String message, int httpStatus) {
        super(errorCode, message, httpStatus);
    }

    public DaoException(String errorCode, String message, int httpStatus, Throwable cause) {
        super(errorCode, message, httpStatus, cause);
    }

    public DaoException(Throwable cause) {
        super(cause);
        setHttpStatus(DEFAULT_HTTP_STATUS);
    }

    public DaoException(Throwable cause, int httpStatus) {
        super(cause, httpStatus);
    }
}
