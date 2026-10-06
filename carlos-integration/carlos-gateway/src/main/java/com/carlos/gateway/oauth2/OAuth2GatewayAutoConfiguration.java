package com.carlos.gateway.oauth2;

import com.carlos.gateway.oauth2.provider.DefaultPermissionProvider;
import com.carlos.gateway.oauth2.provider.PermissionProvider;
import com.carlos.gateway.oauth2.validator.JwtTokenValidator;
import com.carlos.gateway.oauth2.validator.OpaqueTokenValidator;
import com.carlos.gateway.oauth2.validator.TokenValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * OAuth2 网关认证自动配置类
 * <p>
 * 负责 OAuth2 认证相关的 Bean 配置：
 * - Token 验证器（JWT / Opaque Token）
 * - 认证过滤器
 * - 授权过滤器
 *
 * @author carlos
 * @date 2026/3/27
 */
@Slf4j
@AutoConfiguration
@ConditionalOnProperty(name = "carlos.gateway.oauth2.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(OAuth2GatewayProperties.class)
public class OAuth2GatewayAutoConfiguration {

    /**
     * Token 验证器
     * 根据配置选择 JWT 或 Opaque Token 验证器
     */
    @Bean
    @ConditionalOnMissingBean
    public TokenValidator tokenValidator(OAuth2GatewayProperties properties,
                                         ReactiveStringRedisTemplate redisTemplate,
                                         WebClient.Builder webClientBuilder) {
        return switch (properties.getTokenType()) {
            case JWT -> new JwtTokenValidator(properties, redisTemplate);
            case OPAQUE -> new OpaqueTokenValidator(properties, webClientBuilder, redisTemplate);
        };
    }

    /**
     * OAuth2 认证过滤器
     * 统一处理 JWT 和 Opaque Token 认证
     */
    @Bean
    @ConditionalOnMissingBean
    public OAuth2AuthenticationFilter oAuth2AuthenticationFilter(
        OAuth2GatewayProperties properties,
        TokenValidator tokenValidator) {
        log.info("Initializing OAuth2 Authentication Filter with type: {}", properties.getTokenType());
        return new OAuth2AuthenticationFilter(properties, tokenValidator);
    }

    /**
     * 默认权限提供者
     */
    @Bean
    @ConditionalOnMissingBean(PermissionProvider.class)
    public PermissionProvider permissionProvider(ReactiveStringRedisTemplate redisTemplate) {
        return new DefaultPermissionProvider(redisTemplate);
    }

    /**
     * OAuth2 授权过滤器（RBAC/ABAC 权限控制）
     */
    @Bean
    @ConditionalOnProperty(name = "carlos.gateway.oauth2.authorization-enabled", havingValue = "true")
    @ConditionalOnMissingBean
    public OAuth2AuthorizationFilter oAuth2AuthorizationFilter(
        OAuth2GatewayProperties properties,
        PermissionProvider permissionProvider) {
        log.info("Initializing OAuth2 Authorization Filter with mode: {}", properties.getAuthorizationMode());
        return new OAuth2AuthorizationFilter(properties, permissionProvider);
    }
}
