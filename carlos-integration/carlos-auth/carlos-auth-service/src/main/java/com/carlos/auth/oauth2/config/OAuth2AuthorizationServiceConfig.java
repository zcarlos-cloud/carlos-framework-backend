package com.carlos.auth.oauth2.config;

import com.carlos.auth.config.OAuth2Properties;
import com.carlos.auth.oauth2.repository.RedisOAuth2AuthorizationConsentService;
import com.carlos.auth.oauth2.repository.RedisOAuth2AuthorizationService;
import com.carlos.auth.oauth2.server.AuthorizationServerProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;

/**
 * 授权服务与服务器设置配置
 *
 * <p>管理 OAuth2 授权信息存储、授权确认服务和授权服务器端点设置。</p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(OAuth2Properties.class)
@ConditionalOnProperty(
    prefix = "carlos.auth.authorization-server",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class OAuth2AuthorizationServiceConfig {

    private final OAuth2Properties oauth2Properties;

    /**
     * 授权服务
     *
     * @return OAuth2AuthorizationService 授权服务
     */
    @Bean
    @ConditionalOnMissingBean
    public RedisOAuth2AuthorizationService authorizationService() {
        log.info("==========================================================================");
        log.info(" Using Customize OAuth2 Authorization Service (Redis-based, production ready)");
        log.info("==========================================================================");
        return new RedisOAuth2AuthorizationService();
    }

    /**
     * 授权确认服务
     *
     * @return OAuth2AuthorizationConsentService 授权确认服务
     */
    @Bean
    @ConditionalOnMissingBean
    public OAuth2AuthorizationConsentService authorizationConsentService() {
        log.info("Using Customize OAuth2 Authorization Consent Service (Redis-based)");
        return new RedisOAuth2AuthorizationConsentService();
    }

    /**
     * 授权服务器设置
     *
     * @return AuthorizationServerSettings 授权服务器设置
     */
    @Bean
    @ConditionalOnMissingBean
    public AuthorizationServerSettings authorizationServerSettings() {
        AuthorizationServerProperties config = oauth2Properties.getAuthorizationServer();

        return AuthorizationServerSettings.builder()
            .issuer(oauth2Properties.getJwt().getIssuer())
            .authorizationEndpoint(config.getAuthorizationEndpoint())
            .tokenEndpoint(config.getTokenEndpoint())
            .tokenRevocationEndpoint(config.getTokenRevocationEndpoint())
            .tokenIntrospectionEndpoint(config.getTokenIntrospectionEndpoint())
            .jwkSetEndpoint(config.getJwkSetEndpoint())
            .oidcUserInfoEndpoint(config.getOidcUserInfoEndpoint())
            .build();
    }
}
