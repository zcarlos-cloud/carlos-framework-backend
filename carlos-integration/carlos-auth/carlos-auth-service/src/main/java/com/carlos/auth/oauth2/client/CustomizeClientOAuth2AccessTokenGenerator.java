package com.carlos.auth.oauth2.client;

import cn.hutool.core.util.IdUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClaimAccessor;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.*;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * <p>
 * 自定义token生成 这个类就是抄的源码
 *
 * @see OAuth2AccessTokenGenerator
 *
 * </p>
 *
 * @author carlos
 * @date 2022/11/4 15:59
 */
@Slf4j
public class CustomizeClientOAuth2AccessTokenGenerator implements OAuth2TokenGenerator<OAuth2AccessToken> {

    private OAuth2TokenCustomizer<OAuth2TokenClaimsContext> accessTokenCustomizer;

    @Nullable
    @Override
    public OAuth2AccessToken generate(OAuth2TokenContext context) {
        // 仅处理客户端信息
        AuthorizationGrantType grantType = context.getAuthorizationGrantType();
        if (!AuthorizationGrantType.CLIENT_CREDENTIALS.equals(grantType)) {
            return null;
        }

        if (!OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
            return null;
        }

        String issuer = null;
        if (context.getAuthorizationServerContext() != null) {
            AuthorizationServerSettings authorizationServerSettings = context.getAuthorizationServerContext().getAuthorizationServerSettings();
            if (authorizationServerSettings != null) {
                issuer = authorizationServerSettings.getIssuer();
            }
        }

        RegisteredClient registeredClient = context.getRegisteredClient();

        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(registeredClient.getTokenSettings().getAccessTokenTimeToLive());

        // @formatter:off
        OAuth2TokenClaimsSet.Builder claimsBuilder = OAuth2TokenClaimsSet.builder();
        if (StringUtils.hasText(issuer)) {
            claimsBuilder.issuer(issuer);
        }
        claimsBuilder
                .subject(context.getPrincipal().getName())
                .audience(List.of(registeredClient.getClientId()))
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .notBefore(issuedAt)
                .id(UUID.randomUUID().toString());
        if (!CollectionUtils.isEmpty(context.getAuthorizedScopes())) {
            claimsBuilder.claim(OAuth2ParameterNames.SCOPE, context.getAuthorizedScopes());
        }
        // @formatter:on

        if (this.accessTokenCustomizer != null) {
            // @formatter:off
            OAuth2TokenClaimsContext.Builder accessTokenContextBuilder = OAuth2TokenClaimsContext.with(claimsBuilder)
                    .registeredClient(context.getRegisteredClient())
                    .principal(context.getPrincipal())
                    .authorizedScopes(context.getAuthorizedScopes())
                    .tokenType(context.getTokenType())
                    .authorizationGrantType(context.getAuthorizationGrantType());
            if (context.getAuthorization() != null) {
                accessTokenContextBuilder.authorization(context.getAuthorization());
            }
            if (context.getAuthorizationGrant() != null) {
                accessTokenContextBuilder.authorizationGrant(context.getAuthorizationGrant());
            }
            // @formatter:on

            OAuth2TokenClaimsContext accessTokenContext = accessTokenContextBuilder.build();
            this.accessTokenCustomizer.customize(accessTokenContext);
        }

        OAuth2TokenClaimsSet accessTokenClaimsSet = claimsBuilder.build();

        // 组装key token:client:username:uuid
        // String key = String.format("%s::%s::%s", SecurityContextHolder.getContext().getAuthentication().getPrincipal(),
        //         context.getPrincipal().getName(), UUID.randomUUID());
        String key = IdUtil.randomUUID();

        return new OAuth2AccessTokenClaims(OAuth2AccessToken.TokenType.BEARER, key, accessTokenClaimsSet.getIssuedAt(),
            accessTokenClaimsSet.getExpiresAt(), context.getAuthorizedScopes(), accessTokenClaimsSet.getClaims());
    }

    /**
     * Sets the {@link OAuth2TokenCustomizer} that customizes the {@link OAuth2TokenClaimsContext#getClaims() claims} for the
     * {@link OAuth2AccessToken}.
     *
     * @param accessTokenCustomizer the {@link OAuth2TokenCustomizer} that customizes the claims for the {@code OAuth2AccessToken}
     */
    public void setAccessTokenCustomizer(OAuth2TokenCustomizer<OAuth2TokenClaimsContext> accessTokenCustomizer) {
        Assert.notNull(accessTokenCustomizer, "accessTokenCustomizer cannot be null");
        this.accessTokenCustomizer = accessTokenCustomizer;
    }

    private static final class OAuth2AccessTokenClaims extends OAuth2AccessToken implements ClaimAccessor {

        private final Map<String, Object> claims;

        private OAuth2AccessTokenClaims(TokenType tokenType, String tokenValue, Instant issuedAt, Instant expiresAt,
                                        Set<String> scopes, Map<String, Object> claims) {
            super(tokenType, tokenValue, issuedAt, expiresAt, scopes);
            this.claims = claims;
        }

        @Override
        public Map<String, Object> getClaims() {
            return this.claims;
        }

    }

}
