package com.carlos.ai.config;

import lombok.Data;

/**
 * <p>
 * AI 嵌入向量配置属性
 * </p>
 *
 * @author carlos
 * @since 3.0.0
 */
@Data
public class AiEmbeddingProperties {

    /**
     * 是否启用嵌入向量功能
     */
    private boolean enabled = false;

    /**
     * 嵌入模型提供商，默认 openai
     */
    private String provider = "openai";

    /**
     * 嵌入模型名称，如 text-embedding-3-small
     */
    private String model;

    /**
     * 向量存储类型：redis / in-memory
     */
    private String storeType = "redis";

    /**
     * Redis 向量存储配置（store-type=redis 时生效）
     */
    private AiEmbeddingRedisProperties redis = new AiEmbeddingRedisProperties();

}
