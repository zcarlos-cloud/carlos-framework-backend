package com.carlos.mq.client.redis;

import lombok.RequiredArgsConstructor;

/**
 * Redis 传输的命名规约：topic → Stream key / 死信 key / 默认消费组
 * <p>
 * 把「业务语义的 topic」翻译成「Redis 的 key」，这是 MQ 层的职责——原语层只认识 key。
 * key 形如 {@code {prefix}{topic}}，遵循项目 Redis Key 命名格式
 * {@code {模块}:{业务}:{标识}}（此处前缀默认 {@code framework:mq:stream:}）。
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@RequiredArgsConstructor
public class MqTopics {

    /** 死信 key 后缀 */
    private static final String DLQ_SUFFIX = ":dlq";

    /** 默认消费组后缀 */
    private static final String DEFAULT_GROUP_SUFFIX = ":default";

    /** Stream key 前缀（来自 carlos.redis.stream.key-prefix） */
    private final String streamPrefix;

    /**
     * 主题对应的 Stream key
     *
     * @param topic 主题
     * @return Stream key
     */
    public String streamKey(String topic) {
        return streamPrefix + topic;
    }

    /**
     * 主题对应的死信 Stream key
     *
     * @param topic 主题
     * @return 死信 key
     */
    public String dlqKey(String topic) {
        return streamKey(topic) + DLQ_SUFFIX;
    }

    /**
     * 未显式指定消费组时的默认组名
     *
     * @param topic 主题
     * @return 组名
     */
    public String defaultGroup(String topic) {
        return topic + DEFAULT_GROUP_SUFFIX;
    }

    /**
     * 从前缀反推主题（用于把底层 key 还原为业务语义，例如指标维度）
     *
     * @param streamKey Stream key
     * @return 主题；不属于本前缀时原样返回
     */
    public String topicOf(String streamKey) {
        if (streamKey != null && streamKey.startsWith(streamPrefix)) {
            return streamKey.substring(streamPrefix.length());
        }
        return streamKey;
    }

    /**
     * Stream key 前缀
     *
     * @return 前缀
     */
    public String streamPrefix() {
        return streamPrefix;
    }
}
