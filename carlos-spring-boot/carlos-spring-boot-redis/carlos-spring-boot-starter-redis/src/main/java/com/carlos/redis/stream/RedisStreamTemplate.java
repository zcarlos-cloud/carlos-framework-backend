package com.carlos.redis.stream;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Redis Stream 原语模板
 *
 * <h2>分层红线（本接口不含任何 MQ 语义）</h2>
 * <p>
 * 参数只有 {@code key} / {@code group} / {@code recordId} 等 Redis 概念，
 * <b>不认识 {@code topic} / 重试 / 死信 / 能力</b>。「topic → key」的命名规约、
 * 重试策略、死信路由都由上层（{@code com.carlos.mq}）负责。
 * </p>
 *
 * <h2>序列化约定</h2>
 * <p>
 * key 与字段名恒为字符串，字段值为<b>显式 JSON 字符串</b>。
 * <b>不要</b>把 {@code ConfigurableRedisSerializer} 挂到本模板的 Stream 序列化上——
 * 它默认是 Jackson，会把 Long 等标量写成带引号的字符串，导致 Stream 字段读出为转义字符串，
 * 排查困难且跨语言不友好。
 * </p>
 *
 * <h2>长度上限（本项目的硬约束）</h2>
 * <p>
 * {@link #add} 默认带上 {@code MAXLEN ~ N} 修剪——这是「与缓存/token 共用 Redis 实例」
 * 场景下把无界积压变成固定预算的核心手段，不是可选优化。仅测试或确有界场景才用
 * {@link StreamAddOptions#unlimited()}。
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
public interface RedisStreamTemplate {

    /* ==================== 写 ==================== */

    /**
     * 追加一条条目（默认带长度上限修剪）
     *
     * @param key     流 key
     * @param fields  字段（值应为 JSON 字符串）
     * @param options 写入选项；为 null 时使用全局默认上限
     * @return 新条目的 recordId
     */
    String add(String key, Map<String, String> fields, StreamAddOptions options);

    /**
     * 按长度修剪
     *
     * @param key         流 key
     * @param maxLength   保留的最大长度
     * @param approximate 是否近似修剪（{@code ~}）
     * @return 被删除的条目数
     */
    long trim(String key, long maxLength, boolean approximate);

    /* ==================== 消费组 ==================== */

    /**
     * 幂等地创建消费组
     * <p>
     * 组已存在（{@code BUSYGROUP}）时静默成功并返回 {@code false}；
     * 流不存在时用 {@code XGROUP CREATE ... MKSTREAM} 一并创建空流。
     * </p>
     * <p>
     * ⚠️ <b>不要</b>改用「写占位条目 → 建组 → 删掉占位条目」的引导方式：占位条目是流里
     * 唯一一条，删掉它会让 Redis 移除整个 stream key，<b>消费组随之消失</b>，而方法仍
     * 返回成功——此后每一次 {@code XREADGROUP} 都恒报 {@code NOGROUP}，消费链路静默失效
     * （2026-09-26 真机实测确认）。
     * </p>
     *
     * @param key         流 key
     * @param group       消费组名
     * @param startOffset 起始 offset（{@code 0} 从头，{@code $} 只消费新消息）；
     *                    为 null 或空白时取 {@code carlos.redis.stream.group-start-offset}
     * @return 本次是否新建了消费组
     */
    boolean createGroupIfAbsent(String key, String group, String startOffset);

    /**
     * 以消费组成员身份读取（{@code XREADGROUP}）
     * <p>
     * <b>重要语义</b>：无论是否 ACK，读到的条目都会进入待处理队列（PEL）。
     * 因此「不 ACK」恰好等价于「留在待处理队列等待重投」——这是可靠消费设计的支点。
     * </p>
     *
     * @param key      流 key
     * @param group    消费组
     * @param consumer 消费者名
     * @param options  读取选项
     * @return 读到的条目（无数据时为空列表）
     */
    List<StreamEntry> readGroup(String key, String group, String consumer, StreamReadOptions options);

    /**
     * 确认已处理（{@code XACK}）
     *
     * @param key       流 key
     * @param group     消费组
     * @param recordIds 待确认的 id
     * @return 实际确认的条数
     */
    long ack(String key, String group, List<String> recordIds);

    /* ==================== 待处理条目（PEL） ==================== */

    /**
     * 待处理条目汇总（{@code XPENDING key group}）
     *
     * @param key   流 key
     * @param group 消费组
     * @return 汇总信息
     */
    StreamPendingSummary pendingSummary(String key, String group);

    /**
     * 待处理条目明细（{@code XPENDING key group IDLE minIdle - + count}）
     * <p>返回值含<b>权威的投递次数</b>，是判断是否转死信的依据。</p>
     *
     * @param key     流 key
     * @param group   消费组
     * @param minIdle 最小空闲时长
     * @param count   条数上限（有界化，避免 O(N) 阻塞全实例）
     * @return 明细列表
     */
    List<StreamPendingEntry> pending(String key, String group, Duration minIdle, int count);

    /**
     * 认领待处理条目（{@code XCLAIM}）
     * <p>
     * <b>不要显式传 retryCount</b>：Redis 在未传 {@code RETRYCOUNT} 时会自动递增投递次数，
     * 这正是死信判定所依赖的行为；显式传值会把计数重置为 1，导致重试永不终止、死信永不触发。
     * </p>
     *
     * @param key       流 key
     * @param group     消费组
     * @param consumer  认领后的归属消费者
     * @param minIdle   最小空闲时长
     * @param recordIds 待认领的 id
     * @return 认领到的条目
     */
    List<StreamEntry> claim(String key, String group, String consumer, Duration minIdle, List<String> recordIds);

    /* ==================== 观测 ==================== */

    /**
     * 流长度（{@code XLEN}，O(1)，可高频调用）
     *
     * @param key 流 key
     * @return 条目数
     */
    long length(String key);

    /**
     * 读取头部若干条目（{@code XRANGE key - + COUNT n}，用于死信排查）
     *
     * @param key   流 key
     * @param count 条数上限
     * @return 条目列表
     */
    List<StreamEntry> range(String key, int count);

    /**
     * 删除条目（{@code XDEL}）
     *
     * @param key       流 key
     * @param recordIds 待删除的 id
     * @return 实际删除的条数
     */
    long delete(String key, List<String> recordIds);

    /**
     * Redis 实例已用内存（{@code INFO memory} 的 {@code used_memory}）
     * <p>
     * 供「共用实例」场景下的容量监控与拆分决策使用。
     * </p>
     *
     * @return 字节数；取不到时返回 -1
     */
    long usedMemoryBytes();
}
