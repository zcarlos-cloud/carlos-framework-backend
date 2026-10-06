package com.carlos.mq.client.redis;

import com.carlos.json.JsonUtils;
import com.carlos.mq.core.MqMessage;
import com.carlos.mq.core.MqPayloadCodec;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stream 条目与消息信封的双向映射
 * <p>
 * <b>生产端与消费端共用本类</b>，以免两端字段名/结构各自演进导致漂移（这类漂移极难排查）。
 * </p>
 * <p>
 * 字段刻意<b>拆开存</b>而不是把整个信封塞进一个 JSON 字段：这样 {@code redis-cli XRANGE}
 * 能直接读出 messageId / payloadType，排查时无需二次解析；也避免「JSON 里套 JSON」的转义字符串。
 * 业务负载本体仍以 JSON 字符串原样存于 {@code data}。
 * </p>
 * <p>
 * 字段值全部为字符串（字段名与值均用字符串序列化器），不挂
 * {@code ConfigurableRedisSerializer}——后者默认 Jackson，会把 Long 等标量写成带引号字符串。
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
public final class MqRecordMapper {

    /** 业务负载（JSON 字符串） */
    public static final String FIELD_DATA = "data";

    /** 消息唯一 id */
    public static final String FIELD_MESSAGE_ID = "messageId";

    /** 业务键（可空） */
    public static final String FIELD_KEY = "key";

    /** 负载类型全限定名（可空） */
    public static final String FIELD_PAYLOAD_TYPE = "payloadType";

    /** 生产端创建时刻（epoch millis） */
    public static final String FIELD_CREATE_TIME = "createTime";

    /** 自定义消息头（JSON；无头时不写该字段） */
    public static final String FIELD_HEADERS = "headers";

    /** 链路追踪 id（可空） */
    public static final String FIELD_TRACE_ID = "traceId";

    private MqRecordMapper() {
    }

    /**
     * 信封 → Stream 字段
     *
     * @param message 信封
     * @param codec   负载编解码（用于序列化消息头 Map）
     * @return 字段映射
     */
    public static Map<String, String> toFields(MqMessage message, MqPayloadCodec codec) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put(FIELD_DATA, message.payload());
        fields.put(FIELD_MESSAGE_ID, message.messageId());
        fields.put(FIELD_CREATE_TIME, String.valueOf(message.createTime()));
        if (message.key() != null) {
            fields.put(FIELD_KEY, message.key());
        }
        if (message.payloadType() != null) {
            fields.put(FIELD_PAYLOAD_TYPE, message.payloadType());
        }
        if (message.traceId() != null) {
            fields.put(FIELD_TRACE_ID, message.traceId());
        }
        if (!message.headers().isEmpty()) {
            fields.put(FIELD_HEADERS, codec.serialize(message.headers()));
        }
        return fields;
    }

    /**
     * Stream 字段 → 信封（投递元信息由调用方补齐）
     *
     * @param fields 字段映射
     * @param topic  主题（字段中不含主题，由订阅方提供）
     * @return 信封
     */
    public static MqMessage toMessage(Map<String, String> fields, String topic) {
        return new MqMessage(
            fields.get(FIELD_MESSAGE_ID),
            topic,
            fields.get(FIELD_KEY),
            fields.get(FIELD_PAYLOAD_TYPE),
            fields.get(FIELD_DATA),
            parseHeaders(fields.get(FIELD_HEADERS)),
            parseLong(fields.get(FIELD_CREATE_TIME)),
            1,
            null,
            null,
            fields.get(FIELD_TRACE_ID));
    }

    private static Map<String, String> parseHeaders(String headersJson) {
        if (headersJson == null || headersJson.isBlank()) {
            return Map.of();
        }
        try {
            Map<String, String> parsed = JsonUtils.fromJson(headersJson, Map.class);
            return parsed == null ? Map.of() : parsed;
        } catch (Exception e) {
            // 头信息解析失败不应导致消息不可消费（负载本体仍在），降级为空头
            return Map.of();
        }
    }

    private static long parseLong(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
