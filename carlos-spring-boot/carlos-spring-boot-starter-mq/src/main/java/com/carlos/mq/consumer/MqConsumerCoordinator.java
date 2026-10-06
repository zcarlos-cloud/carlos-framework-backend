package com.carlos.mq.consumer;

import com.carlos.mq.client.redis.RedisMqPendingReclaimer;
import com.carlos.mq.config.MqCapabilityValidator;
import com.carlos.mq.config.MqProperties;
import com.carlos.mq.listener.MqHandlerMethodAdapter;
import com.carlos.mq.listener.MqListenerEndpoint;
import com.carlos.mq.listener.MqListenerRegistry;
import com.carlos.mq.spi.MqSubscription;
import com.carlos.mq.spi.MqTransport;
import com.carlos.mq.spi.MqTransportRegistry;
import com.carlos.mq.support.ConsumeMode;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;

import java.util.List;

/**
 * 消费侧协调器
 * <p>
 * 启动时把 {@code @MqListener} 端点翻译为 SPI 的 {@link MqSubscription}，
 * 先做<b>能力校验</b>（不匹配即启动失败），再交给传输订阅、并启动待处理认领器。
 * </p>
 * <p>
 * 消费组只在此处计算一次（含广播模式的实例隔离），订阅与认领共用同一份描述，
 * 避免两处各自计算导致消费组不一致——那种不一致会表现为「重投永远不发生」这类极难定位的问题。
 * </p>
 * <p>
 * 由 {@code @MqListener} 注解驱动的订阅<b>不经由</b>业务代码，因此接入新传输时消费侧同样零改动。
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class MqConsumerCoordinator {

    private final MqListenerRegistry listenerRegistry;
    private final MqHandlerMethodAdapter handlerAdapter;
    private final MqConsumerNameFactory nameFactory;
    private final MqCapabilityValidator capabilityValidator;
    private final MqProperties properties;
    private final MqTransportRegistry transportRegistry;
    private final RedisMqPendingReclaimer reclaimer;

    /**
     * 应用就绪后：能力校验 → 订阅 → 启动认领
     * <p>@Order(10)：晚于传输启动（{@code MqTransportLifecycle} 为 0）。</p>
     */
    @EventListener(ApplicationReadyEvent.class)
    @Order(10)
    public void onReady() {
        List<MqListenerEndpoint> endpoints = listenerRegistry.all();
        if (endpoints.isEmpty()) {
            log.info("未发现 @MqListener 端点，消费侧保持空闲");
            return;
        }
        MqTransport transport = transportRegistry.current();
        // 能力校验放在订阅之前：让「传输不支持该组合」变成一次明确的启动失败
        capabilityValidator.validate(transport, endpoints);

        List<MqSubscription> subscriptions = endpoints.stream().map(this::toSubscription).toList();
        for (MqSubscription subscription : subscriptions) {
            transport.subscribe(subscription);
        }
        reclaimer.start(subscriptions);
        log.info("MQ 消费侧已就绪：端点={}, 订阅={}", endpoints.size(),
            subscriptions.stream().map(MqSubscription::display).toList());
    }

    /**
     * 关闭消费与认领（先停认领，避免关闭过程中仍在重投）
     */
    @PreDestroy
    public void onShutdown() {
        reclaimer.stop();
    }

    /**
     * 端点 → 订阅描述
     * <p>
     * 两处解析在此完成，从而下游（传输、认领器）拿到的都是具体值：
     * 广播模式的消费组隔离、以及「投递上限为负表示用全局配置」。
     * </p>
     */
    private MqSubscription toSubscription(MqListenerEndpoint endpoint) {
        String group = endpoint.consumeMode() == ConsumeMode.BROADCAST
            ? endpoint.group() + ":bcast:" + nameFactory.base()
            : endpoint.group();
        int maxDelivery = endpoint.maxDeliveryCount() > 0
            ? endpoint.maxDeliveryCount()
            : properties.getConsumer().getMaxDeliveryCount();
        return new MqSubscription(
            endpoint.topic(),
            group,
            endpoint.consumerId(),
            endpoint.concurrency(),
            endpoint.batchSize(),
            maxDelivery,
            envelope -> handlerAdapter.handle(endpoint, envelope));
    }
}
