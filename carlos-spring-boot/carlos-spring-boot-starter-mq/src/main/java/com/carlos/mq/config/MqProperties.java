package com.carlos.mq.config;

import com.carlos.mq.support.MqType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MQ 配置属性
 * <p>
 * 前缀 {@code carlos.mq}。<b>只放业务语义量</b>；物理量（Stream 长度上限、阻塞超时、
 * 认领批量、延时容量等）在 {@code carlos.redis.*} 下——两层职责不重叠，一处默认值只定义一次。
 * </p>
 * <p>
 * 本次清理说明：原 {@code global.*} 的 5 个配置项（sendTimeout / maxRetryTimes /
 * enableTrace / enableCompress / compressThreshold）<b>全模块零读取</b>，属死配置，
 * 已随契约收敛移除。
 * </p>
 *
 * @author Carlos
 * @date 2026/3/14
 */
@Data
@ConfigurationProperties(prefix = "carlos.mq")
public class MqProperties {

    /**
     * 是否启用 MQ
     */
    private boolean enabled = true;

    /**
     * MQ 类型：auto(自动检测) / redis / none
     * <p>
     * {@code none} 表示显式声明「本次不接 MQ」，此时装配不投递任何消息的兜底模板；
     * 而「配置了不存在的类型」会直接启动失败（不静默降级）。
     * </p>
     */
    private MqType type = MqType.AUTO;

    /**
     * 单条消息负载上限（字节）
     * <p>超限时返回 {@code SendStatus.PAYLOAD_TOO_LARGE}，不截断。</p>
     */
    private int maxPayloadBytes = 65536;

    /**
     * 按主题的覆盖配置
     */
    private Map<String, TopicConfig> topics = new LinkedHashMap<>();

    /**
     * 消费侧配置
     */
    private ConsumerConfig consumer = new ConsumerConfig();

    /**
     * 能力校验配置
     */
    private CapabilityConfig capability = new CapabilityConfig();

    /**
     * 能力校验配置
     */
    @Data
    public static class CapabilityConfig {

        /**
         * 校验不通过时是否拒绝启动
         * <p>
         * <b>默认 true（启动失败）</b>：让「传输不支持该组合」在启动期暴露，
         * 而不是等到运行到某条消息才发现相关语义悄悄失效。
         * 仅在迁移期确有需要时关闭（关闭后只记 ERROR 日志）。
         * </p>
         */
        private boolean failFast = true;
    }

    /**
     * 单主题配置
     */
    @Data
    public static class TopicConfig {

        /**
         * 本主题的 Stream 长度上限覆盖值；为 null 时使用全局物理配置
         */
        private Long maxLength;

        /**
         * 本主题的延时桶名（需出现在 {@code carlos.redis.delay.buckets} 中）
         * <p>
         * {@code null} 表示<b>跟随原语层的默认桶</b>（{@code carlos.redis.delay.buckets}
         * 的第一个）。刻意不再自带默认值 {@code "default"}：那与原语层默认的
         * {@code [normal]} 冲突——未显式配置 bucket 的主题在延时发送时会失败，
         * 且同一默认值出现在两处，违反本类「一处默认值只定义一次」的约定。
         * </p>
         */
        private String bucket;
    }

    /**
     * 消费侧配置
     */
    @Data
    public static class ConsumerConfig {

        /**
         * 最大投递次数；达到后由认领器转入死信，不再重投
         */
        private int maxDeliveryCount = 3;

        /**
         * 待处理条目认领的轮询间隔；{@code 0} 表示关闭认领
         */
        private Duration pendingClaimInterval = Duration.ofSeconds(30);

        /**
         * 认领的空闲阈值
         * <p>
         * <b>必须显著大于单条消息的处理耗时 P99</b>，否则会把正在处理中的消息抢走，
         * 造成重复消费。
         * </p>
         */
        private Duration pendingClaimMinIdle = Duration.ofSeconds(60);

        /**
         * 是否启用死信；关闭时超限消息只计数不落死信
         */
        private boolean dlqEnabled = true;

        /**
         * 死信 Stream 的长度上限
         * <p>死信也必须有界，否则「毒消息无限增长」只是从待处理队列转移到死信队列。</p>
         */
        private Long dlqMaxLength = 5000L;

        /**
         * 关闭消费者时等待退出的上限
         */
        private Duration closeTimeout = Duration.ofSeconds(10);
    }

    /**
     * 取指定主题的配置（不存在时返回默认值，不返回 null）
     *
     * @param topic 主题
     * @return 主题配置
     */
    public TopicConfig topicConfig(String topic) {
        TopicConfig config = topics.get(topic);
        return config == null ? new TopicConfig() : config;
    }
}
