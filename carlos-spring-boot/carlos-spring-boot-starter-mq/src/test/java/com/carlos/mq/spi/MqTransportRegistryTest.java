package com.carlos.mq.spi;

import com.carlos.mq.core.MqCapability;
import com.carlos.mq.core.MqMessage;
import com.carlos.mq.core.SendResult;
import com.carlos.mq.exception.MqException;
import com.carlos.mq.support.MqType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MQ 传输注册表测试
 * <p>
 * 验证选择规则的<b>确定性</b>：旧实现遍历 {@code ConcurrentHashMap} 决定优先级，
 * 顺序无保证且与另一套探测逻辑互相矛盾。新实现的规则必须可预测、可断言。
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@DisplayName("MqTransportRegistry 选择规则")
class MqTransportRegistryTest {

    /** 最小可用的测试传输 */
    private static class StubTransport implements MqTransport {

        private final MqType type;

        StubTransport(MqType type) {
            this.type = type;
        }

        @Override
        public MqType getType() {
            return type;
        }

        @Override
        public String getName() {
            return "stub-" + type.getCode();
        }

        @Override
        public Set<MqCapability> capabilities() {
            return Set.of(MqCapability.BASIC);
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
    }

    private final StubTransport rocketmq = new StubTransport(MqType.ROCKETMQ);
    private final StubTransport kafka = new StubTransport(MqType.KAFKA);

    @Test
    @DisplayName("显式指定类型时应选中对应实现")
    void should_pick_explicit_type() {
        MqTransportRegistry registry = new MqTransportRegistry(List.of(rocketmq, kafka), MqType.KAFKA);
        assertEquals(MqType.KAFKA, registry.current().getType());
    }

    @Test
    @DisplayName("AUTO 且仅一个实现时应自动选中")
    void should_auto_pick_single_candidate() {
        MqTransportRegistry registry = new MqTransportRegistry(List.of(rocketmq), MqType.AUTO);
        assertEquals(MqType.ROCKETMQ, registry.current().getType());
    }

    @Test
    @DisplayName("AUTO 且有多个实现时应报错并提示显式配置，而不是猜一个")
    void should_fail_fast_on_ambiguous_auto() {
        MqException ex = assertThrows(MqException.class,
            () -> new MqTransportRegistry(List.of(rocketmq, kafka), MqType.AUTO));

        assertEquals("50011", ex.getErrorCode());
        assertTrue(ex.getMessage().contains("显式配置"), "错误信息应提示如何解决");
    }

    @Test
    @DisplayName("显式指定了不存在的类型时应报错并列出已注册类型")
    void should_fail_when_explicit_type_missing() {
        MqException ex = assertThrows(MqException.class,
            () -> new MqTransportRegistry(List.of(rocketmq), MqType.RABBITMQ));

        assertEquals("50011", ex.getErrorCode());
        assertTrue(ex.getMessage().contains("rabbitmq"));
    }

    @Test
    @DisplayName("无任何实现时 currentOrEmpty 为空，current 报错")
    void should_be_empty_when_no_transport() {
        MqTransportRegistry registry = new MqTransportRegistry(List.of(), MqType.AUTO);

        assertTrue(registry.isEmpty());
        assertFalse(registry.currentOrEmpty().isPresent());
        assertEquals("50011", assertThrows(MqException.class, registry::current).getErrorCode());
    }

    @Test
    @DisplayName("显式配置 none 表示不启用传输：当前传输为空，且不视为配置错误")
    void should_resolve_empty_when_type_is_none() {
        MqTransportRegistry registry = new MqTransportRegistry(List.of(rocketmq), MqType.NONE);

        assertFalse(registry.currentOrEmpty().isPresent(),
            "type=none 时应无当前传输，由上层装配 NoOp 模板");
        assertEquals("50011", assertThrows(MqException.class, registry::current).getErrorCode());
    }

    @Test
    @DisplayName("查找与类型集合应反映已注册实现")
    void should_expose_registered_transports() {
        MqTransportRegistry registry = new MqTransportRegistry(List.of(rocketmq, kafka), MqType.ROCKETMQ);

        assertTrue(registry.find(MqType.KAFKA).isPresent());
        assertFalse(registry.find(MqType.RABBITMQ).isPresent());
        assertEquals(Set.of(MqType.ROCKETMQ, MqType.KAFKA), registry.availableTypes());
        assertEquals(2, registry.all().size());
    }
}
