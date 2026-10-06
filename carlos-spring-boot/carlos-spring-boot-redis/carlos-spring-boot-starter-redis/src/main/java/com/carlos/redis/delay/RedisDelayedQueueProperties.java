package com.carlos.redis.delay;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * 延时队列原语层配置
 * <p>
 * 前缀 {@code carlos.redis.delay}。仅物理量：桶名、容量与时间上限、线程退出与退避参数。
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@Data
@ConfigurationProperties(prefix = "carlos.redis.delay")
public class RedisDelayedQueueProperties {

    /**
     * 默认队列名前缀（以冒号结尾）
     */
    public static final String DEFAULT_QUEUE_PREFIX = "framework:mq:delay:";

    /**
     * 是否启用
     */
    private boolean enabled = true;

    /**
     * 队列名前缀
     */
    private String queuePrefix = DEFAULT_QUEUE_PREFIX;

    /**
     * 延时桶（默认单桶，即零优先级复杂度）
     * <p>多桶示例：{@code [high, normal, low]}。</p>
     */
    private List<String> buckets = List.of("normal");

    /**
     * 单条消息的延迟上限
     * <p>
     * <b>默认 1 小时而非 1 天</b>，是刻意的架构边界：任何「1 小时后才需要发生」的事情
     * 几乎必然需要调度中心视图、错过补偿与人工干预，那属于 {@code carlos-job}（Quartz）
     * 的职责。用默认值而非文档来约束边界——文档约束在本项目已被证明不生效。
     * </p>
     */
    private Duration maxDelay = Duration.ofHours(1);

    /**
     * 每个桶的容量上限（软上限，并发下可能略超）
     */
    private int maxSize = 10000;

    /**
     * 关闭时等待转移线程退出的上限
     */
    private Duration closeTimeout = Duration.ofSeconds(5);

    /**
     * 转移线程异常后的退避上限
     */
    private Duration transferBackoffMax = Duration.ofSeconds(5);

    /**
     * 从阻塞队列取到期条目的等待时长（用于感知关闭信号）
     */
    private Duration pollTimeout = Duration.ofSeconds(1);
}
