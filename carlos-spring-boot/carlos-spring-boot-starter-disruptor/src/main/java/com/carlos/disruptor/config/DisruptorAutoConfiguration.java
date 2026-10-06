package com.carlos.disruptor.config;

import com.carlos.disruptor.core.DisruptorManager;
import com.carlos.disruptor.metrics.DisruptorHealthIndicator;
import com.carlos.disruptor.metrics.DisruptorMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Disruptor 自动配置类
 *
 * @author Carlos
 * @date 2026-03-14
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(DisruptorProperties.class)
@ConditionalOnProperty(prefix = "carlos.disruptor", name = "enabled", havingValue = "true")
public class DisruptorAutoConfiguration {

    /**
     * Disruptor 监控指标
     *
     * <p>当 Micrometer MeterRegistry 可用时自动注入，否则传 null。
     * 使用 ObjectProvider 消除之前两个 Bean 方法竞争的问题。</p>
     */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean
    public DisruptorMetrics disruptorMetrics(ObjectProvider<MeterRegistry> registryProvider) {
        return new DisruptorMetrics(registryProvider.getIfAvailable());
    }

    /**
     * Disruptor 管理器
     */
    @Bean
    @ConditionalOnMissingBean
    public DisruptorManager disruptorManager(DisruptorProperties properties, DisruptorMetrics metrics) {
        if (log.isInfoEnabled()) {
            log.info("Disruptor Manager initializing. defaultBufferSize={}, defaultWaitStrategy={}",
                properties.getBufferSize(), properties.getWaitStrategy());
        }
        return new DisruptorManager(properties, metrics);
    }

    /**
     * Disruptor 健康检查
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(HealthIndicator.class)
    public DisruptorHealthIndicator disruptorHealthIndicator(DisruptorManager disruptorManager) {
        return new DisruptorHealthIndicator(disruptorManager);
    }
}
