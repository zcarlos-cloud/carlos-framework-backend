package com.carlos.auth.oauth2;

import cn.hutool.json.JSONUtil;
import com.carlos.core.response.CommonErrorCode;
import com.carlos.core.response.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * <p>
 * 未认证端点处理器
 * </p>
 *
 * <p>处理未登录或 Token 过期的情况，智能区分浏览器请求和 API 请求：</p>
 * <ul>
 *   <li><strong>浏览器请求</strong>（Accept 包含 text/html）：重定向到登录页 {@code /login}</li>
 *   <li><strong>API 请求</strong>：返回 401 JSON 响应</li>
 * </ul>
 *
 * <p>此设计确保 OAuth2 授权码模式下，浏览器访问授权端点未认证时能正确跳转到登录页，
 * 同时保持 API 端点的统一 JSON 错误响应格式。</p>
 *
 * @author carlos
 * @version 3.0.0
 * @since 2026-04-08
 */
@Slf4j
public class UnauthorizedEntryPoint implements AuthenticationEntryPoint {

    /**
     * 登录页路径
     */
    private static final String LOGIN_PAGE = "/login";

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        log.debug("Unauthorized request: {} {}", request.getMethod(), request.getRequestURI());

        // 判断是否为浏览器请求
        if (isBrowserRequest(request)) {
            // 浏览器请求：重定向到登录页
            log.debug("Browser request detected, redirecting to {}", LOGIN_PAGE);
            response.sendRedirect(LOGIN_PAGE);
            return;
        }

        // API 请求：返回 JSON 401
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(JSONUtil.toJsonStr(Result.error(CommonErrorCode.UNAUTHORIZED)));
    }

    /**
     * 判断是否为浏览器请求
     *
     * <p>综合判断逻辑（优先级从高到低）：</p>
     * <ol>
     *   <li>如果请求路径以 {@code /auth/} 开头 → 强制视为 API 请求</li>
     *   <li>如果包含 {@code X-Requested-With: XMLHttpRequest} → 视为 API 请求</li>
     *   <li>如果 Accept 头包含 {@code text/html} → 视为浏览器请求</li>
     *   <li>否则视为 API 请求</li>
     * </ol>
     *
     * @param request HTTP 请求
     * @return true-浏览器请求
     */
    private boolean isBrowserRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();

        // 1. 明确以 /auth/ 开头的路径强制视为 API（防止 AJAX 误标）
        if (uri != null && uri.startsWith("/auth/")) {
            return false;
        }

        // 2. 如果包含 X-Requested-With: XMLHttpRequest，视为 API 请求
        String requestedWith = request.getHeader("X-Requested-With");
        if ("XMLHttpRequest".equalsIgnoreCase(requestedWith)) {
            return false;
        }

        // 3. 根据 Accept 头判断
        String accept = request.getHeader("Accept");
        if (accept == null) {
            return false;
        }
        return accept.contains(MediaType.TEXT_HTML_VALUE);
    }
}
