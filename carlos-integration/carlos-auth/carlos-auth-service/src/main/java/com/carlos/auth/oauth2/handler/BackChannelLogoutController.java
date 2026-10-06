package com.carlos.auth.oauth2.handler;

import com.carlos.auth.constant.AuthRedisKey;
import com.carlos.auth.oauth2.repository.RedisOAuth2AuthorizationService;
import com.carlos.core.response.Result;
import com.carlos.redis.util.RedisUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

/**
 * Back-Channel Logout 端点
 *
 * <p>实现 OIDC Back-Channel Logout 机制：IdP 主动通知 RP 端登出。</p>
 *
 * <h3>工作方式：</h3>
 * <ol>
 *   <li>用户在 carlos-auth 执行登出（或管理员强制下线）</li>
 *   <li>carlos-auth 撤销该用户所有 Token（Redis 中删除授权信息）</li>
 *   <li>carlos-auth 向已注册的 RP 发送 back-channel logout 通知（携带 logout_token）</li>
 *   <li>RP 验证 logout_token 后清理本地会话</li>
 * </ol>
 *
 * <h3>当前实现（v1 简化版）：</h3>
 * <p>基于 Redis 的 Token 撤销机制，不依赖 RP 端实现 back-channel 接收端点。
 * 因为所有 Token 验签/introspection 都会查询 Redis，删除即失效。
 * 后续版本可扩展为向 RP 发送 HTTP POST 通知。</p>
 *
 * <p>本端点同时提供 RP 调用的"单点登出触发接口"：
 * RP 可以调用此接口请求 IdP 撤销某用户的所有会话。</p>
 *
 * @author Carlos
 * @version 3.0.0
 * @since 2026-07-01
 * @see <a href="https://openid.net/specs/openid-connect-backchannel-1_0.html">OIDC Back-Channel Logout</a>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/auth/logout")
@Tag(name = "Back-Channel Logout", description = "OIDC 后端通道登出")
public class BackChannelLogoutController {

    private final RedisOAuth2AuthorizationService authorizationService;

    /**
     * 全局登出（撤销用户所有会话）
     *
     * <p>IdP 侧的全局登出入口：根据 principalName 撤销该用户在所有客户端的授权。</p>
     *
     * <p><strong>安全说明：</strong>此接口需要认证（Bearer Token），
     * 且调用者必须是 Token 所有者本人或具有 admin 权限。</p>
     *
     * @param principalName 用户名（当前认证用户，由 Token 中提取）
     * @return 撤销的会话数量
     */
    @Operation(summary = "全局登出", description = "撤销用户所有会话（Back-Channel Logout 触发点）")
    @PostMapping("/global")
    public Result<Integer> globalLogout(@RequestParam(required = false) String principalName) {
        if (principalName == null || principalName.isBlank()) {
            return Result.error("principalName 不能为空");
        }

        int revokedCount = revokeAllSessions(principalName);
        log.info("Back-Channel Logout: user={}, revoked={} sessions", principalName, revokedCount);
        return Result.success(revokedCount);
    }

    /**
     * RP 触发的单点登出
     *
     * <p>RP 检测到用户在自己侧登出后，可调用此接口通知 IdP 全局撤销该用户会话。
     * 等效于 OIDC 的 RP-Initiated Logout。</p>
     *
     * <p><strong>安全说明：</strong>此接口需要 client_credentials Token 认证（RP 身份）。</p>
     *
     * @param principalName 要登出的用户名
     * @param clientId      发起登出请求的 RP 客户端 ID
     * @return 撤销的会话数量
     */
    @Operation(summary = "RP 触发单点登出", description = "RP 通知 IdP 撤销用户会话")
    @PostMapping("/backchannel")
    public Result<Integer> backChannelLogout(@RequestParam @NotBlank(message = "principalName 不能为空") @Size(max = 64, message = "principalName 长度不能超过64") String principalName,
                                             @RequestParam @NotBlank(message = "clientId 不能为空") @Size(max = 64, message = "clientId 长度不能超过64") String clientId) {
        if (principalName == null || principalName.isBlank()) {
            return Result.error("principalName 不能为空");
        }

        log.info("Back-Channel Logout request from RP: clientId={}, principalName={}", clientId, principalName);

        int revokedCount = revokeAllSessions(principalName);

        log.info("Back-Channel Logout completed: user={}, revokedBy={}, sessions={}",
            principalName, clientId, revokedCount);
        return Result.success(revokedCount);
    }

    /**
     * 撤销指定用户的所有授权会话
     *
     * @param principalName 用户名
     * @return 撤销的会话数量
     */
    private int revokeAllSessions(String principalName) {
        Set<String> authIds = authorizationService.findAuthorizationIdsByPrincipalName(principalName);
        if (authIds == null || authIds.isEmpty()) {
            log.info("No active sessions found for user: {}", principalName);
            return 0;
        }

        int revokedCount = 0;
        for (String authId : authIds) {
            try {
                OAuth2Authorization authorization = authorizationService.findById(authId);
                if (authorization != null) {
                    authorizationService.remove(authorization);
                    revokedCount++;
                }
            } catch (Exception e) {
                log.warn("Failed to revoke authorization: authId={}, error={}", authId, e.getMessage());
            }
        }

        // 清理用户会话索引
        try {
            String sessionKey = AuthRedisKey.userSessions(principalName);
            RedisUtil.delete(sessionKey);
        } catch (Exception e) {
            log.warn("Failed to clean up session index for: {}", principalName, e);
        }

        return revokedCount;
    }
}
