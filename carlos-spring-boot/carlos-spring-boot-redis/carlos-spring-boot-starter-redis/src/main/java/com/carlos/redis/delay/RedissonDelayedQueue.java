package com.carlos.redis.delay;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

/**
 * 通用延时队列原语（基于 Redisson {@code RDelayedQueue}）
 *
 * <h2>不通用的地方（分层红线）</h2>
 * <p>
 * 本类<b>不知道</b>到期条目要送去哪里——它只把「桶名 + 载荷」交给构造时注入的回调。
 * 「延时 → Stream」的编排属于上层（{@code com.carlos.mq}）。
 * </p>
 *
 * <h2>机制说明（不沿用既有实现的错误注释）</h2>
 * <p>
 * Redisson 的 {@code RDelayedQueue} 实际是「内部 ZSET + 定时任务 + 通道通知」实现的，
 * <b>不是</b> Redis key-space 事件驱动。所谓「零轮询」仅在<b>应用线程</b>层面成立
 * （应用侧阻塞等待，不轮询），Redisson 内部仍有定时扫描。既有
 * {@code RedisStreamMessageQueue} 的类注释在这点上与实现不符，此处不再沿用。
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@Slf4j
public class RedissonDelayedQueue {

    private final RedissonClient redissonClient;
    private final DelayBucket bucket;
    private final RedisDelayedQueueProperties properties;

    /** 到期回调：入参为「桶名, 载荷」，便于上层按桶路由 */
    private final BiConsumer<String, String> dueHandler;

    /** 启动标志，保证 {@link #start()} 幂等 */
    private final AtomicBoolean started = new AtomicBoolean(false);

    /** 运行标志，仅用于终止转移循环 */
    private volatile boolean running;

    private RBlockingQueue<String> blockingQueue;
    private RDelayedQueue<String> delayedQueue;
    private Thread transferThread;

    public RedissonDelayedQueue(RedissonClient redissonClient, DelayBucket bucket,
                               RedisDelayedQueueProperties properties,
                               BiConsumer<String, String> dueHandler) {
        this.redissonClient = redissonClient;
        this.bucket = bucket;
        this.properties = properties;
        this.dueHandler = dueHandler;
    }

    /**
     * 启动转移线程（幂等：重复调用不会启动第二组线程）
     */
    public void start() {
        if (!started.compareAndSet(false, true)) {
            log.debug("延时队列已启动，跳过重复调用：bucket={}", bucket.name());
            return;
        }
        this.blockingQueue = redissonClient.getBlockingQueue(bucket.queueName());
        this.delayedQueue = redissonClient.getDelayedQueue(blockingQueue);
        this.running = true;

        Thread thread = new Thread(this::transferLoop, "redis-delay-transfer-" + bucket.name());
        thread.setDaemon(true);
        this.transferThread = thread;
        thread.start();
        log.info("延时队列已启动：bucket={}, queue={}", bucket.name(), bucket.queueName());
    }

    /**
     * 停止转移线程（幂等；允许在未启动时调用）
     * <p>
     * <b>绝不调用 {@code RDelayedQueue.destroy()}</b>：它会删除 Redisson 的内部 ZSET，
     * 导致尚未到期的延时消息全部丢失。未到期项应留在 Redis 中，由下次启动继续转移。
     * </p>
     * <p>
     * 同样<b>不做 drain</b>：已到期项推入阻塞队列后消费者已停，只是换个地方滞留；
     * 留在延时结构里语义更正确。这是刻意的取舍。
     * </p>
     */
    public void stop() {
        if (!started.compareAndSet(true, false)) {
            return;
        }
        this.running = false;
        Thread thread = this.transferThread;
        this.transferThread = null;
        if (thread != null) {
            thread.interrupt();
            try {
                thread.join(properties.getCloseTimeout().toMillis());
                if (thread.isAlive()) {
                    log.warn("延时转移线程未在 {} 内退出：bucket={}",
                        properties.getCloseTimeout(), bucket.name());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        log.info("延时队列已停止（未到期项保留在 Redis）：bucket={}", bucket.name());
    }

    /**
     * 是否运行中
     *
     * @return 运行中返回 true
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * 桶名
     *
     * @return 桶名
     */
    public String bucketName() {
        return bucket.name();
    }

    /**
     * 入队一条延时消息
     *
     * @param payload 载荷（应为 JSON 字符串）
     * @param delay   延迟时长；为 null 或非正数视为立即到期
     * @return 是否入队成功
     * @throws RedisDelayException 未启动、超过延迟上限或容量超限时抛出
     */
    public boolean offer(String payload, Duration delay) {
        if (!running || delayedQueue == null) {
            throw new RedisDelayException(RedisDelayErrorCode.DELAY_OFFER_FAILED,
                "延时队列未启动：bucket=" + bucket.name());
        }
        Duration effective = delay == null ? Duration.ZERO : delay;
        if (properties.getMaxDelay() != null && effective.compareTo(properties.getMaxDelay()) > 0) {
            throw new RedisDelayException(RedisDelayErrorCode.DELAY_EXCEEDS_MAX,
                "延迟时长 %s 超过上限 %s：bucket=%s".formatted(
                    effective, properties.getMaxDelay(), bucket.name()));
        }
        if (properties.getMaxSize() > 0 && size() >= properties.getMaxSize()) {
            throw new RedisDelayException(RedisDelayErrorCode.DELAY_QUEUE_FULL,
                "延时队列已达容量上限 %d：bucket=%s".formatted(properties.getMaxSize(), bucket.name()));
        }
        try {
            delayedQueue.offer(payload, Math.max(0L, effective.toMillis()), TimeUnit.MILLISECONDS);
            return true;
        } catch (Exception e) {
            throw new RedisDelayException(RedisDelayErrorCode.DELAY_OFFER_FAILED,
                "延时入队失败：bucket=" + bucket.name(), e);
        }
    }

    /**
     * 尚未到期的条目数
     *
     * @return 条数；未启动时返回 0
     */
    public long size() {
        if (delayedQueue == null) {
            return 0L;
        }
        try {
            return delayedQueue.size();
        } catch (Exception e) {
            log.warn("查询延时队列长度失败：bucket={}", bucket.name(), e);
            return 0L;
        }
    }

    /**
     * 转移循环
     * <p>
     * <b>健壮性的关键</b>：捕获 {@link Throwable} 并在异常后退避继续，<b>绝不退出循环</b>。
     * 既有实现只捕获 {@code InterruptedException} 与序列化异常，导致 Redisson 的运行时
     * 异常会让该桶的转移线程<b>永久死亡且无人重启</b>。
     * </p>
     */
    private void transferLoop() {
        long backoffMillis = 100L;
        long backoffMaxMillis = Math.max(100L, properties.getTransferBackoffMax().toMillis());

        while (running) {
            try {
                String payload = blockingQueue.poll(properties.getPollTimeout().toMillis(),
                    TimeUnit.MILLISECONDS);
                backoffMillis = 100L;
                if (payload == null) {
                    continue;
                }
                try {
                    dueHandler.accept(bucket.name(), payload);
                } catch (Throwable t) {
                    // 单条失败不影响后续：此条目已从延时结构取出，不再重投（刻意取舍，
                    // 与「可容忍丢失」的边界一致）
                    log.error("延时条目投递失败（该条不再重投）：bucket={}", bucket.name(), t);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Throwable t) {
                log.error("延时转移线程异常，退避 {}ms 后继续：bucket={}", backoffMillis, bucket.name(), t);
                if (!sleepQuietly(backoffMillis)) {
                    break;
                }
                backoffMillis = Math.min(backoffMillis * 2, backoffMaxMillis);
            }
        }
        log.debug("延时转移线程退出：bucket={}", bucket.name());
    }

    /**
     * 可被中断的退避等待
     *
     * @param millis 等待毫秒数
     * @return true 表示等待完成；false 表示被中断（调用方应退出）
     */
    private boolean sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
