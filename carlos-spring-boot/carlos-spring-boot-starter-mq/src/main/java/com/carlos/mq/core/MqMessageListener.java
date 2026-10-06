package com.carlos.mq.core;

/**
 * MQ 消息监听器接口
 *
 * @author Carlos
 * @date 2026/3/14
 */
@FunctionalInterface
public interface MqMessageListener {

    /**
     * 消费消息
     *
     * @param message 消息
     * @return 消费结果
     */
    ConsumeResult onMessage(MqMessage message);
}
