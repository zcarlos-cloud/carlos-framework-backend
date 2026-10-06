package com.carlos.gateway.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;

/**
 * 网关安全防护自动配置类
 * <p>
 * 负责安全防护相关的 Bean 配置：
 * - WAF 过滤器（SQL 注入、XSS、CSRF 等防护）
 * - 防重放攻击过滤器
 *
 * @author carlos
 * @date 2026/3/27
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties({
    WafProperties.class,
    ReplayProtectionProperties.class
})
public class SecurityGatewayAutoConfiguration {

    /**
     * Web 应用防火墙（WAF）过滤器
     * 防护：SQL 注入、XSS、CSRF、路径遍历、敏感文件访问、命令注入
     */
    @Bean
    @ConditionalOnProperty(name = "carlos.gateway.waf.enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean
    public WafFilter wafFilter(WafProperties properties) {
        log.info("Initializing WAF Filter");
        return new WafFilter(properties);
    }

    /**
     * 防重放攻击过滤器
     * 基于请求签名和时间戳验证
     */
    @Bean
    @ConditionalOnProperty(name = "carlos.gateway.replay.enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean
    public ReplayProtectionFilter replayProtectionFilter(
        ReplayProtectionProperties properties,
        ReactiveStringRedisTemplate redisTemplate) {
        log.info("Initializing Replay Protection Filter");
        return new ReplayProtectionFilter(properties, redisTemplate);
    }
}
