package com.carlos.redis.delay;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * 延时队列桶管理器
 * <p>
 * 按配置的桶名管理多个 {@link RedissonDelayedQueue}，统一启停并转发到期回调。
 * 桶按需惰性创建。
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@Slf4j
public class RedissonDelayedQueueManager {

    private final RedissonClient redissonClient;
    private final RedisDelayedQueueProperties properties;

    /**
     * 到期回调：入参为「桶名, 载荷」。
     * <p>带上桶名是必要的——上层需要据此把条目路由到正确的目标（例如对应的 Stream）。</p>
     */
    private final BiConsumer<String, String> dueHandler;

    private final Map<String, RedissonDelayedQueue> queues = new LinkedHashMap<>();

    public RedissonDelayedQueueManager(RedissonClient redissonClient,
                                      RedisDelayedQueueProperties properties,
                                      BiConsumer<String, String> dueHandler) {
        this.redissonClient = redissonClient;
        this.properties = properties;
        this.dueHandler = dueHandler;
    }

    /**
     * 取（必要时创建）指定桶
     *
     * @param bucketName 桶名
     * @return 延时队列
     */
    public synchronized RedissonDelayedQueue bucket(String bucketName) {
        return queues.computeIfAbsent(bucketName, name ->
            new RedissonDelayedQueue(redissonClient,
                DelayBucket.of(properties.getQueuePrefix(), name), properties, dueHandler));
    }

    /**
     * 默认桶名：未显式指定桶时使用配置中的第一个
     * <p>
     * <b>为什么由本层决定默认桶</b>：默认值只应定义一次。上层
     * （{@code com.carlos.mq}）此前自带默认桶名 {@code "default"}，与本层默认的
     * {@code [normal]} 冲突——未显式配置的主题在延时发送时会落到一个从未启动的桶，
     * 报出的却是「发送失败（可重试）」。
     * </p>
     *
     * @return 默认桶名；未配置任何桶时返回 {@code null}
     */
    public String defaultBucket() {
        List<String> buckets = properties.getBuckets();
        return buckets == null || buckets.isEmpty() ? null : buckets.get(0);
    }

    /**
     * 启动配置中的全部桶（幂等）
     * <p>
     * 未提供到期处理器时<b>拒绝启动</b>并告警——否则到期条目会被静默丢弃，
     * 这正是项目约束所禁止的失败模式。
     * </p>
     */
    public synchronized void startAll() {
        if (dueHandler == null) {
            log.warn("未提供延时到期处理器（DelayDueHandler），延时队列不启动；"
                + "若要使用延时能力，请由上层的传输实现提供该 bean");
            return;
        }
        for (String name : properties.getBuckets()) {
            bucket(name).start();
        }
        log.info("延时队列管理器已启动：buckets={}", properties.getBuckets());
    }

    /**
     * 停止全部桶（幂等）
     */
    public synchronized void stopAll() {
        for (RedissonDelayedQueue queue : queues.values()) {
            queue.stop();
        }
    }

    /**
     * 任意桶是否在运行
     *
     * @return 有桶运行中返回 true
     */
    public boolean isRunning() {
        return queues.values().stream().anyMatch(RedissonDelayedQueue::isRunning);
    }

    /**
     * 已创建的桶名
     *
     * @return 不可变集合
     */
    public synchronized Set<String> bucketNames() {
        return Collections.unmodifiableSet(queues.keySet());
    }

    /**
     * 指定桶的未到期条数
     *
     * @param bucketName 桶名
     * @return 条数；桶不存在时返回 0
     */
    public synchronized long size(String bucketName) {
        RedissonDelayedQueue queue = queues.get(bucketName);
        return queue == null ? 0L : queue.size();
    }

    /**
     * 全部桶的未到期条数合计
     *
     * @return 合计条数
     */
    public synchronized long totalSize() {
        return queues.values().stream().mapToLong(RedissonDelayedQueue::size).sum();
    }
}
