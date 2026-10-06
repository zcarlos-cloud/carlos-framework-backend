package com.carlos.mq;

import com.carlos.mq.annotation.MqListener;
import com.carlos.mq.client.redis.MqTopics;
import com.carlos.mq.client.redis.RedisMqConsumerContainer;
import com.carlos.mq.client.redis.RedisMqPendingReclaimer;
import com.carlos.mq.core.ConsumeResult;
import com.carlos.mq.core.MqMessage;
import com.carlos.mq.core.MqTemplate;
import com.carlos.redis.stream.RedisStreamTemplate;
import com.carlos.redis.stream.StreamEntry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.stream.StreamInfo;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 消费侧真机集成测试
 *
 * <h2>为什么单独一个类</h2>
 * <p>
 * 本类验证的是模块<b>最核心的可靠性承诺</b>，且这些承诺<b>只可能</b>在真实 Redis 上验证——
 * 它们全部建立在「{@code XREADGROUP} 无论是否 ACK 都会把条目放进待处理队列（PEL）」
 * 这一 Redis 语义之上。mock 掉 Redis 就等于 mock 掉了被验证的东西。
 * </p>
 *
 * <h2>被验证的语义（逐条对应实现里的注释）</h2>
 * <ol>
 *   <li><b>先处理成功、再确认</b>：handler 正常返回 → {@code XACK} → <b>PEL 清空</b>；</li>
 *   <li><b>处理失败不确认</b>：handler 抛异常 → 不 ACK → <b>PEL 保留</b>；</li>
 *   <li><b>认领重投并递增投递次数</b>：认领器按空闲阈值接管，handler 看到
 *       {@code deliveryCount} 增长（计数来自 PEL 自身，非框架自己存）；</li>
 *   <li><b>达投递上限转死信</b>：超过 {@code maxDeliveryCount} 后消息进入死信 Stream；</li>
 *   <li><b>显式 DROP = 确认 + 写死信</b>：{@code ConsumeResult.drop(...)} 既 ACK 又落死信。</li>
 * </ol>
 * <p>
 * 第 1 与第 2 条是一对：<b>成功则 PEL 空、失败则 PEL 非空</b>。这一对断言是整个可靠消费
 * 设计的支点，此前从未在真机上验过。
 * </p>
 *
 * <h2>与发送侧测试的隔离</h2>
 * <p>
 * 本类用<b>不同的主题与配置</b>（需要开启认领器，发送侧测试刻意关闭），因此会另起一个
 * Spring 上下文。监听器用 {@code @TestConfiguration} 注册而<b>不是</b>
 * {@code @Component}：{@code @TestConfiguration} 不被组件扫描拾取，从而不会污染
 * {@link MqIntegrationTest} 的上下文（否则那一侧会莫名多出订阅）。
 * </p>
 * <p>
 * 库与键前缀隔离、自清理方式同 {@link MqIntegrationTest}：db 3、前缀
 * {@code framework:mq:ittest:}，{@code @AfterAll} 按前缀删除并打印实际键名。
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@SpringBootTest(
    classes = {MqConsumerIntegrationTest.TestApplication.class,
        MqConsumerIntegrationTest.ListenerConfig.class},
    properties = {
        "spring.data.redis.host=${CARLOS_TEST_REDIS_HOST:}",
        "spring.data.redis.port=${CARLOS_TEST_REDIS_PORT:6379}",
        "spring.data.redis.password=${CARLOS_TEST_REDIS_PASSWORD:}",
        "spring.data.redis.database=3",
        "carlos.mq.type=redis",
        "carlos.redis.stream.key-prefix=" + MqConsumerIntegrationTest.STREAM_PREFIX,
        // 本类不验证延时，关掉以免多起一组 Redisson 转移线程
        "carlos.redis.delay.enabled=false",
        // 认领器必须开启：重投与死信都依赖它
        "carlos.mq.consumer.pending-claim-interval=1s",
        // 阈值取小值以缩短测试；处理器是瞬时的，不存在「抢走正在处理的消息」
        "carlos.mq.consumer.pending-claim-min-idle=2s",
        "carlos.mq.consumer.max-delivery-count=2",
        "carlos.redis.rate-limit.enabled=false"
    })
@EnabledIfEnvironmentVariable(named = "CARLOS_TEST_REDIS_HOST", matches = ".+")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("MQ 真机集成（消费侧：确认 / 重投 / 死信）")
class MqConsumerIntegrationTest {

    static final String STREAM_PREFIX = "framework:mq:ittest:";

    /** 死信条目的负载字段名（自解释格式，不同于普通条目的 {@code data}） */
    private static final String DLQ_FIELD_PAYLOAD = "payload";

    private static final Logger log = LoggerFactory.getLogger(MqConsumerIntegrationTest.class);

    /** 正常消费：void 返回 ⇒ 视为成功 ⇒ ACK */
    private static final String OK_TOPIC = "ittest-consume-topic";
    private static final String OK_GROUP = "ittest-consume-group";

    /** 总是抛异常 ⇒ 不 ACK ⇒ 认领重投 ⇒ 达上限转死信 */
    private static final String RETRY_TOPIC = "ittest-retry-topic";
    private static final String RETRY_GROUP = "ittest-retry-group";

    /** 显式 DROP ⇒ ACK + 写死信 */
    private static final String DROP_TOPIC = "ittest-drop-topic";
    private static final String DROP_GROUP = "ittest-drop-group";

    @SpringBootApplication
    public static class TestApplication {
    }

    /** 监听器注册：{@code @TestConfiguration} 不被组件扫描拾取，故不会串到发送侧上下文 */
    @TestConfiguration
    public static class ListenerConfig {

        @Bean
        RecordingListener recordingListener() {
            return new RecordingListener();
        }

        @Bean
        RetryListener retryListener() {
            return new RetryListener();
        }

        @Bean
        DroppingListener droppingListener() {
            return new DroppingListener();
        }
    }

    /**
     * 监听器与测试之间的观测点
     * <p>
     * 用静态状态是因为监听器 bean 由 Spring 创建，测试无法直接持有引用；
     * 用并发队列而非闩锁，是为了「可轮询、可多次投递累积」，且天然线程安全
     * （handler 跑在消费线程 / 认领器线程，断言跑在测试线程）。
     * </p>
     */
    public static final class Signals {

        /** 正常消费收到的 orderId */
        static final ConcurrentLinkedQueue<String> received = new ConcurrentLinkedQueue<>();

        /** 重投场景每次投递看到的 deliveryCount */
        static final ConcurrentLinkedQueue<Integer> retryDeliveryCounts =
            new ConcurrentLinkedQueue<>();

        /** DROP 场景收到过的 orderId */
        static final ConcurrentLinkedQueue<String> dropped = new ConcurrentLinkedQueue<>();

        private Signals() {
        }

        static void reset() {
            received.clear();
            retryDeliveryCounts.clear();
            dropped.clear();
        }
    }

    /** 形状一：只接业务负载；void 返回即成功 */
    public static class RecordingListener {

        @MqListener(topic = OK_TOPIC, group = OK_GROUP)
        public void onOrder(ConsumePayload payload) {
            Signals.received.add(payload.orderId());
        }
    }

    /** 形状二：负载 + 信封；总是抛异常以触发重投直至死信 */
    public static class RetryListener {

        @MqListener(topic = RETRY_TOPIC, group = RETRY_GROUP, maxDeliveryCount = 2)
        public void onOrder(ConsumePayload payload, MqMessage envelope) {
            Signals.retryDeliveryCounts.add(envelope.deliveryCount());
            throw new IllegalStateException("刻意失败以验证重投与死信：orderId=" + payload.orderId());
        }
    }

    /** 显式控制结果：DROP ⇒ ACK + 死信 */
    public static class DroppingListener {

        @MqListener(topic = DROP_TOPIC, group = DROP_GROUP)
        public ConsumeResult onOrder(ConsumePayload payload) {
            Signals.dropped.add(payload.orderId());
            return ConsumeResult.drop(null, DROP_TOPIC, "刻意放弃：orderId=" + payload.orderId());
        }
    }

    /** 测试用负载 */
    public record ConsumePayload(String orderId) {
    }

    @Autowired
    private MqTemplate mqTemplate;

    @Autowired
    private RedisStreamTemplate streamTemplate;

    @Autowired
    private MqTopics topics;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /** 仅用于 @AfterAll 先停消费者再清理，避免制造误导性的 NOGROUP 噪音 */
    @Autowired
    private RedisMqConsumerContainer consumerContainer;

    /** 同上：认领器也必须在删键前停掉 */
    @Autowired
    private RedisMqPendingReclaimer reclaimer;

    @BeforeEach
    void resetSignals() {
        Signals.reset();
    }

    @AfterAll
    void cleanUp() {
        // 先停消费者与认领器，再删键。
        //
        // 否则二者会继续轮询刚被删掉的键，在日志里刷出一串 NOGROUP —— 那是本测试自己制造的
        // 假象：本类开发过程中就因此把「测试删键」误判成「认领器存在空 PEL 缺陷」，
        // 白排查了一轮。清理不能给排障留下误导性痕迹。
        consumerContainer.stop();
        reclaimer.stop();

        Set<String> matched = new LinkedHashSet<>();
        redissonClient.getKeys().getKeysByPattern(STREAM_PREFIX + "*").forEach(matched::add);
        long deleted = redissonClient.getKeys().delete(matched.toArray(String[]::new));
        log.info("消费侧集成测试清理：删除 {} 个键 {}", deleted, matched);
    }

    /* ==================== 1 & 2：成功则确认、失败则不确认 ==================== */

    @Test
    @DisplayName("处理成功 → 已确认：handler 收到消息，且待处理队列清空")
    void should_ack_after_successful_handling() {
        String orderId = "ok-" + System.nanoTime();

        mqTemplate.send(OK_TOPIC, new ConsumePayload(orderId));

        assertTrue(awaitNonEmpty(Signals.received, Duration.ofSeconds(20)),
            "handler 未收到消息 —— 消费链路未接通");
        assertEquals(orderId, Signals.received.peek());

        // 支点：处理成功 ⇒ 已 ACK ⇒ PEL 为空
        assertTrue(awaitPendingZero(OK_TOPIC, OK_GROUP, Duration.ofSeconds(10)),
            "处理成功后待处理队列仍有残留 —— 说明没有「先处理成功再确认」");
    }

    @Test
    @DisplayName("处理失败 → 不确认：抛异常的条目留在待处理队列，并出现重投（投递次数递增）")
    void should_keep_pending_and_redeliver_after_failure() {
        mqTemplate.send(RETRY_TOPIC, new ConsumePayload("retry-" + System.nanoTime()));

        // 首次投递：deliveryCount = 1
        assertTrue(awaitNonEmpty(Signals.retryDeliveryCounts, Duration.ofSeconds(20)),
            "handler 未被调用");

        // 失败 ⇒ 不 ACK ⇒ PEL 保留（与上一条用例的成功路径形成对照）
        assertTrue(pendingCount(RETRY_TOPIC, RETRY_GROUP) > 0,
            "处理失败后待处理队列为空 —— 说明异常路径把它 ACK 了，消息会丢");

        // 认领器接管并重投：投递次数必须递增（计数来自 PEL 自身，不是框架自己存的）
        assertTrue(awaitDeliveryCountAtLeast(2, Duration.ofSeconds(30)),
            "未观察到重投（deliveryCount 未增长）—— 认领器未生效。已观察到："
                + Signals.retryDeliveryCounts);
    }

    /* ==================== 3：达上限转死信 ==================== */

    @Test
    @DisplayName("超过投递上限 → 转入死信 Stream，且死信内容自解释")
    void should_move_to_dead_letter_after_exhausting_deliveries() {
        String orderId = "dlq-" + System.nanoTime();

        mqTemplate.send(RETRY_TOPIC, new ConsumePayload(orderId));

        // maxDeliveryCount = 2，认领器在投递次数达上限后转死信
        StreamEntry dead = awaitDeadLetter(RETRY_TOPIC, orderId, Duration.ofSeconds(40));

        assertNotNull(dead, "超过投递上限后消息未进入死信 Stream");
        assertEquals(orderId, dlqPayloadOrderId(dead), "死信中的负载与原文不一致");
        assertEquals(RETRY_TOPIC, dead.field("originalTopic"));
        assertNotNull(dead.field("reason"), "死信必须带失败原因，否则无法排查");
        assertNotNull(dead.field("deliveryCount"), "死信必须带投递次数");
    }

    /* ==================== 4：DROP = 确认 + 死信 ==================== */

    @Test
    @DisplayName("显式 DROP → 既确认又落死信（这是 DROP 与 RETRY 的区别）")
    void should_ack_and_dead_letter_on_drop() {
        String orderId = "drop-" + System.nanoTime();

        mqTemplate.send(DROP_TOPIC, new ConsumePayload(orderId));

        assertTrue(awaitNonEmpty(Signals.dropped, Duration.ofSeconds(20)), "handler 未被调用");

        StreamEntry dead = awaitDeadLetter(DROP_TOPIC, orderId, Duration.ofSeconds(20));
        assertNotNull(dead, "DROP 的消息未进入死信 Stream");

        // 与 RETRY 的关键区别：DROP 会确认
        assertTrue(awaitPendingZero(DROP_TOPIC, DROP_GROUP, Duration.ofSeconds(10)),
            "DROP 之后待处理队列仍有残留 —— DROP 应当既确认又落死信");
    }

    /* ==================== 辅助 ==================== */

    /**
     * 待处理条目数
     * <p>
     * <b>刻意用 {@code XINFO GROUPS} 而非 {@code XPENDING}</b>：本环境的连接工厂是
     * {@code RedissonConnectionFactory}，其 {@code XPENDING} 实现遇到<b>空 PEL</b> 时会抛
     * {@code NoSuchElementException: No value present}（真机实测），无法用它断言「PEL 已清空」。
     * {@code XINFO GROUPS} 的 {@code pending} 字段是权威计数，且不受该缺陷影响。
     * </p>
     *
     * @return 待处理条目数；消费组不存在时返回 -1
     */
    private long pendingCount(String topic, String group) {
        byte[] key = topics.streamKey(topic).getBytes(StandardCharsets.UTF_8);
        Long pending = stringRedisTemplate.execute((RedisCallback<Long>) connection -> {
            for (StreamInfo.XInfoGroup info : connection.streamCommands().xInfoGroups(key)) {
                if (group.equals(info.groupName())) {
                    return (long) info.pendingCount();
                }
            }
            return -1L;
        });
        return pending == null ? -1L : pending;
    }

    private boolean awaitPendingZero(String topic, String group, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (pendingCount(topic, group) == 0) {
                return true;
            }
            sleep();
        }
        return false;
    }

    private boolean awaitNonEmpty(ConcurrentLinkedQueue<?> queue, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (!queue.isEmpty()) {
                return true;
            }
            sleep();
        }
        return false;
    }

    private boolean awaitDeliveryCountAtLeast(int expected, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (Signals.retryDeliveryCounts.stream().anyMatch(count -> count >= expected)) {
                return true;
            }
            sleep();
        }
        return false;
    }

    /** 轮询死信 Stream，直到出现指定 orderId 的条目 */
    private StreamEntry awaitDeadLetter(String topic, String orderId, Duration timeout) {
        String dlqKey = topics.dlqKey(topic);
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            for (StreamEntry entry : streamTemplate.range(dlqKey, Integer.MAX_VALUE)) {
                if (orderId.equals(dlqPayloadOrderId(entry))) {
                    return entry;
                }
            }
            sleep();
        }
        return null;
    }

    /**
     * 从死信条目里取出原负载的 orderId
     * <p>
     * 注意字段名是 {@code payload} 而<b>不是</b>普通 Stream 条目的 {@code data}：
     * 死信是一份<b>自解释</b>的记录（见 {@code RedisMqDeadLetterHandler}），
     * 字段集与普通消息不同。
     * </p>
     */
    private static String dlqPayloadOrderId(StreamEntry entry) {
        String payload = entry.field(DLQ_FIELD_PAYLOAD);
        if (payload == null) {
            return null;
        }
        // 形如 {"orderId":"xxx"}；刻意不引 JSON 解析器，避免测试依赖实现细节
        int start = payload.indexOf(":\"");
        int end = payload.lastIndexOf('"');
        return start < 0 || end <= start + 2 ? payload : payload.substring(start + 2, end);
    }

    private void sleep() {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
