package com.carlos.redis.serialize;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONFactory;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

/**
 * Fastjson2 JSON 序列化实现
 * <p>
 * 特点：性能较好，功能丰富，但需要注意安全性配置
 * </p>
 *
 * @author carlos
 * @date 2026-03-14
 */
@Slf4j
public class FastjsonSerializer implements RedisSerializerStrategy {

    private final JSONWriter.Context writerContext;
    private final JSONReader.Context readerContext;

    public FastjsonSerializer() {
        // 安全加固：禁用 autoType，仅允许指定包反序列化
        JSONFactory.setUseJacksonAnnotation(false);
        this.writerContext = JSONFactory.createWriteContext(
            JSONWriter.Feature.WriteMapNullValue,
            JSONWriter.Feature.WriteNullStringAsEmpty,
            JSONWriter.Feature.WriteNullListAsEmpty,
            JSONWriter.Feature.WriteNullBooleanAsFalse,
            JSONWriter.Feature.WriteLongAsString
        );
        this.readerContext = JSONFactory.createReadContext(
            JSONReader.Feature.FieldBased,
            JSONReader.Feature.IgnoreNullPropertyValue
        );
    }

    @Override
    public byte[] serialize(Object obj) {
        if (obj == null) {
            return new byte[0];
        }
        try {
            String json = JSON.toJSONString(obj, writerContext);
            return json.getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Fastjson2 serialize error", e);
            throw new SerializationException("Fastjson2 serialize error: " + e.getMessage(), e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            String json = new String(bytes, StandardCharsets.UTF_8);
            return JSON.parseObject(json, clazz, readerContext);
        } catch (Exception e) {
            log.error("Fastjson2 deserialize error", e);
            throw new SerializationException("Fastjson2 deserialize error: " + e.getMessage(), e);
        }
    }

    @Override
    public String getType() {
        return "fastjson";
    }

    /**
     * 序列化异常
     */
    public static class SerializationException extends com.carlos.core.exception.BusinessException {
        public SerializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
