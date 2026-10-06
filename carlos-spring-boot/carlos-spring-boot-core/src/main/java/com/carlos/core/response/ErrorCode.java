package com.carlos.core.response;

import com.carlos.core.exception.BusinessException;

/**
 * 错误码接口
 * <p>
 * 所有错误码枚举必须实现此接口，统一规范错误码的定义和使用。
 * 错误码格式：5位数字字符串 A-BB-CC
 * <ul>
 *   <li>A - 错误级别（1位）：0成功，1客户端错误，2业务错误，3第三方错误，5系统错误</li>
 *   <li>BB - 模块编码（2位）：00通用，01用户，02认证，03订单等</li>
 *   <li>CC - 具体错误序号（2位）</li>
 * </ul>
 *
 * @author carlos
 * @since 3.0.0
 */
public interface ErrorCode {

    /**
     * 获取错误码
     *
     * @return 5位数字字符串，如 "00000", "20101"
     */
    String getCode();

    /**
     * 获取错误消息
     *
     * @return 人类可读的错误描述
     */
    String getMessage();

    /**
     * 获取HTTP状态码
     *
     * @return HTTP状态码，如 200, 400, 404, 500
     */
    int getHttpStatus();

    /**
     * 获取错误级别
     *
     * @return 错误级别枚举
     */
    default ErrorLevel getLevel() {
        if (getCode() == null || getCode().length() < 1) {
            return ErrorLevel.SYSTEM_ERROR;
        }
        char levelCode = getCode().charAt(0);
        return ErrorLevel.fromCode(levelCode);
    }

    /**
     * 创建业务异常（使用默认消息）
     *
     * @return BusinessException
     */
    default BusinessException exception() {
        return new BusinessException(this);
    }

    /**
     * 创建业务异常（自定义消息）
     *
     * @param message 自定义错误消息
     * @return BusinessException
     */
    default BusinessException exception(String message) {
        return new BusinessException(this, message);
    }

    /**
     * 创建业务异常（格式化消息）
     *
     * @param format 格式化字符串
     * @param args   格式化参数
     * @return BusinessException
     */
    default BusinessException exception(String format, Object... args) {
        return new BusinessException(this, String.format(format, args));
    }
}
