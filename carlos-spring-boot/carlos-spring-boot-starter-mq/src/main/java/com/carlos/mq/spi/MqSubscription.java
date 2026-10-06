package com.carlos.mq.spi;

import com.carlos.mq.core.MqMessageListener;

/**
 * 消费订阅描述（不可变）
 * <p>
 * <b>这是消费侧的统一入口</b>：{@code @MqListener} 注解驱动与编程式订阅都归约到它，
 * 传输实现只面对这一种形状，无需感知注解。
 * </p>
 * <p>
 * 消费组名由上层决定（广播模式下上层会为其追加实例后缀，以保证每实例各收一份），
 * 传输层不修改其中的语义。
 * </p>
 *
 * @param topic            主题
 * @param group            生效消费组（上层已处理广播模式的隔离）
 * @param consumerId       消费者名；为空时由传输层按实例派生唯一名
 * @param concurrency      并发消费者数（同组内分摊）
 * @param batchSize        单次拉取条数
 * @param maxDeliveryCount 最大投递次数（达到后转死信）
 * @param handler          消息处理器
 * @author Carlos
 * @since 3.0.0
 */
public record MqSubscription(
    String topic,
    String group,
    String consumerId,
    int concurrency,
    int batchSize,
    int maxDeliveryCount,
    MqMessageListener handler
) {

    /**
     * 紧凑构造器：校验必要字段并把非法数值回落为安全值。
     */
    public MqSubscription {
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("MqSubscription.topic 不能为空");
        }
        if (group == null || group.isBlank()) {
            throw new IllegalArgumentException("MqSubscription.group 不能为空");
        }
        if (handler == null) {
            throw new IllegalArgumentException("MqSubscription.handler 不能为空");
        }
        if (concurrency <= 0) {
            concurrency = 1;
        }
        if (batchSize <= 0) {
            batchSize = 1;
        }
        if (maxDeliveryCount <= 0) {
            maxDeliveryCount = 1;
        }
    }

    /**
     * 用于日志的可读标识
     *
     * @return 形如 {@code topic@group}
     */
    public String display() {
        return topic + "@" + group;
    }
}
