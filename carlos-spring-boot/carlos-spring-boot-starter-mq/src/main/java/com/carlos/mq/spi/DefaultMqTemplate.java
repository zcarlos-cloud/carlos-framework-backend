package com.carlos.mq.spi;

import com.carlos.mq.core.MqCapability;
import com.carlos.mq.core.MqMessage;
import com.carlos.mq.core.MqPayloadCodec;
import com.carlos.mq.core.MqSendCallback;
import com.carlos.mq.core.MqSendOptions;
import com.carlos.mq.core.MqTemplate;
import com.carlos.mq.core.SendResult;
import com.carlos.mq.core.SendStatus;
import com.carlos.mq.support.MqType;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * 传输无关的 {@link MqTemplate} 默认实现
 * <p>
 * <b>这是「新增传输无需改动模板」的关键</b>：本类只依赖 {@link MqTransport} SPI 与
 * {@link MqPayloadCodec}，因此接入一种新 MQ 时只需实现 SPI，业务侧的 {@code MqTemplate}
 * 无需任何改动。
 * </p>
 * <p>
 * 职责边界：
 * </p>
 * <ul>
 *   <li>本类负责：主题校验、负载序列化与体积校验、选项默认值、异步提交、结果映射、
 *       能力缺失时的<b>显式失败结果</b>。</li>
 *   <li>传输负责：信封收发、生命周期、能力声明、延迟与消费的具体机制。</li>
 * </ul>
 * <p>
 * <b>失败一律通过 {@link SendResult} 表达，不抛异常</b>（本项目禁止使用
 * {@code RuntimeException}，且发送失败属于数据而非编程错误）。
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@Slf4j
public class DefaultMqTemplate implements MqTemplate, AutoCloseable {

    /** 主题合法字符（主题会成为底层 key 的一部分，必须收紧） */
    private static final Pattern TOPIC_PATTERN = Pattern.compile("[A-Za-z0-9:_.\\-]+");

    private static final int TOPIC_MAX_LENGTH = 200;

    /** 默认单条负载上限（字节） */
    private static final int DEFAULT_MAX_PAYLOAD_BYTES = 65536;

    private final MqTransport transport;
    private final MqPayloadCodec codec;
    private final int maxPayloadBytes;
    private final ExecutorService asyncExecutor;

    public DefaultMqTemplate(MqTransport transport, MqPayloadCodec codec) {
        this(transport, codec, DEFAULT_MAX_PAYLOAD_BYTES);
    }

    public DefaultMqTemplate(MqTransport transport, MqPayloadCodec codec, int maxPayloadBytes) {
        this.transport = transport;
        this.codec = codec;
        this.maxPayloadBytes = maxPayloadBytes <= 0 ? DEFAULT_MAX_PAYLOAD_BYTES : maxPayloadBytes;
        // 异步发送是 IO 等待型任务，用虚拟线程每任务一线程；不做池化以免长驻占用
        this.asyncExecutor = Executors.newVirtualThreadPerTaskExecutor();
    }

    /* ==================== 能力 ==================== */

    @Override
    public Set<MqCapability> capabilities() {
        // 异步发送由本类的虚拟线程实现，故与传输能力求并集：
        // 若只透传传输能力，会出现「明明实现了 sendAsync 却报不支持」的自相矛盾
        Set<MqCapability> union = EnumSet.noneOf(MqCapability.class);
        union.addAll(transport.capabilities());
        union.add(MqCapability.ASYNC_SEND);
        return Set.copyOf(union);
    }

    /**
     * 释放异步执行器（作为 Spring bean 时由容器在关闭阶段调用）
     */
    @Override
    public void close() {
        asyncExecutor.shutdownNow();
    }

    /* ==================== 发送 ==================== */

    @Override
    public SendResult send(String topic, Object payload) {
        return send(topic, payload, MqSendOptions.none());
    }

    @Override
    public SendResult send(String topic, Object payload, MqSendOptions options) {
        Prepared prepared = prepare(topic, payload, options);
        if (prepared.failure() != null) {
            return prepared.failure();
        }
        long begin = System.currentTimeMillis();
        SendResult result = transport.send(prepared.message());
        result.setSendCostTime(System.currentTimeMillis() - begin);
        return result;
    }

    @Override
    public void sendAsync(String topic, Object payload, MqSendCallback callback) {
        MqSendCallback effective = callback == null ? ignored -> {
        } : callback;
        if (!supports(MqCapability.ASYNC_SEND)) {
            effective.onComplete(SendResult.failure(
                SendStatus.CAPABILITY_NOT_SUPPORTED, null, topic));
            return;
        }
        asyncExecutor.execute(() -> {
            SendResult result;
            try {
                result = send(topic, payload);
            } catch (Exception e) {
                log.error("异步发送异常：topic={}", topic, e);
                result = SendResult.failure(SendStatus.SEND_FAILED, null, topic, e);
            }
            effective.onComplete(result);
        });
    }

    @Override
    public void sendOneWay(String topic, Object payload) {
        Prepared prepared = prepare(topic, payload, MqSendOptions.none());
        if (prepared.failure() != null) {
            // 单向发送没有返回值可承载失败，必须留痕
            log.warn("单向发送未投递：topic={}, status={}", topic, prepared.failure().getStatus());
            return;
        }
        transport.sendOneWay(prepared.message());
    }

    @Override
    public SendResult sendDelayed(String topic, Object payload, Duration delay) {
        if (!supports(MqCapability.DELAYED)) {
            return SendResult.failure(SendStatus.CAPABILITY_NOT_SUPPORTED, null, topic);
        }
        Prepared prepared = prepare(topic, payload, MqSendOptions.none());
        if (prepared.failure() != null) {
            return prepared.failure();
        }
        return transport.sendDelayed(prepared.message(), delay == null ? Duration.ZERO : delay);
    }

    /* ==================== 观测 ==================== */

    @Override
    public boolean ping() {
        return transport.ping();
    }

    @Override
    public MqType getMqType() {
        return transport.getType();
    }

    @Override
    public String getTransportName() {
        return transport.getName();
    }

    /* ==================== 内部 ==================== */

    /**
     * 组装结果：成功时 {@code message} 非空，失败时 {@code failure} 非空
     */
    private record Prepared(MqMessage message, SendResult failure) {
    }

    /**
     * 校验并组装待发送消息（不抛异常，失败以结果承载）
     *
     * @param topic   主题
     * @param payload 负载
     * @param options 选项
     * @return 组装结果
     */
    private Prepared prepare(String topic, Object payload, MqSendOptions options) {
        if (topic == null || topic.isBlank()
            || topic.length() > TOPIC_MAX_LENGTH
            || !TOPIC_PATTERN.matcher(topic).matches()) {
            return new Prepared(null, SendResult.failure(SendStatus.INVALID_ARGUMENT, null, topic));
        }
        if (payload == null) {
            return new Prepared(null, SendResult.failure(SendStatus.INVALID_ARGUMENT, null, topic));
        }
        String json;
        try {
            json = codec.serialize(payload);
        } catch (Exception e) {
            return new Prepared(null,
                SendResult.failure(SendStatus.SERIALIZATION_ERROR, null, topic, e));
        }
        if (json.getBytes(StandardCharsets.UTF_8).length > maxPayloadBytes) {
            return new Prepared(null, SendResult.failure(SendStatus.PAYLOAD_TOO_LARGE, null, topic));
        }
        MqMessage message = MqMessage.of(topic, json, payload.getClass().getName(),
            options == null ? MqSendOptions.none() : options);
        return new Prepared(message, null);
    }
}
