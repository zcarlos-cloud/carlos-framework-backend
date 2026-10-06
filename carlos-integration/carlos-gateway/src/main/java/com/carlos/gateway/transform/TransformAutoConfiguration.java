package com.carlos.gateway.transform;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 请求转换自动配置类
 * <p>
 * 负责请求转换相关的 Bean 配置：
 * - 请求转换过滤器（API 版本控制、路径重写、Header 转换）
 *
 * @author carlos
 * @date 2026/3/27
 */
@Slf4j
@AutoConfiguration
@ConditionalOnProperty(name = "carlos.gateway.transform.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(TransformProperties.class)
public class TransformAutoConfiguration {

    /**
     * 请求转换过滤器
     * 支持：API 版本控制、路径重写、Header 转换
     */
    @Bean
    @ConditionalOnMissingBean
    public RequestTransformFilter requestTransformFilter(TransformProperties properties) {
        log.info("Initializing Request Transform Filter");
        return new RequestTransformFilter(properties);
    }
}
