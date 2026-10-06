package com.carlos.gateway.observability;

import brave.Tracer;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.micrometer.tracing.brave.autoconfigure.BraveAutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * 网关可观测性自动配置类
 * <p>
 * 负责可观测性相关的 Bean 配置：
 * - 请求链路追踪过滤器（Request ID / Trace ID）
 * - Reactor Context → MDC 桥接（使 WebFlux 异步链路上的日志带上 traceId）
 * - 访问日志过滤器
 * - 指标收集过滤器
 * <p>
 * 装配顺序说明：{@link BraveAutoConfiguration} 负责创建 {@code brave.Tracer} Bean。
 * 若不声明先后关系，本类按类名字母序会先于它被解析，依赖 Tracer 的装配会静默失效。
 *
 * @author carlos
 * @date 2026/3/27
 */
@Slf4j
@AutoConfiguration
@AutoConfigureAfter(BraveAutoConfiguration.class)
@EnableConfigurationProperties({
    TracingProperties.class,
    AccessLogProperties.class
})
public class ObservabilityAutoConfiguration {

    /**
     * 统一链路追踪过滤器
     * <p>
     * 注入 B3 + sw8 + X-Trace-ID 多层传播头。
     * <p>
     * 这里用 {@link ObjectProvider} 延迟解析 Tracer 而不是 {@code @ConditionalOnBean}：
     * 后者只看得见「已处理过」的 Bean 定义，一旦自动配置顺序变化就会静默不注册该过滤器。
     */
    @Bean
    @ConditionalOnProperty(name = "carlos.gateway.tracing.enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean
    public RequestTracingFilter requestTracingFilter(
        ObjectProvider<Tracer> tracerProvider,
        TracingProperties properties) {
        Tracer tracer = tracerProvider.getIfAvailable();
        if (tracer == null) {
            log.warn("Brave Tracer 不存在, RequestTracingFilter 未注册; 请确认已引入 micrometer-tracing-bridge-brave");
            return null;
        }
        log.info("Initializing Request Tracing Filter");
        return new RequestTracingFilter(tracer, properties);
    }

    /**
     * Reactor Context → MDC 桥接钩子
     * <p>
     * traceId / spanId 由 {@link RequestTracingFilter} 写入 Reactor Context，
     * 本钩子把它们同步进 SLF4J MDC，logback 的 {@code %X{traceId}} 才能取到值。
     * 没有 Tracer 时 Context 里不会有值，因此不安装钩子（避免无谓的算子包装开销）。
     */
    @Bean
    @ConditionalOnProperty(name = "carlos.gateway.tracing.enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean
    public MdcContextHook mdcContextHook(ObjectProvider<Tracer> tracerProvider, TracingProperties properties) {
        if (!properties.isMdcEnabled()) {
            log.info("carlos.gateway.tracing.mdc-enabled=false, 跳过 Reactor Context -> MDC Hook");
            return null;
        }
        if (tracerProvider.getIfAvailable() == null) {
            log.warn("Brave Tracer 不存在, 跳过 Reactor Context -> MDC Hook");
            return null;
        }
        MdcContextHook hook = new MdcContextHook();
        hook.install();
        return hook;
    }

    /**
     * 访问日志过滤器
     * 记录所有请求的访问日志
     */
    @Bean
    @ConditionalOnProperty(name = "carlos.gateway.access-log.enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean
    public AccessLogFilter accessLogFilter() {
        log.info("Initializing Access Log Filter");
        return new AccessLogFilter();
    }

    /**
     * 指标收集过滤器
     * 基于 Micrometer 实现 Prometheus 指标暴露
     */
    @Bean
    @ConditionalOnBean(MeterRegistry.class)
    @ConditionalOnProperty(name = "carlos.gateway.metrics.enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean
    public MetricsFilter metricsFilter(MeterRegistry meterRegistry) {
        log.info("Initializing Metrics Filter");
        return new MetricsFilter(meterRegistry);
    }
}
