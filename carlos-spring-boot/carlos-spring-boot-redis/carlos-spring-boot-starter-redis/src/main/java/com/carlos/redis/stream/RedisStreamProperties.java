package com.carlos.redis.stream;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Redis Stream 原语层配置
 * <p>
 * 前缀 {@code carlos.redis.stream}。<b>本层只放物理量</b>（key 前缀、XADD 上限、
 * 阻塞超时、认领批量等），不得出现 {@code topic}/{@code retry}/{@code dlq}/{@code listener}
 * 等 MQ 语义词汇——那些属于 {@code com.carlos.mq} 层。
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@Data
@ConfigurationProperties(prefix = "carlos.redis.stream")
public class RedisStreamProperties {

    /**
     * 默认 key 前缀（以冒号结尾，与 framework:lock: / framework:ratelimit: 风格一致）
     * <p>仅为默认值，实际前缀由 MQ 层传入。</p>
     */
    public static final String DEFAULT_KEY_PREFIX = "framework:mq:stream:";

    /**
     * 是否启用
     */
    private boolean enabled = true;

    /**
     * key 前缀
     */
    private String keyPrefix = DEFAULT_KEY_PREFIX;

    /**
     * 单条 Stream 的长度上限（{@code XADD MAXLEN}）
     * <p>
     * <b>这是本项目「共用 Redis 实例」约束下的核心保护项</b>：把无界积压变成固定预算。
     * 取值应按内存预算反算而非拍脑袋：
     * {@code maxLength <= maxmemory * 20% * 0.5 / avgPayloadBytes}。
     * 默认 20000 假设 {@code maxmemory=512MB} + 平均负载 2KB，部署时须按实际值复核。
     * </p>
     * <p>为 {@code null} 表示不限制（<b>不推荐</b>，会在启动期打 WARN）。</p>
     */
    private Long maxLength = 20000L;

    /**
     * 是否使用近似修剪（{@code XADD MAXLEN ~}）
     * <p>
     * 近似修剪把清理工作摊到节点边界，避免每次写入都做 O(N) 精确裁剪——后者会阻塞
     * Redis 单线程并直接表现为全实例延迟尖刺。除非有强需求，否则不要关闭。
     * </p>
     */
    private boolean approximateTrim = true;

    /**
     * 分组读取的阻塞时长（{@code XREADGROUP BLOCK}）
     */
    private Duration blockTimeout = Duration.ofSeconds(2);

    /**
     * 单次待处理条目查询/认领的批量上限
     * <p>有界化以避免在大 Stream 上做 O(N) 操作阻塞全实例。</p>
     */
    private int claimBatchSize = 50;

    /**
     * 建组起始 offset：{@code 0} 从头消费，{@code $} 只消费新消息
     * <p>注意：切换消费组时用 {@code $} 会跳过已有消息。</p>
     */
    private String groupStartOffset = "0";

    /**
     * 是否注册指标
     */
    private boolean metricsEnabled = true;

    /**
     * 指标刷新间隔（也用于发送侧背压的本地计数校正）
     */
    private Duration metricsInterval = Duration.ofSeconds(10);
}
