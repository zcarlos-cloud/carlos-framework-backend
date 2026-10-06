package com.carlos.gateway.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.ObjectMapper;

/**
 * 网关缓存自动配置类
 * <p>
 * 负责响应缓存相关的 Bean 配置：
 * - 响应缓存过滤器（本地 Caffeine + Redis 两级缓存）
 *
 * @author carlos
 * @date 2026/3/27
 */
@Slf4j
@AutoConfiguration
@ConditionalOnProperty(name = "carlos.gateway.cache.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(CacheProperties.class)
public class CacheGatewayAutoConfiguration {

    /**
     * 响应缓存过滤器
     * 两级缓存：本地 Caffeine + Redis
     */
    @Bean
    @ConditionalOnMissingBean
    public ResponseCacheFilter responseCacheFilter(CacheProperties properties, ObjectMapper objectMapper) {
        log.info("Initializing Response Cache Filter");
        return new ResponseCacheFilter(properties, objectMapper);
    }
}
