package com.carlos.mq.spi;

import com.carlos.mq.core.MqCapability;
import com.carlos.mq.core.MqMessage;
import com.carlos.mq.core.SendResult;
import com.carlos.mq.exception.MqCapabilityNotSupportedException;
import com.carlos.mq.support.MqType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MQ 能力门控测试
 * <p>
 * 验证「不得静默降级」这一硬约束：未声明的能力被调用时必须
 * <b>显式抛出</b> {@link MqCapabilityNotSupportedException}（错误码 {@code 20002}），
 * 而不是悄悄退化成语义不同的操作并返回成功。
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@DisplayName("MqTransport 能力门控")
class MqCapabilityTest {

    /** 只发不收、仅支持基础发送的最小实现，用于验证默认方法的能力门控 */
    private static class BasicOnlyTransport implements MqTransport {

        private boolean running;

        @Override
        public MqType getType() {
            return MqType.ROCKETMQ;
        }

        @Override
        public String getName() {
            return "basic-only";
        }

        @Override
        public Set<MqCapability> capabilities() {
            return Set.of(MqCapability.BASIC);
        }

        @Override
        public void start() {
            running = true;
        }

        @Override
        public void stop() {
            running = false;
        }

        @Override
        public boolean isRunning() {
            return running;
        }

        @Override
        public boolean ping() {
            return running;
        }

        @Override
        public SendResult send(MqMessage message) {
            return SendResult.success(message.messageId(), message.topic(), null);
        }
    }

    private final MqTransport transport = new BasicOnlyTransport();
    private final MqMessage message = MqMessage.of("t", "{}", null, null);

    @Test
    @DisplayName("未声明 DELAYED 时 sendDelayed 应显式报错，而非退化为立即发送")
    void should_reject_delayed_when_not_declared() {
        MqCapabilityNotSupportedException ex = assertThrows(
            MqCapabilityNotSupportedException.class,
            () -> transport.sendDelayed(message, Duration.ofSeconds(5)));

        assertEquals("20002", ex.getErrorCode());
        assertEquals(MqCapability.DELAYED, ex.getCapability());
        assertEquals(MqType.ROCKETMQ, ex.getTransportType());
    }

    @Test
    @DisplayName("未声明 ASYNC_SEND 时 sendAsync 应显式报错")
    void should_reject_async_when_not_declared() {
        MqCapabilityNotSupportedException ex = assertThrows(
            MqCapabilityNotSupportedException.class,
            () -> transport.sendAsync(message, result -> {
                // 不应被调用
            }));

        assertEquals(MqCapability.ASYNC_SEND, ex.getCapability());
    }

    @Test
    @DisplayName("未声明 CONSUME 时 subscribe 应报 consume 不支持，而不是自相矛盾的 BASIC")
    void should_reject_consume_with_dedicated_capability() {
        MqCapabilityNotSupportedException ex = assertThrows(
            MqCapabilityNotSupportedException.class,
            () -> transport.subscribe(new MqSubscription("t", "g", null, 1, 1, 3, m -> null)));

        assertEquals(MqCapability.CONSUME, ex.getCapability(),
            "只声明 BASIC 的实现，其 subscribe 应如实报 consume 不支持");
        assertTrue(transport.supports(MqCapability.BASIC));
    }

    @Test
    @DisplayName("sendOneWay 默认实现应委托给 send（语义诚实：发送但不等待结果）")
    void should_delegate_send_one_way_to_send() {
        transport.sendOneWay(message);
        // 默认实现即「发送并忽略结果」，此处只验证不抛异常且能力未被误报
        assertFalse(transport.supports(MqCapability.DELAYED));
    }

    @Test
    @DisplayName("requireCapability 应在能力缺失时抛错，支持时放行")
    void should_assert_capability() {
        assertThrows(MqCapabilityNotSupportedException.class,
            () -> transport.requireCapability(MqCapability.TRANSACTIONAL));

        MqTransport same = transport;
        transport.requireCapability(MqCapability.BASIC);
        assertSame(same, transport);
    }

    @Test
    @DisplayName("capabilities 声明的能力集合应被 supports 一致反映")
    void should_reflect_capabilities() {
        assertTrue(transport.supports(MqCapability.BASIC));
        for (MqCapability capability : MqCapability.values()) {
            assertEquals(transport.capabilities().contains(capability),
                transport.supports(capability),
                "supports 与 capabilities 必须一致：" + capability);
        }
    }
}
