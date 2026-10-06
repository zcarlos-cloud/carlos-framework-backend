package com.carlos.ai.service.impl;

import com.carlos.ai.config.CarlosAiProperties;
import com.carlos.ai.enums.AiErrorCode;
import com.carlos.ai.exception.AiException;
import com.carlos.ai.service.AiChatService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * <p>
 * AI 对话服务实现
 * </p>
 *
 * <p>
 * 当前版本仅支持单一 ChatModel Bean（由引入的 langchain4j 提供商 starter 自动注入）。
 * 多提供商切换需引入对应 starter 并在未来版本扩展模型路由能力。
 * </p>
 *
 * @author carlos
 * @since 3.0.0
 */
@Slf4j
public class AiChatServiceImpl implements AiChatService {

    private final ObjectProvider<ChatModel> chatModelProvider;
    private final ObjectProvider<StreamingChatModel> streamingChatModelProvider;
    private final CarlosAiProperties properties;

    private final Cache<String, ChatMemory> memoryStore;

    public AiChatServiceImpl(ObjectProvider<ChatModel> chatModelProvider,
                             ObjectProvider<StreamingChatModel> streamingChatModelProvider,
                             CarlosAiProperties properties) {
        this.chatModelProvider = chatModelProvider;
        this.streamingChatModelProvider = streamingChatModelProvider;
        this.properties = properties;
        this.memoryStore = Caffeine.newBuilder()
            .maximumSize(properties.getMemoryMaxSize())
            .expireAfterAccess(properties.getMemoryExpireMinutes(), TimeUnit.MINUTES)
            .build();
    }

    /**
     * 校验提供商名称是否有效
     *
     * @param provider 提供商名称
     */
    private void validateProvider(String provider) {
        if (provider == null || provider.isBlank()) {
            throw AiErrorCode.AI_PROVIDER_NOT_FOUND.exception("提供商名称不能为空");
        }
        // 当前版本仅支持配置的默认提供商（单模型环境）
        String defaultProvider = properties.getDefaultProvider();
        if (!provider.equals(defaultProvider)) {
            throw AiErrorCode.AI_PROVIDER_NOT_FOUND.exception(
                "当前仅支持默认提供商 '" + defaultProvider + "'，"
                    + "使用其他提供商请引入对应的 langchain4j starter 依赖");
        }
    }

    /**
     * 获取对话模型
     *
     * @return ChatModel
     */
    private ChatModel getChatModel() {
        ChatModel model = chatModelProvider.getIfAvailable();
        if (model == null) {
            throw AiErrorCode.AI_MODEL_NOT_CONFIGURED.exception();
        }
        return model;
    }

    /**
     * 获取流式对话模型
     *
     * @return StreamingChatModel
     */
    private StreamingChatModel getStreamingModel() {
        StreamingChatModel model = streamingChatModelProvider.getIfAvailable();
        if (model == null) {
            throw AiErrorCode.AI_STREAMING_NOT_SUPPORTED.exception();
        }
        return model;
    }

    /**
     * 获取或创建会话记忆
     *
     * @param sessionId 会话标识
     * @return ChatMemory
     */
    private ChatMemory getOrCreateMemory(String sessionId) {
        if (!properties.isMemoryEnabled()) {
            throw AiErrorCode.AI_MEMORY_NOT_ENABLED.exception();
        }
        return memoryStore.get(sessionId, k ->
            MessageWindowChatMemory.builder()
                .maxMessages(properties.getMemoryMaxMessages())
                .build());
    }

    @Override
    public String chat(String message) {
        return chatWithProvider(properties.getDefaultProvider(), message);
    }

    @Override
    public String chat(String sessionId, String message) {
        return chatWithProvider(properties.getDefaultProvider(), sessionId, message);
    }

    @Override
    public String chatWithProvider(String provider, String message) {
        try {
            validateProvider(provider);
            ChatModel model = getChatModel();
            return model.chat(message);
        } catch (AiException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI 单轮对话失败, provider={}", provider, e);
            throw new AiException("AI 对话调用失败", e);
        }
    }

    @Override
    public String chatWithProvider(String provider, String sessionId, String message) {
        try {
            validateProvider(provider);
            ChatModel model = getChatModel();
            ChatMemory memory = getOrCreateMemory(sessionId);
            memory.add(UserMessage.from(message));
            ChatResponse response = model.chat(memory.messages());
            memory.add(response.aiMessage());
            return response.aiMessage().text();
        } catch (AiException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI 多轮对话失败, provider={}, sessionId={}", provider, sessionId, e);
            throw new AiException("AI 对话调用失败", e);
        }
    }

    @Override
    public void chatStream(String message, Consumer<String> consumer) {
        chatStream(null, message, consumer);
    }

    @Override
    public void chatStream(String sessionId, String message, Consumer<String> consumer) {
        chatStreamWithProvider(properties.getDefaultProvider(), sessionId, message, consumer, null);
    }

    @Override
    public void chatStreamWithProvider(String provider, String message, Consumer<String> consumer) {
        chatStreamWithProvider(provider, null, message, consumer, null);
    }

    @Override
    public void chatStreamWithProvider(String provider, String sessionId, String message, Consumer<String> consumer) {
        chatStreamWithProvider(provider, sessionId, message, consumer, null);
    }

    @Override
    public void chatStream(String message, Consumer<String> onToken, Consumer<Throwable> onError) {
        chatStreamWithProvider(properties.getDefaultProvider(), null, message, onToken, onError);
    }

    @Override
    public void chatStream(String sessionId, String message, Consumer<String> onToken, Consumer<Throwable> onError) {
        chatStreamWithProvider(properties.getDefaultProvider(), sessionId, message, onToken, onError);
    }

    @Override
    public void chatStreamWithProvider(String provider, String message, Consumer<String> onToken, Consumer<Throwable> onError) {
        chatStreamWithProvider(provider, null, message, onToken, onError);
    }

    @Override
    public void chatStreamWithProvider(String provider, String sessionId, String message,
                                       Consumer<String> onToken, Consumer<Throwable> onError) {
        try {
            validateProvider(provider);
            StreamingChatModel model = getStreamingModel();
            if (sessionId != null && properties.isMemoryEnabled()) {
                ChatMemory memory = getOrCreateMemory(sessionId);
                memory.add(UserMessage.from(message));
                StringBuilder fullResponse = new StringBuilder();
                model.chat(memory.messages(), new StreamingChatResponseHandler() {
                    @Override
                    public void onPartialResponse(String token) {
                        onToken.accept(token);
                        fullResponse.append(token);
                    }

                    @Override
                    public void onCompleteResponse(ChatResponse response) {
                        memory.add(AiMessage.from(fullResponse.toString()));
                    }

                    @Override
                    public void onError(Throwable error) {
                        log.error("AI 流式对话失败, provider={}, sessionId={}", provider, sessionId, error);
                        if (onError != null) {
                            onError.accept(error);
                        }
                    }
                });
            } else {
                model.chat(message, new StreamingChatResponseHandler() {
                    @Override
                    public void onPartialResponse(String token) {
                        onToken.accept(token);
                    }

                    @Override
                    public void onCompleteResponse(ChatResponse response) {
                        // 单轮流式无需处理记忆
                    }

                    @Override
                    public void onError(Throwable error) {
                        log.error("AI 流式对话失败, provider={}", provider, error);
                        if (onError != null) {
                            onError.accept(error);
                        }
                    }
                });
            }
        } catch (AiException e) {
            if (onError != null) {
                onError.accept(e);
            } else {
                throw e;
            }
        } catch (Exception e) {
            log.error("AI 流式对话失败, provider={}", provider, e);
            if (onError != null) {
                onError.accept(e);
            } else {
                throw new AiException("AI 流式对话调用失败", e);
            }
        }
    }

}
