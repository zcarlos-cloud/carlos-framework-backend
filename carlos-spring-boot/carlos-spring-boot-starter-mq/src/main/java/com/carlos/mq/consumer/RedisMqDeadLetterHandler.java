package com.carlos.mq.consumer;

import com.carlos.mq.client.redis.MqRecordMapper;
import com.carlos.mq.client.redis.MqStreamOptions;
import com.carlos.mq.client.redis.MqTopics;
import com.carlos.mq.config.MqProperties;
import com.carlos.mq.core.ConsumeResult;
import com.carlos.mq.core.MqMessage;
import com.carlos.redis.stream.RedisStreamTemplate;
import com.carlos.redis.stream.StreamAddOptions;
import com.carlos.redis.stream.StreamEntry;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 死信处理器
 * <p>
 * 投递次数达到上限、或反序列化失败（无法被重试修复）的消息写入按主题独立的死信 Stream。
 * </p>
 *
 * <h2>两条设计原则</h2>
 * <ol>
 *   <li><b>死信也必须有界</b>：写死信同样带长度上限，否则「毒消息无限增长」只是从待处理
 *       队列转移到死信队列，问题换个地方继续。</li>
 *   <li><b>字段必须自解释</b>：死信是给人排查用的，必须能回答「原主题是什么、原 recordId 是
 *       哪个、投递了几次、为什么失败、被谁处理的」——否则只能看到一坨 JSON。</li>
 * </ol>
 *
 * <p>
 * <b>刻意不提供 HTTP 端点</b>：框架擅自暴露「重放」这类会放大流量的运维端点属安全风险，
 * 且违反项目「网关路由必须显式加」的约定。业务模块如需重放，请自行包装并加权限。
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@Slf4j
public class RedisMqDeadLetterHandler {

    private final RedisStreamTemplate streamTemplate;
    private final MqTopics topics;
    private final MqProperties properties;

    public RedisMqDeadLetterHandler(RedisStreamTemplate streamTemplate, MqTopics topics,
                                   MqProperties properties) {
        this.streamTemplate = streamTemplate;
        this.topics = topics;
        this.properties = properties;
    }

    /**
     * 写入死信
     *
     * @param message        消息信封
     * @param reason         失败原因摘要
     * @return 死信 Stream 的 recordId；未启用或写入失败时返回 null
     */
    public String deadLetter(MqMessage message, String reason) {
        if (!properties.getConsumer().isDlqEnabled()) {
            log.warn("死信已禁用，消息被直接丢弃：topic={}, messageId={}, reason={}",
                message.topic(), message.messageId(), reason);
            return null;
        }
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("originalTopic", message.topic());
        fields.put("originalRecordId", message.recordId() == null ? "" : message.recordId());
        fields.put("messageId", message.messageId() == null ? "" : message.messageId());
        fields.put("payloadType", message.payloadType() == null ? "" : message.payloadType());
        fields.put("payload", message.payload());
        fields.put("deliveryCount", String.valueOf(message.deliveryCount()));
        fields.put("firstSeenAt", String.valueOf(message.createTime()));
        fields.put("failedAt", String.valueOf(System.currentTimeMillis()));
        fields.put("consumerName", message.consumerName() == null ? "" : message.consumerName());
        fields.put("reason", truncate(reason, 512));
        try {
            String recordId = streamTemplate.add(topics.dlqKey(message.topic()), fields,
                StreamAddOptions.maxLen(properties.getConsumer().getDlqMaxLength()));
            log.error("消息转入死信：topic={}, messageId={}, deliveryCount={}, reason={}",
                message.topic(), message.messageId(), message.deliveryCount(), reason);
            return recordId;
        } catch (Exception e) {
            // 死信写入失败不能再抛给调用方（会把消费链路拖垮），但必须留下明确痕迹
            log.error("死信写入失败（消息将丢失）：topic={}, messageId={}",
                message.topic(), message.messageId(), e);
            return null;
        }
    }

    /**
     * 查看死信头部若干条（不删除）
     *
     * @param topic 原主题
     * @param count 条数上限
     * @return 死信条目
     */
    public List<StreamEntry> peek(String topic, int count) {
        return streamTemplate.range(topics.dlqKey(topic), count);
    }

    /**
     * 死信深度
     *
     * @param topic 原主题
     * @return 条数
     */
    public long size(String topic) {
        return streamTemplate.length(topics.dlqKey(topic));
    }

    /**
     * 重放一条死信回原主题
     * <p>
     * 仅移动消息体，不携带投递次数（重放后按新消息重新计数）。
     * </p>
     *
     * @param topic         原主题
     * @param dlqRecordId   死信条目的 recordId
     * @param deleteAfter   重放后是否删除死信条目
     * @return 原主题的 recordId；失败返回 null
     */
    public String replay(String topic, String dlqRecordId, boolean deleteAfter) {
        StreamEntry target = peek(topic, Integer.MAX_VALUE).stream()
            .filter(entry -> entry.recordId().equals(dlqRecordId))
            .findFirst()
            .orElse(null);
        if (target == null) {
            log.warn("重放失败：死信条目不存在：topic={}, dlqRecordId={}", topic, dlqRecordId);
            return null;
        }
        // 重放按「新消息」对待：只搬运负载与原 messageId，不携带投递次数（否则会立即再次超限）
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put(MqRecordMapper.FIELD_DATA, nullToEmpty(target.field("payload")));
        fields.put(MqRecordMapper.FIELD_MESSAGE_ID, nullToEmpty(target.field("messageId")));
        fields.put(MqRecordMapper.FIELD_CREATE_TIME, String.valueOf(System.currentTimeMillis()));
        String payloadType = target.field("payloadType");
        if (payloadType != null && !payloadType.isBlank()) {
            fields.put(MqRecordMapper.FIELD_PAYLOAD_TYPE, payloadType);
        }

        String recordId = streamTemplate.add(topics.streamKey(topic), fields,
            MqStreamOptions.of(properties, topic));
        if (deleteAfter && recordId != null) {
            streamTemplate.delete(topics.dlqKey(topic), List.of(dlqRecordId));
        }
        log.info("死信已重放：topic={}, messageId={}, newRecordId={}",
            topic, target.field("messageId"), recordId);
        return recordId;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
