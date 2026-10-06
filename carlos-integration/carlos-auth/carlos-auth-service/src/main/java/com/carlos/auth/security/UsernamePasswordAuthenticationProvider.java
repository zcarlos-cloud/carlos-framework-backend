package com.carlos.auth.security;

import com.carlos.auth.idp.IdentityProvider;
import com.carlos.auth.idp.IdentityProviderRegistry;
import com.carlos.auth.idp.IdentityProviderRequest;
import com.carlos.auth.idp.UserIdentity;
import com.carlos.auth.oauth2.OAuth2ErrorCodesExpand;
import com.carlos.auth.security.password.PasswordProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * 用户名密码认证提供者
 *
 * <p>处理 {@link UsernamePasswordAuthenticationToken}，内部委托给
 * {@link IdentityProvider} 完成认证（复用 /oauth2/token 端点的同一套
 * IdentityProvider 认证链路）。</p>
 *
 * <h3>与 ExtendAuthenticationProvider 的区别：</h3>
 * <ul>
 *   <li><b>ExtendAuthenticationProvider</b> — 处理 OAuth2 Token 端点
 *       ({@code /oauth2/token})，接收 {@code ExtendAuthenticationToken}，
 *       支持 password/sms_code/social 等多种 grant_type</li>
 *   <li><b>本 Provider</b> — 处理 REST API 登录端点
 *       ({@code /auth/user/login})，接收 {@code UsernamePasswordAuthenticationToken}，
 *       仅支持用户名密码认证</li>
 * </ul>
 *
 * <p>两条路径共享同一个 {@link IdentityProvider} 实例
 * （通常为 {@code LocalIdentityProvider} → {@code ApiOrgUser} → carlos-org），
 * 确保密码验证逻辑唯一。</p>
 *
 * <h3>P1-001 修复：</h3>
 * <ul>
 *   <li>增加 {@link #checkUserStatus(UserIdentity)} 校验（与 ExtendAuthenticationProvider 对齐）</li>
 *   <li>将 {@link UserIdentity} 存入 Token details，消除下游 UserLoginService 的重复查询</li>
 * </ul>
 *
 * @author Carlos
 * @version 3.0.0
 * @since 2026-06-20
 * @see IdentityProviderRegistry
 * @see com.carlos.auth.security.ext.ExtendAuthenticationProvider
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UsernamePasswordAuthenticationProvider implements AuthenticationProvider {

    private final IdentityProviderRegistry providerRegistry;
    private final PasswordProperties passwordProperties;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        UsernamePasswordAuthenticationToken token =
            (UsernamePasswordAuthenticationToken) authentication;

        String username = token.getName();
        String password = token.getCredentials() != null
            ? token.getCredentials().toString()
            : null;

        log.debug("Authenticating user via IdentityProvider: {}", username);

        // 委托给 identity provider（LocalIdentityProvider → ApiOrgUser → carlos-org）
        // S2 租户管理：UserLoginService 在 details 中放入已校验的租户ID（Long），
        // 经 additionalParameters 传递给身份源做租户内用户定位；未开启租户校验时 details 为其他类型，忽略
        IdentityProvider provider = providerRegistry.findByGrantType("password");
        IdentityProviderRequest.IdentityProviderRequestBuilder requestBuilder =
            IdentityProviderRequest.builder()
                .principal(username)
                .credentials(password);
        if (token.getDetails() instanceof Long tenantId) {
            requestBuilder.additionalParameters(
                Collections.singletonMap(TenantLoginChecker.PARAM_TENANT_ID, String.valueOf(tenantId)));
        }
        UserIdentity identity = provider.authenticate(requestBuilder.build());

        // P1-001: 检查用户状态（与 ExtendAuthenticationProvider 保持一致）
        checkUserStatus(identity);

        // 转换角色编码为 GrantedAuthority
        Collection<GrantedAuthority> authorities = identity.getRoleCodes() != null
            ? identity.getRoleCodes().stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList())
            : Collections.emptyList();

        // 构建已认证令牌（清除凭证，防止泄露）
        // P1-001: 将 UserIdentity 存入 details，下游可直接使用，避免重复 Feign 查询
        UsernamePasswordAuthenticationToken authenticatedToken =
            new UsernamePasswordAuthenticationToken(identity.getUsername(), null, authorities);
        authenticatedToken.setDetails(identity);

        log.info("Authentication successful for user: {}", identity.getUsername());
        return authenticatedToken;
    }

    /**
     * 检查用户状态
     *
     * <p>P1-001: 与 {@code ExtendAuthenticationProvider.checkUserStatus()} 保持一致，
     * 确保通过 REST API 登录路径也能校验账号锁定/禁用/密码过期状态。</p>
     *
     * @param identity 认证成功后的用户身份信息
     * @throws LockedException 账号已锁定时抛出
     * @throws DisabledException 账号已禁用时抛出
     * @throws CredentialsExpiredException 密码已过期时抛出
     */
    private void checkUserStatus(UserIdentity identity) {
        if (identity == null || identity.getStatus() == null) {
            return;
        }
        String status = identity.getStatus();
        if ("LOCKED".equals(status)) {
            log.warn("Account is locked: {}", identity.getUsername());
            throw new LockedException(OAuth2ErrorCodesExpand.USER_LOCKED.getErrorDescription());
        }
        if ("DISABLE".equals(status)) {
            log.warn("Account is disabled: {}", identity.getUsername());
            throw new DisabledException(OAuth2ErrorCodesExpand.USER_DISABLE.getErrorDescription());
        }
        // 校验密码是否过期（使用配置的 maxAgeDays，0=永不过期）
        int maxAgeDays = passwordProperties.getStrength().getMaxAgeDays();
        if (maxAgeDays > 0) {
            LocalDateTime pwdLastModify = identity.getPwdLastModify();
            if (pwdLastModify != null && pwdLastModify.plusDays(maxAgeDays).isBefore(LocalDateTime.now())) {
                log.warn("Password expired for user: {}", identity.getUsername());
                throw new CredentialsExpiredException(OAuth2ErrorCodesExpand.CREDENTIALS_EXPIRED.getErrorDescription());
            }
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
