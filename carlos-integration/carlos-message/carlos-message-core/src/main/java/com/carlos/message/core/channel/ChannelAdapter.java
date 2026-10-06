package com.carlos.message.core.channel;

import com.carlos.message.core.protocol.MessageContext;
import com.carlos.message.core.protocol.SendResult;

import java.util.List;

/**
 * <p>
 * 渠道适配器接口
 * </p>
 *
 * @author Carlos
 * @date 2026/3/11
 */
public interface ChannelAdapter {

    /**
     * 获取渠道编码
     *
     * @return 渠道编码
     */
    String getChannelCode();

    /**
     * 获取渠道类型
     *
     * @return 渠道类型
     */
    ChannelType getChannelType();

    /**
     * 检查渠道是否可用
     *
     * @return 是否可用
     */
    boolean isAvailable();

    /**
     * 发送消息
     *
     * @param context 消息上下文
     * @return 发送结果
     */
    SendResult send(MessageContext context);

    /**
     * 批量发送
     *
     * @param contexts 消息上下文列表
     * @return 批量发送结果
     */
    default List<SendResult> sendBatch(List<MessageContext> contexts) {
        return contexts.stream()
                .map(this::send)
                .toList();
    }

    /**
     * 检查是否支持该消息类型
     *
     * @param messageType 消息类型
     * @return 是否支持
     */
    boolean supports(String messageType);

    /**
     * 健康检查
     *
     * @return 是否健康
     */
    default boolean healthCheck() {
        return isAvailable();
    }
}
