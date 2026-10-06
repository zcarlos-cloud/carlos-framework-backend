package com.carlos.apm.config;

import brave.Tracer;
import brave.propagation.B3Propagation;
import brave.propagation.Propagation;
import brave.sampler.Sampler;
import com.carlos.apm.aop.TraceTagAspect;
import com.carlos.apm.mdc.MdcFilter;
import com.carlos.apm.skywalking.SkyWalkingDetector;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.micrometer.tracing.brave.autoconfigure.BraveAutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

/**
 * APM 统一自动配置
 * <p>
 * 渐进式可观测性架构的入口配置，按层级自动装配：
 * <ul>
 *   <li><b>Layer 1（日志关联）</b>: MDC Filter 始终启用</li>
 *   <li><b>Layer 2（链路追踪）</b>: Brave Sampler + Propagation + TraceTagAspect</li>
 *   <li><b>Layer 3（SkyWalking）</b>: 自动检测 Agent 存在后增强</li>
 * </ul>
 * <p>
 * 装配顺序说明：{@code traceTagAspect} 依赖 {@code brave.Tracer} Bean，而该 Bean 由
 * {@link BraveAutoConfiguration} 创建。若不声明先后关系，本类按类名字母序会先于
 * BraveAutoConfiguration 被解析，{@code @ConditionalOnBean} 必然不匹配而静默跳过。
 *
 * @author Carlos
 * @date 2024-12-06
 */
@Slf4j
@AutoConfiguration
@AutoConfigureAfter(BraveAutoConfiguration.class)
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "carlos.apm", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(ApmProperties.class)
public class ApmConfig {

    private final ApmProperties apmProperties;

    @PostConstruct
    public void init() {
        log.info("[APM] 渐进式可观测性模块已启动");
        log.info("[APM] 日志追踪: {} | 链路追踪: {} | SkyWalking 增强: {}",
            "已启用",
            apmProperties.getTracing().isEnabled() ? "已启用 (Brave)" : "已禁用",
            SkyWalkingDetector.shouldEnhance() ? "已激活 (Agent 检测到)" : "未激活 (未部署 Agent)");
    }

    // ==================== Layer 1: 日志关联（始终启用） ====================

    /**
     * MDC 过滤器 — 将 Trace ID 注入日志上下文
     */
    @Bean
    @ConditionalOnWebApplication
    @ConditionalOnProperty(prefix = "carlos.apm.mdc", name = "enabled", havingValue = "true", matchIfMissing = true)
    public FilterRegistrationBean<MdcFilter> mdcFilterRegistration() {
        FilterRegistrationBean<MdcFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new MdcFilter(apmProperties));
        registration.addUrlPatterns("/*");
        registration.setName("mdcFilter");
        // 最高优先级，确保在其他 Filter 之前设置 MDC
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        log.debug("[APM] MDC Filter 已注册");
        return registration;
    }

    // ==================== Layer 2: 链路追踪（内置 Brave） ====================

    /**
     * 采样器 — 基于配置的采样率
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(Tracer.class)
    @ConditionalOnProperty(prefix = "carlos.apm.tracing", name = "enabled", havingValue = "true", matchIfMissing = true)
    public Sampler apmSampler() {
        double probability = apmProperties.getTracing().getSampleRate();
        if (probability >= 1.0) {
            return Sampler.ALWAYS_SAMPLE;
        } else if (probability <= 0.0) {
            return Sampler.NEVER_SAMPLE;
        }
        return Sampler.create((float) probability);
    }

    /**
     * B3 传播工厂 — 支持单头和多头格式
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(Tracer.class)
    @ConditionalOnProperty(prefix = "carlos.apm.tracing", name = "enabled", havingValue = "true", matchIfMissing = true)
    public Propagation.Factory apmPropagationFactory() {
        String propagationType = apmProperties.getTracing().getPropagation();
        if ("B3_MULTI".equalsIgnoreCase(propagationType)) {
            // 多头部格式：X-B3-TraceId, X-B3-SpanId, X-B3-Sampled
            return B3Propagation.newFactoryBuilder()
                .injectFormat(B3Propagation.Format.MULTI)
                .build();
        }
        // 默认单头格式：b3={traceId}-{spanId}-{sampled}
        return B3Propagation.newFactoryBuilder()
            .injectFormat(B3Propagation.Format.SINGLE_NO_PARENT)
            .build();
    }

    /**
     * TraceTag 注解切面 — 提供方法级自定义追踪标签
     */
    @Bean
    @ConditionalOnClass(Tracer.class)
    @ConditionalOnBean(Tracer.class)
    @ConditionalOnProperty(prefix = "carlos.apm.tracing", name = "enabled", havingValue = "true", matchIfMissing = true)
    public TraceTagAspect traceTagAspect(Tracer tracer) {
        log.debug("[APM] TraceTag 切面已注册");
        return new TraceTagAspect(tracer);
    }

    // ==================== Feign 传播 ====================

    /**
     * Feign 拦截器 — 向下游服务透传 B3 头
     */
    @Bean
    @ConditionalOnClass(name = "feign.RequestInterceptor")
    @ConditionalOnProperty(prefix = "carlos.apm.feign", name = "enabled", havingValue = "true", matchIfMissing = true)
    public TracingFeignInterceptor tracingFeignInterceptor(ObjectProvider<Tracer> tracerProvider) {
        log.debug("[APM] Feign 链路追踪拦截器已注册");
        return new TracingFeignInterceptor(tracerProvider.getIfAvailable());
    }
}
