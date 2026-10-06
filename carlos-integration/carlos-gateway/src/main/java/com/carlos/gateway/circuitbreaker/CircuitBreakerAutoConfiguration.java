package com.carlos.gateway.circuitbreaker;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * 熔断降级自动配置类
 * <p>
 * 负责熔断相关的 Bean 配置：
 * - Resilience4j 熔断过滤器
 *
 * @author carlos
 * @date 2026/3/27
 */
@Slf4j
@AutoConfiguration
@ConditionalOnProperty(name = "carlos.gateway.circuitbreaker.enabled", havingValue = "true", matchIfMissing = true)
public class CircuitBreakerAutoConfiguration {

    /**
     * Resilience4j 熔断过滤器
     */
    @Bean
    @ConditionalOnMissingBean
    public Resilience4jCircuitBreakerFilter circuitBreakerFilter() {
        log.info("Initializing Circuit Breaker Filter");
        return new Resilience4jCircuitBreakerFilter();
    }
}
