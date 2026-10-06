package com.carlos.auth.security.service;

import com.carlos.auth.provider.UserInfo;
import com.carlos.auth.provider.UserProvider;
import com.carlos.auth.security.TenantLoginChecker;
import com.carlos.auth.security.password.PasswordProperties;
import com.carlos.core.auth.LoginUserInfo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 基于 carlos-org 远程服务的用户详情服务
 *
 * <p>将 carlos-org 的用户数据适配为 Spring Security 的
 * {@link UserDetails} 和框架的 {@link LoginUserInfo}。</p>
 *
 * <p>替代 {@link DefaultExtendUserDetailsService}（开发环境内存实现），
 * 为 {@code DaoAuthenticationProvider} 提供生产级的用户加载能力。</p>
 *
 * <h3>用户状态映射：</h3>
 * <ul>
 *   <li>ENABLE → accountNonLocked=true, enabled=true</li>
 *   <li>DISABLE → enabled=false</li>
 *   <li>LOCKED → accountNonLocked=false</li>
 *   <li>密码过期（>90天）→ credentialsNonExpired=false</li>
 * </ul>
 *
 * @author Carlos
 * @version 3.0.0
 * @since 2026-06-20
 * @see ExtendUserDetailsService
 * @see DefaultExtendUserDetailsService
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrgRemoteUserDetailsService implements ExtendUserDetailsService {

    private final UserProvider userProvider;
    private final PasswordProperties passwordProperties;
    private final TenantLoginChecker tenantLoginChecker;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserInfo userInfo = loadUserByCurrentRequestTenant(username);

        if (userInfo == null) {
            log.warn("User not found: {}", username);
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        // 转换角色编码为 GrantedAuthority
        List<String> roleCodes = userProvider.getUserRoleCodes(userInfo.getUserId());
        List<GrantedAuthority> authorities = roleCodes.stream()
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList());

        // 密码过期检查（90天）
        boolean credentialsExpired = isPasswordExpired(userInfo.getPwdLastModify());

        return User.builder()
            .username(userInfo.getUsername())
            .password(userInfo.getPassword() != null ? userInfo.getPassword() : "")
            .authorities(authorities)
            .accountLocked(userInfo.isAccountLocked())
            .disabled(!userInfo.isAccountEnabled())
            .credentialsExpired(credentialsExpired)
            .build();
    }

    @Override
    public LoginUserInfo loadLoginUserInfo(String username) {
        // 与 loadUserByUsername 保持同一租户定位逻辑，避免用户查到别的租户（JWT tenant_id claim 来源）
        UserInfo userInfo = loadUserByCurrentRequestTenant(username);

        if (userInfo == null) {
            log.debug("No user info found for: {}", username);
            return null;
        }

        // 转换角色编码为角色ID集合（用于 JWT Token 增强）
        Set<java.io.Serializable> roleIds = new HashSet<>();
        List<String> roleCodes = userProvider.getUserRoleCodes(userInfo.getUserId());
        if (roleCodes != null) {
            for (String roleCode : roleCodes) {
                // 保持角色编码作为标识（与 SecurityUser 的行为一致）
                roleIds.add(roleCode);
            }
        }

        LoginUserInfo info = new LoginUserInfo();
        info.setId(userInfo.getUserId());
        info.setAccount(userInfo.getUsername());
        info.setPassword(userInfo.getPassword());
        info.setEnable(userInfo.isAccountEnabled());
        info.setClientId(userInfo.getTenantId());
        info.setDepartmentId(userInfo.getDeptId());
        info.setRoleIds(roleIds);

        return info;
    }

    /**
     * formLogin 的登录处理 URL（见 OAuth2AuthorizationServerConfig），
     * 仅该请求的表单里才有 tenantCode 字段
     */
    private static final String LOGIN_PROCESSING_URL = "/login";

    /**
     * 按当前请求的租户加载用户
     *
     * <p>S2 租户管理：仅在 formLogin 登录处理请求（POST /login）中从 tenantCode 表单字段
     * 解析并校验租户（feature flag 默认关闭，关闭时返回 null 走原有非租户化查询）。
     * 令牌颁发（授权码交换）与刷新请求没有该表单字段，且用户身份在登录时已按租户定位，
     * 此处保持原有非租户化加载用于 JWT claims 填充——否则开启开关后非平台租户用户
     * 换发的令牌会丢失 user_id 等 claims。</p>
     *
     * @param username 登录账号
     * @return 用户信息，不存在返回 null
     */
    private UserInfo loadUserByCurrentRequestTenant(String username) {
        if (!isLoginProcessingRequest()) {
            return userProvider.loadUserByIdentifier(username);
        }
        Long tenantId = tenantLoginChecker.resolveTenantId(resolveRequestTenantCode());
        if (tenantId == null) {
            return userProvider.loadUserByIdentifier(username);
        }
        return userProvider.loadUserByIdentifierAndTenant(username, tenantId);
    }

    /**
     * 当前请求是否为 formLogin 的登录处理请求（POST /login）
     *
     * @return true-是登录提交，false-其他请求（令牌颁发/刷新等）或无请求上下文
     */
    private boolean isLoginProcessingRequest() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            HttpServletRequest request = servletAttrs.getRequest();
            return LOGIN_PROCESSING_URL.equals(request.getRequestURI());
        }
        return false;
    }

    /**
     * 从当前 HTTP 请求读取租户编码表单字段
     *
     * @return 租户编码，无请求上下文或未携带时返回 null
     */
    private String resolveRequestTenantCode() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            HttpServletRequest request = servletAttrs.getRequest();
            return request.getParameter("tenantCode");
        }
        return null;
    }

    /**
     * 检查密码是否过期
     *
     * <p>使用配置的 maxAgeDays（0=永不过期，NIST 推荐），
     * pwdLastModify为空时视为不过期（兼容旧数据）。</p>
     *
     * @param pwdLastModify 密码最后修改时间
     * @return true-已过期，false-未过期
     */
    private boolean isPasswordExpired(LocalDateTime pwdLastModify) {
        int maxAgeDays = passwordProperties.getStrength().getMaxAgeDays();
        if (maxAgeDays <= 0) {
            return false;
        }
        if (pwdLastModify == null) {
            return false;
        }
        return pwdLastModify.plusDays(maxAgeDays).isBefore(LocalDateTime.now());
    }
}
