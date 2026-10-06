package com.carlos.redis.serialize;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 序列化器工厂
 * <p>
 * 管理各种序列化策略的创建和缓存
 * </p>
 *
 * @author carlos
 * @date 2026-03-14
 */
@Slf4j
public class SerializerFactory {

    /**
     * 序列化器缓存
     */
    private static final Map<String, RedisSerializerStrategy> STRATEGY_CACHE = new ConcurrentHashMap<>();

    /**
     * 获取序列化器
     *
     * @param type 序列化类型
     * @return 序列化策略实现
     */
    public static RedisSerializerStrategy getSerializer(SerializerType type) {
        return STRATEGY_CACHE.computeIfAbsent(type.getCode(), k -> createSerializer(type));
    }

    /**
     * 根据代码获取序列化器
     *
     * @param code 序列化类型代码
     * @return 序列化策略实现
     */
    public static RedisSerializerStrategy getSerializer(String code) {
        return getSerializer(SerializerType.fromCode(code));
    }

    /**
     * 创建序列化器
     *
     * @param type 序列化类型
     * @return 序列化策略实现
     */
    private static RedisSerializerStrategy createSerializer(SerializerType type) {
        log.info("Creating serializer: {}", type.getCode());
        return switch (type) {
            case JACKSON -> new JacksonSerializer();
            case FASTJSON -> new FastjsonSerializer();
            case KRYO -> new KryoSerializer();
            default -> {
                log.warn("Unknown serializer type: {}, using Jackson as default", type.getCode());
                yield new JacksonSerializer();
            }
        };
    }

    /**
     * 清除缓存
     */
    public static void clearCache() {
        STRATEGY_CACHE.clear();
        log.info("Serializer cache cleared");
    }
}
