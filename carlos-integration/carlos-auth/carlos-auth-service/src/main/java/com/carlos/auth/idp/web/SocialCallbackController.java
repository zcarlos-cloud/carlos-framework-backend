package com.carlos.auth.idp.web;

import cn.hutool.core.util.StrUtil;
import com.carlos.auth.config.OAuth2Properties;
import com.carlos.auth.constant.AuthRedisKey;
import com.carlos.auth.idp.IdentityProvider;
import com.carlos.auth.idp.IdentityProviderRegistry;
import com.carlos.auth.idp.UserIdentity;
import com.carlos.core.response.Result;
import com.carlos.redis.util.RedisUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 第三方 OAuth2 回调控制器
 *
 * <p>处理钉钉、微信等第三方平台的 OAuth2 授权回调，同时提供授权 URL 生成接口。</p>
 *
 * <p><strong>流程说明：</strong></p>
 * <ol>
 *   <li>前端调用 {@code GET /auth/oauth2/authorize?provider={provider}} 获取授权 URL</li>
 *   <li>前端引导用户跳转至授权 URL</li>
 *   <li>第三方平台授权后，回调到 {@code GET /auth/oauth2/callback?provider={provider}}</li>
 *   <li>后端用授权码换取用户信息，生成 JWT，重定向回前端</li>
 * </ol>
 *
 * @author Carlos
 * @version 3.0.0
 * @since 2026-05-19
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/auth/oauth2")
@Tag(name = "第三方登录", description = "钉钉、微信等 OAuth2 登录授权与回调")
public class SocialCallbackController {

    private static final Set<String> ALLOWED_PROVIDERS = Set.of("dingtalk", "wechat");
    private static final String CALLBACK_PATH = "/social-callback";
    private static final long STATE_TTL_SECONDS = 300; // 5 min

    private final IdentityProviderRegistry providerRegistry;
    private final OAuth2Properties oAuth2Properties;

    /**
     * 获取第三方授权 URL
     *
     * @param provider    身份源标识：dingtalk / wechat
     * @param redirectUri 前端最终接收 token 的回调地址（可选，默认使用配置）
     * @param state       防 CSRF 状态值（可选，默认自动生成）
     * @return 授权 URL
     */
    @Operation(summary = "获取第三方授权 URL", description = "传入 provider 类型，返回对应平台的 OAuth2 授权链接")
    @GetMapping("/authorize")
    public Result<String> buildAuthorizeUrl(
        @Parameter(description = "身份源：dingtalk / wechat") @RequestParam @NotBlank(message = "身份源类型不能为空") @Size(max = 32, message = "身份源类型长度不能超过32") String provider,
        @RequestParam(required = false) String redirectUri,
        @RequestParam(required = false) String state) {

        if (!ALLOWED_PROVIDERS.contains(provider)) {
            return Result.error("不支持的第三方登录类型: " + provider);
        }

        IdentityProvider identityProvider = providerRegistry.findByProviderId(provider);
        if (identityProvider == null) {
            return Result.error("第三方登录服务未配置: " + provider);
        }

        String callbackBaseUrl = oAuth2Properties.getLogin().getMethods().getSocial().getCallbackBaseUrl();
        if (StrUtil.isBlank(callbackBaseUrl)) {
            return Result.error("未配置社交登录回调基础地址 (carlos.auth.login.methods.social.callback-base-url)");
        }

        // 构建后端回调地址
        String backendCallback = callbackBaseUrl + "/auth/oauth2/callback";

        // 生成带时间戳的 state，并存入 Redis 防 CSRF
        String finalState = StrUtil.isNotBlank(state) ? state : generateState();

        // 将 state 存入 Redis，回调时校验（5 min TTL）
        String stateKey = AuthRedisKey.socialState(finalState);
        RedisUtil.setValue(stateKey, provider, STATE_TTL_SECONDS, TimeUnit.SECONDS);

        String authorizeUrl = identityProvider.buildAuthorizeUrl(finalState, backendCallback);
        log.info("Built authorize URL for provider={}, state={}", provider, finalState);

        return Result.success(authorizeUrl);
    }

    /**
     * 处理第三方 OAuth2 回调
     *
     * <p>接收第三方平台的授权码，换取用户信息后重定向回前端。</p>
     *
     * @param provider 身份源标识：dingtalk / wechat
     * @param code     授权码
     * @param state    状态值
     * @param request  HTTP 请求
     * @param response HTTP 响应
     */
    @Operation(summary = "第三方 OAuth2 回调", description = "钉钉/微信授权后回调到此端点")
    @GetMapping("/callback")
    public void handleCallback(
        @Parameter(description = "身份源：dingtalk / wechat") @RequestParam @NotBlank(message = "身份源类型不能为空") @Size(max = 32, message = "身份源类型长度不能超过32") String provider,
        @RequestParam @NotBlank(message = "授权码不能为空") @Size(max = 512, message = "授权码长度不能超过512") String code,
        @RequestParam(required = false) String state,
        HttpServletRequest request,
        HttpServletResponse response) throws IOException {

        log.info("Received OAuth2 callback: provider={}, state={}, remoteAddr={}",
            provider, state, request.getRemoteAddr());

        // 校验 state（防 CSRF）：从 Redis 获取 + 原子删除
        if (StrUtil.isNotBlank(state)) {
            String stateKey = AuthRedisKey.socialState(state);
            String storedProvider = RedisUtil.getValue(stateKey);
            RedisUtil.delete(stateKey);

            if (storedProvider == null) {
                log.warn("Invalid or expired OAuth2 state: provider={}, state={}", provider, state);
                redirectWithError(response, state, "无效的授权请求或已过期");
                return;
            }
            if (!storedProvider.equals(provider)) {
                log.warn("Provider mismatch in OAuth2 state: expected={}, actual={}", storedProvider, provider);
                redirectWithError(response, state, "授权请求参数不匹配");
                return;
            }
        }

        if (!ALLOWED_PROVIDERS.contains(provider)) {
            log.error("Unknown provider in callback: {}", provider);
            redirectWithError(response, state, "不支持的登录类型");
            return;
        }

        IdentityProvider identityProvider = providerRegistry.findByProviderId(provider);
        if (identityProvider == null) {
            log.error("Provider not configured: {}", provider);
            redirectWithError(response, state, "第三方登录服务未配置");
            return;
        }

        try {
            Map<String, String> callbackParams = new HashMap<>();
            callbackParams.put("code", code);
            if (StrUtil.isNotBlank(state)) {
                callbackParams.put("state", state);
            }

            UserIdentity userIdentity = identityProvider.handleCallback(callbackParams);

            // TODO: 此处应调用登录服务生成 JWT Token
            // 当前方案：将用户信息编码后重定向回前端，由前端再调用 /oauth2/token 完成登录
            // 生产环境建议改为后端直接生成 Token 后重定向

            String frontendUrl = buildFrontendRedirectUrl(userIdentity, state);
            log.info("OAuth2 callback success, redirecting to frontend: provider={}, isNewUser={}",
                userIdentity.getProviderId(), userIdentity.isNewUser());

            response.sendRedirect(frontendUrl);

        } catch (BadCredentialsException e) {
            log.warn("OAuth2 callback auth failed: provider={}, error={}", provider, e.getMessage());
            redirectWithError(response, state, "第三方登录认证失败");
        } catch (Exception e) {
            log.error("OAuth2 callback processing failed: provider={}", provider, e);
            redirectWithError(response, state, "登录处理失败，请稍后重试");
        }
    }

    /**
     * 生成防 CSRF 的 state 参数
     *
     * <p>格式: {@code UUID}，写入 Redis（5 min TTL），回调时校验 + 删除。</p>
     */
    private String generateState() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 构建前端重定向 URL
     *
     * <p>仅传递最小必要信息（provider、isNewUser、state），敏感信息不通过 URL 暴露。
     * 前端收到回调后，应使用 social grant_type 调用 /oauth2/token 换取正式 Token。</p>
     */
    private String buildFrontendRedirectUrl(UserIdentity identity, String state) {
        String callbackBaseUrl = oAuth2Properties.getLogin().getMethods().getSocial().getCallbackBaseUrl();
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(callbackBaseUrl)
            .path(CALLBACK_PATH)
            .queryParam("provider", identity.getProviderId())
            .queryParam("isNewUser", identity.isNewUser());
        if (StrUtil.isNotBlank(state)) {
            builder.queryParam("state", state);
        }
        return builder.toUriString();
    }

    private void redirectWithError(HttpServletResponse response, String state, String errorMsg) throws IOException {
        String callbackBaseUrl = oAuth2Properties.getLogin().getMethods().getSocial().getCallbackBaseUrl();
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(callbackBaseUrl)
            .path(CALLBACK_PATH)
            .queryParam("error", errorMsg);
        if (StrUtil.isNotBlank(state)) {
            builder.queryParam("state", state);
        }
        response.sendRedirect(builder.toUriString());
    }
}
