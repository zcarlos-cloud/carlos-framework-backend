package com.carlos.ai.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>
 * AI 模型提供商枚举
 * </p>
 *
 * @author carlos
 * @since 3.0.0
 */
@Getter
@RequiredArgsConstructor
public enum AiProviderEnum {

    /**
     * OpenAI
     */
    OPENAI("openai", "OpenAI"),

    /**
     * 智谱 AI
     */
    ZHIPU("zhipu", "智谱AI"),

    /**
     * 百度千帆
     */
    QIANFAN("qianfan", "百度千帆"),

    /**
     * 阿里云 DashScope
     */
    DASHSCOPE("dashscope", "阿里云 DashScope"),

    /**
     * Ollama 本地模型
     */
    OLLAMA("ollama", "Ollama");

    private final String code;
    private final String desc;

}
