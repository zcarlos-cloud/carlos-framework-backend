package com.carlos.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Carlos AI 配置属性
 * </p>
 *
 * <p>
 * 配置前缀：{@code carlos.ai}
 * </p>
 *
 * @author carlos
 * @since 3.0.0
 */
@Data
@ConfigurationProperties(prefix = "carlos.ai")
public class CarlosAiProperties {

    /**
     * 是否启用 AI 功能
     */
    private boolean enabled = true;

    /**
     * 默认模型提供商名称（如 openai、zhipu、ollama）
     */
    private String defaultProvider = "openai";

    /**
     * 是否启用对话记忆
     */
    private boolean memoryEnabled = true;

    /**
     * 对话记忆最大消息数
     */
    private int memoryMaxMessages = 20;

    /**
     * 会话记忆最大缓存条目数
     */
    private long memoryMaxSize = 10000;

    /**
     * 会话记忆过期时间（分钟），无访问后自动清理
     */
    private long memoryExpireMinutes = 120;

    /**
     * 各模型提供商配置（key 为提供商名称）
     */
    private Map<String, AiProviderProperties> providers = new HashMap<>();

    /**
     * 嵌入向量配置
     */
    private AiEmbeddingProperties embedding = new AiEmbeddingProperties();

    /**
     * 文档解析配置
     */
    private Document document = new Document();

    /**
     * 文档解析配置
     */
    @Data
    public static class Document {

        /**
         * 最大允许解析的文档大小（字节），默认 10MB
         */
        private long maxSizeBytes = 10 * 1024 * 1024;
    }

}
