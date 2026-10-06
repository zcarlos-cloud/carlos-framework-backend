package com.carlos.mq.client.redis;

import com.carlos.mq.config.MqProperties;
import com.carlos.mq.core.MqMessage;
import com.carlos.mq.core.MqSendOptions;
import com.carlos.mq.spi.JsonMqPayloadCodec;
import com.carlos.redis.stream.RedisStreamTemplate;
import com.carlos.redis.stream.StreamAddOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 延时到期处理器
 *
 * <h2>本测试守卫的核心缺陷</h2>
 * <p>
 * 到期条目此前<b>无法路由</b>：入队时只写了业务负载本体，<b>topic 丢失</b>，
 * 而桶与主题是多对多关系，仅凭 {@code onDue} 收到的桶名决定不了该写入哪个 Stream。
 * 因此本测试的重点是 <b>「按载荷里的主题路由，而不是按桶」</b>。
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@DisplayName("RedisMqDelayDueHandler 到期投递")
class RedisMqDelayDueHandlerTest {

    private static final String TOPIC = "order-delay-topic";
    private static final String STREAM_PREFIX = "framework:mq:stream:";
    private static final String STREAM_KEY = STREAM_PREFIX + TOPIC;

    private final RedisStreamTemplate streamTemplate = mock(RedisStreamTemplate.class);
    private final JsonMqPayloadCodec codec = new JsonMqPayloadCodec();

    private RedisMqDelayDueHandler handler;

    @BeforeEach
    void setUp() {
        handler = new RedisMqDelayDueHandler(streamTemplate, new MqTopics(STREAM_PREFIX),
            new MqProperties(), codec);
    }

    private static Map<String, String> fieldsOf() {
        return Map.of(
            MqRecordMapper.FIELD_DATA, "{\"orderId\":\"A1\"}",
            MqRecordMapper.FIELD_MESSAGE_ID, "m1",
            MqRecordMapper.FIELD_CREATE_TIME, "1700000000000");
    }

    private String encode(String topic, Map<String, String> fields) {
        return codec.serialize(new MqDelayPayload(topic, fields));
    }

    /* ==================== 正常路径 ==================== */

    @Test
    @DisplayName("按载荷中的主题投递到对应 Stream（而非按桶名）")
    void should_route_by_topic_from_payload() {
        Map<String, String> fields = fieldsOf();

        handler.onDue("normal", encode(TOPIC, fields));

        // 关键：目标 Stream 来自载荷中的 topic，与桶名 "normal" 无关
        verify(streamTemplate).add(STREAM_KEY, fields, null);
    }

    @Test
    @DisplayName("同一桶可承载不同主题，各自投递到各自的 Stream")
    void should_route_two_topics_in_same_bucket() {
        Map<String, String> first = fieldsOf();
        Map<String, String> second = Map.of(MqRecordMapper.FIELD_DATA, "{}");

        handler.onDue("normal", encode(TOPIC, first));
        handler.onDue("normal", encode("audit-topic", second));

        verify(streamTemplate).add(STREAM_KEY, first, null);
        verify(streamTemplate).add(STREAM_PREFIX + "audit-topic", second, null);
    }

    @Test
    @DisplayName("主题配置了长度上限时，写入选项随之生效")
    void should_honour_topic_max_length() {
        MqProperties.TopicConfig topicConfig = new MqProperties.TopicConfig();
        topicConfig.setMaxLength(20000L);
        MqProperties properties = new MqProperties();
        properties.setTopics(Map.of(TOPIC, topicConfig));
        handler = new RedisMqDelayDueHandler(streamTemplate, new MqTopics(STREAM_PREFIX),
            properties, codec);

        Map<String, String> fields = fieldsOf();
        handler.onDue("normal", encode(TOPIC, fields));

        verify(streamTemplate).add(STREAM_KEY, fields, StreamAddOptions.maxLen(20000L));
    }

    /* ==================== 失败路径：丢弃且不打断转移线程 ==================== */

    @Test
    @DisplayName("无法解析的条目被丢弃，不抛异常（否则会打断整个桶的转移线程）")
    void should_drop_unparseable_payload() {
        assertDoesNotThrow(() -> handler.onDue("normal", "{ 这不是合法 JSON"));

        verifyNoInteractions(streamTemplate);
    }

    @Test
    @DisplayName("缺少目标主题的条目被丢弃")
    void should_drop_payload_without_topic() {
        assertDoesNotThrow(() -> handler.onDue("normal", encode(null, fieldsOf())));

        verifyNoInteractions(streamTemplate);
    }

    @Test
    @DisplayName("投递失败被吞掉并记录，不向原语层抛出")
    void should_swallow_append_failure() {
        when(streamTemplate.add(any(), any(), any()))
            .thenThrow(new RuntimeException("Redis 不可达"));

        assertDoesNotThrow(() -> handler.onDue("normal", encode(TOPIC, fieldsOf())));
    }

    /* ==================== 载荷形状 ==================== */

    @Test
    @DisplayName("载荷 JSON 往返不丢字段（含 headers 这种嵌套 JSON 字符串）")
    void should_round_trip_through_codec() {
        MqMessage original = MqMessage.of(TOPIC, "{\"orderId\":\"A1\"}", "com.carlos.Order",
            MqSendOptions.ofKey("order-A1").withHeader("source", "order-service"));
        MqDelayPayload payload = new MqDelayPayload(TOPIC,
            MqRecordMapper.toFields(original, codec));

        MqDelayPayload decoded = codec.deserialize(codec.serialize(payload), MqDelayPayload.class);

        assertEquals(payload, decoded);
        assertEquals("order-A1", decoded.fields().get(MqRecordMapper.FIELD_KEY));
        assertEquals("com.carlos.Order", decoded.fields().get(MqRecordMapper.FIELD_PAYLOAD_TYPE));
    }
}
