package com.carlos.auth.security.filter;

import com.carlos.auth.security.SecurityUser;
import com.carlos.core.auth.LoginUserInfo;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Opaque Bearer Token 认证过滤器
 *
 * <p>密码登录等自定义授权类型签发的是不透明 Token（非 JWT），而默认资源服务器仅支持 JWT。
 * 本过滤器在 BearerTokenAuthenticationFilter 之前执行：当 Bearer Token 为不透明格式时，
 * 通过本地 {@link OAuth2AuthorizationService} 解析授权信息并构建 SecurityContext，
 * 使 {@code /auth/user/me} 等端点同时支持 JWT（SSO 授权码流程）与 Opaque（密码登录）两种 Token。</p>
 *
 * @author Carlos
 * @version 3.0.0
 * @since 2026-09-15
 */
@Slf4j
@RequiredArgsConstructor
public class OpaqueTokenAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final OAuth2AuthorizationService authorizationService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);
        // 仅处理不透明 Token（JWT 含两个 '.'，交给 BearerTokenAuthenticationFilter 处理）
        if (token == null || token.contains(".")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            OAuth2Authorization authorization = authorizationService.findByToken(token, OAuth2TokenType.ACCESS_TOKEN);
            if (authorization == null) {
                filterChain.doFilter(request, response);
                return;
            }

            OAuth2Authorization.Token<OAuth2AccessToken> accessToken = authorization.getAccessToken();
            if (accessToken == null || !accessToken.isActive()) {
                filterChain.doFilter(request, response);
                return;
            }

            Map<String, Object> claims = accessToken.getClaims();
            if (claims == null || claims.isEmpty()) {
                filterChain.doFilter(request, response);
                return;
            }

            SecurityUser securityUser = buildSecurityUser(claims);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(securityUser, token, securityUser.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("Opaque token authenticated for user: {}", securityUser.getAccount());
        } catch (Exception e) {
            // 解析失败不阻断请求，交由下游 BearerTokenAuthenticationFilter/AuthorizationFilter 处理
            log.debug("Opaque token authentication failed: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(header) || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return header.substring(BEARER_PREFIX.length());
    }

    private SecurityUser buildSecurityUser(Map<String, Object> claims) {
        LoginUserInfo userInfo = new LoginUserInfo();
        Object userId = claims.get("user_id");
        if (userId instanceof Number number) {
            userInfo.setId(number.longValue());
        } else if (userId != null) {
            userInfo.setId(userId.toString());
        }
        userInfo.setAccount((String) claims.getOrDefault("username", claims.get("sub")));
        userInfo.setEnable(true);

        List<String> authorities = new ArrayList<>();
        Object authoritiesClaim = claims.get("authorities");
        if (authoritiesClaim instanceof String str && StringUtils.hasText(str)) {
            for (String authority : str.split(",")) {
                if (StringUtils.hasText(authority)) {
                    authorities.add(authority.trim());
                }
            }
        }
        Object roleIds = claims.get("role_ids");
        if (authorities.isEmpty() && roleIds instanceof List<?> list) {
            list.forEach(role -> authorities.add(String.valueOf(role)));
        }
        userInfo.setRoleIds(new HashSet<Serializable>(authorities));

        return new SecurityUser(userInfo);
    }
}
