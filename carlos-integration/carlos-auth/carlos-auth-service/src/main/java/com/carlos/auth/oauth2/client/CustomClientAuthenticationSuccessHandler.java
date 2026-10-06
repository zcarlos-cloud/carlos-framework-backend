package com.carlos.auth.oauth2.client;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import com.carlos.boot.web.util.ResponseWriter;
import com.carlos.core.response.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 客户端认证成功处理器
 *
 * <p>处理 OAuth2 客户端认证成功后的逻辑，记录日志并返回认证成功的客户端信息。</p>
 *
 * <h3>响应内容：</h3>
 * <ul>
 *   <li>clientId - 客户端ID</li>
 *   <li>clientName - 客户端名称</li>
 *   <li>authenticationMethod - 认证方法</li>
 *   <li>scopes - 授权范围</li>
 *   <li>authenticatedAt - 认证时间</li>
 * </ul>
 *
 * <h3>使用场景：</h3>
 * <p>当客户端使用 client_credentials 或其他需要客户端认证的方式成功认证后，
 * 此处理器会被触发，记录认证成功的日志并返回客户端信息。</p>
 *
 * @author Carlos
 * @version 3.0.0
 * @since 2026-04-08
 * @see CustomClientAuthenticationFailureHandler
 * @see org.springframework.security.web.authentication.AuthenticationSuccessHandler
 */
@Slf4j
public class CustomClientAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    /**
     * 时间格式化器
     */
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 处理客户端认证成功
     *
     * @param request        HTTP 请求
     * @param response       HTTP 响应
     * @param authentication 认证信息
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) {
        OAuth2ClientAuthenticationToken clientAuth = (OAuth2ClientAuthenticationToken) authentication;
        String clientId = clientAuth.getPrincipal().toString();

        log.info("Client authentication successful: clientId={}, method={}, time={}",
            clientId,
            clientAuth.getClientAuthenticationMethod(),
            DateUtil.format(LocalDateTime.now(), DatePattern.NORM_DATETIME_PATTERN));

        // 构建响应数据
        Map<String, Object> responseData = buildSuccessResponse(clientAuth);

        // 输出 JSON 响应
        ResponseWriter.writeJson(response, Result.success(responseData));
    }

    /**
     * 构建成功响应数据
     *
     * @param clientAuth 客户端认证令牌
     * @return 响应数据映射
     */
    private Map<String, Object> buildSuccessResponse(OAuth2ClientAuthenticationToken clientAuth) {
        Map<String, Object> data = new HashMap<>();

        // 基本信息
        data.put("clientId", clientAuth.getPrincipal());
        data.put("authenticationMethod", clientAuth.getClientAuthenticationMethod().getValue());
        data.put("authenticatedAt", LocalDateTime.now().format(DATE_TIME_FORMATTER));

        // 注册客户端信息（如果可用）
        if (clientAuth.getRegisteredClient() != null) {
            var registeredClient = clientAuth.getRegisteredClient();
            data.put("clientName", registeredClient.getClientName());

            // 授权范围
            if (registeredClient.getScopes() != null && !registeredClient.getScopes().isEmpty()) {
                data.put("scopes", String.join(" ", registeredClient.getScopes()));
            }

            // 授权类型
            if (registeredClient.getAuthorizationGrantTypes() != null) {
                data.put("grantTypes", registeredClient.getAuthorizationGrantTypes().stream()
                    .map(type -> type.getValue())
                    .collect(Collectors.toList()));
            }
        }

        return data;
    }

}
