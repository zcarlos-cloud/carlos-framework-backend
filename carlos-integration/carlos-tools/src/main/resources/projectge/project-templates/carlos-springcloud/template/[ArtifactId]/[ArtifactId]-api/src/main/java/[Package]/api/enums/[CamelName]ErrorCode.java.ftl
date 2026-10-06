package com.carlos.audit.api.enums;

import com.carlos.core.exception.BusinessException;
import com.carlos.core.response.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * ${project.describe}模块错误码
 * <p>
 * ${project.describe}编码：xx（${project.describe}服务）
 * <p>
 * 错误码格式：A-BB-CC
 * <ul>
 *   <li>A - 错误级别：1客户端错误，2业务错误，3第三方错误，5系统错误</li>
 *   <li>BB - 模块编码：xx服务</li>
 *   <li>CC - 具体错误序号</li>
 * </ul>
 *
 * @author  ${project.author}
 * @date    ${.now}
 * @since 3.0.0
 */
@Getter
@RequiredArgsConstructor
public enum ${project.camelName}ErrorCode implements ErrorCode {

    // ==================== 业务错误 (2-xx-xx) ====================


    // ==================== 第三方错误 (3-xx-xx) ====================

    // ==================== 系统错误 (5-xx-xx) ====================

    ;

    private final String code;
    private final String message;
    private final int httpStatus;

    /**
     * 创建业务异常（使用默认消息）
     *
     * @return BusinessException
     */
    @Override
    public BusinessException exception() {
        return new BusinessException(this);
    }

    /**
     * 创建业务异常（自定义消息）
     *
     * @param customMessage 自定义消息
     * @return BusinessException
     */
    @Override
    public BusinessException exception(String customMessage) {
        return new BusinessException(this, customMessage);
    }

    /**
     * 创建业务异常（格式化消息）
     *
     * @param format 格式字符串
     * @param args   参数
     * @return BusinessException
     */
    @Override
    public BusinessException exception(String format, Object... args) {
        return new BusinessException(this, String.format(format, args));
    }
}
