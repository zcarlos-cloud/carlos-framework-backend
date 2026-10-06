package com.carlos.message.core.queue;

import com.carlos.message.core.protocol.MessageContext;

import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

/**
 * <p>
 * 消息队列接口（过渡期保留）
 * </p>
 *
 * <p>
 * <b>本次收缩说明</b>：本接口的六个公开方法在本仓库中<b>全无调用方</b>（消息中心的真实发送链路
 * 走渠道直连、异步线程池、数据库轮询与 Redis ZSet 重试，完全绕开队列），因此按事实收缩为
 * 「三个投递方法 + 两个已废弃的消费方法」：
 * </p>
 * <ul>
 *   <li>{@code enqueue(context, priority)} → {@link #enqueue(MessageContext)}。原方法的
 *       {@code priority} 参数在即时投递上<b>没有任何效果</b>（新模型用主题分流，
 *       优先级只影响延时消息的命运桶），保留一个不起作用的参数只会误导调用方，
 *       故直接移除。</li>
 *   <li>{@code schedule(context, LocalDateTime)} → {@link #scheduleAt(MessageContext, Instant)}。
 *       原实现用 {@code ZoneId.systemDefault()} 换算绝对时间，跨时区部署会整体偏移
 *       （UTC 容器与 Asia/Shanghai 容器对同一 {@code LocalDateTime} 相差 8 小时）；
 *       改用 {@link Instant} 从根本上消除时区歧义。</li>
 *   <li>{@code getQueueSize(priority)} → <b>移除</b>。队列深度已由框架统一暴露为指标
 *       （{@code carlos.redis.mq.stream.length} 与 {@code carlos.mq.consume.pending}），
 *       业务模块不必再自持一个查询接口。</li>
 *   <li>{@link #consume} / {@link #consumeBatch} —— 标记废弃：消费应使用框架的
 *       {@code @MqListener} 注解（自带消费者组、确认、重投、死信），
 *       而不再由业务侧持有阻塞读取循环。</li>
 * </ul>
 *
 * @author Carlos
 * @date 2026/3/11
 */
public interface MessageQueue {

    /**
     * 投递消息（立即）
     *
     * @param context 消息上下文
     */
    void enqueue(MessageContext context);

    /**
     * 延时投递
     * <p>生效的优先级取自 {@link MessageContext#getPriority()}，由它决定落到哪个延时桶。</p>
     *
     * @param context     消息上下文
     * @param delayMillis 延迟毫秒；非正数表示立即投递
     */
    void delay(MessageContext context, long delayMillis);

    /**
     * 定时投递（绝对时刻）
     * <p>已过去的时刻按「立即投递」处理，不报错。</p>
     *
     * @param context   消息上下文
     * @param deliverAt 目标投递时刻
     */
    void scheduleAt(MessageContext context, Instant deliverAt);

    /**
     * 消费消息
     *
     * @param priority 优先级
     * @param handler  处理器
     * @deprecated 请改用框架的 {@code @MqListener} 注解。注解方式自带消费者组、
     *     处理成功后再确认、失败重投与死信，而阻塞式自持循环一旦遇到非预期异常会
     *     永久停止且无人重启。
     */
    @Deprecated(since = "3.0.0")
    void consume(int priority, Consumer<MessageContext> handler);

    /**
     * 批量消费消息
     *
     * @param priority  优先级
     * @param batchSize 批量大小
     * @param handler   处理器
     * @deprecated 同 {@link #consume(int, Consumer)}，请改用 {@code @MqListener}。
     */
    @Deprecated(since = "3.0.0")
    void consumeBatch(int priority, int batchSize, Consumer<List<MessageContext>> handler);
}
