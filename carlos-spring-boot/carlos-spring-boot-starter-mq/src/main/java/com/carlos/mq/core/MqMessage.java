package com.carlos.mq.core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 传输中立的消息信封（不可变）
 * <p>
 * <b>为什么非泛型</b>：泛型在 JSON 往返时必然被擦除（{@code MqMessage<T>} 反序列化后
 * {@code body} 会退化成 {@code Map}），无法可靠跨进程。因此本信封把业务负载作为
 * <b>JSON 字符串原样承载</b>，并用 {@link #payloadType} 记录其原始类型，
 * 由消费端按 handler 形参类型自行反序列化。
 * </p>
 * <p>
 * <b>不可变性由语言保证</b>（record）：无 setter，每个 {@code withXxx} 返回新实例。
 * 因此不存在旧实现中 {@code markSent()}/{@code markConsumed()}/{@code incrementRetry()}
 * 那类原地修改对象的方法（那违反项目的不可变性强制规范）。
 * 注意：发送时刻/消费时刻属于<b>框架的观测数据</b>，归属
 * {@link SendResult}/{@link ConsumeResult}，不应污染消息本身。
 * </p>
 *
 * @param messageId     消息唯一 id（生产端生成，可作为幂等键）
 * @param topic         主题
 * @param key           业务键（用于分区、去重、查询；可为 null）
 * @param payloadType   业务负载的全限定类型名（消费端校验用；可为 null 表示不校验）
 * @param payload       业务负载的 JSON 字符串（<b>不在此处解析</b>）
 * @param headers       自定义消息头（不可变 Map）
 * @param createTime    生产端创建时刻（epoch millis）
 * @param deliveryCount 已投递次数（消费端由待处理队列填充；生产端恒为 1）
 * @param recordId      底层传输的物理 id（消费端填充；生产端为 null）
 * @param consumerName  本次投递的消费者名（消费端填充；生产端为 null）
 * @param traceId       链路追踪 id（可为 null）
 * @author Carlos
 * @since 3.0.0
 */
public record MqMessage(
    String messageId,
    String topic,
    String key,
    String payloadType,
    String payload,
    Map<String, String> headers,
    long createTime,
    int deliveryCount,
    String recordId,
    String consumerName,
    String traceId
) {

    /**
     * 紧凑构造器：把可变 Map 复制为不可变，并兜住 null。
     */
    public MqMessage {
        headers = headers == null ? Map.of() : Map.copyOf(headers);
    }

    /**
     * 生成消息唯一 id
     *
     * @return 32 位小写十六进制 id
     */
    public static String generateMessageId() {
        return UUID.randomUUID().toString().replace("-", "").toLowerCase();
    }

    /**
     * 生产端工厂：构造一条待发送的消息
     * <p>
     * 本方法<b>不做序列化</b>——调用方（模板层）负责把业务对象序列化为 JSON 字符串，
     * 从而让信封与具体 JSON 引擎解耦，也便于单元测试。
     * </p>
     *
     * @param topic       主题
     * @param payloadJson 业务负载的 JSON 字符串
     * @param payloadType 业务负载类型名（可为 null）
     * @param options     发送选项（可为 null）
     * @return 新的消息信封
     */
    public static MqMessage of(String topic, String payloadJson, String payloadType, MqSendOptions options) {
        MqSendOptions opts = options == null ? MqSendOptions.none() : options;
        return new MqMessage(
            generateMessageId(),
            topic,
            opts.key(),
            payloadType,
            payloadJson,
            opts.headers(),
            System.currentTimeMillis(),
            1,
            null,
            null,
            null
        );
    }

    /**
     * 消费端工厂：由待处理条目重建信封并补齐投递元信息
     *
     * @param source        原始信封
     * @param newRecordId   底层物理 id
     * @param newConsumer   消费者名
     * @param deliveryCount 累计投递次数
     * @return 新的消息信封
     */
    public static MqMessage fromRecord(MqMessage source, String newRecordId,
                                       String newConsumer, int deliveryCount) {
        return new MqMessage(
            source.messageId(),
            source.topic(),
            source.key(),
            source.payloadType(),
            source.payload(),
            source.headers(),
            source.createTime(),
            deliveryCount,
            newRecordId,
            newConsumer,
            source.traceId()
        );
    }

    /**
     * 只读派生：是否为重投递
     *
     * @return 投递次数 &gt; 1 时为 true
     */
    public boolean isRedelivery() {
        return deliveryCount > 1;
    }

    /**
     * 只读派生：投递次数是否已达上限
     *
     * @param maxDeliveryCount 上限
     * @return 达到或超过上限时为 true
     */
    public boolean isRetryExhausted(int maxDeliveryCount) {
        return maxDeliveryCount > 0 && deliveryCount >= maxDeliveryCount;
    }

    /**
     * 读取消息头
     *
     * @param name 头名称
     * @return 头值，不存在时为 null
     */
    public String header(String name) {
        return headers.get(name);
    }

    public MqMessage withDeliveryCount(int newDeliveryCount) {
        return new MqMessage(messageId, topic, key, payloadType, payload, headers,
            createTime, newDeliveryCount, recordId, consumerName, traceId);
    }

    public MqMessage withRecordId(String newRecordId) {
        return new MqMessage(messageId, topic, key, payloadType, payload, headers,
            createTime, deliveryCount, newRecordId, consumerName, traceId);
    }

    public MqMessage withConsumerName(String newConsumerName) {
        return new MqMessage(messageId, topic, key, payloadType, payload, headers,
            createTime, deliveryCount, recordId, newConsumerName, traceId);
    }

    public MqMessage withTraceId(String newTraceId) {
        return new MqMessage(messageId, topic, key, payloadType, payload, headers,
            createTime, deliveryCount, recordId, consumerName, newTraceId);
    }

    /**
     * 添加消息头（返回新实例）
     *
     * @param name  头名称
     * @param value 头值
     * @return 新实例
     */
    public MqMessage withHeader(String name, String value) {
        Map<String, String> merged = new HashMap<>(headers);
        merged.put(name, value);
        return new MqMessage(messageId, topic, key, payloadType, payload, merged,
            createTime, deliveryCount, recordId, consumerName, traceId);
    }
}
