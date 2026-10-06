package com.carlos.auth.sso.service;

import cn.hutool.core.util.StrUtil;
import com.carlos.auth.api.enums.AuthErrorCode;
import com.carlos.auth.config.OAuth2Properties;
import com.carlos.auth.sso.pojo.vo.SsoTokenExchangeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SSO Token 交换服务
 *
 * <p>实现基于有效访问令牌的免密跳转 Token 交换，支持跨客户端的单点登录场景。</p>
 *
 * <p><strong>使用场景：</strong></p>
 * <ul>
 *   <li>用户已在系统 A 登录，免密跳转到系统 B</li>
 *   <li>网关层统一认证后，向子系统分发 Token</li>
 *   <li>移动端与 Web 端 Token 互通</li>
 * </ul>
 *
 * <p><strong>安全约束：</strong></p>
 * <ul>
 *   <li>源 Token 必须有效且未过期</li>
 *   <li>目标客户端必须存在且启用</li>
 *   <li>交换后的 scope 为源 scope 与目标客户端支持 scope 的交集</li>
 *   <li>源 Token 不会被撤销（交换是放大而非替换）</li>
 * </ul>
 *
 * @author Carlos
 * @version 3.0.0
 * @since 2026-05-19
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SsoTokenExchangeService {

    private final OAuth2AuthorizationService authorizationService;
    private final RegisteredClientRepository registeredClientRepository;
    private final OAuth2TokenGenerator<?> tokenGenerator;
    private final OAuth2Properties oAuth2Properties;

    /**
     * 交换 Token
     *
     * <p>基于源访问令牌为目标客户端生成新的访问令牌。</p>
     *
     * @param sourceToken       源访问令牌
     * @param targetClientId    目标客户端ID
     * @param requestedScopes   请求的授权范围（可选）
     * @return 新 Token 信息
     */
    public SsoTokenExchangeVO exchangeToken(String sourceToken, String targetClientId, Set<String> requestedScopes) {
        // 1. 检查 Token 交换功能是否启用
        if (!oAuth2Properties.getSso().isTokenExchangeEnabled()) {
            log.warn("SSO Token exchange is disabled");
            throw AuthErrorCode.AUTH_SSO_TOKEN_EXCHANGE_DISABLED.exception();
        }

        // 2. 验证源 Token
        OAuth2Authorization sourceAuth = validateSourceToken(sourceToken);
        String principalName = sourceAuth.getPrincipalName();
        log.info("SSO Token exchange requested: principalName={}, targetClientId={}", principalName, targetClientId);

        // 3. 解析目标客户端
        RegisteredClient targetClient = resolveTargetClient(targetClientId);

        // 4. 确定授权范围（交集约束）
        Set<String> authorizedScopes = resolveAuthorizedScopes(sourceAuth, targetClient, requestedScopes);

        // 5. 从源 Token Claims 中提取权限信息
        List<GrantedAuthority> authorities = extractAuthorities(sourceAuth);

        // 6. 构建认证对象（已认证状态）
        Authentication authentication = buildAuthentication(principalName, authorities);

        // 7. 为目标客户端生成 Token
        DefaultOAuth2TokenContext.Builder tokenContextBuilder = DefaultOAuth2TokenContext.builder()
                .registeredClient(targetClient)
                .principal(authentication)
                .authorizedScopes(authorizedScopes)
                .authorizationGrant(authentication);

        OAuth2AccessToken accessToken = generateAccessToken(tokenContextBuilder);
        OAuth2RefreshToken refreshToken = generateRefreshToken(tokenContextBuilder, targetClient);

        // 8. 保存新的授权信息
        saveAuthorization(accessToken, refreshToken, targetClient, authentication);

        // 9. 组装响应
        long expiresIn = 0;
        if (accessToken.getIssuedAt() != null && accessToken.getExpiresAt() != null) {
            expiresIn = Duration.between(accessToken.getIssuedAt(), accessToken.getExpiresAt()).getSeconds();
        }

        log.info("SSO Token exchange successful: principalName={}, targetClientId={}, scopes={}",
                principalName, targetClientId, authorizedScopes);

        return SsoTokenExchangeVO.builder()
                .accessToken(accessToken.getTokenValue())
                .refreshToken(refreshToken != null ? refreshToken.getTokenValue() : null)
                .tokenType(accessToken.getTokenType().getValue())
                .expiresIn(expiresIn)
                .scope(String.join(" ", authorizedScopes))
                .targetClientId(targetClientId)
                .username(principalName)
                .build();
    }

    /**
     * 验证源 Token
     *
     * @param sourceToken 源访问令牌
     * @return 源授权信息
     */
    private OAuth2Authorization validateSourceToken(String sourceToken) {
        if (StrUtil.isBlank(sourceToken)) {
            throw AuthErrorCode.AUTH_PARAM_INVALID.exception("源访问令牌不能为空");
        }

        OAuth2Authorization authorization = authorizationService.findByToken(
                sourceToken, OAuth2TokenType.ACCESS_TOKEN);
        if (authorization == null) {
            log.warn("Source token not found or expired");
            throw AuthErrorCode.AUTH_TOKEN_INVALID.exception();
        }

        OAuth2Authorization.Token<OAuth2AccessToken> accessToken = authorization.getAccessToken();
        if (accessToken == null || accessToken.getToken() == null) {
            throw AuthErrorCode.AUTH_TOKEN_INVALID.exception();
        }

        // 检查是否过期
        Instant expiresAt = accessToken.getToken().getExpiresAt();
        if (expiresAt != null && expiresAt.isBefore(Instant.now())) {
            log.warn("Source token expired");
            throw AuthErrorCode.AUTH_TOKEN_EXPIRED.exception();
        }

        log.debug("Source token validated: principalName={}, clientId={}",
                authorization.getPrincipalName(), authorization.getRegisteredClientId());
        return authorization;
    }

    /**
     * 解析目标客户端
     *
     * @param targetClientId 目标客户端ID
     * @return 注册的客户端
     */
    private RegisteredClient resolveTargetClient(String targetClientId) {
        if (StrUtil.isBlank(targetClientId)) {
            throw AuthErrorCode.AUTH_PARAM_CLIENT_ID_MISSING.exception();
        }

        RegisteredClient client = registeredClientRepository.findByClientId(targetClientId);
        if (client == null) {
            log.error("Target client not found: {}", targetClientId);
            throw AuthErrorCode.AUTH_SSO_TARGET_CLIENT_NOT_FOUND.exception("目标客户端不存在: %s", targetClientId);
        }

        // 检查客户端是否被禁用（通过 clientSettings）
        if (client.getClientSettings() != null
                && Boolean.FALSE.equals(client.getClientSettings().getSetting("enabled"))) {
            throw AuthErrorCode.AUTH_CLIENT_DISABLED.exception();
        }

        return client;
    }

    /**
     * 确定授权范围
     *
     * <p>取源 Token 范围、目标客户端支持范围、请求范围的交集。</p>
     *
     * @param sourceAuth      源授权
     * @param targetClient    目标客户端
     * @param requestedScopes 请求的范围
     * @return 最终授权范围
     */
    private Set<String> resolveAuthorizedScopes(OAuth2Authorization sourceAuth,
                                                RegisteredClient targetClient,
                                                Set<String> requestedScopes) {
        // 源 Token 的范围
        Set<String> sourceScopes = new HashSet<>();
        if (sourceAuth.getAccessToken() != null && sourceAuth.getAccessToken().getToken() != null) {
            Set<String> scopes = sourceAuth.getAccessToken().getToken().getScopes();
            if (scopes != null) {
                sourceScopes.addAll(scopes);
            }
        }

        // 目标客户端支持的范围
        Set<String> clientScopes = targetClient.getScopes();

        // 取交集：源 ∩ 目标客户端
        Set<String> intersectedScopes = new HashSet<>(sourceScopes);
        intersectedScopes.retainAll(clientScopes);

        // 如果请求了特定范围，再取交集
        if (!CollectionUtils.isEmpty(requestedScopes)) {
            intersectedScopes.retainAll(requestedScopes);
        }

        // 如果配置了 exchangeAllowedScopes，再取交集（限制可交换的最大范围）
        List<String> exchangeAllowedScopes = oAuth2Properties.getSso().getExchangeAllowedScopes();
        if (exchangeAllowedScopes != null && !exchangeAllowedScopes.isEmpty()) {
            intersectedScopes.retainAll(exchangeAllowedScopes);
        }

        // 如果交集为空，使用目标客户端的默认范围（openid 或第一个）
        if (intersectedScopes.isEmpty()) {
            if (clientScopes.contains("openid")) {
                intersectedScopes.add("openid");
            } else if (!clientScopes.isEmpty()) {
                intersectedScopes.add(clientScopes.iterator().next());
            }
        }

        return intersectedScopes;
    }

    /**
     * 从源 Token Claims 中提取权限
     *
     * @param sourceAuth 源授权
     * @return 权限列表
     */
    @SuppressWarnings("unchecked")
    private List<GrantedAuthority> extractAuthorities(OAuth2Authorization sourceAuth) {
        if (sourceAuth.getAccessToken() == null || sourceAuth.getAccessToken().getClaims() == null) {
            return Collections.emptyList();
        }
        Map<String, Object> claims = sourceAuth.getAccessToken().getClaims();
        if (!claims.containsKey("authorities")) {
            return Collections.emptyList();
        }

        Object authoritiesObj = claims.get("authorities");
        if (authoritiesObj instanceof String str) {
            return Arrays.stream(str.split(","))
                    .filter(StrUtil::isNotBlank)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        } else if (authoritiesObj instanceof Collection<?> collection) {
            return collection.stream()
                    .map(Object::toString)
                    .filter(StrUtil::isNotBlank)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /**
     * 构建认证对象
     *
     * @param principalName 主体名称
     * @param authorities   权限列表
     * @return 认证对象
     */
    private Authentication buildAuthentication(String principalName, List<GrantedAuthority> authorities) {
        return new UsernamePasswordAuthenticationToken(
                principalName, null, authorities);
    }

    /**
     * 生成访问令牌
     */
    private OAuth2AccessToken generateAccessToken(DefaultOAuth2TokenContext.Builder tokenContextBuilder) {
        OAuth2TokenContext tokenContext = tokenContextBuilder
                .tokenType(OAuth2TokenType.ACCESS_TOKEN)
                .build();
        OAuth2Token generatedAccessToken = tokenGenerator.generate(tokenContext);

        if (generatedAccessToken == null) {
            log.error("Failed to generate access token during SSO exchange");
            throw AuthErrorCode.AUTH_TOKEN_GENERATE_FAILED.exception();
        }

        // 直接返回生成器产物，保留 claims（供 /oauth2/introspect 内省返回用户声明）
        if (generatedAccessToken instanceof OAuth2AccessToken oauth2AccessToken) {
            return oauth2AccessToken;
        }

        return new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                generatedAccessToken.getTokenValue(),
                generatedAccessToken.getIssuedAt(),
                generatedAccessToken.getExpiresAt(),
                tokenContext.getAuthorizedScopes()
        );
    }

    /**
     * 生成刷新令牌
     */
    private OAuth2RefreshToken generateRefreshToken(
            DefaultOAuth2TokenContext.Builder tokenContextBuilder, RegisteredClient registeredClient) {

        if (!registeredClient.getAuthorizationGrantTypes().contains(AuthorizationGrantType.REFRESH_TOKEN)) {
            return null;
        }

        OAuth2TokenContext tokenContext = tokenContextBuilder
                .tokenType(OAuth2TokenType.REFRESH_TOKEN)
                .build();
        OAuth2Token generatedRefreshToken = tokenGenerator.generate(tokenContext);

        if (generatedRefreshToken instanceof OAuth2RefreshToken refreshToken) {
            return refreshToken;
        }
        return null;
    }

    /**
     * 保存授权信息
     */
    private void saveAuthorization(OAuth2AccessToken accessToken, OAuth2RefreshToken refreshToken,
                                   RegisteredClient registeredClient, Authentication authentication) {
        OAuth2Authorization.Builder authorizationBuilder = OAuth2Authorization
                .withRegisteredClient(registeredClient)
                .principalName(authentication.getName())
                .id(UUID.randomUUID().toString());

        if (accessToken instanceof ClaimAccessor claimAccessor) {
            // 归一化为标准 OAuth2AccessToken（token 索引按 OAuth2AccessToken.class 查找），
            // claims 写入元数据，/oauth2/introspect 才能返回用户声明
            Map<String, Object> claims = claimAccessor.getClaims();
            OAuth2AccessToken normalizedToken = new OAuth2AccessToken(
                    accessToken.getTokenType(), accessToken.getTokenValue(),
                    accessToken.getIssuedAt(), accessToken.getExpiresAt(), accessToken.getScopes());
            authorizationBuilder.token(normalizedToken, metadata ->
                    metadata.put(OAuth2Authorization.Token.CLAIMS_METADATA_NAME, claims));
        } else {
            authorizationBuilder.accessToken(accessToken);
        }

        if (refreshToken != null) {
            authorizationBuilder.refreshToken(refreshToken);
        }

        authorizationService.save(authorizationBuilder.build());
    }
}
