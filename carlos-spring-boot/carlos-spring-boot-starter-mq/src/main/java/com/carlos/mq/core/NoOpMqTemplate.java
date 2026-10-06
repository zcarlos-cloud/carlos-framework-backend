package com.carlos.mq.core;

import com.carlos.mq.support.MqType;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Set;

/**
 * 「显式未启用传输」时的兜底模板
 * <p>
 * 与旧实现的关键区别：旧版把 {@code sendDelayed} / {@code sendOrdered} /
 * {@code sendTransaction} <b>静默委托给普通 send 并返回「成功」</b>，语义丢失而调用方无感。
 * 新版一律返回 <b>失败结果</b>（{@link SendStatus#CAPABILITY_NOT_SUPPORTED}），
 * 明确告知能力缺失，绝不谎报成功。
 * </p>
 * <p>
 * 仅在显式配置 {@code carlos.mq.type=none} 时装配（表示「本次不接 MQ」，
 * 与「配置错误」区分开）。未配置传输实现时框架<b>不会</b>静默降级到本类，而是启动失败。
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@Slf4j
public class NoOpMqTemplate implements MqTemplate {

    private static final String REASON = "未启用 MQ 传输（carlos.mq.type=none）";

    @Override
    public Set<MqCapability> capabilities() {
        // 连 BASIC 都不声明：本实现不提供任何能力
        return Set.of();
    }

    @Override
    public SendResult send(String topic, Object payload) {
        return unsupported(topic);
    }

    @Override
    public SendResult send(String topic, Object payload, MqSendOptions options) {
        return unsupported(topic);
    }

    @Override
    public void sendAsync(String topic, Object payload, MqSendCallback callback) {
        SendResult result = unsupported(topic);
        if (callback != null) {
            callback.onComplete(result);
        }
    }

    @Override
    public void sendOneWay(String topic, Object payload) {
        // 单向发送没有返回值，无法通过结果告知失败；此处必须留痕，不能静默
        log.warn("NoOpMqTemplate：单向发送未投递，topic={}，原因={}", topic, REASON);
    }

    @Override
    public SendResult sendDelayed(String topic, Object payload, Duration delay) {
        return unsupported(topic);
    }

    @Override
    public boolean ping() {
        return false;
    }

    @Override
    public MqType getMqType() {
        return MqType.NONE;
    }

    @Override
    public String getTransportName() {
        return "none";
    }

    private SendResult unsupported(String topic) {
        log.warn("NoOpMqTemplate：发送未投递，topic={}，原因={}", topic, REASON);
        return SendResult.failure(SendStatus.CAPABILITY_NOT_SUPPORTED, null, topic);
    }
}
