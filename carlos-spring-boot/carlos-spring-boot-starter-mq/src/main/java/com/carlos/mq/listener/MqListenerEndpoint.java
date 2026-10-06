package com.carlos.mq.listener;

import com.carlos.mq.support.ConsumeMode;

import java.lang.reflect.Method;

/**
 * 消息监听端点（不可变）
 * <p>
 * 一个 {@code @MqListener} 方法对应一个端点。由
 * {@link MqListenerAnnotationBeanPostProcessor} 在启动期从注解构建，
 * 之后只读使用。
 * </p>
 *
 * @param beanName        宿主 bean 名（日志用）
 * @param bean            宿主 bean 实例
 * @param method          被注解的方法
 * @param topic           主题
 * @param group           消费组
 * @param consumerId      显式消费者名；为空时由框架按实例派生唯一名
 * @param consumeMode     消费模式
 * @param concurrency     并发消费者数
 * @param batchSize       单次拉取条数
 * @param maxDeliveryCount 最大投递次数；非正数表示使用全局配置
 * @param shape           处理方法的入参形状
 * @param payloadType     负载类型（形状需要反序列化时使用）
 * @author Carlos
 * @since 3.0.0
 */
public record MqListenerEndpoint(
    String beanName,
    Object bean,
    Method method,
    String topic,
    String group,
    String consumerId,
    ConsumeMode consumeMode,
    int concurrency,
    int batchSize,
    int maxDeliveryCount,
    HandlerShape shape,
    Class<?> payloadType
) {

    /**
     * 处理器入参形状
     */
    public enum HandlerShape {

        /** 只接收业务负载：{@code void handle(OrderMessage msg)} */
        PAYLOAD,

        /** 接收负载与信封：{@code void handle(OrderMessage msg, MqMessage envelope)} */
        PAYLOAD_WITH_MESSAGE,

        /** 只接收信封：{@code void handle(MqMessage envelope)} */
        MESSAGE,

        /** 无参：{@code void handle()} */
        NO_ARGS
    }

    /**
     * 用于日志的可读标识
     *
     * @return 形如 {@code Bean#method}
     */
    public String display() {
        return beanName + "#" + method.getName();
    }

    /**
     * 订阅键（topic + 消费组）
     *
     * @return 形如 {@code topic@group}
     */
    public String subscriptionKey() {
        return topic + "@" + group;
    }
}
