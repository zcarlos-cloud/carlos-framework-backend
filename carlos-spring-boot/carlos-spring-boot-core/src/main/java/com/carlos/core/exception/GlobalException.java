package com.carlos.core.exception;

import com.carlos.core.response.ErrorCode;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 全局异常基类
 * <p>
 * 所有自定义异常的父类，提供统一的错误码、错误消息和 HTTP 状态码管理。
 * 支持新旧两种错误码体系：
 * <ul>
 *   <li>新版：使用 {@link ErrorCode} 接口（5位字符串错误码）</li>
 *   <li>旧版：使用 Integer 错误码（4位数字，兼容保留）</li>
 * </ul>
 *
 * @author carlos
 * @since 1.0.0
 * @see BusinessException
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GlobalException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 默认 HTTP 状态码
     */
    public static final int DEFAULT_HTTP_STATUS = 500;

    /**
     * 错误码（新版为5位字符串，旧版为4位整数）
     */
    private String errorCode;

    /**
     * 错误提示
     */
    private String message;

    /**
     * HTTP 状态码
     */
    private int httpStatus = DEFAULT_HTTP_STATUS;

    /**
     * 空构造方法，避免反序列化问题
     */
    public GlobalException() {
        super("服务异常，请联系管理员");
        this.message = "服务异常，请联系管理员";
    }

    /**
     * 使用消息创建异常
     *
     * @param message 错误消息
     */
    public GlobalException(String message) {
        super(message);
        this.message = message;
    }

    /**
     * 使用消息和原因创建异常
     *
     * @param message 错误消息
     * @param cause   原始异常
     */
    public GlobalException(String message, Throwable cause) {
        super(message, cause);
        this.message = message;
    }

    /**
     * 使用消息和 HTTP 状态码创建异常
     *
     * @param message    错误消息
     * @param httpStatus HTTP 状态码
     */
    public GlobalException(String message, int httpStatus) {
        super(message);
        this.message = message;
        this.httpStatus = httpStatus;
    }

    /**
     * 使用消息、HTTP 状态码和原因创建异常
     *
     * @param message    错误消息
     * @param httpStatus HTTP 状态码
     * @param cause      原始异常
     */
    public GlobalException(String message, int httpStatus, Throwable cause) {
        super(message, cause);
        this.message = message;
        this.httpStatus = httpStatus;
    }

    /**
     * 使用 ErrorCode 创建异常
     *
     * @param errorCode 错误码
     */
    public GlobalException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode.getCode();
        this.message = errorCode.getMessage();
        this.httpStatus = errorCode.getHttpStatus();
    }

    /**
     * 使用 ErrorCode 和自定义消息创建异常
     *
     * @param errorCode 错误码
     * @param message   自定义错误消息
     */
    public GlobalException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode.getCode();
        this.message = message;
        this.httpStatus = errorCode.getHttpStatus();
    }

    /**
     * 使用 ErrorCode 和原因创建异常
     *
     * @param errorCode 错误码
     * @param cause     原始异常
     */
    public GlobalException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode.getCode();
        this.message = errorCode.getMessage();
        this.httpStatus = errorCode.getHttpStatus();
    }

    /**
     * 使用 ErrorCode、自定义消息和原因创建异常
     *
     * @param errorCode 错误码
     * @param message   自定义错误消息
     * @param cause     原始异常
     */
    public GlobalException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode.getCode();
        this.message = message;
        this.httpStatus = errorCode.getHttpStatus();
    }

    /**
     * 使用字符串错误码和错误消息创建异常
     *
     * @param errorCode 错误码
     * @param message   错误消息
     */
    public GlobalException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.message = message;
    }

    /**
     * 使用字符串错误码、错误消息和 HTTP 状态码创建异常
     *
     * @param errorCode  错误码
     * @param message    错误消息
     * @param httpStatus HTTP 状态码
     */
    public GlobalException(String errorCode, String message, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    /**
     * 使用字符串错误码、错误消息、HTTP 状态码和原因创建异常
     *
     * @param errorCode  错误码
     * @param message    错误消息
     * @param httpStatus HTTP 状态码
     * @param cause      原始异常
     */
    public GlobalException(String errorCode, String message, int httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    /**
     * 使用原始异常创建
     *
     * @param cause 原始异常
     */
    public GlobalException(Throwable cause) {
        super(cause);
        this.message = cause != null ? cause.getMessage() : null;
    }

    /**
     * 使用原始异常和 HTTP 状态码创建
     *
     * @param cause      原始异常
     * @param httpStatus HTTP 状态码
     */
    public GlobalException(Throwable cause, int httpStatus) {
        super(cause);
        this.message = cause != null ? cause.getMessage() : null;
        this.httpStatus = httpStatus;
    }
}
