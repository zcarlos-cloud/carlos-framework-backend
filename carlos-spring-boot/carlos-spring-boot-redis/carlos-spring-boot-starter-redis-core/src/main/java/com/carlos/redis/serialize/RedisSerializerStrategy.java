package com.carlos.redis.serialize;

/**
 * Redis 序列化策略接口
 * <p>
 * 提供可插拔的序列化实现，支持 Jackson、Fastjson、Kryo、JDK 等多种序列化方式
 * </p>
 *
 * @author carlos
 * @date 2026-03-14
 */
public interface RedisSerializerStrategy {

    /**
     * 序列化对象
     *
     * @param obj 待序列化对象
     * @return 字节数组
     */
    byte[] serialize(Object obj);

    /**
     * 反序列化对象
     *
     * @param bytes 字节数组
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 反序列化后的对象
     */
    <T> T deserialize(byte[] bytes, Class<T> clazz);

    /**
     * 获取序列化类型名称
     *
     * @return 序列化类型名称
     */
    String getType();

    /**
     * 是否为二进制序列化（非文本格式）
     *
     * @return true 表示二进制格式，false 表示文本格式
     */
    default boolean isBinary() {
        return false;
    }
}
