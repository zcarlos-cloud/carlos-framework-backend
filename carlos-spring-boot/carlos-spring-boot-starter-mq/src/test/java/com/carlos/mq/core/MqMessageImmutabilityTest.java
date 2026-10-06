package com.carlos.mq.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 消息信封不可变性护栏测试
 * <p>
 * 项目的不可变性强制规范要求「始终创建新对象，严禁修改原对象」。旧实现
 * （{@code @Data} + {@code markSent()}/{@code markConsumed()}/{@code incrementRetry()}）
 * 违反了该规范。本测试把「新信封必须是不可变值对象」变成测试期可捕获的约束。
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@DisplayName("MqMessage 不可变性")
class MqMessageImmutabilityTest {

    @Test
    @DisplayName("应为 record 类型，且直接父类为 Record")
    void should_be_record_type() {
        assertTrue(MqMessage.class.isRecord(), "MqMessage 必须是 record");
        assertEquals(Record.class, MqMessage.class.getSuperclass());
    }

    @Test
    @DisplayName("不得存在任何 setter 或原地改状态的方法")
    void should_not_expose_mutators() {
        for (Method method : MqMessage.class.getDeclaredMethods()) {
            String name = method.getName();
            assertFalse(name.startsWith("set"),
                "不得存在 setter 方法：" + name);
            assertFalse(Stream.of("markSent", "markConsumed", "incrementRetry", "addHeader")
                    .anyMatch(name::equals),
                "不得存在原地改状态的方法：" + name);
        }
    }

    @Test
    @DisplayName("record 组件应为 final，且不存在可变公开字段")
    void should_have_final_components() {
        assertEquals(11, MqMessage.class.getRecordComponents().length,
            "record 组件数量应与设计一致（11 个）");
        for (var field : MqMessage.class.getDeclaredFields()) {
            if (!field.isSynthetic()) {
                assertTrue(Modifier.isFinal(field.getModifiers()),
                    "字段必须为 final：" + field.getName());
            }
        }
    }

    @Test
    @DisplayName("withXxx 应返回新实例而非修改原对象")
    void should_return_new_instance_when_with_xxx() {
        MqMessage origin = MqMessage.of("t", "{}", null, MqSendOptions.none());
        MqMessage changed = origin.withHeader("k", "v");

        assertNotSame(origin, changed);
        assertTrue(origin.headers().isEmpty(), "原对象的 headers 不应被修改");
        assertEquals("v", changed.header("k"));
    }

    @Test
    @DisplayName("headers 应被复制为不可变 Map")
    void should_copy_headers_to_immutable_map() {
        MqMessage message = new MqMessage("id", "t", null, null, "{}",
            Map.of("a", "1"), 0L, 1, null, null, null);
        assertThrows(UnsupportedOperationException.class,
            () -> message.headers().put("b", "2"),
            "headers 必须是不可变 Map");
    }

    @Test
    @DisplayName("of 应生成唯一 id 且投递次数初值为 1")
    void should_generate_unique_id_with_initial_delivery_count() {
        MqMessage first = MqMessage.of("t", "{}", null, MqSendOptions.none());
        MqMessage second = MqMessage.of("t", "{}", null, MqSendOptions.none());

        assertFalse(first.messageId().equals(second.messageId()), "messageId 应唯一");
        assertEquals(1, first.deliveryCount());
        assertFalse(first.isRedelivery());
        assertTrue(first.isRedelivery() == false && first.withDeliveryCount(2).isRedelivery(),
            "投递次数大于 1 时应判定为重投递");
    }

    @Test
    @DisplayName("isRetryExhausted 应在达到上限时返回 true")
    void should_report_retry_exhausted() {
        MqMessage message = MqMessage.of("t", "{}", null, MqSendOptions.none()).withDeliveryCount(3);
        assertTrue(message.isRetryExhausted(3));
        assertFalse(message.isRetryExhausted(4));
        assertFalse(message.isRetryExhausted(0), "上限非正数时不应判定为耗尽");
    }
}
