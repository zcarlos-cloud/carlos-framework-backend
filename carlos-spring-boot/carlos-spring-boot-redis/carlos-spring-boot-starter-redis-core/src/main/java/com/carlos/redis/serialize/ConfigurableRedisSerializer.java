package com.carlos.redis.serialize;

import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

/**
 * 可配置的 Redis 序列化器
 * <p>
 * 基于策略模式的 Spring Data Redis 序列化器实现
 * </p>
 *
 * @author carlos
 * @date 2026-03-14
 */
public class ConfigurableRedisSerializer implements RedisSerializer<Object> {

    /**
     * 序列化策略
     */
    private final RedisSerializerStrategy strategy;

    /**
     * 是否序列化为字符串（用于兼容某些 Redis 客户端）
     */
    private final boolean serializeToString;

    public ConfigurableRedisSerializer(RedisSerializerStrategy strategy) {
        this(strategy, false);
    }

    public ConfigurableRedisSerializer(RedisSerializerStrategy strategy, boolean serializeToString) {
        this.strategy = strategy;
        this.serializeToString = serializeToString;
    }

    @Override
    public byte[] serialize(Object o) throws SerializationException {
        if (o == null) {
            return new byte[0];
        }
        try {
            byte[] bytes = strategy.serialize(o);
            if (serializeToString && !strategy.isBinary()) {
                // 如果是 JSON 格式，确保是 UTF-8 字符串
                return bytes;
            }
            return bytes;
        } catch (Exception e) {
            throw new SerializationException("Serialize error with " + strategy.getType(), e);
        }
    }

    @Override
    public Object deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        // 反序列化为 Object 类型
        return deserialize(bytes, Object.class);
    }

    /**
     * 反序列化为指定类型
     *
     * @param bytes 字节数组
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 反序列化后的对象
     */
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            return strategy.deserialize(bytes, clazz);
        } catch (Exception e) {
            throw new SerializationException("Deserialize error with " + strategy.getType(), e);
        }
    }

    /**
     * 获取当前使用的序列化策略
     *
     * @return 序列化策略
     */
    public RedisSerializerStrategy getStrategy() {
        return strategy;
    }

    /**
     * 获取序列化类型
     *
     * @return 序列化类型名称
     */
    public String getSerializerType() {
        return strategy.getType();
    }
}
