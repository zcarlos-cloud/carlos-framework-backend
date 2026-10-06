package com.carlos.security.config;

import com.carlos.core.auth.IdentityAssertion;
import com.carlos.core.auth.IdentityAssertionCodec;
import com.carlos.core.constant.HttpHeadersConstant;
import com.carlos.security.auth.IdentityAssertionVerifier;
import com.carlos.security.auth.UserContext;
import com.carlos.security.auth.UserContextAuthenticationToken;
import com.carlos.security.permission.PermissionProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * <p>
 * 用户上下文认证过滤器
 * </p>
 *
 * <p>从网关签发的身份断言重建登录身份：</p>
 * <pre>
 * 客户端 -> Gateway -> 下游服务
 *                    ↓
 *              [OAuth2AuthenticationFilter]  校验 token，得到身份
 *                    ↓
 *              [IdentityAssertionSignFilter] 用共享密钥签名，写入 X-User-Context
 *                    ↓
 *              [UserContextAuthenticationFilter] 验签，构建 SecurityContext
 * </pre>
 *
 * <h3>身份来源（唯一）</h3>
 * <p>只认 {@code X-User-Context}：形如 {@code v1.<payloadBase64Url>.<signatureBase64Url>} 的
 * HMAC-SHA256 断言（详见 {@link com.carlos.core.auth.IdentityAssertionCodec}）。验签由
 * {@link IdentityAssertionVerifier} 完成，通过后按其载荷构建 SecurityContext。</p>
 *
 * <p><b>为什么不再有「明文头 + 可信来源 IP」通道：</b>那条通道的信任锚是「请求来自哪个网段」，
 * 它既挡不住同网段的伪造者，又和内部 Feign 调用天然冲突 —— 网关注入的身份头会被
 * {@code FeignRequestInterceptor} 转发给下游，而源 IP 变成发起方服务，于是多主机部署下每次
 * 内部调用都会丢身份（本机全服务同 IP 才侥幸没暴雷）。签名的完整性由密码学保证，与调用方网段
 * 无关，因此可以安全地多跳传播。该通道已连同 {@code carlos.resource-server.auth.trusted-ip-ranges}
 * 一并删除。</p>
 *
 * <h3>行为约定</h3>
 * <ul>
 *   <li>断言<b>验签通过</b>：建立身份，身份与权限只取自断言的载荷。</li>
 *   <li>断言<b>缺失</b>：跳过，降级为匿名（例如网关 oauth2 白名单路径本就不签发断言）。</li>
 *   <li>断言<b>存在但验签失败或已过期</b>：降级为匿名并留 WARN。此处<b>不存在</b>任何更弱的
 *       回退通道，因此不会形成「发个垃圾断言即可迫使下游降级」的攻击面；
 *       {@code strict-header-auth=true} 时直接返回 401。</li>
 * </ul>
 *
 * <p><b>本过滤器是 best-effort 的「身份解析器」，不是准入控制点</b>：拿不到身份时一律降级为匿名，
 * 由下游的 {@code @PreAuthorize} 决定具体接口是否需要登录人。请求能否到达本服务，由网关的
 * {@code OAuth2AuthenticationFilter} 负责（下游端口在网络上只应对网关可达）。</p>
 *
 * @author carlos
 * @version 3.0.0
 * @since 2026-04-08
 * @see IdentityAssertionVerifier
 * @see HttpHeadersConstant#X_USER_CONTEXT
 */
@Slf4j
public class UserContextAuthenticationFilter extends OncePerRequestFilter {

    /**
     * 白名单路径：直接放行，不做任何身份解析
     */
    @Setter
    private Set<String> whitelistPaths = new HashSet<>();

    /**
     * 是否启用严格模式
     * <p>启用后，断言存在但验签失败时直接返回 401，不再静默降级为匿名。</p>
     */
    @Setter
    private boolean strictHeaderAuth = false;

    /**
     * 超级管理员角色编码（透传给构建出的 {@link UserContext}，供 isSuperAdmin() 判定）
     */
    @Setter
    private String superRoleCode = UserContext.DEFAULT_SUPER_ROLE_CODE;

    /**
     * 身份断言验签器（为 null 表示本服务未装配验签通道，等同于一概匿名）
     */
    @Setter
    private IdentityAssertionVerifier identityAssertionVerifier;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * 权限提供者
     */
    private final PermissionProvider permissionProvider;

    public UserContextAuthenticationFilter(PermissionProvider permissionProvider) {
        this.permissionProvider = permissionProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestPath = request.getRequestURI();
        if (isWhitelisted(requestPath)) {
            log.debug("Request whitelisted: {}", requestPath);
            filterChain.doFilter(request, response);
            return;
        }

        if (identityAssertionVerifier == null || !identityAssertionVerifier.isEnabled()) {
            log.debug("Identity assertion verification disabled, skipping authentication");
            filterChain.doFilter(request, response);
            return;
        }

        String assertionValue = request.getHeader(HttpHeadersConstant.X_USER_CONTEXT);
        if (!IdentityAssertionCodec.isPresent(assertionValue)) {
            // 网关未签发（白名单路径 / 未认证）：降级为匿名
            log.debug("No identity assertion present, skipping authentication");
            filterChain.doFilter(request, response);
            return;
        }

        Optional<IdentityAssertion> verified = identityAssertionVerifier.verify(assertionValue);
        if (verified.isEmpty()) {
            // 断言存在但不可验证：拒绝。这里刻意没有任何更弱的回退通道。
            log.warn("Rejected non-verifiable identity assertion, remoteAddr={}, path={}",
                request.getRemoteAddr(), requestPath);
            if (strictHeaderAuth) {
                response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid identity assertion");
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }

        IdentityAssertion assertion = verified.get();
        try {
            authenticate(assertion.getUid(), assertion.getAcc(), toRoleCodeSet(assertion.getRoles()),
                assertion.getTen(), assertion.getDept(), request);
        } catch (Exception e) {
            log.error("Failed to build authentication from verified assertion", e);
            if (strictHeaderAuth) {
                response.sendError(HttpStatus.UNAUTHORIZED.value(), "Authentication processing failed");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 构建登录身份并写入 SecurityContext
     *
     * @param userId    用户 ID
     * @param username  登录账号
     * @param roleCodes 角色编码集合
     * @param tenantId  租户 ID（可空）
     * @param deptId    主部门 ID（可空）
     * @param request   当前请求（作为认证令牌的 details）
     */
    private void authenticate(Long userId, String username, Set<String> roleCodes,
                              Long tenantId, Long deptId, HttpServletRequest request) {
        Set<String> permissions = loadPermissions(roleCodes);

        UserContext userContext = new UserContext();
        userContext.setUserId(userId);
        userContext.setUsername(username);
        userContext.setRoleCodes(roleCodes);
        userContext.setSuperRoleCode(superRoleCode);
        userContext.setPermissions(permissions);
        userContext.setTenantId(tenantId);
        userContext.setDeptId(deptId);

        UserContextAuthenticationToken authentication =
            new UserContextAuthenticationToken(userContext, userContext.getAuthorities());
        authentication.setDetails(request);

        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.debug("User context authenticated: userId={}, roles={}, permissions={}",
            userId, roleCodes.size(), permissions.size());
    }

    /**
     * 角色编码列表转集合
     */
    private Set<String> toRoleCodeSet(List<String> roles) {
        return roles == null ? Collections.emptySet() : new HashSet<>(roles);
    }

    /**
     * 加载角色权限
     *
     * @param roleCodes 角色编码集合
     * @return 权限集合
     */
    private Set<String> loadPermissions(Set<String> roleCodes) {
        if (roleCodes.isEmpty() || permissionProvider == null) {
            return Collections.emptySet();
        }

        try {
            return permissionProvider.getPermissions(roleCodes);
        } catch (Exception e) {
            log.error("Failed to load permissions for roles: {}", roleCodes, e);
            return Collections.emptySet();
        }
    }

    /**
     * 检查路径是否在白名单中
     */
    private boolean isWhitelisted(String requestPath) {
        for (String pattern : whitelistPaths) {
            if (pathMatcher.match(pattern, requestPath)) {
                return true;
            }
        }
        return false;
    }

}
