package com.carlos.auth.sso.web;

import com.carlos.auth.api.enums.AuthErrorCode;
import com.carlos.auth.config.OAuth2Properties;
import com.carlos.auth.constant.AuthRedisKey;
import com.carlos.auth.login.UserLoginService;
import com.carlos.auth.sso.pojo.param.SsoTokenExchangeParam;
import com.carlos.auth.sso.pojo.vo.SsoTokenExchangeVO;
import com.carlos.auth.sso.service.SsoTokenExchangeService;
import com.carlos.auth.util.IpLocationUtil;
import com.carlos.core.exception.BusinessException;
import com.carlos.core.response.Result;
import com.carlos.redis.ratelimit.RateLimitUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * SSO 单点登录控制器
 *
 * <p>提供跨系统的 Token 交换和全局登出接口，支持免密跳转场景。</p>
 *
 * @author Carlos
 * @version 3.0.0
 * @since 2026-05-19
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/sso")
@Tag(name = "SSO单点登录", description = "Token 交换、全局登出等单点登录接口")
public class SsoController {

    private final SsoTokenExchangeService ssoTokenExchangeService;
    private final UserLoginService userLoginService;
    private final OAuth2Properties oAuth2Properties;
    private final IpLocationUtil ipLocationUtil;

    /**
     * SSO Token 交换
     *
     * <p>基于当前有效的访问令牌，为目标客户端申请新的访问令牌，实现免密跳转。</p>
     *
     * <p><strong>典型场景：</strong></p>
     * <ul>
     *   <li>用户已在门户系统登录，点击链接跳转到子系统时无需再次输入密码</li>
     *   <li>网关层统一认证后，向多个子系统分发 Token</li>
     * </ul>
     *
     * @param param Token 交换参数
     * @return 新的访问令牌
     */
    @Operation(summary = "SSO Token 交换", description = "基于已有有效 Token，为目标客户端生成新的访问令牌")
    @PostMapping("/exchange")
    public Result<SsoTokenExchangeVO> exchangeToken(@Valid @RequestBody SsoTokenExchangeParam param,
                                                    HttpServletRequest request) {
        log.info("SSO token exchange request: targetClientId={}", param.getTargetClientId());

        // IP 级别速率限制：每 IP 每分钟最多 20 次交换请求
        String clientIp = ipLocationUtil.getClientIp(request);
        if (!RateLimitUtil.tryAcquire(AuthRedisKey.rateSsoExchangeIp(clientIp), 20, 1, TimeUnit.MINUTES)) {
            log.warn("SSO token exchange rate limit exceeded: ip={}", clientIp);
            throw AuthErrorCode.AUTH_SSO_EXCHANGE_RATE_LIMIT.exception();
        }

        try {
            SsoTokenExchangeVO result = ssoTokenExchangeService.exchangeToken(
                param.getSourceToken(),
                param.getTargetClientId(),
                param.getRequestedScopes()
            );
            return Result.success(result, "Token 交换成功");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("SSO token exchange failed: targetClientId={}", param.getTargetClientId(), e);
            return Result.error("Token 交换失败");
        }
    }

    /**
     * SSO 全局登出
     *
     * <p>撤销当前用户的所有有效访问令牌，实现跨系统统一登出。</p>
     *
     * <p><strong>效果：</strong></p>
     * <ul>
     *   <li>当前 Token 立即失效</li>
     *   <li>该用户在所有客户端（Web、App、第三方应用）的 Token 全部撤销</li>
     *   <li>下次访问任何受保护资源时都需要重新登录</li>
     * </ul>
     *
     * @return 登出结果
     */
    @Operation(summary = "SSO 全局登出", description = "撤销当前用户在所有客户端的访问令牌")
    @PostMapping("/global-logout")
    public Result<String> globalLogout(HttpServletRequest request) {
        // 检查全局登出功能是否启用
        if (!oAuth2Properties.getSso().isGlobalLogoutEnabled()) {
            log.warn("SSO global logout is disabled");
            throw AuthErrorCode.AUTH_SSO_GLOBAL_LOGOUT_DISABLED.exception();
        }

        String token = extractBearerToken(request.getHeader("Authorization"));
        if (token == null || token.isBlank()) {
            return Result.error("未提供访问令牌");
        }

        // 速率限制：每 Token 每小时最多 10 次全局登出
        if (!RateLimitUtil.tryAcquire(AuthRedisKey.rateSsoLogout(maskToken(token)), 10, 1, TimeUnit.HOURS)) {
            log.warn("SSO global logout rate limit exceeded");
            throw AuthErrorCode.AUTH_SSO_EXCHANGE_RATE_LIMIT.exception();
        }

        log.info("SSO global logout request");

        try {
            int revokedCount = userLoginService.globalLogout(token);
            log.info("SSO global logout successful, revoked {} sessions", revokedCount);
            return Result.success("全局登出成功，已撤销 " + revokedCount + " 个会话");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("SSO global logout failed", e);
            return Result.error("全局登出失败");
        }
    }

    /**
     * 从 Authorization 头提取 Bearer Token
     */
    private static String extractBearerToken(String authHeader) {
        if (authHeader == null || authHeader.isBlank()) {
            return null;
        }
        String prefix = "Bearer ";
        if (authHeader.startsWith(prefix)) {
            return authHeader.substring(prefix.length()).trim();
        }
        return authHeader.trim();
    }

    /**
     * 脱敏 Token
     */
    private static String maskToken(String token) {
        if (token == null || token.length() < 10) {
            return "***";
        }
        return token.substring(0, 6) + "****" + token.substring(token.length() - 4);
    }
}
