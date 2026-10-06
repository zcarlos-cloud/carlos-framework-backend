package com.carlos.mq.spi;

import com.carlos.mq.core.MqCapability;
import com.carlos.mq.core.MqMessage;
import com.carlos.mq.core.MqSendCallback;
import com.carlos.mq.core.SendResult;
import com.carlos.mq.exception.MqCapabilityNotSupportedException;
import com.carlos.mq.support.MqType;

import java.time.Duration;
import java.util.Set;

/**
 * MQ 传输 SPI —— <b>新增一种 MQ 实现只需实现本接口并注册，调用方零改动。</b>
 *
 * <h2>设计契约</h2>
 * <ol>
 *   <li><b>能力要么真做，要么不声明</b>：{@link #capabilities()} 返回实际支持的能力集；
 *       未声明的能力一旦被调用，必须抛 {@link MqCapabilityNotSupportedException}，
 *       <b>禁止静默降级</b>（如把延迟消息退化成立即发送、把事务消息退化成普通发送并谎报成功）。</li>
 *   <li><b>可选能力用 default 方法</b>：默认实现即为抛「能力不支持」，
 *       因此最小实现只需提供 {@link #send(MqMessage)} 等基础方法，无需为不支持的能力写空壳。</li>
 *   <li><b>生命周期由容器驱动</b>：{@link #start()} / {@link #stop()} 应幂等；
 *       {@link #stop()} 必须能安全地在未 start 时调用。</li>
 *   <li><b>健康检查必须真实</b>：{@link #ping()} 应做一次真实往返（或至少一次真实命令），
 *       不得仅返回「bean 非空 &amp;&amp; 已初始化」这种恒真判定。</li>
 * </ol>
 *
 * <h2>实现清单（照此顺序写）</h2>
 * <ol>
 *   <li>实现 {@link #getType()}（{@link MqType} 中登记新类型）、{@link #getName()}、
 *       {@link #capabilities()}</li>
 *   <li>实现 {@link #start()} / {@link #stop()} / {@link #isRunning()} / {@link #ping()}</li>
 *   <li>实现 {@link #send(MqMessage)}；再按能力补
 *       {@link #sendDelayed} / {@link #sendAsync} / {@link #subscribe}。
 *       只发不收时声明 {@link MqCapability#BASIC} 即可；
 *       要消费须额外声明 {@link MqCapability#CONSUME} 并覆写 {@link #subscribe}。</li>
 *   <li>在传输注册表（{@link MqTransportRegistry}）中注册为 bean</li>
 * </ol>
 *
 * @author Carlos
 * @since 3.0.0
 */
public interface MqTransport {

    /**
     * 传输类型
     *
     * @return 类型枚举
     */
    MqType getType();

    /**
     * 人类可读的传输名（用于日志与健康检查明细）
     *
     * @return 如 {@code "redis-stream"}
     */
    String getName();

    /**
     * 本实现支持的能力集合（不可变）
     *
     * @return 能力集合
     */
    Set<MqCapability> capabilities();

    /**
     * 是否支持某能力
     *
     * @param capability 能力
     * @return 支持返回 true
     */
    default boolean supports(MqCapability capability) {
        return capabilities().contains(capability);
    }

    /**
     * 断言支持某能力，否则抛「能力不支持」异常
     *
     * @param capability 能力
     * @throws MqCapabilityNotSupportedException 不支持时抛出
     */
    default void requireCapability(MqCapability capability) {
        if (!supports(capability)) {
            throw new MqCapabilityNotSupportedException(capability, getType());
        }
    }

    /* ==================== 生命周期 ==================== */

    /**
     * 启动传输（应幂等）
     */
    void start();

    /**
     * 关闭传输（应幂等，且允许在未启动时调用）
     */
    void stop();

    /**
     * 是否处于运行态
     *
     * @return 运行中返回 true
     */
    boolean isRunning();

    /**
     * 健康探测（必须真实往返，不得恒真）
     *
     * @return 可用返回 true
     */
    boolean ping();

    /* ==================== 发送 ==================== */

    /**
     * 同步发送（基础能力，必须实现）
     *
     * @param message 消息信封
     * @return 发送结果
     */
    SendResult send(MqMessage message);

    /**
     * 单向发送：不等待结果
     * <p>默认实现即「发送并忽略结果」，语义诚实。</p>
     *
     * @param message 消息信封
     */
    default void sendOneWay(MqMessage message) {
        send(message);
    }

    /**
     * 异步发送
     *
     * @param message  消息信封
     * @param callback 完成回调
     */
    default void sendAsync(MqMessage message, MqSendCallback callback) {
        throw new MqCapabilityNotSupportedException(MqCapability.ASYNC_SEND, getType());
    }

    /**
     * 延迟发送
     *
     * @param message 消息信封
     * @param delay   延迟时长
     * @return 发送结果
     */
    default SendResult sendDelayed(MqMessage message, Duration delay) {
        throw new MqCapabilityNotSupportedException(MqCapability.DELAYED, getType());
    }

    /* ==================== 消费 ==================== */

    /**
     * 注册消费订阅
     * <p>声明了 {@link MqCapability#CONSUME} 的实现必须覆写本方法。</p>
     *
     * @param subscription 订阅描述
     */
    default void subscribe(MqSubscription subscription) {
        throw new MqCapabilityNotSupportedException(MqCapability.CONSUME, getType());
    }

    /**
     * 取消订阅
     *
     * @param topic 主题
     * @param group 消费组
     */
    default void unsubscribe(String topic, String group) {
        throw new MqCapabilityNotSupportedException(MqCapability.CONSUME, getType());
    }
}
