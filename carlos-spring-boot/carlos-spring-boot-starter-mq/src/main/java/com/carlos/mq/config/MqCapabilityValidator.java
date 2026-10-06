package com.carlos.mq.config;

import com.carlos.mq.core.MqCapability;
import com.carlos.mq.error.MqErrorCode;
import com.carlos.mq.exception.MqException;
import com.carlos.mq.listener.MqListenerEndpoint;
import com.carlos.mq.spi.MqTransport;
import com.carlos.mq.support.ConsumeMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * 启动期能力校验器
 *
 * <h2>为什么需要「启动期」而不是「调用期」校验</h2>
 * <p>
 * 如果只在调用时才发现能力缺失，问题会在<b>运行到某条消息</b>时才暴露——那时可能已经
 * 上线数小时，且只在特定分支触发。启动期求差集可以把「配了一个传输支持不了的组合」
 * 变成一次明确的启动失败。
 * </p>
 * <p>
 * 这是「不得静默降级」在消费侧的落地手段：宁可启动失败，也不要「声明了批量消费却按单条处理」
 * 这类语义悄悄失效。
 * </p>
 *
 * <h2>校验规则（端点需求 vs 传输能力）</h2>
 * <ul>
 *   <li>所有端点都需要 {@link MqCapability#CONSUME}</li>
 *   <li>{@code batchSize > 1} 需要 {@link MqCapability#BATCH_CONSUME}</li>
 *   <li>广播模式需要 {@link MqCapability#BROADCAST}</li>
 *   <li>开启了投递上限与死信时，需要 {@link MqCapability#DEAD_LETTER}
 *       与 {@link MqCapability#PENDING_CLAIM}（重投依赖认领）</li>
 * </ul>
 *
 * @author Carlos
 * @since 3.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class MqCapabilityValidator {

    private final MqProperties properties;

    /**
     * 校验端点需求与传输能力是否匹配
     *
     * @param transport 当前传输
     * @param endpoints 监听端点
     * @throws MqException 能力不匹配且开启了 fail-fast 时抛出（启动失败）
     */
    public void validate(MqTransport transport, List<MqListenerEndpoint> endpoints) {
        if (transport == null || endpoints.isEmpty()) {
            return;
        }
        Set<MqCapability> available = transport.capabilities();
        List<String> problems = new ArrayList<>();

        for (MqListenerEndpoint endpoint : endpoints) {
            for (MqCapability required : requiredCapabilities(endpoint)) {
                if (!available.contains(required)) {
                    problems.add("%s 需要能力 [%s]，但传输 [%s] 未声明（其能力集：%s）".formatted(
                        endpoint.display() + "@" + endpoint.subscriptionKey(),
                        required.getCode(),
                        transport.getName(),
                        available.stream().map(MqCapability::getCode).sorted().toList()));
                }
            }
        }
        if (problems.isEmpty()) {
            log.info("MQ 能力校验通过：端点={}, 传输={}, 能力集={}", endpoints.size(),
                transport.getName(), available.stream().map(MqCapability::getCode).sorted().toList());
            return;
        }

        String detail = String.join("；", problems);
        if (properties.getCapability().isFailFast()) {
            throw new MqException(MqErrorCode.MQ_CAPABILITY_NOT_SUPPORTED,
                "MQ 能力校验未通过（fail-fast 已开启，拒绝启动）：" + detail);
        }
        log.error("MQ 能力校验未通过（fail-fast 已关闭，继续启动，相关语义将不生效）：{}", detail);
    }

    /**
     * 单个端点需要的能力集
     *
     * @param endpoint 端点
     * @return 能力集合
     */
    Set<MqCapability> requiredCapabilities(MqListenerEndpoint endpoint) {
        Set<MqCapability> required = EnumSet.of(MqCapability.CONSUME);
        if (endpoint.batchSize() > 1) {
            required.add(MqCapability.BATCH_CONSUME);
        }
        if (endpoint.consumeMode() == ConsumeMode.BROADCAST) {
            required.add(MqCapability.BROADCAST);
        }
        boolean needsDeadLetter = properties.getConsumer().isDlqEnabled()
            && endpoint.maxDeliveryCount() != 0;
        if (needsDeadLetter) {
            required.add(MqCapability.DEAD_LETTER);
            required.add(MqCapability.PENDING_CLAIM);
        }
        return required;
    }
}
