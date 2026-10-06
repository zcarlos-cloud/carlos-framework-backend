package com.carlos.mq.annotation;

import com.carlos.mq.support.ConsumeMode;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 消息监听注解
 *
 * <h2>本次收缩说明（10 个属性 → 7 个）</h2>
 * <p>
 * 移除了三个<b>无法真正兑现</b>的属性，避免「声明了却不生效」的误导：
 * </p>
 * <ul>
 *   <li>{@code tag} —— 标签过滤需要服务端支持，当前传输无此能力。用「一个事件类型一个主题」
 *       替代，这也是更好的实践。</li>
 *   <li>{@code ordered} —— 顺序消费在当前传输上无法诚实提供（单 Stream 全局有序，但同组
 *       多消费者会交错）。</li>
 *   <li>{@code autoCommit} —— 确认时机由可靠性语义决定，不应由业务方法开关
 *       （框架坚持「先处理成功再确认」）。</li>
 * </ul>
 * <p>
 * 另将 {@code maxRetryTimes} 更名为 {@code maxDeliveryCount}，与「投递次数」这一
 * 权威语义（来自待处理队列自身的计数器）对齐。
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MqListener {

    /**
     * 主题（必填，可多个）
     *
     * @return 主题数组
     */
    String[] topic();

    /**
     * 消费组；为空时按 {@code {topic}:default} 派生
     *
     * @return 消费组
     */
    String group() default "";

    /**
     * 显式消费者名；为空时由框架按实例派生唯一名
     * <p>
     * 仅用于「固定消费者名做单点顺序消费」等进阶场景——常规场景留空，
     * 因为多副本共用同一个消费者名会导致消息被瓜分、待处理队列归属混乱。
     * </p>
     *
     * @return 消费者名
     */
    String consumerId() default "";

    /**
     * 消费模式
     *
     * @return 集群（默认，同组内分摊）或广播（每实例各收一份）
     */
    ConsumeMode consumeMode() default ConsumeMode.CLUSTER;

    /**
     * 并发消费者数（同组内分摊）
     *
     * @return 并发数
     */
    int concurrency() default 1;

    /**
     * 单次拉取条数
     *
     * @return 批量大小
     */
    int batchSize() default 1;

    /**
     * 最大投递次数；非正数表示使用全局配置
     * <p>达到上限的消息由认领器转入死信，不再重投。</p>
     *
     * @return 最大投递次数
     */
    int maxDeliveryCount() default -1;
}
