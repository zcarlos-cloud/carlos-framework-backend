package com.carlos.ai.config;

import lombok.Data;

/**
 * <p>
 * AI 嵌入向量 Redis 存储配置
 * </p>
 *
 * @author carlos
 * @since 3.0.0
 */
@Data
public class AiEmbeddingRedisProperties {

    /**
     * Redis 主机地址
     */
    private String host = "localhost";

    /**
     * Redis 端口
     */
    private int port = 6379;

    /**
     * 向量索引名称
     */
    private String indexName = "framework:ai:embeddings";

}
