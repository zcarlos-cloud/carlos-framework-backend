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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.Subscription;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Redis Stream 消费容器
 *
 * <h2>用官方容器而非自驱循环</h2>
 * <p>
 * Spring Data 的 {@link StreamMessageListenerContainer} 内置轮询任务、可注入错误处理器
 * （<b>读异常不会杀死循环</b>）并实现 {@code SmartLifecycle} 提供优雅关闭。
 * 既有 {@code RedisStreamMessageQueue} 的自驱循环恰好在「读异常杀死循环」这点上写错，
 * 且它本质是「单次阻塞读、不是循环」，因此 {@code consume()} 只可能被调用一次。
 * </p>
 *
 * <h2>先处理成功、再确认（本框架最重要的可靠性约束）</h2>
 * <p>
 * {@code XREADGROUP} 无论是否确认都会把条目放入待处理队列，因此「不确认」恰好等价于
 * 「留在待处理队列等重投」——这是可靠消费设计的支点。
 * </p>
 * <p>
 * 本类只在 {@link ConsumeResult#shouldAck()} 为真时确认，且<b>确认的入参只能来自单条已处理的
 * record</b>。既有实现用「读到的全部 record」而非「成功处理的那些」来确认，导致毒消息被确认后
 * 静默丢弃；本实现从结构上排除了该错误——确认语句的作用域里不存在「读到的全部」这个集合。
 * </p>
 * <p>
 * 每个订阅使用独立容器，以便各自尊重自己的 {@code batchSize}（容器选项是全局的）。
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@Slf4j
public class RedisMqConsumerContainer implements AutoCloseable {

    private final RedisConnectionFactory connectionFactory;
    private final RedisStreamTemplate streamTemplate;
    private final MqTopics topics;
    private final MqProperties properties;
    private final RedisStreamProperties streamProperties;
    private final MqConsumerNameFactory nameFactory;
    private final RedisMqDeadLetterHandler deadLetterHandler;
    private final MqMetrics metrics;

    private final List<StreamMessageListenerContainer<String, MapRecord<String, String, String>>> containers
        = new ArrayList<>();
    private final List<ExecutorService> executors = new ArrayList<>();
    private final AtomicInteger subscriptionCount = new AtomicInteger();

    /** 按「流 key@消费组」记录订阅句柄，以支持取消订阅 */
    private final Map<String, List<Subscription>> handlesByGroup = new LinkedHashMap<>();

    public RedisMqConsumerContainer(RedisConnectionFactory connectionFactory,
                                    RedisStreamTemplate streamTemplate,
                                    MqTopics topics,
                                    MqProperties properties,
                                    RedisStreamProperties streamProperties,
                                    MqConsumerNameFactory nameFactory,
                                    RedisMqDeadLetterHandler deadLetterHandler,
                                    MqMetrics metrics) {
        this.connectionFactory = connectionFactory;
        this.streamTemplate = streamTemplate;
        this.topics = topics;
        this.properties = properties;
        this.streamProperties = streamProperties;
        this.nameFactory = nameFactory;
        this.deadLetterHandler = deadLetterHandler;
        this.metrics = metrics;
    }

    /**
     * 注册一个订阅（幂等方式建组 + 按并发度注册消费者 + 启动容器）
     *
     * @param subscription 订阅描述
     */
    public void subscribe(MqSubscription subscription) {
        String streamKey = topics.streamKey(subscription.topic());
        String group = subscription.group();
        streamTemplate.createGroupIfAbsent(streamKey, group, null);

        ExecutorService executor = Executors.newFixedThreadPool(
            subscription.concurrency(),
            Thread.ofPlatform().daemon()
                .name("mq-consumer-" + subscription.topic() + "-", 0).factory());
        executors.add(executor);

        StreamMessageListenerContainer<String, MapRecord<String, String, String>> container =
            StreamMessageListenerContainer.create(connectionFactory,
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions
                    .<String, MapRecord<String, String, String>>builder()
                    .pollTimeout(streamProperties.getBlockTimeout())
                    .batchSize(subscription.batchSize())
                    .executor(executor)
                    // 读异常只记日志，绝不让轮询任务退出（既有实现正是在此永久死亡）
                    .errorHandler(error -> {
                        if (isShutdownInterrupt(error)) {
                            // 停止容器会打断阻塞中的读取，这是预期行为而非故障。
                            // 报成 ERROR 会让每次停机都刷出看似故障的日志，误导排障。
                            log.debug("MQ 消费容器读取被中断（正在关闭）：topic={}",
                                subscription.topic());
                            return;
                        }
                        log.error("MQ 消费容器读取异常（容器继续运行）：topic={}",
                            subscription.topic(), error);
                    })
                    .build());

        for (int index = 0; index < subscription.concurrency(); index++) {
            String consumerName = resolveConsumerName(subscription, index);
            Subscription handle = container.receive(Consumer.from(group, consumerName),
                StreamOffset.create(streamKey, ReadOffset.lastConsumed()),
                record -> handle(subscription, streamKey, group, consumerName, record));
            handlesByGroup.computeIfAbsent(streamKey + "@" + group, ignored -> new ArrayList<>())
                .add(handle);
            subscriptionCount.incrementAndGet();
            log.info("订阅就绪：topic={}, group={}, consumer={}, batchSize={}, concurrency={}",
                subscription.topic(), group, consumerName, subscription.batchSize(),
                subscription.concurrency());
        }
        container.start();
        containers.add(container);
        metrics.registerPending(streamKey, group);
    }

    /**
     * 处理单条记录 —— <b>先处理成功、再确认</b>
     */
    private void handle(MqSubscription subscription, String streamKey, String group,
                        String consumerName, MapRecord<String, String, String> record) {
        String recordId = record.getId().getValue();
        try {
            MqMessage envelope = MqMessage.fromRecord(
                MqRecordMapper.toMessage(record.getValue(), subscription.topic()),
                recordId, consumerName, 1);
            ConsumeResult result = subscription.handler().onMessage(envelope);
            if (result == null) {
                result = ConsumeResult.success(envelope.messageId(), subscription.topic());
            }
            if (result.shouldAck()) {
                streamTemplate.ack(streamKey, group, List.of(recordId));
            }
            metrics.recordConsume(subscription.topic(), result);
            if (result.getStatus() == ConsumeResult.ConsumeStatus.DROP) {
                deadLetterHandler.deadLetter(envelope, result.getErrorMessage());
            }
        } catch (Exception e) {
            // 处理链自身的异常（非业务异常）：不确认，留给认领器重投
            log.error("处理消息异常，不确认（等待重投）：topic={}, recordId={}",
                subscription.topic(), recordId, e);
            metrics.recordConsumeError(subscription.topic());
        }
    }

    /**
     * 取消某个（流, 消费组）下的全部订阅句柄
     *
     * @param streamKey 流 key
     * @param group     消费组
     * @return 是否有订阅被取消
     */
    public boolean unsubscribe(String streamKey, String group) {
        List<Subscription> handles = handlesByGroup.remove(streamKey + "@" + group);
        if (handles == null || handles.isEmpty()) {
            return false;
        }
        for (Subscription handle : handles) {
            try {
                handle.cancel();
                subscriptionCount.decrementAndGet();
            } catch (Exception e) {
                log.warn("取消订阅失败：stream={}, group={}", streamKey, group, e);
            }
        }
        log.info("已取消订阅：stream={}, group={}, 句柄数={}", streamKey, group, handles.size());
        return true;
    }

    private String resolveConsumerName(MqSubscription subscription, int index) {
        String explicit = subscription.consumerId();
        if (explicit != null && !explicit.isBlank()) {
            return subscription.concurrency() > 1 ? explicit + "#" + index : explicit;
        }
        return subscription.concurrency() > 1 ? nameFactory.next(index) : nameFactory.next();
    }

    /**
     * 当前活跃订阅数
     *
     * @return 订阅数
     */
    public int activeSubscriptionCount() {
        return subscriptionCount.get();
    }

    /**
     * 是否运行中
     *
     * @return 有容器运行则返回 true
     */
    public boolean isRunning() {
        return containers.stream().anyMatch(StreamMessageListenerContainer::isRunning);
    }

    /**
     * 停止消费（先停订阅 → 再关线程池）
     */
    public void stop() {
        for (StreamMessageListenerContainer<String, MapRecord<String, String, String>> container : containers) {
            try {
                container.stop();
            } catch (Exception e) {
                log.warn("停止消费容器失败", e);
            }
        }
        containers.clear();
        handlesByGroup.clear();
        subscriptionCount.set(0);
        Duration timeout = properties.getConsumer().getCloseTimeout();
        for (ExecutorService executor : executors) {
            executor.shutdownNow();
            try {
                if (!executor.awaitTermination(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                    log.warn("消费线程池未在 {} 内退出", timeout);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        executors.clear();
        log.info("MQ 消费容器已停止");
    }

    /**
     * 判断读异常是否只是「容器正在关闭」导致的正常中断
     * <p>
     * 必须走完整条 cause 链：底层中断会被连接适配器逐层包装
     * （{@code InvalidDataAccessApiUsageException: java.lang.InterruptedException}），
     * 只看最外层类型会漏判。
     * </p>
     */
    private static boolean isShutdownInterrupt(Throwable error) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (current instanceof InterruptedException) {
                return true;
            }
            if (current.getCause() == current) {
                break;
            }
        }
        return false;
    }

    @Override
    public void close() {
        stop();
    }
}
