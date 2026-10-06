package com.carlos.auth.oauth2.server;

import lombok.Data;

/**
 * OAuth2 授权服务器配置属性
 *
 * <p>配置 OAuth2 授权服务器的端点路径、OIDC 开关等。</p>
 *
 * <p>注意：Token 有效期不在此处配置。access/refresh token 的 TTL 以注册客户端
 * （DB app_client.token_settings）为准，未配置时由 {@code carlos.auth.login.access-token-ttl}
 * / {@code carlos.auth.login.refresh-token-ttl} 兜底。</p>
 *
 * @author carlos
 * @version 3.0.0
 * @since 2026-04-08
 */
@Data
public class AuthorizationServerProperties {

    /**
     * 是否启用授权服务器
     */
    private boolean enabled = false;

    /**
     * 授权端点路径
     */
    private String authorizationEndpoint = "/oauth2/authorize";

    /**
     * Token 端点路径
     */
    private String tokenEndpoint = "/oauth2/token";

    /**
     * Token 撤销端点路径
     */
    private String tokenRevocationEndpoint = "/oauth2/revoke";

    /**
     * Token 自省端点路径
     */
    private String tokenIntrospectionEndpoint = "/oauth2/introspect";

    /**
     * JWK Set 端点路径
     */
    private String jwkSetEndpoint = "/oauth2/jwks";

    /**
     * OIDC 用户信息端点路径
     */
    private String oidcUserInfoEndpoint = "/userinfo";

    /**
     * 是否启用 OIDC 支持
     */
    private boolean oidcEnabled = true;

}
