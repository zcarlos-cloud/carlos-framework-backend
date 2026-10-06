package com.carlos.ai.config;

import com.carlos.ai.service.AiChatService;
import com.carlos.ai.service.AiDocumentService;
import com.carlos.ai.service.AiEmbeddingService;
import com.carlos.ai.service.impl.AiChatServiceImpl;
import com.carlos.ai.service.impl.AiDocumentServiceImpl;
import com.carlos.ai.service.impl.AiEmbeddingServiceImpl;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * <p>
 * Carlos AI 自动配置类
 * </p>
 *
 * <p>
 * 当 {@code carlos.ai.enabled=true}（默认）且类路径存在 LangChain4j 时自动启用。
 * 基于 LangChain4j Spring Boot Starter 自动配置的模型 Bean 进行服务封装。
 * </p>
 *
 * @author carlos
 * @since 3.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties(CarlosAiProperties.class)
@ConditionalOnProperty(prefix = "carlos.ai", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(ChatModel.class)
public class CarlosAiAutoConfiguration {

    /**
     * 注册 AI 对话服务
     *
     * @param chatLanguageModelProvider         对话模型提供者
     * @param streamingChatLanguageModelProvider 流式对话模型提供者
     * @param properties                        AI 配置属性
     * @return AiChatService
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(ChatModel.class)
    public AiChatService aiChatService(
        ObjectProvider<ChatModel> chatModelProvider,
        ObjectProvider<StreamingChatModel> streamingChatModelProvider,
        CarlosAiProperties properties) {
        return new AiChatServiceImpl(chatModelProvider, streamingChatModelProvider, properties);
    }

    /**
     * 注册 AI 嵌入向量服务
     *
     * @param embeddingModelProvider 嵌入模型提供者
     * @param properties             AI 配置属性
     * @return AiEmbeddingService
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(EmbeddingModel.class)
    public AiEmbeddingService aiEmbeddingService(ObjectProvider<EmbeddingModel> embeddingModelProvider,
                                                  CarlosAiProperties properties) {
        return new AiEmbeddingServiceImpl(embeddingModelProvider, properties);
    }

    /**
     * 注册 AI 文档解析服务
     *
     * @param properties AI 配置属性
     * @return AiDocumentService
     */
    @Bean
    @ConditionalOnMissingBean
    public AiDocumentService aiDocumentService(CarlosAiProperties properties) {
        return new AiDocumentServiceImpl(properties);
    }

}
