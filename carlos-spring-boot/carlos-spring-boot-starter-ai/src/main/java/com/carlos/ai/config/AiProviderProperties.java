package com.carlos.ai.config;

import lombok.Data;

/**
 * <p>
 * AI 模型提供商配置属性
 * </p>
 *
 * <p>
 * 支持 OpenAI、智谱AI、百度千帆、阿里云 DashScope、Ollama 等提供商的统一配置结构。
 * 实际生效取决于项目中引入的 langchain4j 提供商 starter 依赖。
 * </p>
 *
 * @author carlos
 * @since 3.0.0
 */
@Data
public class AiProviderProperties {

    /**
     * API 密钥（建议通过环境变量或配置中心注入）
     */
    private String apiKey;

    /**
     * 模型名称，如 gpt-4o、glm-4、qwen-max 等
     */
    private String model;

    /**
     * 采样温度（0.0 ~ 2.0），控制输出随机性
     */
    private Double temperature;

    /**
     * 最大生成令牌数
     */
    private Integer maxTokens;

    /**
     * 自定义基础 URL（如 Ollama 本地地址 http://localhost:11434）
     */
    private String baseUrl;

}
