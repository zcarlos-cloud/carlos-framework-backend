package com.carlos.mq.config;

import com.carlos.mq.core.MqCapability;
import com.carlos.mq.core.MqMessage;
import com.carlos.mq.core.MqSendCallback;
import com.carlos.mq.core.SendResult;
import com.carlos.mq.exception.MqException;
import com.carlos.mq.listener.MqListenerEndpoint;
import com.carlos.mq.spi.MqSubscription;
import com.carlos.mq.spi.MqTransport;
import com.carlos.mq.support.ConsumeMode;
import com.carlos.mq.support.MqType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 启动期能力校验测试
 * <p>
 * 守护「不得静默降级」在消费侧的落地：若端点声明的组合（批量/广播/死信）与传输能力不匹配，
 * 应在<b>启动期</b>失败，而不是等到运行到某条消息才发现语义悄悄失效。
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@DisplayName("MqCapabilityValidator 启动期能力校验")
class MqCapabilityValidatorTest {

    public static class Handlers {

        public void handle(String payload) {
        }
    }

    /** 只声明指定能力的传输桩 */
    private static MqTransport transportWith(Set<MqCapability> capabilities) {
        return new MqTransport() {
            @Override
            public MqType getType() {
                return MqType.REDIS;
            }

            @Override
            public String getName() {
                return "stub";
            }

            @Override
            public Set<MqCapability> capabilities() {
                return capabilities;
            }

            @Override
            public void start() {
            }

            @Override
            public void stop() {
            }

            @Override
            public boolean isRunning() {
                return true;
            }

            @Override
            public boolean ping() {
                return true;
            }

            @Override
            public SendResult send(MqMessage message) {
                return SendResult.success(message.messageId(), message.topic(), null);
            }

            @Override
            public void subscribe(MqSubscription subscription) {
            }
        };
    }

    private MqListenerEndpoint endpoint(int batchSize, ConsumeMode mode, int maxDeliveryCount) {
        Method method = Handlers.class.getDeclaredMethods()[0];
        return new MqListenerEndpoint("handlers", new Handlers(), method, "t", "g", null,
            mode, 1, batchSize, maxDeliveryCount, MqListenerEndpoint.HandlerShape.PAYLOAD,
            String.class);
    }

    private MqProperties properties(boolean failFast) {
        MqProperties properties = new MqProperties();
        properties.getCapability().setFailFast(failFast);
        return properties;
    }

    @Test
    @DisplayName("能力齐备时应通过")
    void should_pass_when_capabilities_match() {
        MqCapabilityValidator validator = new MqCapabilityValidator(properties(true));
        MqTransport transport = transportWith(Set.of(MqCapability.CONSUME, MqCapability.BATCH_CONSUME,
            MqCapability.BROADCAST, MqCapability.DEAD_LETTER, MqCapability.PENDING_CLAIM));

        assertDoesNotThrow(() -> validator.validate(transport,
            List.of(endpoint(5, ConsumeMode.BROADCAST, 3))));
    }

    @Test
    @DisplayName("批量消费能力缺失时应启动失败")
    void should_fail_when_batch_capability_missing() {
        MqCapabilityValidator validator = new MqCapabilityValidator(properties(true));
        MqTransport transport = transportWith(Set.of(MqCapability.CONSUME));

        assertThrows(MqException.class, () -> validator.validate(transport,
            List.of(endpoint(5, ConsumeMode.CLUSTER, 3))));
    }

    @Test
    @DisplayName("开启 fail-fast 关闭时只记日志、不阻断启动")
    void should_not_fail_when_fail_fast_disabled() {
        MqCapabilityValidator validator = new MqCapabilityValidator(properties(false));
        MqTransport transport = transportWith(Set.of(MqCapability.CONSUME));

        assertDoesNotThrow(() -> validator.validate(transport,
            List.of(endpoint(5, ConsumeMode.CLUSTER, 3))));
    }

    @Test
    @DisplayName("无端点时不做任何校验")
    void should_skip_when_no_endpoints() {
        MqCapabilityValidator validator = new MqCapabilityValidator(properties(true));

        assertDoesNotThrow(() -> validator.validate(transportWith(Set.of()), List.of()));
    }

    @Test
    @DisplayName("投递上限为 0（不重投）时不应要求死信能力")
    void should_not_require_dead_letter_when_retry_disabled() {
        MqCapabilityValidator validator = new MqCapabilityValidator(properties(true));
        MqTransport transport = transportWith(Set.of(MqCapability.CONSUME));

        assertDoesNotThrow(() -> validator.validate(transport,
            List.of(endpoint(1, ConsumeMode.CLUSTER, 0))));
    }
}
