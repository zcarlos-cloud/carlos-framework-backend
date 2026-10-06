package com.carlos.mq.support;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * MQ 类型枚举
 * <p>
 * 说明：{@link #RABBITMQ} / {@link #ROCKETMQ} / {@link #KAFKA} 三个常量<b>保留</b>但当前
 * 没有任何实现——它们是为「将来接入其它传输」预留的类型位（见
 * {@code docs/design/DESIGN-20260925-MQ技术选型.md} §7 回流预案）。三个历史实现已于重构中
 * 移除，原因见该文档 §1.3.1。
 * </p>
 *
 * @author Carlos
 * @date 2026/3/14
 */
@Getter
@RequiredArgsConstructor
public enum MqType {

    /**
     * 自动检测（容器中恰好只有一个传输实现时选中它；多个则要求显式配置）
     */
    AUTO("auto", "自动检测"),

    /**
     * Redis Stream（Apache 2.0，项目 P0 选型）
     */
    REDIS("redis", "Redis Stream"),

    /**
     * 不启用任何传输（显式声明「本次不接 MQ」，与「配置错误」区分开）
     */
    NONE("none", "不启用传输"),

    /**
     * RabbitMQ（预留类型位，当前无实现）
     */
    RABBITMQ("rabbitmq", "RabbitMQ"),

    /**
     * RocketMQ（预留类型位，当前无实现）
     */
    ROCKETMQ("rocketmq", "RocketMQ"),

    /**
     * Kafka（预留类型位，当前无实现）
     */
    KAFKA("kafka", "Kafka");

    /**
     * 类型编码
     */
    private final String code;

    /**
     * 类型描述
     */
    private final String desc;

    /**
     * 根据编码获取类型
     *
     * @param code 编码；为空或无法识别时返回 {@link #AUTO}
     * @return MQ 类型
     */
    public static MqType fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return AUTO;
        }
        for (MqType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return AUTO;
    }

    /**
     * 是否为「自动」或「不启用」这类非具体传输的值
     *
     * @return 是则返回 true
     */
    public boolean isAbstract() {
        return this == AUTO || this == NONE;
    }
}
