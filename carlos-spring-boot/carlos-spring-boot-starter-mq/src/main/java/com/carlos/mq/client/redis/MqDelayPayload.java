package com.carlos.mq.client.redis;

import java.util.Map;

/**
 * 延时队列中承载的载荷（不可变）
 *
 * <h2>为什么需要它</h2>
 * <p>
 * 原语层的延时队列只认识「桶 + 一个字符串载荷」，<b>不知道到期条目该回到哪个主题</b>
 * （桶与主题是多对多关系，仅凭桶名无法路由）。
 * </p>
 * <p>
 * 早期实现只入队了业务负载本体，<b>topic 随之丢失</b>，到期后无从投递——这是延时能力
 * 长期不可用的原因之一。本记录补齐这一必要信息：<b>目标主题 + 该条目要写入 Stream 的字段</b>。
 * </p>
 *
 * <h2>为什么不存完整信封</h2>
 * <p>
 * {@code fields} 直接复用 {@link MqRecordMapper#toFields} 的形状（即 Stream 条目的真实字段），
 * 因此到期处理器可以<b>原样写入 Stream</b>，无需重建 {@code MqMessage}。
 * 这样「即时投递」与「延时投递」两条路径的字段结构由<b>同一个映射</b>保证一致，
 * 不会各自演进导致漂移。
 * </p>
 *
 * @param topic  目标主题
 * @param fields Stream 条目字段（见 {@link MqRecordMapper}）
 * @author Carlos
 * @since 3.0.0
 */
public record MqDelayPayload(String topic, Map<String, String> fields) {

    /**
     * 紧凑构造器：把可变 Map 复制为不可变，并兜住 null。
     */
    public MqDelayPayload {
        fields = fields == null ? Map.of() : Map.copyOf(fields);
    }
}
