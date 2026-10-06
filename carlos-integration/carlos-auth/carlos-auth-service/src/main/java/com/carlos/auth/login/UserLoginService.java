package com.carlos.auth.login;

import com.alibaba.fastjson2.JSON;
import com.carlos.audit.api.pojo.enums.AuditLogAuthProviderEnum;
import com.carlos.audit.api.pojo.enums.AuditLogAuthTypeEnum;
import com.carlos.audit.api.pojo.enums.AuditLogCategoryEnum;
import com.carlos.audit.api.pojo.enums.AuditLogPrincipalTypeEnum;
import com.carlos.audit.api.pojo.param.ApiAuditLogMainParam;
import com.carlos.auth.api.enums.AuthErrorCode;
import com.carlos.auth.config.OAuth2Properties;
import com.carlos.auth.constant.AuthRedisKey;
import com.carlos.auth.idp.UserIdentity;
import com.carlos.auth.log.LoginAuditLogSender;
import com.carlos.auth.login.dto.LoginRequest;
import com.carlos.auth.login.dto.LoginResponse;
import com.carlos.auth.oauth2.grant.CustomGrantTypes;
import com.carlos.auth.oauth2.repository.RedisOAuth2AuthorizationService;
import com.carlos.auth.provider.UserInfo;
import com.carlos.auth.provider.UserProvider;
import com.carlos.auth.security.TenantLoginChecker;
import com.carlos.auth.security.UsernamePasswordAuthenticationProvider;
import com.carlos.auth.security.manager.IpBlockManager;
import com.carlos.auth.security.manager.LoginAttemptManager;
import com.carlos.auth.util.AuditLogUtil;
import com.carlos.auth.util.IpLocationUtil;
import com.carlos.encrypt.EncryptUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContext;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户登录服务
 *
 * <p>处理用户登录逻辑，包括用户名密码验证、OAuth2 令牌颁发、登录审计等。</p>
 *
 * <p><strong>与标准 OAuth2 流程的关系：</strong></p>
 * <p>此类提供的自定义登录接口（/login）是供内部应用直接获取令牌使用的便捷接口。
 * 它内部使用了与标准 OAuth2 /oauth2/token 端点相同的 Token 生成机制，包括：</p>
 * <ul>
 *   <li>{@link OAuth2TokenGenerator} - 标准 Token 生成器</li>
 *   <li>{@link OAuth2AuthorizationService} - 授权信息存储（支持 Token 撤销）</li>
 *   <li>{@link RegisteredClientRepository} - 客户端配置</li>
 * </ul>
 *
 * <p>对于第三方应用集成，建议直接使用标准 OAuth2 端点：</p>
 * <pre>
 * POST /oauth2/token
 * Content-Type: application/x-www-form-urlencoded
 *
 * grant_type=password&username=user&password=pass&client_id=client&scope=read
 * </pre>
 *
 * @author Carlos
 * @date 2026-02-26
 * @see org.springframework.security.oauth2.server.authorization.web.OAuth2TokenEndpoint
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserLoginService {

    /**
     * 用户服务
     */
    private final UserProvider userProvider;

    /**
     * 用户名密码认证提供者（直接调用，绕过 DaoAuthenticationProvider，确保 details 中包含 UserIdentity）
     */
    private final UsernamePasswordAuthenticationProvider authenticationProvider;

    /**
     * 登录尝试管理器
     */
    private final LoginAttemptManager loginAttemptManager;

    /**
     * IP封禁管理器
     */
    private final IpBlockManager ipBlockManager;

    /**
     * HTTP请求（用于获取客户端IP）
     */
    private final HttpServletRequest request;

    /**
     * IP定位工具
     */
    private final IpLocationUtil ipLocationUtil;

    /**
     * 审计日志工具类
     */
    private final AuditLogUtil auditLogUtil;

    /**
     * OAuth2 Token 生成器
     */
    private final OAuth2TokenGenerator<?> tokenGenerator;

    /**
     * 授权信息服务（Redis 实现，支持用户会话索引）
     */
    private final RedisOAuth2AuthorizationService authorizationService;

    /**
     * 客户端仓库
     */
    private final RegisteredClientRepository registeredClientRepository;

    /**
     * 登录审计日志异步发送器
     */
    private final LoginAuditLogSender loginAuditLogSender;

    /**
     * OAuth2 配置属性
     */
    private final OAuth2Properties oAuth2Properties;

    /**
     * 授权服务器设置（提供 issuer 等配置）
     */
    private final AuthorizationServerSettings authorizationServerSettings;

    /**
     * MFA 服务（用于 step-up 流程签发 mfaToken）
     */
    private final com.carlos.auth.mfa.service.MfaService mfaService;

    /**
     * 登录租户校验器（S2，feature flag：carlos.auth.tenant.enabled，默认关闭）
     */
    private final TenantLoginChecker tenantLoginChecker;

    /**
     * 用户登录
     *
     * @param loginRequest 登录请求
     * @return 登录响应（包含访问令牌）
     */
    public LoginResponse login(LoginRequest loginRequest) {
        log.info("User login attempt: username={}", loginRequest.getUsername());
        long startNanos = System.nanoTime();

        try {
            // S2 租户校验（feature flag 默认关闭，关闭时返回 null 零行为变化）；
            // 解析结果经 token details 传递给 UsernamePasswordAuthenticationProvider，
            // 由 LocalIdentityProvider 在目标租户内定位用户
            Long tenantId = tenantLoginChecker.resolveTenantId(loginRequest.getTenantCode());

            // 构建认证令牌（密码解密由 UserAuthController 统一处理，此处不再重复解密）
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    );
            if (tenantId != null) {
                authenticationToken.setDetails(tenantId);
            }

            // 执行认证（直接调用 UsernamePasswordAuthenticationProvider，确保返回的 details 中包含 UserIdentity）
            Authentication authentication = authenticationProvider.authenticate(authenticationToken);

            // P1-001: 从认证结果 details 中提取 UserIdentity，避免重复 Feign 查询
            // UsernamePasswordAuthenticationProvider 已将 UserIdentity 放入 details
            UserIdentity identity = extractUserIdentity(authentication);
            if (identity == null) {
                log.error("UserIdentity not found in authentication details for: {}",
                        loginRequest.getUsername());
                throw AuthErrorCode.AUTH_USER_NOT_FOUND.exception();
            }

            // 使用 identity 构建 UserInfo（无需再次 Feign 查询 carlos-org）
            UserInfo user = buildUserInfoFromIdentity(identity);

            // 优先使用请求中的 clientId，否则使用默认客户端
            String clientId = StringUtils.hasText(loginRequest.getClientId())
                    ? loginRequest.getClientId()
                    : oAuth2Properties.getLogin().getDefaultClientId();

            // 检查是否需要 MFA（基于风险评估 + 用户已启用 + 角色强制）
            boolean shouldTriggerMfa = evaluateMfaRisk(user) || Boolean.TRUE.equals(user.getMfaEnabled());
            boolean mfaRequired = shouldTriggerMfa && Boolean.TRUE.equals(user.getMfaEnabled());
            boolean shouldRecommendMfa = shouldTriggerMfa && !Boolean.TRUE.equals(user.getMfaEnabled());

            // MFA step-up 流程：需要 MFA 时，签发 mfaToken 而非正式 Token
            if (mfaRequired) {
                log.info("MFA required for user: {}, issuing mfaToken", loginRequest.getUsername());

                // 签发 MFA 临时令牌（5min TTL）
                String mfaToken = mfaService.issueMfaToken(loginRequest.getUsername(), clientId);

                // 记录审计日志（密码验证通过，等待 MFA）
                recordLoginAudit(user, "LOGIN", "SUCCESS", "password verified, MFA pending", loginDurationMs(startNanos));

                // 重置密码失败计数（密码已验证通过）
                recordLoginSuccess(loginRequest.getUsername());

                return LoginResponse.builder()
                        .mfaRequired(true)
                        .mfaRecommended(false)
                        .mfaToken(mfaToken)
                        .build();
            }

            // 不需要 MFA，直接签发正式 Token
            LoginResponse response = buildLoginResponse(user, authentication, clientId);
            response.setMfaRequired(false);
            response.setMfaRecommended(shouldRecommendMfa);

            // 异步记录登录成功审计日志
            recordLoginAudit(user, "LOGIN", "SUCCESS", null, loginDurationMs(startNanos));

            // 记录登录成功
            recordLoginSuccess(loginRequest.getUsername());

            return response;

        } catch (AuthenticationException e) {
            log.warn("Login failed for user: {}", loginRequest.getUsername(), e);
            throw e;
        }
    }

    /**
     * 从认证结果中提取 UserIdentity
     *
     * <p>P1-001: UsernamePasswordAuthenticationProvider 将完整的 UserIdentity
     * 存入 token details，此处直接提取，避免重复 Feign 查询 carlos-org。</p>
     *
     * @param authentication 认证结果
     * @return UserIdentity，不存在返回 null
     */
    private UserIdentity extractUserIdentity(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        Object details = authentication.getDetails();
        if (details instanceof UserIdentity identity) {
            return identity;
        }
        return null;
    }

    /**
     * 从 UserIdentity 构建 UserInfo
     *
     * <p>P1-001: 复用认证阶段已加载的 user 数据，消除 UserLoginService 中
     * 第 2 次 Feign 调用。</p>
     *
     * @param identity 认证成功后的用户身份
     * @return UserInfo
     */
    private UserInfo buildUserInfoFromIdentity(UserIdentity identity) {
        return UserInfo.builder()
                .userId(safeParseLong(identity.getProviderUserId()))
                .username(identity.getUsername())
                .email(identity.getEmail())
                .phone(identity.getPhone())
                .status(identity.getStatus())
                .tenantId(identity.getTenantId())
                .roleCodes(identity.getRoleCodes() != null
                        ? new java.util.ArrayList<>(identity.getRoleCodes())
                        : null)
                .mfaEnabled(false) // MFA 状态需由 MfaService 单独查询（低频）
                .build();
    }

    /**
     * 安全解析 Long
     */
    private Long safeParseLong(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            log.debug("Cannot parse as Long: {}", value);
            return null;
        }
    }

    /**
     * 构建登录响应
     *
     * <p>使用标准 OAuth2 Token 生成机制生成访问令牌和刷新令牌。</p>
     *
     * @param user 用户信息
     * @param authentication 认证信息
     * @param clientId 客户端ID
     * @return 登录响应
     */
    private LoginResponse buildLoginResponse(UserInfo user, Authentication authentication, String clientId) {
        return buildLoginResponse(user, authentication, clientId, CustomGrantTypes.PASSWORD);
    }

    /**
     * 构建登录响应
     *
     * <p>使用标准 OAuth2 Token 生成机制生成访问令牌和刷新令牌。</p>
     *
     * @param user 用户信息
     * @param authentication 认证信息
     * @param clientId 客户端ID
     * @param grantType 授权类型
     * @return 登录响应
     */
    private LoginResponse buildLoginResponse(UserInfo user, Authentication authentication, String clientId,
                                             AuthorizationGrantType grantType) {
        RegisteredClient registeredClient = resolveRegisteredClient(clientId);
        DefaultOAuth2TokenContext.Builder tokenContextBuilder = createTokenContextBuilder(registeredClient, authentication, grantType);

        OAuth2AccessToken accessToken = generateAccessToken(tokenContextBuilder);
        OAuth2RefreshToken refreshToken = generateRefreshToken(tokenContextBuilder, registeredClient);

        saveAuthorization(accessToken, refreshToken, registeredClient, authentication, grantType);

        return assembleLoginResponse(accessToken, refreshToken, user);
    }

    /**
     * 获取默认客户端ID
     */
    public String getDefaultClientId() {
        return oAuth2Properties.getLogin().getDefaultClientId();
    }

    /**
     * 为指定用户签发 Token
     *
     * <p>适用于已通过其他方式完成身份验证的场景（如 Passkey、MFA 二次验证后）。</p>
     *
     * @param username 用户名
     * @param clientId 客户端ID
     * @return 登录响应
     */
    public LoginResponse issueTokenForUser(String username, String clientId) {
        UserInfo user = userProvider.loadUserByIdentifier(username);
        if (user == null) {
            log.error("User not found for token issuance: {}", username);
            throw AuthErrorCode.AUTH_USER_NOT_FOUND.exception();
        }

        if (!user.isAccountEnabled()) {
            log.warn("Account disabled or locked: {}", username);
            throw AuthErrorCode.AUTH_USER_ACCOUNT_DISABLED.exception();
        }

        // 加载用户角色构造完整的 authentication
        List<String> roleCodes = userProvider.getUserRoleCodes(user.getUserId());
        if (roleCodes == null) {
            roleCodes = Collections.emptyList();
        }
        Collection<GrantedAuthority> authorities = roleCodes.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(username, null, authorities);

        LoginResponse response = buildLoginResponse(user, authentication, clientId, CustomGrantTypes.PASSKEY);
        log.info("Token issued for user: {}, grantType=passkey", username);
        return response;
    }

    /**
     * 解析注册客户端
     */
    private RegisteredClient resolveRegisteredClient(String clientId) {
        String resolvedClientId = StringUtils.hasText(clientId)
                ? clientId
                : oAuth2Properties.getLogin().getDefaultClientId();

        RegisteredClient registeredClient = registeredClientRepository.findByClientId(resolvedClientId);
        if (registeredClient == null) {
            log.error("Client not found: {}", resolvedClientId);
            throw AuthErrorCode.AUTH_CLIENT_NOT_FOUND.exception("客户端配置不存在: %s", resolvedClientId);
        }
        return registeredClient;
    }

    /**
     * 创建 Token 上下文构建器
     */
    private DefaultOAuth2TokenContext.Builder createTokenContextBuilder(
            RegisteredClient registeredClient, Authentication authentication, AuthorizationGrantType grantType) {

        Set<String> authorizedScopes = registeredClient.getScopes();
        if (CollectionUtils.isEmpty(authorizedScopes)) {
            authorizedScopes = new HashSet<>();
            authorizedScopes.add(OidcScopes.OPENID);
        }

        // @formatter:off
        return DefaultOAuth2TokenContext.builder()
            .registeredClient(registeredClient)
            .principal(authentication)
            .authorizationServerContext(new AuthorizationServerContext() {
                @Override
                public String getIssuer() {
                    return authorizationServerSettings.getIssuer();
                }

                @Override
                public AuthorizationServerSettings getAuthorizationServerSettings() {
                    return authorizationServerSettings;
                }
            })
            .authorizationGrantType(grantType)
            .authorizedScopes(authorizedScopes)
            .authorizationGrant(authentication);
        // @formatter:on
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
            throw AuthErrorCode.AUTH_TOKEN_GENERATE_FAILED.exception();
        }

        // 直接返回生成器产物：Opaque Token 生成器返回携带 claims 的 OAuth2AccessToken
        // （sub/username/user_id/role_ids 等），重新包装会丢失 claims，导致内省端点无用户声明
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
                                   RegisteredClient registeredClient, Authentication authentication,
                                   AuthorizationGrantType grantType) {
        OAuth2Authorization.Builder authorizationBuilder = OAuth2Authorization
                .withRegisteredClient(registeredClient)
                .principalName(authentication.getName())
                .authorizationGrantType(grantType)
                .id(accessToken.getTokenValue());

        if (accessToken instanceof ClaimAccessor claimAccessor) {
            // 归一化为标准 OAuth2AccessToken：授权对象按 OAuth2AccessToken.class 建立 token 索引，
            // 直接用生成器的子类会导致 getAccessToken() 取不到、访问令牌索引不被写入。
            // claims 写入元数据（随序列化持久化），/oauth2/introspect 才能返回用户声明
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

    /**
     * 组装登录响应
     */
    private LoginResponse assembleLoginResponse(OAuth2AccessToken accessToken,
                                                OAuth2RefreshToken refreshToken, UserInfo user) {
        long expiresIn = 0;
        if (accessToken.getIssuedAt() != null && accessToken.getExpiresAt() != null) {
            expiresIn = Duration.between(accessToken.getIssuedAt(), accessToken.getExpiresAt()).getSeconds();
        }

        LoginResponse.UserInfo userInfo = LoginResponse.UserInfo.builder()
                .id(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .build();

        String tokenValue = accessToken.getTokenValue();
        return LoginResponse.builder()
                .accessToken(tokenValue)
                .refreshToken(refreshToken != null ? refreshToken.getTokenValue() : null)
                .tokenType(accessToken.getTokenType().getValue())
                .expiresIn(expiresIn)
                .userInfo(userInfo)
                .build();
    }

    /**
     * 刷新访问令牌
     *
     * <p>使用有效的刷新令牌生成新的访问令牌和刷新令牌。</p>
     *
     * @param refreshTokenValue 刷新令牌
     * @return 新的登录响应
     */
    public LoginResponse refreshAccessToken(String refreshTokenValue) {
        if (!StringUtils.hasText(refreshTokenValue)) {
            throw AuthErrorCode.AUTH_TOKEN_INVALID.exception("刷新令牌不能为空");
        }

        // 1. 查找刷新令牌对应的授权信息
        OAuth2Authorization authorization = authorizationService.findByToken(
                refreshTokenValue, OAuth2TokenType.REFRESH_TOKEN);

        if (authorization == null) {
            log.warn("Refresh token not found or expired");
            throw AuthErrorCode.AUTH_TOKEN_INVALID.exception("刷新令牌无效或已过期");
        }

        // 2. 获取注册客户端（registeredClientId 存的是客户端主键 ID，非 clientId/appKey）
        RegisteredClient registeredClient = registeredClientRepository.findById(
                authorization.getRegisteredClientId());
        if (registeredClient == null) {
            log.error("Client not found for refresh token: {}", authorization.getRegisteredClientId());
            throw AuthErrorCode.AUTH_CLIENT_NOT_FOUND.exception();
        }

        // 3. 获取用户信息
        String principalName = authorization.getPrincipalName();
        UserInfo user = userProvider.loadUserByIdentifier(principalName);
        if (user == null) {
            log.error("User not found for refresh token: {}", principalName);
            throw AuthErrorCode.AUTH_USER_NOT_FOUND.exception();
        }

        // 4. 重建认证信息（使用原授权的作用域作为权限）
        Set<String> scopes = registeredClient.getScopes();
        if (authorization.getAccessToken() != null
                && authorization.getAccessToken().getClaims() != null) {
            Object scopeClaim = authorization.getAccessToken().getClaims().get("scope");
            if (scopeClaim instanceof Collection<?> scopeCollection) {
                scopes = scopeCollection.stream()
                        .map(String::valueOf)
                        .collect(Collectors.toSet());
            }
        }

        Collection<GrantedAuthority> authorities = scopes.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principalName, null, authorities);

        // 5. 生成新的令牌
        log.info("Refreshing token for user: {}", principalName);
        LoginResponse response = buildLoginResponse(user, authentication, registeredClient.getClientId());

        // 6. 撤销旧的授权（刷新令牌轮换：旧 refreshToken 立即失效，防止重放）
        authorizationService.remove(authorization);

        return response;
    }

    /**
     * 检查账号是否被锁定
     *
     * @param username 用户名
     * @return true-被锁定，false-未锁定
     */
    public boolean isAccountLocked(String username) {
        // 检查账号锁定
        boolean accountLocked = loginAttemptManager.isAccountLocked(username);

        // 检查IP封禁
        boolean ipBlocked = ipBlockManager.isIpBlocked(request);

        return accountLocked || ipBlocked;
    }

    /**
     * 记录登录失败
     *
     * @param username 用户名
     * @param request HTTP请求
     */
    public void recordLoginFailure(String username, HttpServletRequest request) {
        log.warn("Recording login failure for user: {}", username);

        // 记录账号登录失败
        if (username != null && !username.isBlank()) {
            loginAttemptManager.recordLoginFailure(username);
        }

        // 记录IP登录失败
        ipBlockManager.recordLoginFailure(request);
    }

    /**
     * 记录登录失败（仅IP）
     *
     * @param request HTTP请求
     */
    public void recordLoginFailure(HttpServletRequest request) {
        ipBlockManager.recordLoginFailure(request);
    }

    /**
     * 记录登录成功
     *
     * @param username 用户名
     */
    public void recordLoginSuccess(String username) {
        log.info("Recording login success for user: {}", username);

        // 重置账号失败计数
        if (username != null && !username.isBlank()) {
            loginAttemptManager.recordLoginSuccess(username);
        }

        // 重置IP失败计数
        ipBlockManager.recordLoginSuccess(request);
    }

    /**
     * 用户登出
     *
     * <p>记录登出审计日志，将令牌加入黑名单（撤销授权）</p>
     *
     * @param accessToken 访问令牌
     * @param user 当前用户
     */
    public void logout(String accessToken, UserInfo user) {
        String username = user != null ? user.getUsername() : null;
        log.info("User logout: {}", username != null ? username : "unknown");

        try {
            // 撤销授权（使 Token 失效）
            if (accessToken != null && !accessToken.isBlank()) {
                OAuth2Authorization authorization = authorizationService.findByToken(
                        accessToken, OAuth2TokenType.ACCESS_TOKEN);
                if (authorization != null) {
                    authorizationService.remove(authorization);
                    // controller 未传 user 时，用令牌主体兜底审计主体
                    if (username == null) {
                        username = authorization.getPrincipalName();
                    }
                    log.debug("Revoked OAuth2 authorization for user: {}", username);
                }
            }

            // 异步记录登出审计日志（主体无法确定时跳过，避免空指针导致整段登出失败）
            UserInfo auditUser = user;
            if (auditUser == null && username != null) {
                auditUser = UserInfo.builder().username(username).build();
            }
            if (auditUser != null) {
                recordLoginAudit(auditUser, "LOGOUT", "SUCCESS", null);
            }

            log.info("User logged out successfully: {}", username);

        } catch (Exception e) {
            log.error("Error during logout for user: {}", username, e);
        }
    }

    /**
     * SSO 全局登出
     *
     * <p>撤销当前用户在所有客户端的所有有效访问令牌。</p>
     *
     * @param accessToken 当前访问令牌（用于识别用户）
     * @return 撤销的会话数量
     */
    public int globalLogout(String accessToken) {
        log.info("SSO global logout initiated");

        if (accessToken == null || accessToken.isBlank()) {
            log.warn("Global logout called with empty token");
            return 0;
        }

        // 1. 查找当前授权，获取 principalName
        OAuth2Authorization currentAuth = authorizationService.findByToken(
                accessToken, OAuth2TokenType.ACCESS_TOKEN);
        if (currentAuth == null) {
            log.warn("Current token not found for global logout");
            return 0;
        }

        String principalName = currentAuth.getPrincipalName();
        if (principalName == null || principalName.isBlank()) {
            log.warn("Principal name is empty, cannot perform global logout");
            return 0;
        }

        // 2. 查找该用户的所有授权 ID
        Set<String> authIds = authorizationService.findAuthorizationIdsByPrincipalName(principalName);
        if (authIds == null || authIds.isEmpty()) {
            log.info("No active sessions found for user: {}", principalName);
            return 0;
        }

        log.info("Global logout: found {} active sessions for user: {}", authIds.size(), principalName);

        // 3. 逐个撤销授权
        int revokedCount = 0;
        for (String authId : authIds) {
            try {
                OAuth2Authorization authorization = authorizationService.findById(authId);
                if (authorization != null) {
                    authorizationService.remove(authorization);
                    revokedCount++;
                }
            } catch (Exception e) {
                log.warn("Failed to revoke authorization during global logout: authId={}", authId, e);
            }
        }

        // 4. 异步记录全局登出审计日志
        try {
            UserInfo userInfo = new UserInfo();
            userInfo.setUserId(extractUserIdFromClaims(currentAuth));
            userInfo.setUsername(principalName);
            recordLoginAudit(userInfo, "LOGOUT", "SUCCESS",
                    "全局登出，撤销 " + revokedCount + " 个会话");
        } catch (Exception e) {
            log.warn("Failed to record global logout audit log", e);
        }

        // 清理用户会话索引（避免残留过期 authId）
        try {
            String sessionKey = AuthRedisKey.oauth2Key("user_sessions", principalName);
            com.carlos.redis.util.RedisUtil.delete(sessionKey);
            log.debug("Cleaned up user session index for: {}", principalName);
        } catch (Exception e) {
            log.warn("Failed to clean up user session index for: {}", principalName, e);
        }

        log.info("SSO global logout completed: user={}, revoked={}/{}",
                principalName, revokedCount, authIds.size());
        return revokedCount;
    }

    /**
     * 从授权 Claims 中提取用户ID
     */
    private Long extractUserIdFromClaims(OAuth2Authorization authorization) {
        if (authorization.getAccessToken() == null || authorization.getAccessToken().getClaims() == null) {
            return null;
        }
        Object userId = authorization.getAccessToken().getClaims().get("user_id");
        if (userId instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.valueOf(userId.toString());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 记录登录审计日志
     *
     * <p>参数在请求线程内同步构建（依赖 HttpServletRequest 上下文），
     * 实际发送由 {@link LoginAuditLogSender} 异步执行。</p>
     *
     * @param user 用户信息
     * @param eventType 事件类型：LOGIN、LOGOUT
     * @param status 状态：SUCCESS、FAILURE
     * @param errorMessage 错误消息
     */
    private void recordLoginAudit(UserInfo user, String eventType, String status, String errorMessage) {
        recordLoginAudit(user, eventType, status, errorMessage, null);
    }

    /**
     * 记录登录审计日志（带认证耗时）
     *
     * @param durationMs 认证耗时（毫秒），登出等无耗时场景传 null
     */
    private void recordLoginAudit(UserInfo user, String eventType, String status,
                                  String errorMessage, Integer durationMs) {
        ApiAuditLogMainParam param = buildAuditLogParam(user, eventType, status, errorMessage, durationMs);
        loginAuditLogSender.send(param);
    }

    /**
     * 计算登录耗时（毫秒），上限 1 分钟防御纳秒时钟异常
     */
    private Integer loginDurationMs(long startNanos) {
        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
        return (int) Math.min(elapsedMs, 60_000);
    }

    /**
     * 如果密码使用 SM2 加密，执行解密
     *
     * <p>优先使用动态密钥（keyId）解密；无 keyId 时回退到配置文件的静态 SM2 密钥。</p>
     *
     * @param loginRequest 登录请求
     */
    public void decryptPasswordIfNeeded(LoginRequest loginRequest) {
        if (!Boolean.TRUE.equals(loginRequest.getEncrypted()) || loginRequest.getPassword() == null) {
            return;
        }

        String username = loginRequest.getUsername();
        String cipher = loginRequest.getPassword();
        String keyId = loginRequest.getKeyId();

        // 优先使用动态密钥解密
        if (keyId != null && !keyId.isBlank()) {
            String decrypted = PublicKeyController.decryptPassword(keyId, cipher);
            if (decrypted != null && !decrypted.isEmpty()) {
                loginRequest.setPassword(decrypted);
                log.debug("Dynamic SM2 password decrypted successfully for user: {}", username);
                return;
            }
            log.warn("Dynamic SM2 password decryption failed for user: {}, keyId={}", username, keyId);
            return;
        }

        // 回退：使用配置文件中的静态 SM2 密钥（兼容旧模式）
        try {
            String decrypted = EncryptUtil.sm2Decrypt(cipher);
            if (decrypted != null && !decrypted.isEmpty()) {
                loginRequest.setPassword(decrypted);
                log.debug("Static SM2 password decrypted successfully for user: {}", username);
            }
        } catch (Exception e) {
            log.warn("Static SM2 password decryption failed for user: {}", username, e);
            // 解密失败时保留原密码，让后续认证逻辑处理错误
        }
    }

    /**
     * 构建审计日志参数
     *
     * @param durationMs 认证耗时（毫秒），非认证事件传 null
     */
    private ApiAuditLogMainParam buildAuditLogParam(UserInfo user, String eventType,
                                                    String status, String errorMessage,
                                                    Integer durationMs) {
        ApiAuditLogMainParam param = auditLogUtil.buildBaseParam();

        // 日志分类和类型
        param.setCategory(AuditLogCategoryEnum.SECURITY);
        param.setLogType("USER_" + eventType);
        param.setOperation("用户" + getEventDesc(eventType));

        // 主体信息（user 为 null 时仅设置主体类型，避免 NPE）
        if (user != null) {
            if (user.getUserId() != null) {
                param.setPrincipalId(String.valueOf(user.getUserId()));
            }
            param.setPrincipalName(user.getUsername());
            // 租户信息：缺失时下游默认写 0，按真实租户查询登录日志会查不到
            if (user.getTenantId() != null) {
                param.setTenantId(String.valueOf(user.getTenantId()));
            }
            if (user.getDeptId() != null) {
                param.setDeptId(String.valueOf(user.getDeptId()));
            }
            if (user.getRoleCodes() != null && !user.getRoleCodes().isEmpty()) {
                param.setRoles(JSON.toJSONString(user.getRoleCodes()));
            }
        }
        param.setPrincipalType(AuditLogPrincipalTypeEnum.USER);

        // 状态信息
        param.setLogStatus(auditLogUtil.resolveStatus(status));
        param.setResultMessage(errorMessage);
        // 结果码：成功统一 00000；失败无细分错误码时留空（下游按状态聚合）
        if ("SUCCESS".equals(status)) {
            param.setResultCode("00000");
        }
        if (durationMs != null) {
            param.setDurationMs(durationMs);
        }

        // 认证信息
        param.setAuthType(AuditLogAuthTypeEnum.PASSWORD);
        param.setAuthProvider(AuditLogAuthProviderEnum.LOCAL);

        // IP和位置信息
        auditLogUtil.populateIpAndLocation(param, request);

        return param;
    }

    /**
     * 获取事件描述
     */
    private String getEventDesc(String eventType) {
        return switch (eventType) {
            case "LOGIN" -> "登录";
            case "LOGOUT" -> "登出";
            case "REFRESH" -> "刷新令牌";
            case "LOCKED" -> "账号锁定";
            default -> eventType;
        };
    }

    /**
     * MFA 验证通过后签发 Token
     *
     * <p>MFA step-up 验证成功后调用，签发的 JWT Claims 中 aal=2、auth_method 包含 "+totp"。</p>
     *
     * @param user      用户信息
     * @param clientId  客户端 ID
     * @param request   HTTP 请求（用于审计日志）
     * @return 登录响应（含 accessToken、refreshToken）
     */
    public LoginResponse issueTokenAfterMfa(UserInfo user, String clientId, HttpServletRequest request) {
        log.info("Issuing token after MFA for user: {}", user.getUsername());

        // 构建认证信息
        Collection<GrantedAuthority> authorities = user.getRoleCodes() != null
                ? user.getRoleCodes().stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList())
                : java.util.Collections.emptyList();

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user.getUsername(), null, authorities);

        // 构建 Token 响应
        LoginResponse response = buildLoginResponse(user, authentication, clientId);

        // 记录审计日志
        recordLoginAudit(user, "LOGIN", "SUCCESS", "MFA verified (AAL2)");

        return response;
    }

    /**
     * 评估 MFA 风险
     *
     * <p>基于以下因素判断是否需要触发 MFA：</p>
     * <ul>
     *   <li>异地登录（非常用地点）</li>
     *   <li>非工作时间登录（22:00 - 06:00）</li>
     * </ul>
     *
     * @param user 用户信息
     * @return true-存在风险，建议/需要 MFA
     */
    private boolean evaluateMfaRisk(UserInfo user) {
        if (user == null || request == null) {
            return false;
        }

        String ip = ipLocationUtil.getClientIp(request);

        // 异地登录检测
        if (!ipLocationUtil.isCommonLocation(user.getUserId(), ip)) {
            log.info("MFA risk detected: unusual location for user {} (IP: {})", user.getUsername(), ip);
            return true;
        }

        // 非工作时间检测（22:00 - 06:00）
        int hour = LocalTime.now().getHour();
        if (hour >= 22 || hour < 6) {
            log.info("MFA risk detected: off-hours login for user {} (hour: {})", user.getUsername(), hour);
            return true;
        }

        return false;
    }

}
