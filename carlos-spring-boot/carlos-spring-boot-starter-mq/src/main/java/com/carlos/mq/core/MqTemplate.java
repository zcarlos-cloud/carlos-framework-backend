package com.carlos.mq.core;

import com.carlos.mq.support.MqType;

import java.time.Duration;
import java.util.Set;

/**
 * 传输中立的 MQ 操作模板 —— 业务代码唯一需要依赖的 MQ 入口。
 *
 * <h2>本次契约收敛（由 17 个方法收敛为 10 个）</h2>
 * <p>
 * 移除的内容及理由：
 * </p>
 * <ul>
 *   <li>8 个带 {@code tag} 的重载 —— 标签过滤是 RocketMQ 形状，选定传输不支持服务端过滤，
 *       保留只会诱导调用方写出静默降级为「客户端过滤」的低效实现。</li>
 *   <li>2 个 {@code sendDelayed(..., 延迟级别)} —— 18 级<b>量化</b>形状（RocketMQ 风格），
 *       而实际能力支持任意精度延迟，使用它会丢精度（{@link #sendDelayed} 直接收 {@link Duration}）。</li>
 *   <li>2 个 {@code sendOrdered} —— 顺序消费在当前传输上无法诚实提供
 *       （单 Stream 全局有序，但同组多消费者会交错），不保留「声明了却做不到」的签名。</li>
 *   <li>2 个 {@code sendTransaction} —— 事务消息<b>整体不做</b>：需要「事务最终一致性」的场景按
 *       {@code docs/design/DESIGN-20260925-MQ技术选型.md} §4.3 走本地消息表（outbox）+ 重试，
 *       MQ 仅承担投递加速。保留签名等于要求每个实现都给个假答案。</li>
 * </ul>
 *
 * <h2>失败语义（统一约定）</h2>
 * <p>
 * <b>发送族的失败通过 {@link SendResult} 表达，不抛异常</b>（异步路径同样通过
 * {@link MqSendCallback#onComplete} 携带结果）。异常只保留给<b>启动期配置错误</b>
 * （如 {@code @MqListener} 声明了传输不支持的能力，由启动期校验器拦截）。
 * 这样避免同一 API「有时抛异常、有时返回失败」的双重语义。
 * </p>
 *
 * @author Carlos
 * @date 2026/3/14
 */
public interface MqTemplate {

    /* ==================== 能力声明 ==================== */

    /**
     * 当前传输支持的能力集合（不可变）
     *
     * @return 能力集合
     */
    Set<MqCapability> capabilities();

    /**
     * 便捷查询
     *
     * @param capability 能力
     * @return 支持返回 true
     */
    default boolean supports(MqCapability capability) {
        return capabilities().contains(capability);
    }

    /* ==================== 发送 ==================== */

    /**
     * 发送消息
     *
     * @param topic   主题
     * @param payload 业务负载
     * @return 发送结果
     */
    SendResult send(String topic, Object payload);

    /**
     * 发送消息（带选项）
     *
     * @param topic   主题
     * @param payload 业务负载
     * @param options 发送选项
     * @return 发送结果
     */
    SendResult send(String topic, Object payload, MqSendOptions options);

    /**
     * 异步发送
     *
     * @param topic    主题
     * @param payload  业务负载
     * @param callback 完成回调
     */
    void sendAsync(String topic, Object payload, MqSendCallback callback);

    /**
     * 单向发送（不等待发送结果）
     *
     * @param topic   主题
     * @param payload 业务负载
     */
    void sendOneWay(String topic, Object payload);

    /**
     * 延迟发送
     *
     * @param topic   主题
     * @param payload 业务负载
     * @param delay   延迟时长；非正数表示立即投递
     * @return 发送结果
     */
    SendResult sendDelayed(String topic, Object payload, Duration delay);

    /* ==================== 观测 ==================== */

    /**
     * 健康探测（实现必须做真实往返，不得仅判断「bean 非空」）
     *
     * @return 可用返回 true
     */
    boolean ping();

    /**
     * 当前 MQ 类型
     *
     * @return 类型枚举
     */
    MqType getMqType();

    /**
     * 人类可读的传输名（用于日志与健康检查明细）
     *
     * @return 如 {@code "redis-stream"}
     */
    String getTransportName();
}
