package com.carlos.gateway.oauth2;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

/**
 * <p>
 * OAuth2 网关认证属性配置
 * 支持 JWT 和 Opaque Token 双模式
 * </p>
 *
 * @author carlos
 * @date 2026/3/16
 */
@Slf4j
@Data
@ConfigurationProperties(prefix = "carlos.gateway.oauth2")
public class OAuth2GatewayProperties implements InitializingBean {

    /**
     * 是否启用 OAuth2 认证
     */
    private boolean enabled = true;

    /**
     * Token 类型：JWT 或 OPAQUE
     */
    private TokenType tokenType = TokenType.JWT;

    /**
     * JWT 公钥（用于验证 JWT 签名）
     */
    private String jwtPublicKey;

    /**
     * JWT 发行者
     */
    private String jwtIssuer = "carlos-auth";

    /**
     * JWT 受众
     */
    private String jwtAudience = "carlos-gateway";

    /**
     * Opaque Token 认证服务器地址
     */
    private String introspectionUri = "http://carlos-auth/oauth2/introspect";

    /**
     * Opaque Token 客户端 ID
     */
    private String clientId;

    /**
     * Opaque Token 客户端密钥
     */
    private String clientSecret;

    /**
     * Token 检查缓存时间（避免频繁调用认证服务器）
     */
    private Duration introspectionCacheDuration = Duration.ofMinutes(5);

    /**
     * 白名单路径（无需认证）
     */
    private Set<String> whitelist = new HashSet<>();

    /**
     * 是否开启权限校验
     */
    private boolean authorizationEnabled = true;

    /**
     * 权限校验模式：RBAC（角色）或 ABAC（属性）
     */
    private AuthorizationMode authorizationMode = AuthorizationMode.RBAC;

    /**
     * 是否开启 PKCE 支持
     */
    private boolean pkceEnabled = true;

    /**
     * 默认白名单
     */
    private static final Set<String> DEFAULT_WHITELIST = Set.of(
            "/actuator/**",
            "/health",
            "/**/swagger-ui/**",
            "/swagger-resources/**",
            "/**/v3/api-docs/**",
            "/**/v3/api-docs",
            "/**/doc.html",
            "/**/webjars/**",
            "/**/favicon.ico",
            "/oauth2/authorize",
            "/oauth2/token",
            "/oauth2/introspect",
            "/login",
            "/logout",
            "/auth/public-key",
            "/auth/user/login",
            "/auth/user/pre-check",
            "/auth/user/logout",
            "/auth/captcha/**",
            "/auth/mfa/step-up-verify",
            "/public/**"
    );

    @Override
    public void afterPropertiesSet() {
        // 合并默认白名单
        Set<String> merged = new HashSet<>(DEFAULT_WHITELIST);
        merged.addAll(whitelist);
        this.whitelist = merged;

        if (!enabled) {
            log.info("OAuth2 authentication is disabled");
        } else {
            log.info("OAuth2 authentication enabled with token type: {}", tokenType);
        }
    }

    public enum TokenType {
        /**
         * JWT 自包含令牌
         */
        JWT,
        /**
         * Opaque 随机字符串令牌（参考微信/钉钉设计）
         */
        OPAQUE
    }

    public enum AuthorizationMode {
        /**
         * 基于角色的访问控制
         */
        RBAC,
        /**
         * 基于属性的访问控制
         */
        ABAC
    }
}
