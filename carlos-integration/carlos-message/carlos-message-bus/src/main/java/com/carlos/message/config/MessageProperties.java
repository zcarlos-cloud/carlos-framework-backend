package com.carlos.message.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * 消息中心配置属性
 * </p>
 *
 * <h2>本次清理说明（队列段）</h2>
 * <p>
 * 原 {@code carlos.message.queue.*} 的四个键中，<b>两个从未被任何代码读取</b>，
 * 另两个的语义已由框架承接。继续保留它们就是「配置撒谎」——使用者改了不起作用，
 * 排查时还会误以为配置生效。因此一律移除：
 * </p>
 * <ul>
 *   <li>{@code queue.type}（原默认 {@code redis}）—— <b>零读取</b>。把它写成 {@code rocketmq}
 *       或 {@code kafka} 是完全无声的空操作，而实现却始终是同步实现。现由框架的
 *       {@code carlos.mq.type} 承担类型选择，且配成不存在的类型会直接启动失败。</li>
 *   <li>{@code queue.consumer-count}（原默认 3）—— <b>零读取</b>。真实并发由
 *       {@code @MqListener.concurrency} 决定。</li>
 *   <li>{@code queue.stream-prefix}（原默认 {@code stream:message}）—— 物理键前缀已由框架的
 *       {@code carlos.redis.stream.key-prefix} 统一管理；渠道维度改由<b>主题</b>表达。</li>
 *   <li>{@code queue.consumer-group}（原默认 {@code message-consumer-group}）—— 消费组改由
 *       {@code @MqListener.group} 声明。</li>
 *   <li>{@code consumer-id}（原为常量 {@code "message-consumer-1"}）—— 这是「多副本共用同一
 *       消费者名导致消息被瓜分、待处理队列归属混乱」的根因；现由框架按实例派生唯一名。</li>
 * </ul>
 * <p>
 * 移除这些键<b>不会</b>导致启动失败（未知属性默认被忽略），只是不再产生任何效果。
 * </p>
 *
 * @author Carlos
 * @date 2026/3/11
 */
@Data
@Component
@ConfigurationProperties(prefix = "carlos.message")
public class MessageProperties {

    /**
     * 是否启用消息中心
     */
    private boolean enabled = true;

    /**
     * 队列段配置
     */
    private QueueProperties queue = new QueueProperties();

    /**
     * 限流配置
     */
    private RateLimiterProperties rateLimiter = new RateLimiterProperties();

    /**
     * 重试配置
     */
    private RetryProperties retry = new RetryProperties();

    /**
     * 队列段配置
     * <p>仅保留有效开关；其余键的处置理由见类注释。</p>
     */
    @Data
    public static class QueueProperties {

        /**
         * 是否启用队列投递
         */
        private boolean enabled = true;
    }

    /**
     * 限流配置
     */
    @Data
    public static class RateLimiterProperties {
        private boolean enabled = true;
        private int defaultQps = 100;
        private Map<String, Integer> channels = new HashMap<>();
    }

    /**
     * 重试配置
     * <p>
     * 注意：这是<b>业务侧</b>的重试策略（按「接收人 × 渠道」维度、依赖收件人状态机），
     * 并非队列的投递重试。队列侧的投递次数上限由 {@code carlos.mq.consumer.max-delivery-count}
     * 承担，两者语义不同、互不替代。
     * </p>
     */
    @Data
    public static class RetryProperties {
        private int maxAttempts = 3;
        private long initialInterval = 1000;
        private double multiplier = 2;
        private long maxInterval = 30000;
    }
}
