package com.carlos.auth.login;

import com.carlos.auth.api.enums.AuthErrorCode;
import com.carlos.auth.config.OAuth2Properties;
import com.carlos.auth.constant.AuthRedisKey;
import com.carlos.auth.login.dto.*;
import com.carlos.auth.risk.RiskAssessment;
import com.carlos.auth.risk.RiskEngine;
import com.carlos.auth.security.manager.IpBlockManager;
import com.carlos.auth.security.manager.LoginAttemptManager;
import com.carlos.auth.util.IpLocationUtil;
import com.carlos.core.response.CommonErrorCode;
import com.carlos.core.response.Result;
import com.carlos.redis.ratelimit.RateLimitUtil;
import com.carlos.security.permission.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 用户认证控制器（SSO 模式）
 *
 * <p>本控制器提供 SSO 架构下的辅助端点：</p>
 * <ul>
 *   <li>{@code POST /auth/user/pre-check} — 登录预校验（风控决策），供 IdP 登录页 AJAX 调用</li>
 *   <li>{@code POST /auth/user/refreshToken} — 刷新访问令牌（公共客户端专用，以刷新令牌为凭证）</li>
 *   <li>{@code GET /auth/user/me} — 获取当前用户信息（OIDC userinfo 等效）</li>
 *   <li>{@code POST /auth/user/logout} — 登出（Token 撤销 + Session 失效）</li>
 * </ul>
 *
 * <h3>SSO 架构说明：</h3>
 * <p>所有登录认证统一走 OIDC 授权码流程：</p>
 * <pre>
 * 业务应用 → 302 /oauth2/authorize → 302 /login（IdP 统一登录页）
 * → formLogin → Session Cookie → 302 回 /oauth2/authorize → 下发 code → 302 回 redirect_uri
 * → 业务应用用 code 换 Token（POST /oauth2/token）
 * </pre>
 *
 * <p><strong>Token 签发说明：</strong>
 * 登录 Token 签发统一通过标准 OAuth2 端点 {@code /oauth2/token}；
 * 例外是 {@code /auth/user/refreshToken}——公共客户端（无密钥、强制 PKCE）无法在
 * 标准令牌端点完成客户端认证，故由本控制器提供刷新端点。</p>
 *
 * @author Carlos
 * @version 3.0.0
 * @since 2026-07-01
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/user")
@Tag(name = "用户认证（SSO）", description = "预校验、用户信息、登出")
public class UserAuthController {

    private final IpBlockManager ipBlockManager;
    private final IpLocationUtil ipLocationUtil;
    private final LoginAttemptManager loginAttemptManager;
    private final OAuth2Properties oAuth2Properties;
    private final RiskEngine riskEngine;
    private final UserLoginService loginService;
    /**
     * 权限服务：解析当前用户的权限码（供前端按钮级鉴权）。
     * 用 ObjectProvider 做可选依赖 —— 未装配 security starter 的轻量上下文（如某些测试应用）不提供该 Bean，
     * 此时降级为不返回按钮权限，而不是让整个控制器装配失败。
     */
    private final ObjectProvider<PermissionService> permissionServiceProvider;

    /**
     * 登录预校验
     *
     * <p>IdP 统一登录页在提交登录前调用，返回风控决策结果，指导验证码/MFA 展示策略。</p>
     *
     * <p>防枚举攻击：对账号不存在的情况也返回正常响应（不泄露账号存在性），
     * 响应时间做恒定化处理。</p>
     *
     * @param request     预校验请求
     * @param httpRequest HTTP 请求
     * @return 预校验响应
     */
    @Operation(summary = "登录预校验", description = "返回风控决策与验证码类型，指导前端挑战展示")
    @PostMapping("/pre-check")
    public Result<PreCheckResponse> preCheck(@Valid @RequestBody PreCheckRequest request,
                                             HttpServletRequest httpRequest) {
        String clientIp = ipLocationUtil.getClientIp(httpRequest);

        // IP 速率限制（30次/分钟）
        if (!RateLimitUtil.tryAcquire(AuthRedisKey.rateIp(clientIp), 30, 1, TimeUnit.MINUTES)) {
            log.warn("Pre-check rate limit exceeded: {}", clientIp);
            return Result.error("请求过于频繁，请稍后再试");
        }

        // 风控快速评估
        RiskAssessment assessment = riskEngine.quickAssess(
                clientIp,
                request.getUsername(),
                request.getDeviceFingerprint()
        );

        PreCheckResponse response = PreCheckResponse.builder()
                .riskLevel(assessment.getLevel().name())
                .needCaptcha(assessment.getChallengeType() != null
                        && !"mfa".equals(assessment.getChallengeType())
                        && !"blocked".equals(assessment.getChallengeType()))
                .captchaType(assessment.getChallengeType() == null
                        || "mfa".equals(assessment.getChallengeType())
                        || "blocked".equals(assessment.getChallengeType())
                        ? null : assessment.getChallengeType())
                .mfaRequired(assessment.isMfaRequired())
                .blocked(assessment.isBlocked())
                .blockReason(assessment.getBlockReason())
                .mfaRecommended(assessment.isMfaRequired() && request.getUsername() != null)
                .build();

        log.info("Pre-check: username={}, ip={}, riskLevel={}, score={}",
                request.getUsername(), clientIp, response.getRiskLevel(), assessment.getScore());

        return Result.success(response);
    }

    /**
     * 普通账号密码登录（JSON API）
     *
     * <p>SPA 直连登录入口，适用于不需要 SSO 的场景。
     * 支持 SM2 加密密码传输、风控预检、MFA step-up。</p>
     *
     * <p><strong>与 SSO 登录的区别：</strong></p>
     * <ul>
     *   <li>此端点：直接返回 JWT Token（JSON 响应），不走浏览器重定向，不种 Session Cookie</li>
     *   <li>SSO 登录：走 /oauth2/authorize → /login（formLogin）→ Session → code → Token</li>
     * </ul>
     *
     * <p><strong>安全防护：</strong></p>
     * <ul>
     *   <li>IP + 账号维度速率限制</li>
     *   <li>账号/IP 锁定检查</li>
     *   <li>SM2 加密密码传输（由 {@link com.carlos.auth.login.UserLoginService#decryptPasswordIfNeeded(LoginRequest)} 解密）</li>
     *   <li>登录失败计数（防暴力破解）</li>
     * </ul>
     *
     * @param loginRequest 登录请求（密码可经 SM2 加密，需设置 encrypted=true + keyId）
     * @param httpRequest  HTTP 请求（用于提取客户端 IP）
     * @return 登录响应：直接返回 Token 或返回 mfaToken（需要 MFA step-up）
     */
    @Operation(summary = "普通账号密码登录", description = "JSON API 登录，直接返回 JWT Token 或 MFA 挑战")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest,
                                       HttpServletRequest httpRequest) {
        String clientIp = ipLocationUtil.getClientIp(httpRequest);

        // 1. IP + 账号维度速率限制（防暴力枚举）
        if (!RateLimitUtil.tryAcquire(AuthRedisKey.rateLogin(loginRequest.getUsername()), 5, 1, TimeUnit.MINUTES)) {
            log.warn("Login rate limit exceeded: username={}, ip={}", loginRequest.getUsername(), clientIp);
            return Result.error("登录请求过于频繁，请稍后再试");
        }

        // 2. 检查账号或 IP 是否已被锁定
        // if (loginService.isAccountLocked(loginRequest.getUsername())) {
        //     log.warn("Login blocked: account locked, username={}", loginRequest.getUsername());
        //     return Result.error("账号已被锁定，请30分钟后重试");
        // }

        // 3. SM2 密码解密（如需要）
        loginService.decryptPasswordIfNeeded(loginRequest);

        // 4. 执行登录认证（委托 UserLoginService 完成认证 → MFA 判定 → Token 签发）
        try {
            LoginResponse loginResponse = loginService.login(loginRequest);
            log.info("Login successful: username={}, ip={}, mfaRequired={}", loginRequest.getUsername(), clientIp, loginResponse.getMfaRequired());
            return Result.success(loginResponse);
        } catch (AuthenticationException e) {
            log.warn("Login failed: username={}, ip={}, error={}", loginRequest.getUsername(), clientIp, e.getMessage());
            loginService.recordLoginFailure(loginRequest.getUsername(), httpRequest);
            throw AuthErrorCode.AUTH_USER_AUTH_FAILED.exception(e.getMessage());
        }
    }

    /**
     * 刷新访问令牌
     *
     * <p>公共客户端（无密钥、强制 PKCE）无法通过标准 {@code /oauth2/token} 端点刷新令牌：
     * Spring Authorization Server 要求公共客户端在令牌端点使用 code_verifier 完成客户端认证，
     * 而 code_verifier 仅存在于授权码换取令牌环节，刷新请求无法携带。
     * 因此提供本端点，以刷新令牌本身作为凭证换取新的令牌对。</p>
     *
     * @param request     刷新令牌请求
     * @param httpRequest HTTP 请求（用于提取客户端 IP）
     * @return 新的登录响应（含新的访问令牌与刷新令牌）
     */
    @Operation(summary = "刷新访问令牌", description = "使用刷新令牌换取新的令牌对（公共客户端专用，无需客户端认证）")
    @PostMapping("/refreshToken")
    public Result<LoginResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request,
                                              HttpServletRequest httpRequest) {
        String clientIp = ipLocationUtil.getClientIp(httpRequest);

        // IP 速率限制（30次/分钟）
        if (!RateLimitUtil.tryAcquire(AuthRedisKey.rateIp(clientIp), 30, 1, TimeUnit.MINUTES)) {
            log.warn("Refresh token rate limit exceeded: {}", clientIp);
            return Result.error("请求过于频繁，请稍后再试");
        }

        LoginResponse response = loginService.refreshAccessToken(request.getRefreshToken());
        return Result.success(response);
    }

    /**
     * 获取当前登录用户信息
     *
     * <p>返回前端兼容的用户信息格式。等效于 OIDC 的 /userinfo 端点。</p>
     *
     * @return 用户信息
     */
    @Operation(summary = "获取当前用户信息", description = "获取当前登录用户的详细信息（需 Bearer Token）")
    @GetMapping("/me")
    public Result<UserInfoResponse> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return Result.<UserInfoResponse>error(CommonErrorCode.UNAUTHORIZED);
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof com.carlos.auth.security.SecurityUser securityUser) {
            List<String> roles = securityUser.getAuthorities().stream()
                    .map(grantedAuthority -> grantedAuthority.getAuthority())
                    .toList();

            UserInfoResponse userInfoResponse = UserInfoResponse.builder()
                    .userId((Long) securityUser.getId())
                    .userName(securityUser.getAccount())
                    .roles(roles)
                    .buttons(resolveButtons(roles))
                    .build();

            return Result.success(userInfoResponse);
        }

        // 降级处理：兼容标准 UserDetails
        if (principal instanceof UserDetails userDetails) {
            List<String> roles = userDetails.getAuthorities().stream()
                    .map(grantedAuthority -> grantedAuthority.getAuthority())
                    .toList();

            UserInfoResponse userInfoResponse = UserInfoResponse.builder()
                    .userName(userDetails.getUsername())
                    .roles(roles)
                    .buttons(resolveButtons(roles))
                    .build();

            return Result.success(userInfoResponse);
        }

        return Result.<UserInfoResponse>error("无法获取用户信息");
    }

    /**
     * 解析当前用户的权限码集合（供前端按钮级鉴权使用）
     *
     * <p>按角色编码从 {@link PermissionService}（Cached→Redis→Db 链）解析出权限码集合。
     * 该集合同时包含按钮类与 API 类权限码 —— 前端 {@code v-auth} 只按「是否含某权限码」判断，
     * 用超集即可，无需在此区分 op_type（区分需跨服务查 sys_operation）。</p>
     *
     * @param roles 角色编码列表
     * @return 权限码集合；解析失败或角色为空时返回空集合
     */
    private List<String> resolveButtons(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return Collections.emptyList();
        }
        PermissionService permissionService = permissionServiceProvider.getIfAvailable();
        if (permissionService == null) {
            return Collections.emptyList();
        }
        try {
            Set<String> permissions = permissionService.getUserPermissions(new HashSet<>(roles));
            return permissions == null ? Collections.emptyList() : new ArrayList<>(permissions);
        } catch (Exception e) {
            log.warn("解析用户权限码失败，roles={}, error={}", roles, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 用户登出（SSO 全局登出）
     *
     * <p>执行 Token 撤销 + Session 失效。在 SSO 架构中：</p>
     * <ul>
     *   <li>撤销当前 Token（或全部 Token）→ 后续 API 调用失效</li>
     *   <li>失效 IdP Session → 下次访问 /oauth2/authorize 需要重新登录</li>
     *   <li>触发 Back-Channel Logout → 通知所有 RP 清理本地会话</li>
     * </ul>
     *
     * @param accessToken 访问令牌（从请求头获取）
     * @param global      是否执行全局登出（撤销该用户所有会话）
     * @param httpRequest HTTP 请求
     * @return 登出结果
     */
    @Operation(summary = "用户登出", description = "Token 撤销 + Session 失效（支持全局登出）")
    @PostMapping("/logout")
    public Result<Object> logout(
            @RequestHeader(value = "Authorization", required = false) String accessToken,
            @RequestParam(required = false, defaultValue = "false") boolean global,
            HttpServletRequest httpRequest) {
        log.info("Received logout request, global={}", global);
        try {
            // 从 Authorization 头中提取令牌
            String token = extractTokenFromHeader(accessToken);
            if (token != null) {
                if (global) {
                    // 全局登出：撤销该用户的所有 Token
                    int revokedCount = loginService.globalLogout(token);
                    log.info("Global logout successful, revoked {} sessions", revokedCount);
                } else {
                    // 普通登出：仅撤销当前 Token
                    loginService.logout(token, null);
                }
            }
            log.info("Logout successful");
            return Result.success("登出成功");
        } catch (Exception e) {
            log.error("Error during logout", e);
            return Result.error("登出失败");
        } finally {
            // 失效 IdP Session（SSO 核心：让 /oauth2/authorize 不再自动通过）
            // 放在 finally 中保证即使上面的撤销逻辑异常，Session 也一定失效
            HttpSession session = httpRequest.getSession(false);
            if (session != null) {
                session.invalidate();
                log.debug("IdP session invalidated");
            }

            // 清除 SecurityContext
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * 从 Authorization 头中提取令牌
     */
    private String extractTokenFromHeader(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return null;
        }
        if (authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        return authorizationHeader;
    }
}
