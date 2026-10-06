package com.carlos.monitor.collect.sink;

import com.carlos.disruptor.core.DisruptorManager;
import com.carlos.disruptor.core.DisruptorTemplate;
import com.carlos.disruptor.core.DisruptorWaitStrategyType;
import com.carlos.monitor.config.MonitorProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <p>
 * 监控指标 Disruptor 配置（复用 starter-disruptor 的 DisruptorManager/Template）
 * 环形缓冲大小取 carlos.monitor.sink.disruptor.ring-size
 * </p>
 *
 * @author Carlos
 * @date 2026年10月2日
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "carlos.monitor", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MonitorDisruptorConfig {

    private final MonitorProperties monitorProperties;

    private final MonitorMetricEventHandler eventHandler;

    /**
     * 监控指标 Disruptor 模板
     */
    @Bean("monitorDisruptorTemplate")
    @ConditionalOnMissingBean(name = "monitorDisruptorTemplate")
    public DisruptorTemplate<MonitorSinkEvent> monitorDisruptorTemplate(DisruptorManager disruptorManager) {
        int ringSize = monitorProperties.getSink().getDisruptor().getRingSize();
        log.info("初始化监控指标 Disruptor，bufferSize={}, waitStrategy={}", ringSize, DisruptorWaitStrategyType.BLOCKING);
        return disruptorManager.create("monitor-disruptor",
            ringSize, DisruptorWaitStrategyType.BLOCKING.toStrategy(), eventHandler);
    }
}
