package com.carlos.mq.client.redis;

import com.carlos.mq.config.MqProperties;
import com.carlos.redis.stream.StreamAddOptions;

/**
 * Stream 写入选项的统一出口
 * <p>
 * 「按主题取 {@code max-length} 覆盖值，无覆盖则返回 {@code null} 交由原语层使用全局上限」
 * 这一小段逻辑曾散落在<b>即时投递</b>、<b>死信重放</b>、<b>延时到期</b>三处。
 * 集中到一处，避免三份实现各自漂移——「同一默认值只定义一次」是本模块的既定约定。
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
public final class MqStreamOptions {

    private MqStreamOptions() {
    }

    /**
     * 主题对应的写入选项
     *
     * @param properties MQ 配置
     * @param topic      主题
     * @return 写入选项；无主题级覆盖时返回 {@code null}（原语层将使用全局上限）
     */
    public static StreamAddOptions of(MqProperties properties, String topic) {
        Long maxLength = properties.topicConfig(topic).getMaxLength();
        return maxLength == null ? null : StreamAddOptions.maxLen(maxLength);
    }
}
