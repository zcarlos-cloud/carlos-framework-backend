package com.carlos.ai.service.impl;

import com.carlos.ai.config.CarlosAiProperties;
import com.carlos.ai.enums.AiErrorCode;
import com.carlos.ai.exception.AiException;
import com.carlos.ai.service.AiEmbeddingService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

/**
 * <p>
 * AI 嵌入向量服务实现
 * </p>
 *
 * <p>
 * 支持基于配置的嵌入模型选择。当前版本依赖 langchain4j 自动注入的 EmbeddingModel Bean。
 * 向量存储功能需引入 {@code langchain4j-redis} 依赖后扩展。
 * </p>
 *
 * @author carlos
 * @since 3.0.0
 */
@Slf4j
public class AiEmbeddingServiceImpl implements AiEmbeddingService {

    private final ObjectProvider<EmbeddingModel> embeddingModelProvider;
    private final CarlosAiProperties properties;

    public AiEmbeddingServiceImpl(ObjectProvider<EmbeddingModel> embeddingModelProvider,
                                  CarlosAiProperties properties) {
        this.embeddingModelProvider = embeddingModelProvider;
        this.properties = properties;
    }

    /**
     * 检查嵌入功能是否启用
     */
    private void checkEnabled() {
        if (!properties.getEmbedding().isEnabled()) {
            throw AiErrorCode.AI_EMBEDDING_ERROR.exception("AI 嵌入向量功能未启用，请设置 carlos.ai.embedding.enabled=true");
        }
    }

    /**
     * 获取嵌入模型
     *
     * @return EmbeddingModel
     */
    private EmbeddingModel getModel() {
        EmbeddingModel model = embeddingModelProvider.getIfAvailable();
        if (model == null) {
            throw AiErrorCode.AI_MODEL_NOT_CONFIGURED.exception("EmbeddingModel 未配置，请引入对应的 langchain4j embedding starter");
        }
        return model;
    }

    @Override
    public List<Float> embed(String text) {
        checkEnabled();
        try {
            EmbeddingModel model = getModel();
            Response<Embedding> response = model.embed(text);
            return response.content().vectorAsList();
        } catch (AiException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI 嵌入向量失败, text={}", text, e);
            throw new AiException("AI 嵌入向量调用失败", e);
        }
    }

    @Override
    public List<List<Float>> embed(List<String> texts) {
        checkEnabled();
        try {
            EmbeddingModel model = getModel();
            List<TextSegment> segments = texts.stream()
                .map(TextSegment::from)
                .toList();
            Response<List<Embedding>> response = model.embedAll(segments);
            return response.content().stream()
                .map(Embedding::vectorAsList)
                .toList();
        } catch (AiException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI 批量嵌入向量失败, size={}", texts.size(), e);
            throw new AiException("AI 嵌入向量调用失败", e);
        }
    }

}
