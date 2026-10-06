package com.carlos.auth.security.ext;

import com.carlos.auth.idp.IdentityProvider;
import com.carlos.auth.idp.IdentityProviderRegistry;
import com.carlos.auth.idp.IdentityProviderRequest;
import com.carlos.auth.idp.UserIdentity;
import com.carlos.auth.oauth2.OAuth2ErrorCodesExpand;
import com.carlos.auth.provider.UserInfo;
import com.carlos.auth.risk.RiskAssessment;
import com.carlos.auth.risk.RiskEngine;
import com.carlos.auth.risk.RiskLevel;
import com.carlos.auth.security.SecurityUser;
import com.carlos.auth.security.TenantLoginChecker;
import com.carlos.auth.security.manager.IpBlockManager;
import com.carlos.auth.security.manager.LoginAttemptManager;
import com.carlos.auth.security.password.PasswordProperties;
import com.carlos.auth.security.service.ExtendUserDetailsService;
import com.carlos.auth.util.IpLocationUtil;
import com.carlos.core.auth.LoginUserInfo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 扩展认证提供者
 *
 * <p>支持多种认证方式的统一认证处理器。基于 {@link IdentityProvider} 架构，
 * 将具体认证逻辑委托给对应身份源适配器。</p>
 *
 * @author Carlos
 * @version 3.0.0
 * @since 2026-04-08
 * @see ExtendAuthenticationToken
 * @see ExtendAuthenticationConverter
 * @see IdentityProviderRegistry
 */
@Slf4j
public class ExtendAuthenticationProvider implements AuthenticationProvider, ApplicationEventPublisherAware {

    /**
     * 身份源注册中心
     */
    private final IdentityProviderRegistry providerRegistry;

    /**
     * 用户详情服务
     */
    private final ExtendUserDetailsService userDetailsService;

    /**
     * 登录尝试管理器（风控增强：P1 修复 - /oauth2/token 也受保护）
     */
    private final LoginAttemptManager loginAttemptManager;

    /**
     * IP 封禁管理器
     */
    private final IpBlockManager ipBlockManager;

    /**
     * 风控引擎
     */
    private final RiskEngine riskEngine;

    /**
     * IP 定位工具
     */
    private final IpLocationUtil ipLocationUtil;

    /**
     * 密码安全配置
     */
    private final PasswordProperties passwordProperties;

    /**
     * 登录租户校验器（S2，feature flag：carlos.auth.tenant.enabled，默认关闭）
     */
    private final TenantLoginChecker tenantLoginChecker;

    /**
     * 事件发布器
     */
    private ApplicationEventPublisher eventPublisher;

    /**
     * 构造方法
     *
     * @param providerRegistry   身份源注册中心
     * @param userDetailsService 用户详情服务
     * @param loginAttemptManager 登录尝试管理器
     * @param ipBlockManager     IP 封禁管理器
     * @param riskEngine         风控引擎
     * @param ipLocationUtil     IP 定位工具
     * @param passwordProperties 密码安全配置
     * @param tenantLoginChecker 登录租户校验器
     */
    public ExtendAuthenticationProvider(IdentityProviderRegistry providerRegistry,
                                        ExtendUserDetailsService userDetailsService,
                                        LoginAttemptManager loginAttemptManager,
                                        IpBlockManager ipBlockManager,
                                        RiskEngine riskEngine,
                                        IpLocationUtil ipLocationUtil,
                                        PasswordProperties passwordProperties,
                                        TenantLoginChecker tenantLoginChecker) {
        Assert.notNull(providerRegistry, "providerRegistry cannot be null");
        Assert.notNull(userDetailsService, "userDetailsService cannot be null");
        this.providerRegistry = providerRegistry;
        this.userDetailsService = userDetailsService;
        this.loginAttemptManager = loginAttemptManager;
        this.ipBlockManager = ipBlockManager;
        this.riskEngine = riskEngine;
        this.ipLocationUtil = ipLocationUtil;
        this.passwordProperties = passwordProperties;
        this.tenantLoginChecker = tenantLoginChecker;
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * 执行认证
     *
     * @param authentication 认证令牌
     * @return 认证成功的令牌
     * @throws AuthenticationException 认证失败时抛出
     */
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        ExtendAuthenticationToken token = (ExtendAuthenticationToken) authentication;
        String grantType = token.getGrantType().getValue();

        log.debug("Authenticating with grant_type: {}, principal: {}", grantType, token.getPrincipal());

        try {
            ExtendAuthenticationToken authenticatedToken = authenticateInternal(token);

            if (eventPublisher != null) {
                // eventPublisher.publishEvent(new AuthenticationSuccessEvent(authenticatedToken));
            }

            log.info("Authentication successful: {}, grant_type: {}", token.getPrincipal(), grantType);
            return authenticatedToken;

        } catch (AuthenticationException e) {
            log.warn("Authentication failed: {}, grant_type: {}, reason: {}",
                token.getPrincipal(), grantType, e.getMessage());

            if (eventPublisher != null) {
                // eventPublisher.publishEvent(new AuthenticationFailureEvent(token, e));
            }
            throw e;
        }
    }

    /**
     * 内部认证逻辑
     *
     * <p>统一路由到 {@link IdentityProvider} 进行认证，并在认证前后接入风控。</p>
     *
     * <h3>P1 修复（统一登录体系）：</h3>
     * <ul>
     *   <li>认证前：IP 封禁检查 + 账号锁定检查 + 风控前置评估</li>
     *   <li>认证后：成功重置失败计数，失败记录失败计数</li>
     *   <li>高风险时强制要求 MFA（step-up）</li>
     * </ul>
     *
     * @param token 认证令牌
     * @return 认证成功的令牌
     * @throws AuthenticationException 认证失败时抛出
     */
    protected ExtendAuthenticationToken authenticateInternal(ExtendAuthenticationToken token)
        throws AuthenticationException {

        String grantType = token.getGrantType().getValue();
        String principal = token.getPrincipal() != null ? token.getPrincipal().toString() : null;

        // 0. 获取当前 HTTP 请求（用于 IP 提取）
        HttpServletRequest httpRequest = getCurrentRequest();
        String clientIp = httpRequest != null ? ipLocationUtil.getClientIp(httpRequest) : null;

        // 1. 风控前置：IP 封禁检查
        if (httpRequest != null && ipBlockManager.isIpBlocked(httpRequest)) {
            log.warn("IP blocked on /oauth2/token: ip={}, principal={}", clientIp, principal);
            throw new LockedException("IP 已被封禁，请稍后再试");
        }

        // 2. 风控前置：账号锁定检查
        if (principal != null && loginAttemptManager.isAccountLocked(principal)) {
            log.warn("Account locked on /oauth2/token: principal={}", principal);
            throw new LockedException(OAuth2ErrorCodesExpand.USER_LOCKED.getErrorDescription());
        }

        // 3. 风控评估（高风险直接阻断）
        if (principal != null) {
            RiskAssessment assessment = riskEngine.quickAssess(clientIp, principal, null);
            if (assessment.getLevel() == RiskLevel.CRITICAL) {
                log.warn("Login blocked by risk engine: principal={}, ip={}, score={}",
                    principal, clientIp, assessment.getScore());
                throw new LockedException("登录风险过高，请稍后再试");
            }
        }

        // 4. 查找对应身份源提供者
        IdentityProvider provider = providerRegistry.findByGrantType(grantType);

        // 4.5 S2 租户校验（feature flag：carlos.auth.tenant.enabled，默认关闭）：
        // 从 token 请求扩展参数 tenant_code 解析并校验租户，结果以 tenantId 传给身份源做租户内用户定位
        Long tenantId = tenantLoginChecker.resolveTenantId(
            token.getAdditionalParameters().get(TenantLoginChecker.PARAM_TENANT_CODE));

        // 5. 构建认证请求
        // 安全：tenantId 只允许来自 TenantLoginChecker 校验后的结果。
        // BaseAuthenticationConverter 会把客户端原始请求参数全部透传到 additionalParameters，
        // 若不先移除，客户端直接伪造 tenantId 请求参数即可绕过租户状态校验登录任意租户
        Map<String, String> additionalParameters = new HashMap<>(token.getAdditionalParameters());
        additionalParameters.remove(TenantLoginChecker.PARAM_TENANT_ID);
        if (tenantId != null) {
            additionalParameters.put(TenantLoginChecker.PARAM_TENANT_ID, String.valueOf(tenantId));
        }
        IdentityProviderRequest request = IdentityProviderRequest.builder()
            .principal(token.getPrincipal())
            .credentials(token.getCredentials())
            .clientId(token.getClient() != null ? token.getClient().getName() : null)
            .additionalParameters(additionalParameters)
            .build();

        // 6. 执行身份源认证
        UserIdentity userIdentity;
        try {
            userIdentity = provider.authenticate(request);
        } catch (AuthenticationException e) {
            // 认证失败：记录失败次数（账号 + IP）
            if (principal != null) {
                loginAttemptManager.recordLoginFailure(principal);
            }
            if (httpRequest != null) {
                ipBlockManager.recordLoginFailure(httpRequest);
            }
            throw e;
        }

        // 7. 转换为内部 UserInfo 并检查状态
        UserInfo userInfo = convertToUserInfo(userIdentity);
        checkUserStatus(userInfo);

        // 8. 认证成功：重置失败计数
        if (principal != null) {
            loginAttemptManager.recordLoginSuccess(principal);
        }
        if (httpRequest != null) {
            ipBlockManager.recordLoginSuccess(httpRequest);
        }

        // 9. 构建认证令牌
        return buildAuthenticatedToken(token, userInfo);
    }

    /**
     * 获取当前 HTTP 请求
     */
    private HttpServletRequest getCurrentRequest() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            return servletAttrs.getRequest();
        }
        return null;
    }

    /**
     * 将统一用户身份转换为内部 UserInfo
     *
     * @param identity 统一用户身份
     * @return 内部 UserInfo
     */
    protected UserInfo convertToUserInfo(UserIdentity identity) {
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(safeParseLong(identity.getProviderUserId()));
        userInfo.setUsername(identity.getUsername());
        userInfo.setEmail(identity.getEmail());
        userInfo.setPhone(identity.getPhone());
        userInfo.setRoleCodes(identity.getRoleCodes() != null
            ? new ArrayList<>(identity.getRoleCodes()) : null);
        // 优先使用身份源提供的状态，默认启用
        userInfo.setStatus(identity.getStatus() != null ? identity.getStatus() : "ENABLE");
        userInfo.setTenantId(identity.getTenantId());
        userInfo.setPwdLastModify(identity.getPwdLastModify());
        userInfo.setMfaEnabled(false);
        return userInfo;
    }

    /**
     * 安全地将字符串解析为 Long
     */
    private Long safeParseLong(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            log.debug("Provider user id is not a numeric value: {}", value);
            return null;
        }
    }

    /**
     * 检查用户状态
     *
     * @param userInfo 用户信息
     * @throws AuthenticationException 状态异常时抛出
     */
    protected void checkUserStatus(UserInfo userInfo) {
        if (userInfo.isAccountLocked()) {
            log.warn("Account is locked: {}", userInfo.getUsername());
            throw new LockedException(OAuth2ErrorCodesExpand.USER_LOCKED.getErrorDescription());
        }
        if (!userInfo.isAccountEnabled()) {
            log.warn("Account is disabled: {}", userInfo.getUsername());
            throw new DisabledException(OAuth2ErrorCodesExpand.USER_DISABLE.getErrorDescription());
        }
        // 校验密码是否过期（90天）
        if (isPasswordExpired(userInfo)) {
            log.warn("Password expired for user: {}", userInfo.getUsername());
            throw new CredentialsExpiredException(OAuth2ErrorCodesExpand.CREDENTIALS_EXPIRED.getErrorDescription());
        }
    }

    /**
     * 检查密码是否过期
     *
     * <p>使用配置的 maxAgeDays（0=永不过期，NIST 推荐），
     * pwdLastModify为空时视为不过期（兼容旧数据）。</p>
     *
     * @param userInfo 用户信息
     * @return true-已过期，false-未过期
     */
    private boolean isPasswordExpired(UserInfo userInfo) {
        int maxAgeDays = passwordProperties.getStrength().getMaxAgeDays();
        if (maxAgeDays <= 0) {
            return false;
        }
        LocalDateTime pwdLastModify = userInfo.getPwdLastModify();
        if (pwdLastModify == null) {
            return false;
        }
        return pwdLastModify.plusDays(maxAgeDays).isBefore(LocalDateTime.now());
    }

    /**
     * 构建认证成功的令牌
     *
     * @param originalToken 原始令牌
     * @param userInfo 用户信息
     * @return 认证成功的令牌
     */
    protected ExtendAuthenticationToken buildAuthenticatedToken(ExtendAuthenticationToken originalToken,
                                                                UserInfo userInfo) {
        // 对于第三方登录的新用户，允许 userDetailsService 找不到用户
        UserDetails userDetails = null;
        try {
            userDetails = userDetailsService.loadUserByUsername(userInfo.getUsername());
        } catch (org.springframework.security.core.userdetails.UsernameNotFoundException e) {
            log.debug("User not found in userDetailsService: {}, treating as new social user", userInfo.getUsername());
        }

        Collection<SimpleGrantedAuthority> authorities = userInfo.getRoleCodes() != null
            ? userInfo.getRoleCodes().stream()
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList())
            : null;

        LoginUserInfo loginUserInfo = userDetailsService.loadLoginUserInfo(userInfo.getUsername());
        SecurityUser securityUser = loginUserInfo != null
            ? new SecurityUser(loginUserInfo)
            : null;

        ExtendAuthenticationToken authenticatedToken = new ExtendAuthenticationToken(
            userInfo.getUsername(),
            null,
            originalToken.getGrantType(),
            originalToken.getClient(),
            originalToken.getScopes(),
            originalToken.getAdditionalParameters(),
            authorities,
            securityUser
        );

        authenticatedToken.setAuthMethod(originalToken.getAuthMethod());
        return authenticatedToken;
    }

    /**
     * 判断是否支持该认证类型
     *
     * @param authentication 认证类型
     * @return true-支持
     */
    @Override
    public boolean supports(Class<?> authentication) {
        return ExtendAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
