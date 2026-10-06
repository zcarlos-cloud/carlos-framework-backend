package com.carlos.auth.security.handle;

import com.carlos.auth.audit.AuditLogPublisher;
import com.carlos.audit.api.pojo.enums.AuditLogCategoryEnum;
import com.carlos.audit.api.pojo.enums.AuditLogPrincipalTypeEnum;
import com.carlos.audit.api.pojo.enums.AuditLogStatusEnum;
import com.carlos.audit.api.pojo.param.ApiAuditLogMainParam;
import com.carlos.auth.util.AuditLogUtil;
import com.carlos.boot.web.util.ResponseWriter;
import com.carlos.core.auth.Oauth2TokenDTO;
import com.carlos.core.response.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;
import java.time.temporal.ChronoUnit;

/**
 * <p>
 * 自定义表单登录成功处理器
 * </p>
 *
 * <p>处理表单登录成功后的响应，包括：</p>
 * <ul>
 *   <li>记录登录成功日志</li>
 *   <li>设置安全上下文</li>
 *   <li>封装并返回 Token 信息</li>
 *   <li>清理安全上下文（无状态设计）</li>
 * </ul>
 *
 * <p>适用场景：传统表单登录（/login）成功后的处理</p>
 *
 * @author carlos
 * @date 2022/11/4
 */
@Slf4j
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final AuditLogUtil auditLogUtil;
    private final AuditLogPublisher auditLogPublisher;

    public CustomAuthenticationSuccessHandler(AuditLogUtil auditLogUtil, AuditLogPublisher auditLogPublisher) {
        this.auditLogUtil = auditLogUtil;
        this.auditLogPublisher = auditLogPublisher;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) {
        String principalName = authentication.getName();
        log.info("用户登录成功: {}", principalName);

        // 记录登录成功审计日志（通过 carlos-audit-api 发送，auth 模块不存储）
        recordLoginSuccessAudit(request, principalName);

        // 设置安全上下文
        SecurityContextHolder.getContext().setAuthentication(authentication);

        try {
            sendAccessTokenResponse(request, response, authentication);
        } catch (IOException e) {
            log.error("返回登录成功响应失败, 用户: {}", principalName, e);
        }
    }

    /**
     * 记录登录成功审计日志
     *
     * @param request       HTTP 请求
     * @param principalName 主体名称
     */
    private void recordLoginSuccessAudit(HttpServletRequest request, String principalName) {
        try {
            ApiAuditLogMainParam param = auditLogUtil.buildBaseParam();
            param.setCategory(AuditLogCategoryEnum.SECURITY);
            param.setLogType("USER_LOGIN");
            param.setOperation("用户登录成功");
            param.setPrincipalId(principalName);
            param.setPrincipalName(principalName);
            param.setPrincipalType(AuditLogPrincipalTypeEnum.USER);
            param.setLogStatus(AuditLogStatusEnum.SUCCESS);
            param.setResultCode("00000");
            auditLogUtil.populateIpAndLocation(param, request);
            auditLogPublisher.publish(param);
            log.debug("登录成功审计日志已发送: {}", principalName);
        } catch (Exception e) {
            log.error("发送登录成功审计日志失败: {}", principalName, e);
        }
    }

    /**
     * 发送访问令牌响应
     *
     * <p>将 OAuth2 令牌信息封装为统一的响应格式返回给客户端</p>
     *
     * @param request        HTTP 请求
     * @param response       HTTP 响应
     * @param authentication 认证对象（必须是 OAuth2AccessTokenAuthenticationToken）
     * @throws IOException 写入响应时发生 IO 异常
     */
    private void sendAccessTokenResponse(HttpServletRequest request, HttpServletResponse response,
                                         Authentication authentication) throws IOException {
        OAuth2AccessTokenAuthenticationToken accessTokenAuthentication =
            (OAuth2AccessTokenAuthenticationToken) authentication;

        OAuth2AccessToken accessToken = accessTokenAuthentication.getAccessToken();
        OAuth2RefreshToken refreshToken = accessTokenAuthentication.getRefreshToken();

        // 构建 Token DTO
        Oauth2TokenDTO tokenInfo = buildTokenDto(accessToken, refreshToken);

        // 无状态设计：清理上下文信息
        SecurityContextHolder.clearContext();

        // 返回 JSON 响应
        ResponseWriter.writeJson(response, Result.success(tokenInfo));
    }

    /**
     * 构建 Token DTO
     *
     * @param accessToken   访问令牌
     * @param refreshToken  刷新令牌（可能为 null）
     * @return Token DTO
     */
    private Oauth2TokenDTO buildTokenDto(OAuth2AccessToken accessToken, OAuth2RefreshToken refreshToken) {
        Oauth2TokenDTO tokenInfo = new Oauth2TokenDTO();
        tokenInfo.setToken(accessToken.getTokenValue());
        tokenInfo.setTokenType(accessToken.getTokenType().getValue());

        if (refreshToken != null) {
            tokenInfo.setRefreshToken(refreshToken.getTokenValue());
        }

        if (accessToken.getIssuedAt() != null && accessToken.getExpiresAt() != null) {
            tokenInfo.setExpiresIn(ChronoUnit.SECONDS.between(
                accessToken.getIssuedAt(), accessToken.getExpiresAt()));
        }

        return tokenInfo;
    }
}
