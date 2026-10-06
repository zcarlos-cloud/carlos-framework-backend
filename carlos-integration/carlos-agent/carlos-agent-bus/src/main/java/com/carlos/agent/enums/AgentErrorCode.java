package com.carlos.agent.enums;

import com.carlos.core.exception.BusinessException;
import com.carlos.core.response.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * AI 助手服务模块错误码
 * <p>
 * 模块编码：09（AI 助手服务）
 * <p>
 * 错误码格式：A-BB-CC
 * <ul>
 *   <li>A - 错误级别：1客户端错误，2业务错误，3第三方错误，5系统错误</li>
 *   <li>BB - 模块编码：09 AI 助手服务</li>
 *   <li>CC - 具体错误序号</li>
 * </ul>
 *
 * @author carlos
 * @since 3.0.0
 */
@Getter
@RequiredArgsConstructor
public enum AgentErrorCode implements ErrorCode {

    // ==================== 第三方错误 (3-09-xx) ====================
    AGENT_BFF_UNAVAILABLE("30901", "AI 助手运行服务不可用", 502),
    AGENT_BFF_ERROR("30902", "AI 助手运行服务返回错误", 502),

    // ==================== 系统错误 (5-09-xx) ====================
    AGENT_SYSTEM_ERROR("50901", "AI 助手服务内部错误", 500);

    private final String code;
    private final String message;
    private final int httpStatus;

    @Override
    public BusinessException exception() {
        return new BusinessException(this);
    }

    @Override
    public BusinessException exception(String customMessage) {
        return new BusinessException(this, customMessage);
    }

    @Override
    public BusinessException exception(String format, Object... args) {
        return new BusinessException(this, String.format(format, args));
    }
}
