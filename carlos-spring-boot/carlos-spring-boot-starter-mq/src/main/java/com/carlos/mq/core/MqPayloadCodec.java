package com.carlos.mq.core;

/**
 * 消息负载编解码 SPI
 * <p>
 * 把「对象 ↔ JSON 字符串」这一步从信封与传输中分离出来：{@link MqMessage} 只承载
 * JSON 字符串、不依赖任何 JSON 引擎，传输实现也不关心序列化细节。
 * </p>
 * <p>
 * 采用 SPI 而非直接写死引擎，是为了让编解码实现可替换（本模块默认提供基于
 * {@code carlos-spring-boot-starter-json} 的实现），也便于单元测试注入桩实现。
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
public interface MqPayloadCodec {

    /**
     * 序列化负载
     *
     * @param payload 业务负载
     * @return JSON 字符串
     * @throws IllegalArgumentException 负载无法序列化时抛出
     */
    String serialize(Object payload);

    /**
     * 反序列化负载
     *
     * @param json JSON 字符串
     * @param type 目标类型
     * @param <T>  泛型
     * @return 反序列化结果
     * @throws IllegalArgumentException JSON 无法解析为目标类型时抛出
     */
    <T> T deserialize(String json, Class<T> type);
}
