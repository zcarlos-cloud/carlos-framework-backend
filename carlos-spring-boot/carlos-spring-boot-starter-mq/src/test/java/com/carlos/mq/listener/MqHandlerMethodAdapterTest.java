package com.carlos.mq.listener;

import com.carlos.mq.core.ConsumeResult;
import com.carlos.mq.core.MqMessage;
import com.carlos.mq.core.MqPayloadCodec;
import com.carlos.mq.support.ConsumeMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 处理器方法适配器测试
 * <p>
 * <b>这是「不丢消息」的核心语义护栏</b>：
 * </p>
 * <ul>
 *   <li>业务抛异常 → {@code RETRY}（<b>不确认</b>）：因为「代码缺陷」与「下游暂时不可用」
 *       在框架视角无法区分，保守选择是不丢。若此处退化为「确认」，消息会被静默丢弃。</li>
 *   <li>负载反序列化失败 → {@code DROP}（确认并落死信）：这类消息无法被任何重试修复，
 *       留在待处理队列只会阻塞后续消息。</li>
 * </ul>
 *
 * @author Carlos
 * @since 3.0.0
 */
@DisplayName("MqHandlerMethodAdapter 消费语义")
class MqHandlerMethodAdapterTest {

    /** 测试用负载类型（触发走编解码器） */
    public static class Order {
        public String orderId;
    }

    /** 测试用处理器宿主 */
    public static class Handlers {

        public void ok(Order order) {
            // 正常处理
        }

        public void boom(Order order) {
            throw new IllegalStateException("业务处理失败");
        }

        public ConsumeResult dropIt(Order order) {
            return ConsumeResult.drop(null, "t", "不可修复");
        }

        public void envelopeOnly(MqMessage envelope) {
            // 只接收信封
        }

        public void noArgs() {
            // 无参
        }
    }

    /** 反序列化必然失败的编解码桩 */
    private static final MqPayloadCodec FAILING_CODEC = new MqPayloadCodec() {
        @Override
        public String serialize(Object payload) {
            return "{}";
        }

        @Override
        public <T> T deserialize(String json, Class<T> type) {
            throw new IllegalArgumentException("桩编解码无法解析：" + type.getName());
        }
    };

    /** 反序列化成功的编解码桩（返回空对象即可） */
    private static final MqPayloadCodec OK_CODEC = new MqPayloadCodec() {
        @Override
        public String serialize(Object payload) {
            return "{}";
        }

        @Override
        public <T> T deserialize(String json, Class<T> type) {
            try {
                return type.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new IllegalArgumentException("无法构造：" + type.getName(), e);
            }
        }
    };

    private MqListenerEndpoint endpointOf(MqPayloadCodec codec, String methodName,
                                          Class<?> paramType,
                                          MqListenerEndpoint.HandlerShape shape) {
        Method method = resolve(methodName);
        return new MqListenerEndpoint("handlers", new Handlers(), method, "t", "g", null,
            ConsumeMode.CLUSTER, 1, 1, 3, shape, paramType);
    }

    private static Method resolve(String name) {
        for (Method method : Handlers.class.getDeclaredMethods()) {
            if (method.getName().equals(name)) {
                return method;
            }
        }
        throw new IllegalStateException("找不到测试方法：" + name);
    }

    private MqMessage message() {
        return MqMessage.of("t", "{\"orderId\":\"O1\"}", Order.class.getName(), null);
    }

    @Test
    @DisplayName("正常返回 void 应视为成功并确认")
    void should_succeed_on_void_return() {
        MqHandlerMethodAdapter adapter = new MqHandlerMethodAdapter(OK_CODEC);
        ConsumeResult result = adapter.handle(
            endpointOf(OK_CODEC, "ok", Order.class, MqListenerEndpoint.HandlerShape.PAYLOAD),
            message());

        assertEquals(ConsumeResult.ConsumeStatus.SUCCESS, result.getStatus());
        assertTrue(result.shouldAck(), "成功必须确认");
    }

    @Test
    @DisplayName("业务抛异常应转重试且不确认（绝不静默丢弃）")
    void should_retry_on_business_exception() {
        MqHandlerMethodAdapter adapter = new MqHandlerMethodAdapter(OK_CODEC);
        ConsumeResult result = adapter.handle(
            endpointOf(OK_CODEC, "boom", Order.class, MqListenerEndpoint.HandlerShape.PAYLOAD),
            message());

        assertEquals(ConsumeResult.ConsumeStatus.RETRY, result.getStatus());
        assertFalse(result.shouldAck(), "业务异常必须不确认，留给认领器重投");
    }

    @Test
    @DisplayName("反序列化失败应转放弃（确认 + 落死信），因为重试无法修复")
    void should_drop_on_deserialization_failure() {
        MqHandlerMethodAdapter adapter = new MqHandlerMethodAdapter(FAILING_CODEC);
        ConsumeResult result = adapter.handle(
            endpointOf(FAILING_CODEC, "ok", Order.class, MqListenerEndpoint.HandlerShape.PAYLOAD),
            message());

        assertEquals(ConsumeResult.ConsumeStatus.DROP, result.getStatus());
        assertTrue(result.shouldAck(), "反序列化失败必须确认，否则会永久阻塞待处理队列");
        assertEquals("20007", result.getErrorCode(), "应带反序列化失败错误码");
    }

    @Test
    @DisplayName("处理器显式返回放弃时应被尊重")
    void should_honor_explicit_drop() {
        MqHandlerMethodAdapter adapter = new MqHandlerMethodAdapter(OK_CODEC);
        ConsumeResult result = adapter.handle(
            endpointOf(OK_CODEC, "dropIt", Order.class, MqListenerEndpoint.HandlerShape.PAYLOAD),
            message());

        assertEquals(ConsumeResult.ConsumeStatus.DROP, result.getStatus());
        assertTrue(result.shouldAck());
    }

    @Test
    @DisplayName("只接收信封的形状应直接拿到信封，不触发反序列化")
    void should_pass_envelope_without_deserialization() {
        MqHandlerMethodAdapter adapter = new MqHandlerMethodAdapter(FAILING_CODEC);
        ConsumeResult result = adapter.handle(
            endpointOf(FAILING_CODEC, "envelopeOnly", MqMessage.class,
                MqListenerEndpoint.HandlerShape.MESSAGE),
            message());

        assertEquals(ConsumeResult.ConsumeStatus.SUCCESS, result.getStatus(),
            "信封形状不应因编解码器失败而放弃");
    }

    @Test
    @DisplayName("无参形状应正常调用")
    void should_support_no_args_shape() {
        MqHandlerMethodAdapter adapter = new MqHandlerMethodAdapter(FAILING_CODEC);
        ConsumeResult result = adapter.handle(
            endpointOf(FAILING_CODEC, "noArgs", null, MqListenerEndpoint.HandlerShape.NO_ARGS),
            message());

        assertEquals(ConsumeResult.ConsumeStatus.SUCCESS, result.getStatus());
    }
}
