package com.carlos.gateway.gray;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.context.annotation.Bean;

/**
 * 灰度发布自动配置类
 * <p>
 * 负责灰度发布相关的 Bean 配置：
 * - 灰度发布过滤器
 *
 * @author carlos
 * @date 2026/3/27
 */
@Slf4j
@AutoConfiguration
@ConditionalOnProperty(name = "carlos.gateway.gray.enabled", havingValue = "true")
@EnableConfigurationProperties(GrayReleaseProperties.class)
public class GrayReleaseAutoConfiguration {

    /**
     * 灰度发布过滤器
     * 支持基于用户、IP、权重的灰度策略
     */
    @Bean
    @ConditionalOnMissingBean
    public GrayReleaseFilter grayReleaseFilter(GrayReleaseProperties properties,
                                               ReactiveDiscoveryClient discoveryClient) {
        log.info("Initializing Gray Release Filter");
        return new GrayReleaseFilter(properties, discoveryClient);
    }
}
