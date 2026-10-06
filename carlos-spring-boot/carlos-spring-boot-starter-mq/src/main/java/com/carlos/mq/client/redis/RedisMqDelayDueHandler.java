package com.carlos.mq.client.redis;

import com.carlos.mq.config.MqProperties;
import com.carlos.mq.core.MqPayloadCodec;
import com.carlos.redis.delay.DelayDueHandler;
import com.carlos.redis.stream.RedisStreamTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 延时到期处理器：把到期条目投递回其原主题的 Stream
 *
 * <h2>这条接缝此前从未接上</h2>
 * <p>
 * {@link DelayDueHandler} 是原语层与 MQ 层之间的接缝：原语层只负责「到期」，
 * <b>不负责知道到期条目该去哪里</b>；而 {@code RedissonDelayedQueue} 在缺少本实现时
 * <b>不会启动任何桶</b>。此前本模块从未提供实现，导致：
 * </p>
 * <ol>
 *   <li>所有桶都不启动，任何延时入队都抛 {@code DELAY_OFFER_FAILED}；</li>
 *   <li>而传输仍按「延时管理器 bean 在场」声明 {@link com.carlos.mq.core.MqCapability#DELAYED}，
 *       于是失败被报成<b>可重试</b>的错误，调用方无限重试一个永久性配置缺失。</li>
 * </ol>
 * <p>
 * 第二点已在批次 2 修掉（改为按 {@code isRunning()} 判定）。本类补上第一点。
 * </p>
 *
 * <h2>为什么载荷里必须带 topic</h2>
 * <p>
 * 桶与主题是多对多关系，仅凭桶名无法决定写入哪个 Stream。因此入队时使用的是
 * {@link MqDelayPayload}（目标主题 + Stream 字段），而非裸的业务负载——后者会
 * <b>丢失 topic</b>，到期后无从路由。
 * </p>
 *
 * <h2>失败取舍：不重投</h2>
 * <p>
 * 条目在到期时已从延时结构取出，此处投递失败<b>不会重投</b>（与原语层转移线程的取舍一致，
 * 属已声明的「可容忍丢失」边界）。记录 ERROR 日志并继续处理后续条目——
 * 单条失败不应拖垮整个桶。
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class RedisMqDelayDueHandler implements DelayDueHandler {

    private final RedisStreamTemplate streamTemplate;
    private final MqTopics topics;
    private final MqProperties properties;
    private final MqPayloadCodec payloadCodec;

    @Override
    public void onDue(String bucketName, String payload) {
        MqDelayPayload delayed = parse(bucketName, payload);
        if (delayed == null) {
            return;
        }
        String topic = delayed.topic();
        try {
            String recordId = streamTemplate.add(topics.streamKey(topic), delayed.fields(),
                MqStreamOptions.of(properties, topic));
            log.debug("延时条目已投递：bucket={}, topic={}, recordId={}", bucketName, topic, recordId);
        } catch (Exception e) {
            // 已从延时结构取出，不再重投；记录后继续处理同桶的后续条目
            log.error("延时条目投递失败（该条不再重投）：bucket={}, topic={}", bucketName, topic, e);
        }
    }

    /**
     * 解析延时载荷
     *
     * @param bucketName 桶名（仅用于日志）
     * @param payload    载荷 JSON
     * @return 载荷；无法解析或缺少目标主题时返回 {@code null}
     */
    private MqDelayPayload parse(String bucketName, String payload) {
        MqDelayPayload delayed;
        try {
            delayed = payloadCodec.deserialize(payload, MqDelayPayload.class);
        } catch (Exception e) {
            log.error("延时条目无法解析，已丢弃（该条不再重投）：bucket={}, payload={}",
                bucketName, payload, e);
            return null;
        }
        if (delayed == null || delayed.topic() == null || delayed.topic().isBlank()) {
            log.error("延时条目缺少目标主题，已丢弃（该条不再重投）：bucket={}, payload={}",
                bucketName, payload);
            return null;
        }
        return delayed;
    }
}
