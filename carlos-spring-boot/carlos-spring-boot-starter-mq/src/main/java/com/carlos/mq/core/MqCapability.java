package com.carlos.mq.core;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * MQ 能力声明
 * <p>
 * 传输实现必须<b>显式声明</b>自己支持的能力；未声明而被调用的能力必须抛出
 * {@code MqCapabilityNotSupportedException}（错误码 {@code 20002}），
 * <b>禁止静默降级</b>（例如把延迟消息退化成普通发送、把事务消息退化成普通发送并谎报成功）。
 * </p>
 * <p>
 * 设计原则：<b>能力要么真做，要么不声明</b>。声明一个无法兑现的能力
 * （例如声称支持顺序消费但多消费者会交错）比不声明更有害。
 * </p>
 * <p>
 * 本枚举刻意<b>不</b>依赖 {@code com.carlos.mq.exception} 包——抛异常的职责放在
 * {@code MqTransport} 与模板层，以免形成 core ↔ exception 的包循环依赖。
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@Getter
@RequiredArgsConstructor
public enum MqCapability {

    /** 基本发送（所有可用的传输实现都必须声明） */
    BASIC("basic", "基本发送"),

    /**
     * 消息消费
     * <p>
     * 与 {@link #BASIC} 分开声明，是为了避免「声明了 BASIC 却报 BASIC 不支持」的自相矛盾：
     * 只发不收的传输可以只声明 BASIC，其 {@code subscribe} 会如实报「不支持 consume」。
     * </p>
     */
    CONSUME("consume", "消息消费"),

    /** 延迟投递（支持任意精度的延迟时长） */
    DELAYED("delayed", "延迟消息"),

    /** 顺序消费（同一分区键串行且有序） */
    ORDERED("ordered", "顺序消息"),

    /** 事务消息（半消息 + 回查） */
    TRANSACTIONAL("transactional", "事务消息"),

    /** 服务端标签过滤 */
    TAG_FILTER("tag-filter", "标签过滤"),

    /** 批量拉取消费 */
    BATCH_CONSUME("batch-consume", "批量消费"),

    /** 广播消费（每个实例各收一份） */
    BROADCAST("broadcast", "广播消费"),

    /** 死信队列 */
    DEAD_LETTER("dead-letter", "死信队列"),

    /** 待处理消息认领（PEL reclaim） */
    PENDING_CLAIM("pending-claim", "待处理认领"),

    /** 客户端异步发送 */
    ASYNC_SEND("async-send", "异步发送"),
    ;

    /** 能力编码（配置与日志使用） */
    private final String code;
    /** 能力描述 */
    private final String desc;
}
