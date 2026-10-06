package com.carlos.security.config;

import cn.hutool.json.JSONUtil;
import com.carlos.core.response.CommonErrorCode;
import com.carlos.core.response.Result;
import jakarta.servlet.ServletException;
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
 * <p>处理未登录或 Token 过期的情况，返回 401 响应。</p>
 *
 * @author carlos
 * @version 3.0.0
 * @since 2026-04-08
 */
@Slf4j
public class UnauthorizedEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {

        log.debug("Unauthorized request: {} {}", request.getMethod(), request.getRequestURI());
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(JSONUtil.toJsonStr(Result.error(CommonErrorCode.UNAUTHORIZED)));
    }
}
