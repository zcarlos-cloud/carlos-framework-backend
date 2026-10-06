package com.carlos.redis.delay;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 延时队列生命周期与健壮性测试
 * <p>
 * 覆盖既有 {@code RedisStreamMessageQueue} 的 5 个致命缺陷：
 * </p>
 * <ol>
 *   <li>只捕两类异常 ⇒ Redisson 运行时异常会让转移线程<b>永久死亡且无人重启</b></li>
 *   <li>{@code init()} 不复位运行标志 ⇒ 二次初始化后线程立即退出</li>
 *   <li>{@code destroy()} 不 interrupt/join 转移线程</li>
 *   <li>{@code destroy()} 销毁 Redisson 内部结构 ⇒ 未到期消息丢失</li>
 *   <li>关闭时不 drain（刻意取舍，此处仅确认不误伤）</li>
 * </ol>
 *
 * @author Carlos
 * @since 3.0.0
 */
@DisplayName("RedissonDelayedQueue 生命周期与健壮性")
class RedissonDelayedQueueLifecycleTest {

    /** 桶键名：从属性默认前缀派生，避免前缀调整后这里与实现悄悄脱节 */
    private static final String QUEUE_NAME = new RedisDelayedQueueProperties().getQueuePrefix() + "normal";

    private RedissonClient redissonClient;
    private RBlockingQueue<String> blockingQueue;
    private RDelayedQueue<String> delayedQueue;
    private RedissonDelayedQueue queue;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        redissonClient = mock(RedissonClient.class);
        blockingQueue = mock(RBlockingQueue.class);
        delayedQueue = mock(RDelayedQueue.class);
        RedisDelayedQueueProperties properties = new RedisDelayedQueueProperties();

        doReturn(blockingQueue).when(redissonClient).getBlockingQueue(QUEUE_NAME);
        doReturn(delayedQueue).when(redissonClient).getDelayedQueue(blockingQueue);

        queue = new RedissonDelayedQueue(redissonClient,
            DelayBucket.of(properties.getQueuePrefix(), "normal"),
            properties, (bucket, payload) -> {
            });
    }

    @AfterEach
    void tearDown() {
        queue.stop();
    }

    /** 让 poll 短暂阻塞后返回 null，避免转移循环空转烧 CPU */
    private void stubQuietPoll() throws InterruptedException {
        when(blockingQueue.poll(anyLong(), any(TimeUnit.class))).thenAnswer(invocation -> {
            Thread.sleep(10);
            return null;
        });
    }

    @Test
    @DisplayName("重复 start 应幂等，不启动第二组线程")
    void should_be_idempotent_on_repeated_start() throws InterruptedException {
        stubQuietPoll();

        queue.start();
        queue.start();

        verify(redissonClient, times(1)).getBlockingQueue(QUEUE_NAME);
        assertTrue(queue.isRunning());
    }

    @Test
    @DisplayName("stop 绝不能销毁 Redisson 延时结构（否则未到期消息全丢）")
    void should_never_destroy_delayed_queue_on_stop() throws InterruptedException {
        stubQuietPoll();
        queue.start();

        queue.stop();

        verify(delayedQueue, never()).destroy();
        assertFalse(queue.isRunning());
    }

    @Test
    @DisplayName("转移线程遇到运行时异常后必须继续运行，而不是永久死亡")
    void should_survive_transfer_thread_exceptions() throws InterruptedException {
        AtomicInteger polls = new AtomicInteger();
        when(blockingQueue.poll(anyLong(), any(TimeUnit.class))).thenAnswer(invocation -> {
            int n = polls.incrementAndGet();
            if (n <= 3) {
                throw new IllegalStateException("模拟 Redisson 运行时异常 #" + n);
            }
            Thread.sleep(10);
            return null;
        });

        queue.start();

        assertTrue(awaitAtLeast(polls, 5, Duration.ofSeconds(5)),
            "异常后转移线程必须继续调用 poll（旧实现在此永久死亡）");
        assertTrue(queue.isRunning(), "线程存活时才应为运行态");
    }

    @Test
    @DisplayName("stop 应中断并等待转移线程退出")
    void should_interrupt_and_join_on_stop() throws InterruptedException {
        CountDownLatch polling = new CountDownLatch(1);
        when(blockingQueue.poll(anyLong(), any(TimeUnit.class))).thenAnswer(invocation -> {
            polling.countDown();
            Thread.sleep(200);
            return null;
        });

        queue.start();
        assertTrue(polling.await(2, TimeUnit.SECONDS), "转移线程应已开始拉取");

        long begin = System.nanoTime();
        queue.stop();
        long elapsedMillis = (System.nanoTime() - begin) / 1_000_000;

        assertFalse(queue.isRunning());
        assertTrue(elapsedMillis < 1500, "stop 应在关闭超时内返回，实际耗时 " + elapsedMillis + "ms");
    }

    @Test
    @DisplayName("未启动时 stop 应安全（幂等）")
    void should_be_safe_to_stop_without_start() {
        queue.stop();
        assertFalse(queue.isRunning());
    }

    @Test
    @DisplayName("未启动时入队应显式报错，而不是静默丢弃")
    void should_reject_offer_when_not_started() {
        RedisDelayException ex = assertThrows(RedisDelayException.class,
            () -> queue.offer("{}", Duration.ofSeconds(1)));

        assertEquals("50020", ex.getErrorCode());
    }

    @Test
    @DisplayName("超过延迟上限应显式报错且不截断（截断会让消息比预期更早投递）")
    void should_reject_offer_exceeding_max_delay() throws InterruptedException {
        stubQuietPoll();
        queue.start();

        RedisDelayException ex = assertThrows(RedisDelayException.class,
            () -> queue.offer("{}", Duration.ofHours(2)));

        assertEquals("50021", ex.getErrorCode());
    }

    @Test
    @DisplayName("桶容量达到上限时应显式报错")
    void should_reject_offer_when_bucket_full() throws InterruptedException {
        stubQuietPoll();
        queue.start();
        when(delayedQueue.size()).thenReturn(10000);

        RedisDelayException ex = assertThrows(RedisDelayException.class,
            () -> queue.offer("{}", Duration.ofSeconds(1)));

        assertEquals("50023", ex.getErrorCode());
    }

    @Test
    @DisplayName("正常入队应透传到 Redisson 延时队列")
    void should_offer_to_redisson() throws InterruptedException {
        stubQuietPoll();
        queue.start();

        queue.offer("{\"a\":1}", Duration.ofSeconds(30));

        verify(delayedQueue).offer("{\"a\":1}", 30_000L, TimeUnit.MILLISECONDS);
    }

    private boolean awaitAtLeast(AtomicInteger counter, int target, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (counter.get() >= target) {
                return true;
            }
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return counter.get() >= target;
    }
}
