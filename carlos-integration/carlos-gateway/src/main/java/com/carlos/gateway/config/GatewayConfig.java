package com.carlos.gateway.config;

import com.carlos.gateway.cache.CacheGatewayAutoConfiguration;
import com.carlos.gateway.circuitbreaker.CircuitBreakerAutoConfiguration;
import com.carlos.gateway.filter.FilterAutoConfiguration;
import com.carlos.gateway.gray.GrayReleaseAutoConfiguration;
import com.carlos.gateway.oauth2.OAuth2GatewayAutoConfiguration;
import com.carlos.gateway.observability.ObservabilityAutoConfiguration;
import com.carlos.gateway.security.SecurityGatewayAutoConfiguration;
import com.carlos.gateway.transform.TransformAutoConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

/**
 * <p>
 * 现代网关自动配置类
 * 整合所有网关组件
 * </p>
 * <p>
 * 配置结构说明：
 * 本类作为主配置入口，通过 @Import 导入各个功能模块的独立配置类，
 * 遵循高内聚低耦合原则，每个功能模块负责自己的 Bean 定义。
 * </p>
 *
 * <ul>
 *   <li>基础配置：{@link InfrastructureAutoConfiguration} - WebClient、Redis、异常处理等</li>
 *   <li>过滤器配置：{@link com.carlos.gateway.filter.FilterAutoConfiguration} - 路径前缀过滤器等</li>
 *   <li>OAuth2配置：{@link com.carlos.gateway.oauth2.OAuth2GatewayAutoConfiguration} - 认证授权</li>
 *   <li>熔断配置：{@link com.carlos.gateway.circuitbreaker.CircuitBreakerAutoConfiguration} - 熔断降级</li>
 *   <li>灰度配置：{@link com.carlos.gateway.gray.GrayReleaseAutoConfiguration} - 灰度发布</li>
 *   <li>安全配置：{@link com.carlos.gateway.security.SecurityGatewayAutoConfiguration} - WAF、防重放</li>
 *   <li>缓存配置：{@link com.carlos.gateway.cache.CacheGatewayAutoConfiguration} - 响应缓存</li>
 *   <li>可观测性配置：{@link com.carlos.gateway.observability.ObservabilityAutoConfiguration} - 链路追踪、日志、指标</li>
 *   <li>转换配置：{@link com.carlos.gateway.transform.TransformAutoConfiguration} - 请求转换</li>
 * </ul>
 *
 * @author carlos
 * @date 2026/3/16
 * @updated 2026/3/27 重构为配置聚合入口，各功能模块独立配置
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties({
        GatewayProperties.class,
        ExceptionProperties.class
})
@Import({
        InfrastructureAutoConfiguration.class,
        FilterAutoConfiguration.class,
        OAuth2GatewayAutoConfiguration.class,
        CircuitBreakerAutoConfiguration.class,
        GrayReleaseAutoConfiguration.class,
        SecurityGatewayAutoConfiguration.class,
        CacheGatewayAutoConfiguration.class,
        ObservabilityAutoConfiguration.class,
        TransformAutoConfiguration.class
})
public class GatewayConfig {

    public GatewayConfig() {
        log.info("GatewayConfig initialized - all module configurations imported");
    }
}
