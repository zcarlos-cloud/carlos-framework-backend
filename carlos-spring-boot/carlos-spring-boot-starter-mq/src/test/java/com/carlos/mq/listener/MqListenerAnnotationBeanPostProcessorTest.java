package com.carlos.mq.listener;

import com.carlos.mq.annotation.MqListener;
import com.carlos.mq.core.ConsumeResult;
import com.carlos.mq.core.MqMessage;
import com.carlos.mq.exception.MqException;
import com.carlos.mq.support.ConsumeMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code @MqListener} 注解处理器测试
 * <p>
 * <b>本测试守护的是本次重构补上的最大缺口</b>：在此之前该注解没有任何处理器，
 * 写成注解的方法永远不会被调用，且不会报错——「静默失效」的最坏形态。
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@DisplayName("MqListener 注解处理")
class MqListenerAnnotationBeanPostProcessorTest {

    public static class Order {
    }

    /** 覆盖 4 种形状与多主题 */
    public static class GoodHandlers {

        @MqListener(topic = "a", group = "g1")
        public void payload(Order order) {
        }

        @MqListener(topic = "b", group = "g2")
        public void payloadWithMessage(Order order, MqMessage envelope) {
        }

        @MqListener(topic = "c", group = "g3")
        public void messageOnly(MqMessage envelope) {
        }

        @MqListener(topic = "d", group = "g4")
        public void noArgs() {
        }

        @MqListener(topic = {"e1", "e2"}, group = "g5", concurrency = 3, batchSize = 5,
            consumeMode = ConsumeMode.BROADCAST)
        public ConsumeResult multiTopic(Order order) {
            return null;
        }

        @MqListener(topic = "f")
        public void defaultGroup(Order order) {
        }
    }

    /** 返回类型不合法 */
    public static class BadReturnHandler {

        @MqListener(topic = "x")
        public String bad(Order order) {
            return "x";
        }
    }

    /** 参数个数超限 */
    public static class TooManyParamsHandler {

        @MqListener(topic = "y")
        public void bad(Order order, String extra, int number) {
        }
    }

    /** 双参形式的第二个参数不是信封 */
    public static class BadSecondParamHandler {

        @MqListener(topic = "z")
        public void bad(Order order, String extra) {
        }
    }

    /** 重复占用同一 (topic, group) */
    public static class DuplicateA {

        @MqListener(topic = "dup", group = "same")
        public void a(Order order) {
        }
    }

    public static class DuplicateB {

        @MqListener(topic = "dup", group = "same")
        public void b(Order order) {
        }
    }

    private MqListenerRegistry runProcessor(Object bean) {
        MqListenerRegistry registry = new MqListenerRegistry();
        new MqListenerAnnotationBeanPostProcessor(registry)
            .postProcessAfterInitialization(bean, bean.getClass().getSimpleName());
        return registry;
    }

    @Test
    @DisplayName("四种处理器形状都应被识别，多主题应展开为多个端点")
    void should_recognize_all_shapes_and_expand_topics() {
        MqListenerRegistry registry = runProcessor(new GoodHandlers());

        // 4 种形状各一 + 多主题 2 个 + 默认消费组 1 个 = 7
        assertEquals(7, registry.size());

        assertEquals(MqListenerEndpoint.HandlerShape.PAYLOAD,
            registry.find("a", "g1").orElseThrow().shape());
        assertEquals(MqListenerEndpoint.HandlerShape.PAYLOAD_WITH_MESSAGE,
            registry.find("b", "g2").orElseThrow().shape());
        assertEquals(MqListenerEndpoint.HandlerShape.MESSAGE,
            registry.find("c", "g3").orElseThrow().shape());
        assertEquals(MqListenerEndpoint.HandlerShape.NO_ARGS,
            registry.find("d", "g4").orElseThrow().shape());

        assertTrue(registry.find("e1", "g5").isPresent());
        assertTrue(registry.find("e2", "g5").isPresent());
        MqListenerEndpoint broadcast = registry.find("e1", "g5").orElseThrow();
        assertEquals(3, broadcast.concurrency());
        assertEquals(5, broadcast.batchSize());
        assertEquals(ConsumeMode.BROADCAST, broadcast.consumeMode());
    }

    @Test
    @DisplayName("未指定消费组时应按 {topic}:default 派生")
    void should_derive_default_group() {
        MqListenerRegistry registry = runProcessor(new GoodHandlers());

        assertTrue(registry.find("f", "f:default").isPresent(),
            "缺省消费组应按主题派生，而不是留空导致建组失败");
    }

    @Test
    @DisplayName("返回类型不合法应在注册期失败")
    void should_reject_invalid_return_type() {
        assertThrows(MqException.class, () -> runProcessor(new BadReturnHandler()));
    }

    @Test
    @DisplayName("参数个数超限应在注册期失败")
    void should_reject_too_many_params() {
        assertThrows(MqException.class, () -> runProcessor(new TooManyParamsHandler()));
    }

    @Test
    @DisplayName("双参形式第二参必须是 MqMessage")
    void should_reject_invalid_second_param() {
        assertThrows(MqException.class, () -> runProcessor(new BadSecondParamHandler()));
    }

    @Test
    @DisplayName("同一 (topic, group) 重复注册应失败，而不是运行期随机选一个")
    void should_reject_duplicate_subscription() {
        MqListenerRegistry registry = runProcessor(new DuplicateA());

        assertThrows(MqException.class, () ->
            new MqListenerAnnotationBeanPostProcessor(registry)
                .postProcessAfterInitialization(new DuplicateB(), "duplicateB"));
    }

    @Test
    @DisplayName("无注解的方法不应被注册")
    void should_ignore_methods_without_annotation() {
        MqListenerRegistry registry = runProcessor(new Object());
        List<MqListenerEndpoint> endpoints = registry.all();

        assertTrue(endpoints.isEmpty());
    }
}
