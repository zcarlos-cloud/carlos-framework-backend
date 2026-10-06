package com.carlos.mq;

import com.carlos.mq.client.redis.MqTopics;
import com.carlos.mq.core.MqCapability;
import com.carlos.mq.core.MqTemplate;
import com.carlos.mq.core.SendResult;
import com.carlos.redis.delay.RedissonDelayedQueueManager;
import com.carlos.redis.stream.RedisStreamTemplate;
import com.carlos.redis.stream.StreamEntry;
import org.junit.jupiter.api.AfterAll;
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

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MQ 模块真机集成测试
 *
 * <h2>为什么必须有这个测试类</h2>
 * <p>
 * 本模块此前<b>全部是单元测试</b>，而它历史上出问题的每一处——延时链路从未接上、
 * 投递计数、先处理后确认——都<b>只在真实 Redis 上才会暴露</b>：
 * 单元测试用 mock 替掉了全部外部依赖，恰好替掉了出问题的那些部分。
 * 最典型的是延时链路的根因「没有任何 {@code DelayDueHandler} 实现」——
 * 一个全部依赖都被 mock 的测试环境<b>永远发现不了</b>。
 * </p>
 * <p>
 * 本测试启动<b>完整的 Spring 上下文</b>，覆盖单个类测不到的三层：
 * </p>
 * <ol>
 *   <li><b>自动配置装配</b>——尤其是 {@code DelayDueHandler} 能否被原语层的管理器取到
 *       （跨两个自动配置类，静态推断不可靠）；</li>
 *   <li><b>真实 Redisson 行为</b>——{@code RDelayedQueue} 的转移是否真会把到期条目放出来；</li>
 *   <li><b>真实 Redis 语义</b>——{@code XADD} / 键名 / 字段是否如预期落盘。</li>
 * </ol>
 *
 * <h2>运行方式</h2>
 * <p>
 * 需要真实 Redis；<b>未设置 {@code CARLOS_TEST_REDIS_HOST} 时整类跳过</b>，
 * 因此 CI 无需 Redis 也不会红。凭据只从环境变量读，<b>不写入任何文件</b>：
 * </p>
 * <pre>
 * export CARLOS_TEST_REDIS_HOST=...  CARLOS_TEST_REDIS_PORT=...
 * export CARLOS_TEST_REDIS_PASSWORD=...
 * mvn test -Dtest=MqIntegrationTest
 * </pre>
 *
 * <h2>环境隔离（本测试跑在共享实例上）</h2>
 * <ul>
 *   <li><b>库隔离</b>：固定 db 3，与开发库 db0（缓存 / oauth2 token）、db1 / db5、
 *       样例库 db2 完全隔离；</li>
 *   <li><b>键前缀隔离</b>：{@code framework:mq:ittest:}（Stream）与 {@code framework:mq:delay:ittest}
 *       （延时结构），与样例、生产的 {@code framework:mq:stream:} 不重名；</li>
 *   <li><b>自清理</b>：{@link #cleanUp()} 按前缀删除本次产生的全部键。</li>
 * </ul>
 */
@SpringBootTest(
    classes = MqIntegrationTest.TestApplication.class,
    properties = {
        "spring.data.redis.host=${CARLOS_TEST_REDIS_HOST:}",
        "spring.data.redis.port=${CARLOS_TEST_REDIS_PORT:6379}",
        "spring.data.redis.password=${CARLOS_TEST_REDIS_PASSWORD:}",
        "spring.data.redis.database=3",
        "carlos.mq.type=redis",
        "carlos.redis.stream.key-prefix=" + MqIntegrationTest.STREAM_PREFIX,
        "carlos.redis.delay.enabled=true",
        "carlos.redis.delay.buckets[0]=" + MqIntegrationTest.BUCKET,
        // 关闭认领轮询：本测试不引入「他人重投」这一变量
        "carlos.mq.consumer.pending-claim-interval=0",
        // 本模块无 web 依赖（传输层不需要 servlet），而 redis starter 的限流切面硬引用
        // jakarta.servlet 且默认启用（无 @ConditionalOnClass 守卫）——关掉它，
        // 避免与「本测试要验证的 MQ 装配」互相干扰
        "carlos.redis.rate-limit.enabled=false"
    })
@EnabledIfEnvironmentVariable(named = "CARLOS_TEST_REDIS_HOST", matches = ".+")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("MQ 真机集成（装配 / 即时投递 / 延时投递）")
class MqIntegrationTest {

    /** Stream 键前缀（形如 {@code framework:mq:ittest:<topic>}） */
    static final String STREAM_PREFIX = "framework:mq:ittest:";

    /** 延时桶名 */
    static final String BUCKET = "ittest";

    private static final Logger log = LoggerFactory.getLogger(MqIntegrationTest.class);

    private static final String TOPIC = "ittest-topic";
    private static final String DELAY_TOPIC = "ittest-delay-topic";

    /** 每次运行唯一，避免与上次残留的消费组 / 记录互相干扰 */
    private static final String RUN_ID = UUID.randomUUID().toString().substring(0, 8);

    @SpringBootApplication
    static class TestApplication {
    }

    @Autowired
    private MqTemplate mqTemplate;

    @Autowired
    private RedisStreamTemplate streamTemplate;

    @Autowired
    private MqTopics topics;

    @Autowired
    private RedissonDelayedQueueManager delayQueueManager;

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 按前缀清理本次产生的全部键
     * <p>
     * 跑在共享实例上，必须自清理。用 {@code getKeysByPattern} / {@code delete}（内部为 SCAN）
     * 而非 {@code KEYS}，避免在共享实例上造成阻塞。
     * </p>
     * <p>
     * 刻意把<b>匹配到的键名</b>而非仅数量打进日志：这样「清理干净了」是可核验的事实，
     * 而不是一句断言；也顺带暴露原语层实际留下了哪些键。
     * </p>
     */
    @AfterAll
    void cleanUp() {
        Set<String> streamKeys = keys(STREAM_PREFIX + "*");
        // Redisson 的 RDelayedQueue 会派生 ZSET 与通道，键名由它决定
        Set<String> delayKeys = keys("framework:mq:delay:" + BUCKET + "*");
        long streams = redissonClient.getKeys().delete(streamKeys.toArray(String[]::new));
        long delay = redissonClient.getKeys().delete(delayKeys.toArray(String[]::new));
        log.info("集成测试清理：runId={}，删除 Stream 键 {} 个 {}，延时键 {} 个 {}",
            RUN_ID, streams, streamKeys, delay, delayKeys);
    }

    /** 列出匹配的键（SCAN） */
    private Set<String> keys(String pattern) {
        Set<String> matched = new LinkedHashSet<>();
        redissonClient.getKeys().getKeysByPattern(pattern).forEach(matched::add);
        return matched;
    }

    /* ==================== 装配层 ==================== */

    @Test
    @DisplayName("上下文可启动，且延时能力确实被声明（DelayDueHandler 跨自动配置取到了）")
    void should_declare_delayed_capability() {
        assertTrue(delayQueueManager.isRunning(),
            "延时管理器未启动 —— 没有任何桶跑起来，说明 DelayDueHandler 没被原语层取到");
        assertTrue(mqTemplate.capabilities().contains(MqCapability.DELAYED),
            "capabilities 缺少 delayed：能力集合=" + mqTemplate.capabilities());
        assertEquals("redis-stream", mqTemplate.getTransportName());
    }

    @Test
    @DisplayName("ping 做的是真实往返（XLEN），不是恒真判定")
    void should_ping_real_round_trip() {
        assertTrue(mqTemplate.ping());
    }

    /* ==================== 即时投递 ==================== */

    @Test
    @DisplayName("发送后消息真实落到目标 Stream，且字段可读")
    void should_append_to_stream() {
        String topic = TOPIC + "-append-" + RUN_ID;
        long before = streamTemplate.length(topics.streamKey(topic));

        SendResult result = mqTemplate.send(topic, new Payload("order-1"));

        assertTrue(result.isSuccess(), "投递失败：" + result.getStatus());
        assertNotNull(result.getRecordId(), "成功结果必须带 recordId");
        assertEquals(before + 1, streamTemplate.length(topics.streamKey(topic)));

        StreamEntry entry = lastEntry(topic);
        assertNotNull(entry, "Stream 中读不到刚写入的记录");
        assertEquals("{\"orderId\":\"order-1\"}", entry.field("data"));
        assertEquals(result.getMessageId(), entry.field("messageId"),
            "Stream 中的 messageId 必须与发送结果一致");
    }

    /* ==================== 延时投递（本测试的核心） ==================== */

    @Test
    @DisplayName("延时投递端到端：到期后条目回到原主题的 Stream")
    void should_deliver_delayed_message_back_to_its_topic() {
        // 关键：本主题未配置 topics.<t>.bucket —— 走「跟随原语层默认桶」那条路径
        String topic = DELAY_TOPIC + "-" + RUN_ID;
        String streamKey = topics.streamKey(topic);

        SendResult result = mqTemplate.sendDelayed(topic, new Payload("delayed-1"),
            Duration.ofSeconds(1));
        assertTrue(result.isSuccess(), "延时入队失败：" + result.getStatus());

        // 未到期时不应出现在目标 Stream
        assertEquals(0L, streamTemplate.length(streamKey),
            "延时消息在到期前就出现在了目标 Stream");

        StreamEntry arrived = awaitEntry(topic, Duration.ofSeconds(20));

        assertNotNull(arrived, "延时消息到期后未被投递回原主题 —— 延时链路仍未接通");
        assertEquals("{\"orderId\":\"delayed-1\"}", arrived.field("data"));
    }

    /* ==================== 辅助 ==================== */

    /** 测试用负载（字段名须与断言中的 JSON 一致） */
    record Payload(String orderId) {
    }

    /** 读该主题 Stream 的最后一条 */
    private StreamEntry lastEntry(String topic) {
        List<StreamEntry> entries = streamTemplate.range(topics.streamKey(topic), Integer.MAX_VALUE);
        return entries.isEmpty() ? null : entries.get(entries.size() - 1);
    }

    /** 轮询等待条目出现 */
    private StreamEntry awaitEntry(String topic, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            StreamEntry entry = lastEntry(topic);
            if (entry != null) {
                return entry;
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return null;
    }
}
