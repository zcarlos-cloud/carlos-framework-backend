package com.carlos.auth.web;

import cn.hutool.core.util.StrUtil;
import com.carlos.auth.api.pojo.ao.CheckTokenAO;
import com.carlos.auth.config.OAuth2Properties;
import com.carlos.auth.login.UserLoginService;
import com.carlos.core.response.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Token 管理控制器
 *
 * <p>提供 OAuth2 Token 的管理接口，包括令牌检查、撤销等。</p>
 *
 * <p><strong>与 {@link com.carlos.auth.login.UserAuthController} 的区别：</strong></p>
 * <ul>
 *   <li>此类：面向 OAuth2 客户端/资源服务器，处理 Token 级别的操作（检查有效性、撤销令牌）</li>
 *   <li>UserAuthController：面向最终用户，处理用户登录、登出等用户级操作</li>
 * </ul>
 *
 * @author Carlos
 * @date 2022/11/13 21:14
 * @see com.carlos.auth.login.UserAuthController
 */
@Slf4j
@AllArgsConstructor
@RestController
@Validated
@RequestMapping("/auth/token")
@Tag(name = "OAuth2认证接口")
public class TokenController {

    private final OAuth2AuthorizationService authorizationService;

    private final RegisteredClientRepository clientService;

    private final UserLoginService userLoginService;

    private final OAuth2Properties oAuth2Properties;

    /**
     * 退出并删除token
     *
     * @param authHeader Authorization
     * @param global     是否执行全局登出
     */
    @PostMapping("/logout")
    @Operation(summary = "退出登录")
    public Result<?> logout(
        @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
        @RequestParam(required = false, defaultValue = "false") boolean global) {
        String token = extractBearerToken(authHeader);
        if (StrUtil.isBlank(token)) {
            return Result.success();
        }
        if (global) {
            if (!oAuth2Properties.getSso().isGlobalLogoutEnabled()) {
                return Result.error("全局登出功能已禁用");
            }
            int revokedCount = userLoginService.globalLogout(token);
            return Result.success("全局登出成功，已撤销 " + revokedCount + " 个会话");
        }
        return this.removeToken(token);
    }

    @GetMapping("/checkToken")
    @Operation(summary = "检查Token")
    public Result<CheckTokenAO> checkToken(@RequestParam("token") @NotBlank(message = "Token 不能为空") @Size(max = 2048, message = "Token 长度不能超过 2048") String token) {
        if (StrUtil.isBlank(token)) {
            return Result.success(CheckTokenAO.builder()
                .active(false)
                .error("Token 不能为空")
                .build());
        }

        OAuth2Authorization authorization = this.authorizationService.findByToken(token, OAuth2TokenType.ACCESS_TOKEN);
        if (authorization == null) {
            log.debug("Token not found or expired: {}", maskToken(token));
            return Result.success(CheckTokenAO.builder()
                .active(false)
                .error("Token 不存在或已过期")
                .build());
        }

        OAuth2Authorization.Token<OAuth2AccessToken> accessToken = authorization.getAccessToken();
        if (accessToken == null || accessToken.getToken() == null) {
            return Result.success(CheckTokenAO.builder()
                .active(false)
                .error("Token 无效")
                .build());
        }

        // 检查是否过期
        Instant expiresAt = accessToken.getToken().getExpiresAt();
        if (expiresAt != null && expiresAt.isBefore(Instant.now())) {
            return Result.success(CheckTokenAO.builder()
                .active(false)
                .error("Token 已过期")
                .build());
        }

        String registeredClientId = authorization.getRegisteredClientId();
        RegisteredClient client = clientService.findById(registeredClientId);

        // 从 Token Claims 中提取用户上下文
        Map<String, Object> claims = authorization.getAccessToken().getClaims();
        String tokenType = claims != null ? (String) claims.get("token_type") : null;
        Long userId = extractLongClaim(claims, "user_id");
        String username = claims != null ? (String) claims.get("username") : null;
        Long tenantId = extractLongClaim(claims, "tenant_id");
        Long deptId = extractLongClaim(claims, "dept_id");
        Set<Long> roleIds = extractRoleIds(claims);
        List<String> authorities = extractAuthorities(claims);
        Set<String> scopes = accessToken.getToken().getScopes();

        long expiresIn = 0;
        Instant issuedAt = accessToken.getToken().getIssuedAt();
        if (issuedAt != null && expiresAt != null) {
            expiresIn = Duration.between(Instant.now(), expiresAt).getSeconds();
            if (expiresIn < 0) {
                expiresIn = 0;
            }
        }

        CheckTokenAO ao = CheckTokenAO.builder()
            .active(true)
            .tokenType(tokenType != null ? tokenType : "unknown")
            .userId(userId)
            .username(username)
            .tenantId(tenantId)
            .deptId(deptId)
            .roleIds(roleIds)
            .authorities(authorities)
            .clientId(client != null ? client.getClientId() : registeredClientId)
            .scopes(scopes)
            .expiresIn(expiresIn)
            .issuedAt(issuedAt != null ? issuedAt.toEpochMilli() : null)
            .expiresAt(expiresAt != null ? expiresAt.toEpochMilli() : null)
            .build();

        log.debug("Token check success: tokenType={}, userId={}, clientId={}", tokenType, userId,
            client != null ? client.getClientId() : registeredClientId);
        return Result.success(ao);
    }

    /**
     * 删除token
     *
     * @return com.carlos.common.core.response.Result<?>
     * @author Carlos
     * @date 2022/11/13 21:05
     */
    @Operation(summary = "删除Token")
    @PostMapping("/delete")
    public Result<?> removeToken(@RequestParam("token") @NotBlank(message = "Token 不能为空") @Size(max = 2048, message = "Token 长度不能超过 2048") String token) {
        OAuth2Authorization authorization = this.authorizationService.findByToken(token, OAuth2TokenType.ACCESS_TOKEN);
        if (authorization == null) {
            return Result.success();
        }
        OAuth2Authorization.Token<OAuth2AccessToken> accessToken = authorization.getAccessToken();
        if (accessToken == null || StrUtil.isBlank(accessToken.getToken().getTokenValue())) {
            return Result.success();
        }
        // 清空access token
        this.authorizationService.remove(authorization);
        return Result.success();
    }

    // ==================== 私有辅助方法 ====================

    private Long extractLongClaim(Map<String, Object> claims, String key) {
        if (claims == null || !claims.containsKey(key)) {
            return null;
        }
        Object value = claims.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Set<Long> extractRoleIds(Map<String, Object> claims) {
        if (claims == null || !claims.containsKey("role_ids")) {
            return Collections.emptySet();
        }
        Object value = claims.get("role_ids");
        if (value instanceof Collection<?> collection) {
            return collection.stream()
                .map(item -> {
                    if (item instanceof Number number) {
                        return number.longValue();
                    }
                    try {
                        return Long.valueOf(item.toString());
                    } catch (NumberFormatException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        }
        return Collections.emptySet();
    }

    private List<String> extractAuthorities(Map<String, Object> claims) {
        if (claims == null || !claims.containsKey("authorities")) {
            return Collections.emptyList();
        }
        Object value = claims.get("authorities");
        if (value instanceof String str) {
            return Arrays.asList(str.split(","));
        }
        return Collections.emptyList();
    }

    /**
     * 从 Authorization 头提取 Bearer Token
     */
    private static String extractBearerToken(String authHeader) {
        if (StrUtil.isBlank(authHeader)) {
            return null;
        }
        String prefix = "Bearer ";
        if (authHeader.startsWith(prefix)) {
            return authHeader.substring(prefix.length()).trim();
        }
        return authHeader.trim();
    }

    private static String maskToken(String token) {
        if (token == null || token.length() < 10) {
            return "***";
        }
        return token.substring(0, 6) + "****" + token.substring(token.length() - 4);
    }
}
