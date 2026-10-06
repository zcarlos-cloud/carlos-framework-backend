package com.carlos.mq.client.redis;

import com.carlos.mq.core.MqMessage;
import com.carlos.mq.core.MqPayloadCodec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Stream 字段与信封双向映射测试
 * <p>
 * 生产端与消费端共用 {@link MqRecordMapper}，因此本测试锁定的是<b>两端的字段契约</b>：
 * 一旦有人只改一端，往返测试会立刻变红（这类漂移若漏到线上，表现为「消息收到了但字段全空」，
 * 排查成本极高）。
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@DisplayName("MqRecordMapper 字段契约")
class MqRecordMapperTest {

    /** 消息头用 JSON 序列化的桩编解码 */
    private static final MqPayloadCodec CODEC = new MqPayloadCodec() {
        @Override
        public String serialize(Object payload) {
            return "{\"source\":\"web\"}";
        }

        @Override
        public <T> T deserialize(String json, Class<T> type) {
            return null;
        }
    };

    @Test
    @DisplayName("往返后 messageId / key / payloadType / payload 应保持不变")
    void should_round_trip_core_fields() {
        MqMessage origin = new MqMessage("mid-1", "order-topic", "O1",
            "com.x.Order", "{\"orderId\":\"O1\"}", Map.of(), 1234L, 1, null, null, "trace-1");

        Map<String, String> fields = MqRecordMapper.toFields(origin, CODEC);
        MqMessage restored = MqRecordMapper.toMessage(fields, "order-topic");

        assertEquals(origin.messageId(), restored.messageId());
        assertEquals(origin.key(), restored.key());
        assertEquals(origin.payloadType(), restored.payloadType());
        assertEquals(origin.payload(), restored.payload());
        assertEquals(origin.traceId(), restored.traceId());
        assertEquals(origin.createTime(), restored.createTime());
    }

    @Test
    @DisplayName("负载应原样存于 data 字段，便于 redis-cli 直接查看")
    void should_store_payload_verbatim() {
        MqMessage origin = new MqMessage("mid-2", "t", null, null, "{\"a\":1}",
            Map.of(), 0L, 1, null, null, null);

        Map<String, String> fields = MqRecordMapper.toFields(origin, CODEC);

        assertEquals("{\"a\":1}", fields.get(MqRecordMapper.FIELD_DATA));
        assertEquals("mid-2", fields.get(MqRecordMapper.FIELD_MESSAGE_ID));
    }

    @Test
    @DisplayName("无可选字段时不应写入对应键（避免空串污染排查输出）")
    void should_omit_absent_optional_fields() {
        MqMessage origin = new MqMessage("mid-3", "t", null, null, "{}",
            Map.of(), 0L, 1, null, null, null);

        Map<String, String> fields = MqRecordMapper.toFields(origin, CODEC);

        assertEquals(false, fields.containsKey(MqRecordMapper.FIELD_KEY));
        assertEquals(false, fields.containsKey(MqRecordMapper.FIELD_PAYLOAD_TYPE));
        assertEquals(false, fields.containsKey(MqRecordMapper.FIELD_HEADERS));
    }

    @Test
    @DisplayName("有消息头时应序列化保存")
    void should_persist_headers_when_present() {
        MqMessage origin = new MqMessage("mid-4", "t", null, null, "{}",
            Map.of("source", "web"), 0L, 1, null, null, null);

        Map<String, String> fields = MqRecordMapper.toFields(origin, CODEC);

        assertTrue(fields.containsKey(MqRecordMapper.FIELD_HEADERS));
    }

    @Test
    @DisplayName("字段缺失或畸形时应安全降级，不抛异常")
    void should_degrade_safely_on_missing_fields() {
        MqMessage restored = MqRecordMapper.toMessage(
            Map.of(MqRecordMapper.FIELD_CREATE_TIME, "not-a-number"), "t");

        assertNull(restored.messageId());
        assertEquals(0L, restored.createTime());
        assertTrue(restored.headers().isEmpty());
    }
}
