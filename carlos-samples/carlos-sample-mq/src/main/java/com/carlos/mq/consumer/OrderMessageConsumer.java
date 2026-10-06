package com.carlos.mq.consumer;

import com.carlos.mq.annotation.MqListener;
import com.carlos.mq.controller.MqController;
import com.carlos.mq.core.ConsumeResult;
import com.carlos.mq.core.MqMessage;
import com.carlos.mq.support.ConsumeMode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 订单消息消费者示例
 * <p>
 * 本类同时充当<b>消费侧用法文档</b>：覆盖 4 种处理器形状、显式控制消费状态、广播、并发，
 * 以及用于验证死信链路的「毒消息」消费者。
 * </p>
 * <p>
 * 关于原「标签过滤」的替代：标签过滤需要 MQ 服务端支持，当前传输无此能力，
 * 因此改为<b>一个事件类型一个主题</b>——这既能在服务端完成隔离，也避免「所有消费者都读到
 * 全部消息再各自丢弃」的低效做法。下方 {@code order-created/paid/shipped-topic} 即该模式的示例。
 * </p>
 *
 * @author Carlos
 * @date 2026/3/15
 */
@Component
@Slf4j
public class OrderMessageConsumer {

    /**
     * 形状一：只接收业务负载（推荐）
     * <p>框架按方法形参类型自动反序列化负载。并发 5 表示同组内 5 个消费者分摊消息。</p>
     */
    @MqListener(
        topic = "order-topic",
        group = "order-consumer-group",
        concurrency = 5
    )
    public void onOrderMessage(MqController.OrderMessage order) {
        log.info("【普通消费】收到订单消息，orderId: {}, userId: {}, amount: {}",
            order.getOrderId(), order.getUserId(), order.getAmount());
        processOrder(order);
    }

    /**
     * 形状二：负载 + 信封（需要投递次数等元信息时用）
     * <p>演示「一个事件类型一个主题」如何替代标签过滤。</p>
     */
    @MqListener(topic = "order-created-topic", group = "order-created-consumer-group")
    public void onOrderCreated(MqController.OrderMessage order, MqMessage envelope) {
        log.info("【订单创建】orderId: {}, messageId: {}, 第 {} 次投递",
            order.getOrderId(), envelope.messageId(), envelope.deliveryCount());
    }

    /**
     * 形状三：只接收信封（负载类型不定时用）
     */
    @MqListener(topic = "order-paid-topic", group = "order-paid-consumer-group")
    public void onOrderPaid(MqMessage envelope) {
        log.info("【订单支付】payloadType: {}, payload: {}",
            envelope.payloadType(), envelope.payload());
    }

    /**
     * 显式控制消费状态：返回 {@link ConsumeResult}
     * <p>
     * 可用于「明确的业务失败不重试，直接放弃并落死信」等场景。
     * 本例演示按订单号决定「成功」还是「放弃」。
     * </p>
     */
    @MqListener(topic = "order-shipped-topic", group = "order-shipped-consumer-group")
    public ConsumeResult onOrderShipped(MqController.OrderMessage order) {
        if (order.getOrderId() == null || order.getOrderId().isBlank()) {
            // 缺关键字段的消息重试也不会变好：直接放弃（会写入死信，便于排查）
            return ConsumeResult.drop(null, "order-shipped-topic", "缺少 orderId");
        }
        log.info("【订单发货】orderId: {}", order.getOrderId());
        return ConsumeResult.success(null, "order-shipped-topic");
    }

    /**
     * 延迟消息消费（用于订单超时处理）
     */
    @MqListener(topic = "order-delay-topic", group = "order-delay-consumer-group")
    public void onOrderDelayed(MqController.OrderMessage order) {
        log.info("【延迟消费】收到延迟订单消息，orderId: {}, status: {}",
            order.getOrderId(), order.getStatus());
        handleOrderTimeout(order);
    }

    /**
     * 广播消费：每个实例都会收到一份（框架为每实例使用独立消费组实现）
     */
    @MqListener(
        topic = "broadcast-topic",
        group = "broadcast-consumer-group",
        consumeMode = ConsumeMode.BROADCAST
    )
    public void onBroadcastMessage(String message) {
        log.info("【广播消费】收到广播消息: {}", message);
    }

    /**
     * 形状四：无参（仅用于探活/占位，业务极少使用）
     */
    @MqListener(topic = "tick-topic", group = "tick-consumer-group")
    public void onTick() {
        log.info("【无参消费】收到一次心跳消息");
    }

    /**
     * 毒消息消费者 —— <b>用于验证死信链路</b>
     * <p>
     * 处理必然抛异常，投递次数达上限后消息会转入死信 Stream。验证命令见
     * {@code docs/design/DESIGN-20260925-MQ技术选型.md} 与实施计划的验证章节。
     * </p>
     */
    @MqListener(topic = "poison-topic", group = "poison-group", maxDeliveryCount = 3)
    public void onPoisonMessage(String message) {
        throw new IllegalStateException("刻意抛出以便验证重投与死信：payload=" + message);
    }

    private void processOrder(MqController.OrderMessage order) {
        try {
            // 模拟业务处理耗时
            Thread.sleep(50);
            log.info("订单处理完成，orderId: {}", order.getOrderId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("订单处理被中断，orderId: {}", order.getOrderId());
        }
    }

    private void handleOrderTimeout(MqController.OrderMessage order) {
        log.info("处理订单超时，orderId: {}, status: {}", order.getOrderId(), order.getStatus());
    }
}
