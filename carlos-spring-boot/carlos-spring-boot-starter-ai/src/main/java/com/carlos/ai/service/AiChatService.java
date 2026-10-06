package com.carlos.ai.service;

import java.util.function.Consumer;

/**
 * <p>
 * AI 对话服务接口
 * </p>
 *
 * <p>
 * 提供单轮对话、多轮对话（基于 Session 记忆）、流式输出等能力。
 * 支持通过指定提供商进行对话（需引入对应 langchain4j 提供商 starter）。
 * </p>
 *
 * @author carlos
 * @since 3.0.0
 */
public interface AiChatService {

    /**
     * 单轮对话（无记忆，使用默认提供商）
     *
     * @param message 用户消息
     * @return AI 回复内容
     */
    String chat(String message);

    /**
     * 多轮对话（基于 Session 记忆，使用默认提供商）
     *
     * @param sessionId 会话标识
     * @param message   用户消息
     * @return AI 回复内容
     */
    String chat(String sessionId, String message);

    /**
     * 指定提供商单轮对话（无记忆）
     *
     * @param provider 提供商名称（如 openai、zhipu、ollama）
     * @param message  用户消息
     * @return AI 回复内容
     */
    String chatWithProvider(String provider, String message);

    /**
     * 指定提供商多轮对话（基于 Session 记忆）
     *
     * @param provider  提供商名称
     * @param sessionId 会话标识
     * @param message   用户消息
     * @return AI 回复内容
     */
    String chatWithProvider(String provider, String sessionId, String message);

    /**
     * 流式单轮对话（使用默认提供商）
     *
     * @param message  用户消息
     * @param consumer 内容消费回调（逐字/逐句接收）
     */
    void chatStream(String message, Consumer<String> consumer);

    /**
     * 流式多轮对话（基于 Session 记忆，使用默认提供商）
     *
     * @param sessionId 会话标识
     * @param message   用户消息
     * @param consumer  内容消费回调（逐字/逐句接收）
     */
    void chatStream(String sessionId, String message, Consumer<String> consumer);

    /**
     * 指定提供商流式单轮对话
     *
     * @param provider 提供商名称
     * @param message  用户消息
     * @param consumer 内容消费回调
     */
    void chatStreamWithProvider(String provider, String message, Consumer<String> consumer);

    /**
     * 指定提供商流式多轮对话
     *
     * @param provider  提供商名称
     * @param sessionId 会话标识
     * @param message   用户消息
     * @param consumer  内容消费回调
     */
    void chatStreamWithProvider(String provider, String sessionId, String message, Consumer<String> consumer);

    /**
     * 流式单轮对话（带错误回调，使用默认提供商）
     *
     * @param message  用户消息
     * @param onToken  内容消费回调（逐字/逐句接收）
     * @param onError  错误回调
     */
    void chatStream(String message, Consumer<String> onToken, Consumer<Throwable> onError);

    /**
     * 流式多轮对话（带错误回调，使用默认提供商）
     *
     * @param sessionId 会话标识
     * @param message   用户消息
     * @param onToken   内容消费回调
     * @param onError   错误回调
     */
    void chatStream(String sessionId, String message, Consumer<String> onToken, Consumer<Throwable> onError);

    /**
     * 指定提供商流式单轮对话（带错误回调）
     *
     * @param provider 提供商名称
     * @param message  用户消息
     * @param onToken  内容消费回调
     * @param onError  错误回调
     */
    void chatStreamWithProvider(String provider, String message, Consumer<String> onToken, Consumer<Throwable> onError);

    /**
     * 指定提供商流式多轮对话（带错误回调）
     *
     * @param provider  提供商名称
     * @param sessionId 会话标识
     * @param message   用户消息
     * @param onToken   内容消费回调
     * @param onError   错误回调
     */
    void chatStreamWithProvider(String provider, String sessionId, String message, Consumer<String> onToken, Consumer<Throwable> onError);

}
