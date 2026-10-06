package com.carlos.auth.oauth2.config;

import com.carlos.auth.config.OAuth2Properties;
import com.carlos.auth.oauth2.Oauth2JwtTokenCustomizer;
import com.carlos.auth.oauth2.client.CustomizeClientOAuth2AccessTokenGenerator;
import com.carlos.auth.oauth2.client.CustomizeClientOAuth2TokenCustomizer;
import com.carlos.auth.oauth2.user.CustomizeUserOAuth2AccessTokenGenerator;
import com.carlos.auth.oauth2.user.CustomizeUserOAuth2TokenCustomizer;
import com.carlos.auth.security.service.ExtendUserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.server.authorization.token.*;

/**
 * Token 生成与定制配置
 *
 * <p>管理 JWT Token 自定义增强器和 Token 生成器组合。</p>
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
public class OAuth2TokenConfig {

    private final OAuth2Properties oauth2Properties;

    /**
     * JWT Token 自定义增强器
     *
     * @param userDetailsService 用户详情服务
     * @return OAuth2TokenCustomizer Token 自定义增强器
     */
    @Bean
    @ConditionalOnMissingBean
    public OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer(
        ExtendUserDetailsService userDetailsService) {

        if (oauth2Properties.getJwt().isIncludeUserInfo()) {
            log.info("JWT token customizer enabled with user info inclusion");
            return new Oauth2JwtTokenCustomizer(userDetailsService);
        }

        log.info("JWT token customizer enabled without user info");
        return context -> {
            if (oauth2Properties.getJwt().getCustomClaims() != null) {
                oauth2Properties.getJwt().getCustomClaims().forEach(
                    (key, value) -> context.getClaims().claim(key, value)
                );
            }
        };
    }

    /**
     * 配置 Token 生成器组合
     *
     * <p>生成器优先级顺序（DelegatingOAuth2TokenGenerator 按注册顺序尝试）：</p>
     * <ol>
     *   <li>CustomizeUserOAuth2AccessTokenGenerator - 处理自定义 grant_type（password/sms_code 等）</li>
     *   <li>CustomizeClientOAuth2AccessTokenGenerator - 处理 CLIENT_CREDENTIALS</li>
     *   <li>JwtGenerator - 处理标准流程（authorization_code 等），生成 JWT 格式 Access Token</li>
     *   <li>OAuth2RefreshTokenGenerator - 生成 Refresh Token</li>
     * </ol>
     *
     * @param userDetailsService 用户详情服务
     * @param jwtEncoder JWT 编码器
     * @param jwtTokenCustomizer JWT Token 自定义增强器
     * @return OAuth2TokenGenerator 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public OAuth2TokenGenerator<? extends OAuth2Token> oAuth2TokenGenerator(
        ExtendUserDetailsService userDetailsService,
        JwtEncoder jwtEncoder,
        OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer) {

        // 自定义 grant_type（password/sms_code 等）的 Token 生成器
        CustomizeUserOAuth2AccessTokenGenerator userTokenGenerator = new CustomizeUserOAuth2AccessTokenGenerator();
        userTokenGenerator.setAccessTokenCustomizer(new CustomizeUserOAuth2TokenCustomizer(userDetailsService));
        log.info("Configuring CustomizeUserOAuth2AccessTokenGenerator for custom grant types");

        // CLIENT_CREDENTIALS 的 Token 生成器
        CustomizeClientOAuth2AccessTokenGenerator clientTokenGenerator = new CustomizeClientOAuth2AccessTokenGenerator();
        clientTokenGenerator.setAccessTokenCustomizer(new CustomizeClientOAuth2TokenCustomizer());
        log.info("Configuring CustomizeClientOAuth2AccessTokenGenerator for CLIENT_CREDENTIALS");

        // 标准 JWT Token 生成器（处理 authorization_code 等标准 OAuth2 流程）
        JwtGenerator jwtGenerator = new JwtGenerator(jwtEncoder);
        jwtGenerator.setJwtCustomizer(jwtTokenCustomizer);
        log.info("Configuring JwtGenerator for standard OAuth2 flows (authorization_code, etc.)");

        OAuth2RefreshTokenGenerator refreshTokenGenerator = new OAuth2RefreshTokenGenerator();

        return new DelegatingOAuth2TokenGenerator(
            userTokenGenerator, clientTokenGenerator, jwtGenerator, refreshTokenGenerator);
    }
}
