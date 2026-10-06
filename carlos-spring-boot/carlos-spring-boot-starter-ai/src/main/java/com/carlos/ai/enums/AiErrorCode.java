package com.carlos.ai.enums;

import com.carlos.core.exception.BusinessException;
import com.carlos.core.response.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>
 * AI 模块错误码定义
 * </p>
 *
 * <p>
 * 模块编码：12
 * </p>
 *
 * @author carlos
 * @since 3.0.0
 */
@Getter
@RequiredArgsConstructor
public enum AiErrorCode implements ErrorCode {

    // ==================== 第三方错误 (3-12-xx) ====================
    AI_MODEL_NOT_CONFIGURED("31201", "AI 模型未配置", 500),
    AI_STREAMING_NOT_SUPPORTED("31202", "当前模型不支持流式输出", 400),
    AI_CHAT_ERROR("31203", "AI 对话调用失败", 500),
    AI_EMBEDDING_ERROR("31204", "AI 嵌入向量调用失败", 500),

    // ==================== 客户端错误 (1-12-xx) ====================
    AI_DOCUMENT_PARSE_ERROR("11205", "文档解析失败", 400),
    AI_PROVIDER_NOT_FOUND("11206", "AI 模型提供商未找到", 400),
    AI_MEMORY_NOT_ENABLED("11207", "对话记忆未启用", 400);

    private final String code;
    private final String message;
    private final int httpStatus;

    @Override
    public BusinessException exception() {
        return new BusinessException(this);
    }

}
