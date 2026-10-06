package com.carlos.mq.exception;

import com.carlos.core.exception.BusinessException;
import com.carlos.core.exception.GlobalException;
import com.carlos.core.response.ErrorCode;

/**
 * MQ 基础异常
 * <p>
 * 继承 {@link BusinessException}，符合项目「模块自定义异常应继承 BusinessException」的规范，
 * 同时因为它仍是 {@link GlobalException} 的子类，原有的
 * {@code catch (GlobalException)} 捕获点行为不变。
 * </p>
 * <p>
 * 注意：{@link BusinessException} 的字符串构造器默认 HTTP 状态码为 400，而 MQ 故障
 * （传输不可达、超时、配置错误）属系统级。因此本类的字符串构造器<b>显式使用 500</b>，
 * 避免把系统故障误报为客户端错误；需要精确状态码时请使用 {@link ErrorCode} 构造器
 * （由错误码自身携带状态码）。
 * </p>
 *
 * @author Carlos
 * @date 2026/3/14
 */
public class MqException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public MqException(String message) {
        super(message, GlobalException.DEFAULT_HTTP_STATUS);
    }

    public MqException(String message, Throwable cause) {
        super(message, GlobalException.DEFAULT_HTTP_STATUS, cause);
    }

    public MqException(Throwable cause) {
        super(cause, GlobalException.DEFAULT_HTTP_STATUS);
    }

    public MqException(ErrorCode errorCode) {
        super(errorCode);
    }

    public MqException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public MqException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public MqException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
