package com.carlos.mq.spi;

import com.carlos.json.JsonUtils;
import com.carlos.mq.core.MqPayloadCodec;

/**
 * 基于 {@code carlos-spring-boot-starter-json} 的负载编解码实现
 *
 * @author Carlos
 * @since 3.0.0
 */
public class JsonMqPayloadCodec implements MqPayloadCodec {

    @Override
    public String serialize(Object payload) {
        if (payload == null) {
            throw new IllegalArgumentException("消息负载不能为 null");
        }
        try {
            return JsonUtils.toJson(payload);
        } catch (Exception e) {
            throw new IllegalArgumentException("消息负载序列化失败：" + payload.getClass().getName(), e);
        }
    }

    @Override
    public <T> T deserialize(String json, Class<T> type) {
        if (json == null) {
            return null;
        }
        try {
            return JsonUtils.fromJson(json, type);
        } catch (Exception e) {
            throw new IllegalArgumentException("消息负载反序列化失败：" + type.getName(), e);
        }
    }
}
