package com.carlos.mq.spi;

import com.carlos.mq.core.MqCapability;
import com.carlos.mq.core.MqMessage;
import com.carlos.mq.core.MqPayloadCodec;
import com.carlos.mq.core.SendResult;
import com.carlos.mq.core.SendStatus;
import com.carlos.mq.support.MqType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 传输无关模板的发送链路测试
 * <p>
 * 覆盖契约收敛后的关键行为：主题/负载校验、序列化与体积失败、能力缺失时返回<b>明确的失败结果</b>
 * （而非静默降级或抛异常）、异步能力由模板提供。
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@DisplayName("DefaultMqTemplate 发送链路")
class DefaultMqTemplateTest {

    /** 仅支持基础发送、可切换是否支持延迟的传输桩 */
    private static class StubTransport implements MqTransport {

        private final boolean delayed;
        private final AtomicReference<MqMessage> lastSent = new AtomicReference<>();
        private boolean running;

        StubTransport(boolean delayed) {
            this.delayed = delayed;
        }

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
            return delayed ? Set.of(MqCapability.BASIC, MqCapability.DELAYED)
                : Set.of(MqCapability.BASIC);
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
            lastSent.set(message);
            return SendResult.success(message.messageId(), message.topic(), "1-1");
        }

        @Override
        public SendResult sendDelayed(MqMessage message, Duration delay) {
            lastSent.set(message);
            return SendResult.success(message.messageId(), message.topic(), "1-2");
        }
    }

    /** 只接受 String 负载的编解码桩 */
    private static final MqPayloadCodec STUB_CODEC = new MqPayloadCodec() {
        @Override
        public String serialize(Object payload) {
            if (payload instanceof String text) {
                return text;
            }
            throw new IllegalArgumentException("桩编解码只接受 String");
        }

        @Override
        public <T> T deserialize(String json, Class<T> type) {
            return null;
        }
    };

    private StubTransport transport;
    private DefaultMqTemplate template;

    @BeforeEach
    void setUp() {
        transport = new StubTransport(false);
        template = new DefaultMqTemplate(transport, STUB_CODEC);
    }

    @AfterEach
    void tearDown() {
        template.close();
    }

    @Test
    @DisplayName("主题不合法应返回 INVALID_ARGUMENT，且不触达传输")
    void should_reject_invalid_topic() {
        for (String bad : new String[]{null, "", "  ", "has space", "bad/slash", "a".repeat(201)}) {
            SendResult result = template.send(bad, "{}");
            assertEquals(SendStatus.INVALID_ARGUMENT, result.getStatus(), "应拒绝主题：" + bad);
        }
        assertNull(transport.lastSent.get());
    }

    @Test
    @DisplayName("负载为 null 应返回 INVALID_ARGUMENT")
    void should_reject_null_payload() {
        assertEquals(SendStatus.INVALID_ARGUMENT, template.send("order-topic", null).getStatus());
    }

    @Test
    @DisplayName("序列化失败应返回 SERIALIZATION_ERROR，而不是抛异常")
    void should_report_serialization_failure() {
        SendResult result = template.send("order-topic", new Object());

        assertEquals(SendStatus.SERIALIZATION_ERROR, result.getStatus());
        assertFalse(result.isSuccess());
    }

    @Test
    @DisplayName("负载超限应返回 PAYLOAD_TOO_LARGE")
    void should_report_payload_too_large() {
        DefaultMqTemplate small = new DefaultMqTemplate(transport, STUB_CODEC, 16);
        try {
            SendResult result = small.send("order-topic", "x".repeat(64));
            assertEquals(SendStatus.PAYLOAD_TOO_LARGE, result.getStatus());
        } finally {
            small.close();
        }
    }

    @Test
    @DisplayName("正常发送应透传主题并把 recordId 带回结果")
    void should_send_and_carry_record_id() {
        SendResult result = template.send("order-topic", "{\"orderId\":\"O1\"}");

        assertTrue(result.isSuccess());
        assertEquals("1-1", result.getRecordId());
        assertEquals("order-topic", transport.lastSent.get().topic());
        assertEquals("{\"orderId\":\"O1\"}", transport.lastSent.get().payload());
    }

    @Test
    @DisplayName("传输未声明 DELAYED 时 sendDelayed 应返回能力不支持，而非静默改为立即发送")
    void should_report_unsupported_delayed() {
        SendResult result = template.sendDelayed("order-topic", "{}", Duration.ofMinutes(5));

        assertEquals(SendStatus.CAPABILITY_NOT_SUPPORTED, result.getStatus());
        assertNull(transport.lastSent.get(), "不得退化为立即发送");
    }

    @Test
    @DisplayName("传输声明 DELAYED 时应委托延迟发送")
    void should_delegate_delayed_when_supported() {
        DefaultMqTemplate delayedTemplate = new DefaultMqTemplate(new StubTransport(true), STUB_CODEC);
        try {
            assertTrue(delayedTemplate.sendDelayed("order-topic", "{}", Duration.ofMinutes(5)).isSuccess());
        } finally {
            delayedTemplate.close();
        }
    }

    @Test
    @DisplayName("能力集应包含模板自身提供的 ASYNC_SEND（否则会出现实现了却报不支持）")
    void should_include_async_capability() {
        assertTrue(template.supports(MqCapability.ASYNC_SEND),
            "异步发送由模板提供，能力集合必须体现");
        assertFalse(transport.capabilities().contains(MqCapability.ASYNC_SEND),
            "传输本身并不提供异步发送");
    }

    @Test
    @DisplayName("异步发送应通过单一回调交付结果")
    void should_deliver_async_result() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<SendResult> captured = new AtomicReference<>();

        template.sendAsync("order-topic", "{}", result -> {
            captured.set(result);
            latch.countDown();
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS), "异步回调应在超时前完成");
        assertTrue(captured.get().isSuccess());
    }

    @Test
    @DisplayName("单向发送应委托传输且不抛异常")
    void should_delegate_one_way() {
        template.sendOneWay("order-topic", "{}");
        assertEquals("order-topic", transport.lastSent.get().topic());
    }

    @Test
    @DisplayName("类型与传输名应委托传输")
    void should_delegate_identity() {
        assertEquals(MqType.REDIS, template.getMqType());
        assertEquals("stub", template.getTransportName());
        assertFalse(template.ping());
    }
}
