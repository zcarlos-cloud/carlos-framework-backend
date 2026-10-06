package com.carlos.redis.serialize;

import cn.hutool.core.date.DatePattern;
import com.carlos.core.exception.BusinessException;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Jackson JSON 序列化实现
 * <p>
 * 特点：可读性好，跨语言兼容，适合调试场景
 * </p>
 *
 * @author carlos
 * @date 2026-03-14
 */
@Slf4j
public class JacksonSerializer implements RedisSerializerStrategy {

    private final ObjectMapper objectMapper;

    public JacksonSerializer() {
        this.objectMapper = createObjectMapper();
    }

    /**
     * 创建配置好的 ObjectMapper
     */
    private ObjectMapper createObjectMapper() {
        SimpleModule simpleModule = new SimpleModule();
        // 解决前端 JS 处理大数字丢失精度问题，将 Long 和 BigInteger 转换为 String
        simpleModule.addSerializer(BigInteger.class, ToStringSerializer.instance);
        simpleModule.addSerializer(Long.class, ToStringSerializer.instance);
        simpleModule.addSerializer(Long.TYPE, ToStringSerializer.instance);
        // 注册自定义 LocalDateTime 反序列化器
        simpleModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer());

        // 配置多态类型处理 - 使用白名单验证，防止反序列化漏洞
        var ptv = BasicPolymorphicTypeValidator.builder()
            .allowIfSubType("com.carlos")
            .allowIfSubType("java.util")
            .allowIfSubType("java.lang")
            .allowIfSubType("java.time")
            .allowIfSubType("java.math")
            .build();

        return JsonMapper.builder()
            .changeDefaultPropertyInclusion(v ->
                JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.ALWAYS))
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .addModule(simpleModule)
            .activateDefaultTyping(ptv, DefaultTyping.NON_FINAL, JsonTypeInfo.As.WRAPPER_ARRAY)
            .build();
    }

    @Override
    public byte[] serialize(Object obj) {
        if (obj == null) {
            return new byte[0];
        }
        try {
            return objectMapper.writeValueAsBytes(obj);
        } catch (JacksonException e) {
            log.error("Jackson serialize error", e);
            throw new SerializationException("Jackson serialize error: " + e.getMessage(), e);
        }
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            return objectMapper.readValue(bytes, clazz);
        } catch (Exception e) {
            log.error("Jackson deserialize error", e);
            throw new SerializationException("Jackson deserialize error: " + e.getMessage(), e);
        }
    }

    @Override
    public String getType() {
        return "jackson";
    }

    /**
     * 自定义 LocalDateTime 反序列化器
     */
    private static class LocalDateTimeDeserializer extends ValueDeserializer<LocalDateTime> {
        @Override
        public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) {
            String text = p.getText();
            if (text == null || text.isEmpty()) {
                return null;
            }
            // 支持多种格式
            if (text.contains("T")) {
                return LocalDateTime.parse(text);
            }
            return LocalDateTime.parse(text, DateTimeFormatter.ofPattern(DatePattern.NORM_DATETIME_MS_PATTERN));
        }
    }

    /**
     * 序列化异常
     */
    public static class SerializationException extends BusinessException {
        public SerializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
