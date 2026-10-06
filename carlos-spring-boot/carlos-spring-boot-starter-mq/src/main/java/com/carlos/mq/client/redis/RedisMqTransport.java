package com.carlos.mq.client.redis;

import com.carlos.core.exception.GlobalException;
import com.carlos.mq.config.MqProperties;
import com.carlos.mq.core.MqCapability;
import com.carlos.mq.core.MqMessage;
import com.carlos.mq.core.MqSendCallback;
import com.carlos.mq.core.MqPayloadCodec;
import com.carlos.mq.core.SendResult;
import com.carlos.mq.core.SendStatus;
import com.carlos.mq.error.MqErrorCode;
import com.carlos.mq.exception.MqCapabilityNotSupportedException;
import com.carlos.mq.exception.MqException;
import com.carlos.mq.spi.MqSubscription;
import com.carlos.mq.spi.MqTransport;
import com.carlos.mq.support.MqType;
import com.carlos.redis.delay.RedisDelayErrorCode;
import com.carlos.redis.delay.RedisDelayException;
import com.carlos.redis.delay.RedissonDelayedQueueManager;
import com.carlos.redis.stream.RedisStreamException;
import com.carlos.redis.stream.RedisStreamTemplate;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Redis Stream 传输实现（{@link MqTransport} SPI 的第一个实现）
 * <p>
 * 本类是本项目「新增一种 MQ 传输」的活样板：它只实现 SPI、不碰业务侧 API，
 * 因此接入它<b>没有改动任何调用方代码</b>。
 * </p>
 *
 * <h2>能力声明（诚实版）</h2>
 * <p>
 * 只声明<b>真正可用</b>的能力：
 * </p>
 * <ul>
 *   <li>{@link MqCapability#BASIC} —— 基础发送</li>
 *   <li>{@link MqCapability#DELAYED} —— 仅当延时管理器<b>确实已启动</b>（见
 *       {@link #delayAvailable()}），而非仅仅 bean 在场</li>
 *   <li>{@link MqCapability#CONSUME} / {@link MqCapability#PENDING_CLAIM} /
 *       {@link MqCapability#DEAD_LETTER} / {@link MqCapability#BATCH_CONSUME} /
 *       {@link MqCapability#BROADCAST} —— 仅当消费容器在场（已装配）时</li>
 * </ul>
 * <p>
 * 不声明 {@link MqCapability#TAG_FILTER}（Redis Stream 无服务端过滤）、
 * {@link MqCapability#ORDERED}（同组多消费者会交错）、
 * {@link MqCapability#TRANSACTIONAL}（无原生事务消息，走 outbox）。
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@Slf4j
public class RedisMqTransport implements MqTransport {

    private final RedisStreamTemplate streamTemplate;
    private final MqTopics topics;
    private final MqProperties properties;
    private final MqPayloadCodec payloadCodec;

    /** 延时队列管理器；为 null 表示未启用延时能力 */
    private final RedissonDelayedQueueManager delayQueueManager;

    /** 消费容器；为 null 表示未启用消费能力 */
    private final RedisMqConsumerContainer consumerContainer;

    private final AtomicBoolean started = new AtomicBoolean(false);

    /**
     * 延时容量超限的码集合
     * <p>MQ 层与原语层各自定义了一个同义码，此处取并集——码值一律来自枚举，不内联字面量。</p>
     */
    private static final Set<String> DELAY_QUEUE_FULL_CODES = Set.of(
        MqErrorCode.MQ_DELAY_QUEUE_FULL.getCode(),
        RedisDelayErrorCode.DELAY_QUEUE_FULL.getCode());

    /** 延时超过上限的码集合 */
    private static final Set<String> DELAY_EXCEEDS_MAX_CODES = Set.of(
        MqErrorCode.MQ_DELAY_EXCEEDS_MAX.getCode(),
        RedisDelayErrorCode.DELAY_EXCEEDS_MAX.getCode());

    public RedisMqTransport(RedisStreamTemplate streamTemplate,
                            MqTopics topics,
                            MqProperties properties,
                            MqPayloadCodec payloadCodec,
                            RedissonDelayedQueueManager delayQueueManager,
                            RedisMqConsumerContainer consumerContainer) {
        this.streamTemplate = streamTemplate;
        this.topics = topics;
        this.properties = properties;
        this.payloadCodec = payloadCodec;
        this.delayQueueManager = delayQueueManager;
        this.consumerContainer = consumerContainer;
    }

    @Override
    public MqType getType() {
        return MqType.REDIS;
    }

    @Override
    public String getName() {
        return "redis-stream";
    }

    @Override
    public Set<MqCapability> capabilities() {
        Set<MqCapability> capabilities = EnumSet.of(MqCapability.BASIC);
        if (delayAvailable()) {
            capabilities.add(MqCapability.DELAYED);
        }
        if (consumerContainer != null) {
            capabilities.add(MqCapability.CONSUME);
            capabilities.add(MqCapability.PENDING_CLAIM);
            capabilities.add(MqCapability.DEAD_LETTER);
            capabilities.add(MqCapability.BATCH_CONSUME);
            capabilities.add(MqCapability.BROADCAST);
        }
        return Set.copyOf(capabilities);
    }

    /* ==================== 生命周期 ==================== */

    @Override
    public void start() {
        if (!started.compareAndSet(false, true)) {
            return;
        }
        if (delayQueueManager != null) {
            delayQueueManager.startAll();
            validateDelayBuckets();
        }
        log.info("Redis MQ 传输已启动：capabilities={}", capabilities());
    }

    /**
     * 延时能力是否<b>真正可用</b>
     * <p>
     * 只判断「延时管理器 bean 是否在场」是不够的：管理器在<b>没有到期处理器
     * （{@code DelayDueHandler}）时不会启动任何桶</b>，此时任何 {@code offer} 都必然失败。
     * 若仍声明 {@link MqCapability#DELAYED}，就是「声明了却做不到」——调用方会拿到
     * <b>可重试</b>的 {@code SEND_FAILED} 并无限重试，而真实原因是永久性的配置缺失。
     * </p>
     * <p>
     * 改为按 {@link RedissonDelayedQueueManager#isRunning()} 判定：未启动即不声明，
     * 调用方得到明确的「能力不支持」（不可重试）；将来补齐到期处理器后自动恢复声明，
     * 无需改动本类。
     * </p>
     */
    private boolean delayAvailable() {
        return delayQueueManager != null && delayQueueManager.isRunning();
    }

    /**
     * 启动期校验：主题<b>显式配置</b>的延时桶必须存在
     * <p>
     * 不校验时的失败模式：发送延时消息 -&gt; 桶从未启动 -&gt; {@code offer} 抛
     * {@code DELAY_OFFER_FAILED} -&gt; 被映射为<b>可重试</b>的 {@code SEND_FAILED}
     * -&gt; 调用方无限重试一个<b>永远不可能成功</b>的配置错误。
     * </p>
     * <p>
     * 只校验显式配置的桶；未配置的主题走
     * {@link RedissonDelayedQueueManager#defaultBucket()}，天然存在。
     * </p>
     */
    private void validateDelayBuckets() {
        if (!delayAvailable()) {
            // 延时未启用（无到期处理器）：已不声明 DELAYED，桶配置无从谈起
            return;
        }
        Set<String> availableBuckets = delayQueueManager.bucketNames();
        List<String> problems = properties.getTopics().entrySet().stream()
            .filter(entry -> entry.getValue().getBucket() != null)
            .filter(entry -> !availableBuckets.contains(entry.getValue().getBucket()))
            .map(entry -> "carlos.mq.topics['%s'].bucket=%s".formatted(
                entry.getKey(), entry.getValue().getBucket()))
            .toList();
        if (!problems.isEmpty()) {
            throw new MqException(MqErrorCode.MQ_DELAY_BUCKET_NOT_FOUND,
                "以下主题配置的延时桶不存在（当前可用桶：%s）：%s".formatted(
                    availableBuckets, problems));
        }
    }

    @Override
    public void stop() {
        if (!started.compareAndSet(true, false)) {
            return;
        }
        if (consumerContainer != null) {
            consumerContainer.stop();
        }
        if (delayQueueManager != null) {
            delayQueueManager.stopAll();
        }
        log.info("Redis MQ 传输已停止");
    }

    @Override
    public boolean isRunning() {
        return started.get();
    }

    /**
     * 健康探测：做一次真实的 {@code XLEN} 往返，而非仅判断标志位。
     */
    @Override
    public boolean ping() {
        try {
            streamTemplate.length(topics.streamKey("__health__"));
            return true;
        } catch (Exception e) {
            log.warn("Redis MQ 健康探测失败", e);
            return false;
        }
    }

    /* ==================== 发送 ==================== */

    @Override
    public SendResult send(MqMessage message) {
        return append(message);
    }

    @Override
    public void sendOneWay(MqMessage message) {
        append(message);
    }

    @Override
    public void sendAsync(MqMessage message, MqSendCallback callback) {
        // Redis 侧单次 XADD 已是亚毫秒级，异步发送由模板层的虚拟线程统一提供；
        // 此处不重复实现，避免出现两套异步语义
        throw new MqCapabilityNotSupportedException(MqCapability.ASYNC_SEND, getType());
    }

    @Override
    public SendResult sendDelayed(MqMessage message, Duration delay) {
        if (delayQueueManager == null) {
            return SendResult.failure(SendStatus.CAPABILITY_NOT_SUPPORTED,
                message.messageId(), message.topic());
        }
        Duration effective = delay == null ? Duration.ZERO : delay;
        if (effective.isZero() || effective.isNegative()) {
            // 零/负延迟直接投递，省掉一次延时结构往返与线程唤醒
            return append(message);
        }
        String bucket = resolveBucket(message.topic());
        if (bucket == null) {
            // 原语层未配置任何桶：能力事实上不存在，明确失败而非静默
            return SendResult.failure(SendStatus.CAPABILITY_NOT_SUPPORTED,
                message.messageId(), message.topic());
        }
        // 入队的是「目标主题 + Stream 字段」，而非裸负载：桶与主题是多对多，仅凭桶名无法路由
        String payload;
        try {
            payload = payloadCodec.serialize(new MqDelayPayload(
                message.topic(), MqRecordMapper.toFields(message, payloadCodec)));
        } catch (Exception e) {
            log.warn("延时条目序列化失败：topic={}, messageId={}",
                message.topic(), message.messageId(), e);
            return SendResult.failure(SendStatus.SERIALIZATION_ERROR,
                message.messageId(), message.topic(), e);
        }
        try {
            delayQueueManager.bucket(bucket).offer(payload, effective);
            return SendResult.success(message.messageId(), message.topic(), null);
        } catch (RedisStreamException | RedisDelayException e) {
            log.warn("延时投递失败：topic={}, delay={}, bucket={}",
                message.topic(), effective, bucket, e);
            return SendResult.failure(mapDelayStatus(e), message.messageId(), message.topic(), e);
        }
    }

    /* ==================== 消费 ==================== */

    @Override
    public void subscribe(MqSubscription subscription) {
        if (consumerContainer == null) {
            throw new MqCapabilityNotSupportedException(MqCapability.CONSUME, getType());
        }
        consumerContainer.subscribe(subscription);
    }

    @Override
    public void unsubscribe(String topic, String group) {
        if (consumerContainer == null
            || !consumerContainer.unsubscribe(topics.streamKey(topic), group)) {
            log.warn("取消订阅未生效（无匹配订阅）：topic={}, group={}", topic, group);
        }
    }

    /* ==================== 内部 ==================== */

    /**
     * 追加到目标 Stream
     * <p>
     * 字段通过 {@link MqRecordMapper} 生成——<b>生产端与消费端共用同一映射</b>，
     * 避免两端字段名各自演进导致漂移。
     * </p>
     */
    private SendResult append(MqMessage message) {
        String topic = message.topic();
        String streamKey = topics.streamKey(topic);
        try {
            String recordId = streamTemplate.add(streamKey,
                MqRecordMapper.toFields(message, payloadCodec), MqStreamOptions.of(properties, topic));
            return SendResult.success(message.messageId(), topic, recordId);
        } catch (RedisStreamException e) {
            log.warn("消息投递失败：topic={}, messageId={}", topic, message.messageId(), e);
            return SendResult.failure(SendStatus.TRANSPORT_UNAVAILABLE,
                message.messageId(), topic, e);
        }
    }

    /**
     * 解析主题生效的延时桶：主题配置优先，未配置时跟随原语层默认桶
     *
     * @param topic 主题
     * @return 桶名；原语层未配置任何桶时返回 {@code null}
     */
    private String resolveBucket(String topic) {
        String configured = properties.topicConfig(topic).getBucket();
        return configured != null ? configured : delayQueueManager.defaultBucket();
    }

    /**
     * 把延时异常映射为发送状态
     * <p>
     * 码值一律取自各模块的错误码枚举，<b>不再内联字面量</b>——原先硬编码的
     * {@code "50021"} / {@code "50023"} 是 Redis 原语层的码，原语层一改号这里就会
     * 静默失效（退化成笼统的 {@code SEND_FAILED}）。
     * </p>
     * <p>
     * {@code DELAY_OFFER_FAILED}（{@code 50020}）刻意不单独映射：它表示入队动作本身失败，
     * 属传输层临时故障，落到 {@code SEND_FAILED}（可重试）是恰当的——「桶未配置」这类
     * <b>永久性</b>错误已由 {@link #validateDelayBuckets()} 在启动期拦截，不会走到这里。
     * </p>
     */
    private SendStatus mapDelayStatus(Exception e) {
        String code = e instanceof GlobalException global ? global.getErrorCode() : null;
        // 必须先判 null：Set.of 生成的不可变集合对 contains(null) 会抛 NPE
        if (code == null) {
            return SendStatus.SEND_FAILED;
        }
        if (DELAY_QUEUE_FULL_CODES.contains(code)) {
            return SendStatus.DELAY_QUEUE_FULL;
        }
        if (DELAY_EXCEEDS_MAX_CODES.contains(code)) {
            return SendStatus.DELAY_EXCEEDS_MAX;
        }
        return SendStatus.SEND_FAILED;
    }
}
