package com.carlos.auth.oauth2.user;

import com.carlos.auth.provider.UserProvider;
import com.carlos.auth.security.service.ExtendUserDetailsService;
import com.carlos.core.auth.LoginUserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenClaimsContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenClaimsSet;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户模式 Token 自定义增强器
 *
 * <p>针对表单登录、AUTHORIZATION_CODE 等涉及用户身份的认证方式的 Token 进行增强，
 * 添加用户相关信息。</p>
 *
 * <h3>添加的声明：</h3>
 * <ul>
 *   <li>user_id - 用户ID</li>
 *   <li>username - 用户名</li>
 *   <li>tenant_id - 租户ID（多租户场景）</li>
 *   <li>dept_id - 部门ID</li>
 *   <li>role_ids - 角色ID列表</li>
 *   <li>authorities - 权限列表</li>
 *   <li>token_type - 令牌类型（user_token）</li>
 *   <li>auth_method - 认证方式</li>
 * </ul>
 *
 * <h3>使用方式：</h3>
 * <p>默认情况下，此类会尝试从 ExtendUserDetailsService 加载用户详细信息。
 * 如果需要自定义 Token 内容，可以：</p>
 * <ol>
 *   <li>注册一个新的 OAuth2TokenCustomizer Bean（优先级更高）</li>
 *   <li>继承此类并重写 buildUserClaims 方法</li>
 *   <li>在配置中禁用默认增强（carlos.auth.jwt.include-user-info=false）</li>
 * </ol>
 *
 * @author carlos
 * @version 3.0.0
 * @since 2026-04-08
 * @see OAuth2TokenCustomizer
 * @see ExtendUserDetailsService
 * @see UserProvider
 */
@Slf4j
public class CustomizeUserOAuth2TokenCustomizer implements OAuth2TokenCustomizer<OAuth2TokenClaimsContext> {

    /**
     * Token 类型标识
     */
    private static final String TOKEN_TYPE = "user_token";

    /**
     * 时间格式化器
     */
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    /**
     * 用户详情服务（可选）
     */
    private final ExtendUserDetailsService userDetailsService;

    /**
     * 构造函数
     *
     * <p>P1-001: 移除 userProvider 依赖。CustomizeUserOAuth2TokenCustomizer
     * 之前分别调用 loadLoginUserInfo() 和 loadUserInfo()，
     * 两者底层都通过 OrgRemoteUserDetailsService → OrgRemoteUserProvider
     * → apiOrgUser.getUserByIdentifier() 查询同一用户，造成不必要的重复 Feign 调用。
     * 现在统一只通过 loadLoginUserInfo() 加载用户数据。</p>
     *
     * @param userDetailsService 用户详情服务
     */
    public CustomizeUserOAuth2TokenCustomizer(ExtendUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    /**
     * 自定义 Token 声明
     *
     * <p>处理涉及用户身份的授权类型（AUTHORIZATION_CODE、REFRESH_TOKEN、表单登录等）。</p>
     *
     * @param context Token 声明上下文
     */
    @Override
    public void customize(OAuth2TokenClaimsContext context) {
        AuthorizationGrantType grantType = context.getAuthorizationGrantType();

        // 跳过客户端凭证模式
        if (AuthorizationGrantType.CLIENT_CREDENTIALS.equals(grantType)) {
            return;
        }

        OAuth2TokenClaimsSet.Builder claims = context.getClaims();

        // 获取用户名
        String username = context.getPrincipal().getName();
        if (username == null || username.isEmpty()) {
            log.warn("Cannot customize token: username is empty");
            return;
        }

        // 构建用户声明
        Map<String, Object> userClaims = buildUserClaims(context, username, grantType);

        // 添加到 Token
        userClaims.forEach(claims::claim);

        log.debug("Enhanced user token for user: {}", username);
    }

    /**
     * 构建用户声明
     *
     * <p>子类可以重写此方法添加自定义声明。</p>
     *
     * @param context Token 上下文
     * @param username 用户名
     * @return 用户声明映射
     */
    protected Map<String, Object> buildUserClaims(OAuth2TokenClaimsContext context, String username,
                                                  AuthorizationGrantType grantType) {
        Map<String, Object> claims = new HashMap<>();

        // 基础用户信息
        claims.put("username", username);
        claims.put("token_type", TOKEN_TYPE);
        claims.put("grant_type", grantType != null ? grantType.getValue() : "direct_login");

        // 时间信息
        Instant issuedAt = context.getClaims().build().getIssuedAt();
        if (issuedAt != null) {
            claims.put("issued_at", DATE_TIME_FORMATTER.format(issuedAt));
        }

        // P1-001: 仅通过 loadLoginUserInfo 加载用户数据（不再重复调用 loadUserInfo），
        // 消除 CustomizeUserOAuth2TokenCustomizer 内第 2 次 Feign 查询
        try {
            LoginUserInfo loginUserInfo = loadLoginUserInfo(username);
            if (loginUserInfo != null) {
                enrichClaimsFromLoginUserInfo(claims, loginUserInfo);
            }
        } catch (Exception e) {
            log.warn("Failed to load user info for token enhancement: {}", username, e);
            // 加载失败不影响 Token 生成，继续处理
        }

        // 添加权限信息
        enrichAuthorities(claims, context.getPrincipal());

        // 授权范围
        if (context.getAuthorizedScopes() != null && !context.getAuthorizedScopes().isEmpty()) {
            claims.put("scope", String.join(" ", context.getAuthorizedScopes()));
        }

        return claims;
    }

    /**
     * 从 ExtendUserDetailsService 加载用户信息
     *
     * @param username 用户名
     * @return LoginUserInfo 或 null
     */
    protected LoginUserInfo loadLoginUserInfo(String username) {
        if (userDetailsService == null) {
            return null;
        }
        try {
            return userDetailsService.loadLoginUserInfo(username);
        } catch (Exception e) {
            log.debug("Failed to load LoginUserInfo for: {}", username);
            return null;
        }
    }

    /**
     * 从 LoginUserInfo 丰富声明
     *
     * @param claims 声明映射
     * @param loginUserInfo 登录用户信息
     */
    protected void enrichClaimsFromLoginUserInfo(Map<String, Object> claims, LoginUserInfo loginUserInfo) {
        if (loginUserInfo.getId() != null) {
            claims.put("user_id", loginUserInfo.getId());
        }
        if (loginUserInfo.getClientId() != null) {
            claims.put("tenant_id", loginUserInfo.getClientId());
        }
        if (loginUserInfo.getDepartmentId() != null) {
            claims.put("dept_id", loginUserInfo.getDepartmentId());
        }
        if (loginUserInfo.getRoleIds() != null && !loginUserInfo.getRoleIds().isEmpty()) {
            claims.put("role_ids", loginUserInfo.getRoleIds());
        }
    }

    /**
     * 丰富权限信息
     *
     * @param claims 声明映射
     * @param principal 认证主体
     */
    protected void enrichAuthorities(Map<String, Object> claims, Authentication principal) {
        if (principal == null || principal.getAuthorities() == null) {
            return;
        }

        String authorities = principal.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.joining(","));

        if (!authorities.isEmpty()) {
            claims.put("authorities", authorities);
        }
    }

    /**
     * 创建简化的用户声明（当无法加载详细信息时使用）
     *
     * @param username 用户名
     * @return 简化声明映射
     */
    protected Map<String, Object> buildMinimalUserClaims(String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", username);
        claims.put("token_type", TOKEN_TYPE);
        return claims;
    }

}
