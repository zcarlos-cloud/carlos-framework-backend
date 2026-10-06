package com.carlos.mq.support;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 消息消费模式枚举
 *
 * @author Carlos
 * @date 2026/3/14
 */
@Getter
@RequiredArgsConstructor
public enum ConsumeMode {

    /**
     * 集群模式（默认）
     * <p>
     * 同一条消息只会被集群中的一个消费者消费
     * </p>
     */
    CLUSTER("cluster", "集群模式"),

    /**
     * 广播模式
     * <p>
     * 同一条消息会被集群中的所有消费者消费
     * </p>
     */
    BROADCAST("broadcast", "广播模式");

    /**
     * 模式编码
     */
    private final String code;

    /**
     * 模式描述
     */
    private final String desc;

    /**
     * 根据编码获取模式
     *
     * @param code 编码
     * @return 消费模式
     */
    public static ConsumeMode fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return CLUSTER;
        }
        for (ConsumeMode mode : values()) {
            if (mode.code.equalsIgnoreCase(code)) {
                return mode;
            }
        }
        return CLUSTER;
    }
}
