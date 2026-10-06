package com.carlos.mq.client.redis;

import com.carlos.mq.config.MqProperties;
import com.carlos.mq.consumer.MqConsumerNameFactory;
import com.carlos.mq.consumer.RedisMqDeadLetterHandler;
import com.carlos.mq.core.ConsumeResult;
import com.carlos.mq.core.MqMessage;
import com.carlos.mq.metrics.MqMetrics;
import com.carlos.mq.spi.MqSubscription;
import com.carlos.redis.stream.RedisStreamProperties;
import com.carlos.redis.stream.RedisStreamTemplate;
import com.carlos.redis.stream.StreamEntry;
import com.carlos.redis.stream.StreamPendingEntry;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 待处理条目认领器
 *
 * <h2>解决什么问题</h2>
 * <p>
 * 消费者崩溃、或在确认前进程退出时，条目会留在待处理队列里。由于读取用的 offset 是
 * 「只取新消息」，这些条目<b>永远不会被自动重投</b>——既有实现因此表现为
 * 「永久滞留 + 待处理队列泄漏」。本认领器按空闲时长把它们接管过来重投。
 * </p>
 *
 * <h2>为什么走 XPENDING → XCLAIM 两步</h2>
 * <p>
 * Redisson 4.5.0 的 Stream 命令集里<b>没有 {@code XAUTOCLAIM}</b>（已实测确认），
 * 因此只能先 {@code XPENDING} 取明细、再 {@code XCLAIM} 认领。两步非原子，但
 * 「先查后认领」在并发下最坏结果是重复一次重投，而不是丢消息——符合保守优先。
 * </p>
 *
 * <h2>投递次数：不自己存，用待处理队列的计数器</h2>
 * <p>
 * {@code XCLAIM} 在不显式传 {@code RETRYCOUNT} 时会自行递增投递次数，这正是死信判定所依赖的
 * 语义；<b>显式传值会把计数重置</b>，导致重试永不终止、死信永不触发。
 * 因此本类只传空闲阈值与 id，绝不传重试次数。
 * </p>
 *
 * <h2>认领后「就地处理」而非「重新投回 Stream」</h2>
 * <p>
 * 重新 {@code XADD} 会产生新的 recordId、投递计数归 1 ⇒ 计数失去意义、死信永不触发、
 * 原待处理条目悬挂。就地处理保持待处理队列中的身份与计数。
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@Slf4j
public class RedisMqPendingReclaimer implements AutoCloseable {

    private final RedisStreamTemplate streamTemplate;
    private final RedisStreamProperties streamProperties;
    private final MqProperties properties;
    private final MqTopics topics;
    private final RedisMqDeadLetterHandler deadLetterHandler;
    private final MqConsumerNameFactory nameFactory;
    private final MqMetrics metrics;

    private ScheduledExecutorService scheduler;
    private List<MqSubscription> subscriptions = List.of();

    public RedisMqPendingReclaimer(RedisStreamTemplate streamTemplate,
                                   RedisStreamProperties streamProperties,
                                   MqProperties properties,
                                   MqTopics topics,
                                   RedisMqDeadLetterHandler deadLetterHandler,
                                   MqConsumerNameFactory nameFactory,
                                   MqMetrics metrics) {
        this.streamTemplate = streamTemplate;
        this.streamProperties = streamProperties;
        this.properties = properties;
        this.topics = topics;
        this.deadLetterHandler = deadLetterHandler;
        this.nameFactory = nameFactory;
        this.metrics = metrics;
    }

    /**
     * 启动认领轮询
     *
     * @param subscriptions 订阅描述（与消费容器共用同一份，避免两处各自计算消费组导致不一致）
     */
    public void start(List<MqSubscription> subscriptions) {
        Duration interval = properties.getConsumer().getPendingClaimInterval();
        if (interval == null || interval.isZero() || interval.isNegative()) {
            log.info("待处理认领已关闭（pending-claim-interval 为 0），残留条目不会被自动重投");
            return;
        }
        this.subscriptions = List.copyOf(subscriptions);
        this.scheduler = Executors.newSingleThreadScheduledExecutor(
            Thread.ofPlatform().daemon().name("mq-pending-reclaimer", 0).factory());
        long millis = Math.max(1000L, interval.toMillis());
        scheduler.scheduleWithFixedDelay(this::reclaimSafely, millis, millis, TimeUnit.MILLISECONDS);
        log.info("待处理认领器已启动：间隔={}, 空闲阈值={}, 订阅={}",
            interval, properties.getConsumer().getPendingClaimMinIdle(), subscriptions.size());
    }

    /**
     * 逐个订阅执行认领，<b>单个订阅失败不影响其余订阅</b>
     * <p>
     * 此前把整个 {@code for} 循环包在一个 try 里：某个订阅抛异常会<b>中断对后续订阅的处理</b>，
     * 表现为「排在它后面的主题那一轮根本不会被认领」。这类问题只在特定订阅组合下出现，
     * 且表现为「重投偶发不生效」，极难定位。
     * </p>
     * <p>
     * 认领承担的是<b>自愈</b>职责，任何单个订阅的问题都不应让其他主题失去重投机会。
     * </p>
     */
    private void reclaimSafely() {
        for (MqSubscription subscription : subscriptions) {
            try {
                reclaimOnce(subscription);
            } catch (Throwable t) {
                // 轮询本身必须永远存活；失败范围限于本轮该订阅
                log.error("待处理认领失败（继续处理其余订阅）：{}", subscription.display(), t);
            }
        }
    }

    /**
     * 对单个订阅执行一次认领（包级可见，便于测试直接驱动）
     *
     * @param subscription 订阅
     */
    void reclaimOnce(MqSubscription subscription) {
        String streamKey = topics.streamKey(subscription.topic());
        String group = subscription.group();
        Duration minIdle = properties.getConsumer().getPendingClaimMinIdle();
        int batch = streamProperties.getClaimBatchSize();

        List<StreamPendingEntry> pending =
            streamTemplate.pending(streamKey, group, minIdle, batch);
        if (pending.isEmpty()) {
            return;
        }
        int maxDelivery = resolveMaxDelivery(subscription);

        List<String> toClaim = new ArrayList<>();
        Map<String, Long> deliveryCounts = new LinkedHashMap<>();
        for (StreamPendingEntry entry : pending) {
            if (maxDelivery > 0 && entry.deliveryCount() >= maxDelivery) {
                expire(subscription, streamKey, group, entry);
            } else {
                toClaim.add(entry.recordId());
                deliveryCounts.put(entry.recordId(), entry.deliveryCount());
            }
        }
        if (toClaim.isEmpty()) {
            return;
        }
        String consumerName = resolveReclaimConsumer(subscription);
        List<StreamEntry> claimed = streamTemplate.claim(streamKey, group, consumerName,
            minIdle, toClaim);
        for (StreamEntry entry : claimed) {
            handleClaimed(subscription, streamKey, group, entry, consumerName,
                claimedDeliveryCount(deliveryCounts.get(entry.recordId())));
        }
        log.debug("认领完成：{}, 认领={}, 转死信={}", subscription.display(),
            claimed.size(), pending.size() - toClaim.size());
    }

    /**
     * 超过投递上限：写死信并把原条目移出待处理队列
     */
    private void expire(MqSubscription subscription, String streamKey, String group,
                        StreamPendingEntry entry) {
        List<StreamEntry> entries = streamTemplate.range(streamKey, Integer.MAX_VALUE);
        StreamEntry source = entries.stream()
            .filter(item -> item.recordId().equals(entry.recordId()))
            .findFirst()
            .orElse(null);
        if (source != null) {
            MqMessage message = MqRecordMapper.toMessage(source.fields(), subscription.topic())
                .withRecordId(entry.recordId())
                .withDeliveryCount((int) entry.deliveryCount());
            deadLetterHandler.deadLetter(message,
                "投递次数达上限 %d（最后处理者 %s）".formatted(entry.deliveryCount(),
                    entry.consumerName()));
        } else {
            log.error("投递超限但读不到原条目（可能已被修剪）：topic={}, recordId={}",
                subscription.topic(), entry.recordId());
        }
        // 无论是否取到原文，都必须把原条目移出待处理队列，否则会反复触发
        streamTemplate.ack(streamKey, group, List.of(entry.recordId()));
        metrics.recordDlq(subscription.topic());
    }

    /**
     * 处理认领到的条目（就地处理，不重新投递）
     */
    private void handleClaimed(MqSubscription subscription, String streamKey, String group,
                               StreamEntry entry, String consumerName, long deliveryCount) {
        String recordId = entry.recordId();
        try {
            MqMessage envelope = MqMessage.fromRecord(
                MqRecordMapper.toMessage(entry.fields(), subscription.topic()),
                recordId, consumerName, (int) deliveryCount);
            ConsumeResult result = subscription.handler().onMessage(envelope);
            if (result == null) {
                result = ConsumeResult.success(envelope.messageId(), subscription.topic());
            }
            if (result.shouldAck()) {
                streamTemplate.ack(streamKey, group, List.of(recordId));
            }
            metrics.recordConsume(subscription.topic(), result);
            metrics.recordReclaim(subscription.topic());
            if (result.getStatus() == ConsumeResult.ConsumeStatus.DROP) {
                deadLetterHandler.deadLetter(envelope, result.getErrorMessage());
            }
        } catch (Exception e) {
            log.error("认领后处理失败，留待下次认领：topic={}, recordId={}",
                subscription.topic(), recordId, e);
        }
    }

    /**
     * 认领后该条目实际的投递次数
     * <p>
     * {@code XPENDING} 给出的是<b>认领前</b>的计数，而 {@code XCLAIM}（未显式传
     * {@code RETRYCOUNT} 时）会把计数<b>自增 1</b>。若把认领前的值直接交给处理器，
     * 处理器看到的计数就<b>永远滞后一次</b>——实测表现为「重投确实发生了，但每次都被
     * 告知是第 1 次投递」。依赖 {@code deliveryCount} 做退避或上限判断的业务会整体偏移。
     * </p>
     * <p>
     * {@code claim()} 只返回<b>确实被认领</b>的条目（空闲时长不足的不会返回），
     * 因此对每个返回值 +1 是精确的，无需再查一次 {@code XPENDING}。
     * </p>
     *
     * @param countBeforeClaim 认领前 {@code XPENDING} 给出的计数；缺失时为 {@code null}
     * @return 本次投递的次数
     */
    private static long claimedDeliveryCount(Long countBeforeClaim) {
        return (countBeforeClaim == null ? 1L : countBeforeClaim) + 1;
    }

    /**
     * 解析生效的投递次数上限
     * <p>
     * 订阅上的值已由上层把「-1 表示用全局」解析过，此处仅兜底。
     * </p>
     */
    private int resolveMaxDelivery(MqSubscription subscription) {
        return subscription.maxDeliveryCount() > 0
            ? subscription.maxDeliveryCount()
            : properties.getConsumer().getMaxDeliveryCount();
    }

    private String resolveReclaimConsumer(MqSubscription subscription) {
        String explicit = subscription.consumerId();
        return explicit == null || explicit.isBlank() ? nameFactory.next() : explicit;
    }

    /**
     * 停止认领（幂等）
     */
    public void stop() {
        ScheduledExecutorService current = scheduler;
        scheduler = null;
        if (current != null) {
            current.shutdownNow();
            try {
                current.awaitTermination(properties.getConsumer().getCloseTimeout().toMillis(),
                    TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 是否运行中
     *
     * @return 运行中返回 true
     */
    public boolean isRunning() {
        ScheduledExecutorService current = scheduler;
        return current != null && !current.isShutdown();
    }

    @Override
    public void close() {
        stop();
    }

    /**
     * 订阅列表（诊断用）
     *
     * @return 订阅列表
     */
    public List<MqSubscription> subscriptions() {
        return subscriptions;
    }
}
