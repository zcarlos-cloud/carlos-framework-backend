package com.carlos.ai.service;

import java.util.List;

/**
 * <p>
 * AI 嵌入向量服务接口
 * </p>
 *
 * <p>
 * 提供文本向量化、批量向量化等能力。
 * </p>
 *
 * @author carlos
 * @since 3.0.0
 */
public interface AiEmbeddingService {

    /**
     * 单条文本向量化
     *
     * @param text 文本内容
     * @return 向量结果（浮点数组）
     */
    List<Float> embed(String text);

    /**
     * 批量文本向量化
     *
     * @param texts 文本列表
     * @return 向量结果列表
     */
    List<List<Float>> embed(List<String> texts);

}
