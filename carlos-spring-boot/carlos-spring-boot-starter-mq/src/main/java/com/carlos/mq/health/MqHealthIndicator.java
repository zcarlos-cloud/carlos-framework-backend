package com.carlos.mq.health;

import com.carlos.mq.core.MqCapability;
import com.carlos.mq.core.MqTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;

/**
 * MQ 健康指示器
 * <p>
 * 相比旧实现的关键修正：旧版的 {@code ping()} 在三个传输实现里都只是
 * 「模板 bean 非空 &amp;&amp; initialized」的恒真判定，<b>从不做真实往返</b>，
 * 因此健康检查形同虚设。现在 {@code ping()} 由传输实现做一次真实命令往返
 * （Redis 传输为一次 {@code XLEN}）。
 * </p>
 * <p>
 * 明细中暴露传输名、类型与<b>能力集合</b>——能力是排查「为什么某方法不生效」的第一手信息
 * （例如看不到 {@code delayed} 就说明延时不可用）。
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@RequiredArgsConstructor
public class MqHealthIndicator implements HealthIndicator {

    private final MqTemplate mqTemplate;

    @Override
    public Health health() {
        boolean reachable;
        try {
            reachable = mqTemplate.ping();
        } catch (Exception e) {
            return Health.down()
                .withDetail("transport", safeTransportName())
                .withException(e)
                .build();
        }
        Health.Builder builder = reachable ? Health.up() : Health.down();
        return builder
            .withDetail("transport", safeTransportName())
            .withDetail("type", mqTemplate.getMqType() == null ? "unknown" : mqTemplate.getMqType().getCode())
            .withDetail("capabilities", mqTemplate.capabilities().stream()
                .map(MqCapability::getCode)
                .toList())
            .build();
    }

    private String safeTransportName() {
        try {
            return mqTemplate.getTransportName();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
