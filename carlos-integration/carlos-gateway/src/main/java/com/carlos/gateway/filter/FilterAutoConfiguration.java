package com.carlos.gateway.filter;

import com.carlos.gateway.config.GatewayProperties;
import com.carlos.gateway.whitelist.UnifiedWhitelistFilter;
import com.carlos.gateway.whitelist.UnifiedWhitelistProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 网关过滤器自动配置类 - 性能优化版
 * <p>
 * 负责过滤器相关的 Bean 配置：
 * - 统一前缀剥离 WebFilter（最先执行，在路由匹配前剥离 /manage-api）
 * - 统一白名单过滤器（将匹配结果存入 Exchange Attributes）
 * - 路径前缀过滤器（已废弃，由 PrefixWebFilter 替代）
 * - 选择性路径前缀剥离过滤器工厂（Swagger/Knife4j）
 *
 * @author carlos
 * @date 2026/3/27
 * @updated 2026/4/10 添加统一白名单过滤器
 * @updated 2026/6/13 使用 PrefixWebFilter 替代 PathPrefixFilter，实现路由前统一前缀剥离
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties({
    UnifiedWhitelistProperties.class
})
public class FilterAutoConfiguration {

    // ==================== 统一前缀剥离 WebFilter（最先执行）====================

    /**
     * 统一前缀剥离 WebFilter
     * <p>
     * 在 Spring Cloud Gateway 路由匹配之前执行，提前剥离 {@code /manage-api} 等统一前缀。
     * 这样路由 predicate 可以基于剥离后的路径编写，前缀只在一处定义。
     * </p>
     *
     * @param gatewayProperties   网关配置
     * @param whitelistProperties 统一白名单配置
     * @return PrefixWebFilter
     */
    @Bean
    @ConditionalOnProperty(name = "carlos.gateway.prefix-enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean
    public PrefixWebFilter prefixWebFilter(GatewayProperties gatewayProperties,
                                           UnifiedWhitelistProperties whitelistProperties) {
        log.info("Initializing Prefix Web Filter with prefix: {}", gatewayProperties.getPrefix());
        return new PrefixWebFilter(gatewayProperties, whitelistProperties);
    }

    // ==================== 统一白名单过滤器 ====================

    /**
     * 统一白名单过滤器
     * 最先执行，统一检查所有类型的白名单，将结果存入 Exchange Attributes
     * 后续过滤器直接从 Attributes 读取，避免重复检查
     */
    @Bean
    @ConditionalOnProperty(name = "carlos.gateway.whitelist.enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean
    public UnifiedWhitelistFilter unifiedWhitelistFilter(UnifiedWhitelistProperties properties) {
        log.info("Initializing Unified Whitelist Filter (optimized path matching)");
        return new UnifiedWhitelistFilter(properties);
    }

    // ==================== 路径处理过滤器 ====================

    /**
     * 选择性路径前缀剥离过滤器工厂
     * 用于处理 Swagger/Knife4j 文档路径的前缀剥离
     */
    @Bean
    @ConditionalOnMissingBean
    public SelectStripPrefixGatewayFilterFactory selectStripPrefixGatewayFilterFactory() {
        log.info("Initializing Select Strip Prefix Gateway Filter Factory (Optimized with Caffeine Cache)");
        return new SelectStripPrefixGatewayFilterFactory();
    }

}
