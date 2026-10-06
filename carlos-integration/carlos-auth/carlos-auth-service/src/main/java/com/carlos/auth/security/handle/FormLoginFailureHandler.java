package com.carlos.auth.security.handle;

import com.carlos.auth.audit.AuditLogPublisher;
import com.carlos.audit.api.pojo.enums.AuditLogAuthTypeEnum;
import com.carlos.audit.api.pojo.enums.AuditLogCategoryEnum;
import com.carlos.audit.api.pojo.enums.AuditLogPrincipalTypeEnum;
import com.carlos.audit.api.pojo.enums.AuditLogStatusEnum;
import com.carlos.audit.api.pojo.param.ApiAuditLogMainParam;
import com.carlos.auth.util.AuditLogUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 表单登录失败处理器（SSO 链路专用）
 *
 * <p>认证失败后重定向回 /login?error，并记录审计日志。</p>
 *
 * @author Carlos
 * @version 3.0.0
 * @since 2026-07-01
 */
@Slf4j
@Component
public class FormLoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final AuditLogUtil auditLogUtil;
    private final AuditLogPublisher auditLogPublisher;

    public FormLoginFailureHandler(AuditLogUtil auditLogUtil, AuditLogPublisher auditLogPublisher) {
        this.auditLogUtil = auditLogUtil;
        this.auditLogPublisher = auditLogPublisher;
        setDefaultFailureUrl("/login?error");
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, jakarta.servlet.ServletException {
        String username = request.getParameter("username");
        log.warn("表单登录失败: {}, 原因: {}", username, exception.getMessage());

        // 记录审计日志
        try {
            ApiAuditLogMainParam param = auditLogUtil.buildBaseParam();
            param.setCategory(AuditLogCategoryEnum.SECURITY);
            param.setLogType("USER_LOGIN");
            param.setOperation("用户表单登录失败");
            // principalId 为 @NotBlank 必填：登录失败未加载用户ID，用用户名兜底
            param.setPrincipalId(username);
            param.setPrincipalName(username);
            param.setPrincipalType(AuditLogPrincipalTypeEnum.USER);
            param.setLogStatus(AuditLogStatusEnum.FAIL);
            param.setResultCode(extractErrorCode(exception));
            param.setResultMessage(exception.getMessage());
            param.setAuthType(AuditLogAuthTypeEnum.PASSWORD);
            auditLogUtil.populateIpAndLocation(param, request);
            auditLogPublisher.publish(param);
        } catch (Exception e) {
            log.warn("记录表单登录失败审计日志失败: {}", username, e);
        }

        super.onAuthenticationFailure(request, response, exception);
    }

    /**
     * 提取 OAuth2 错误码，非 OAuth2 异常时返回 null
     */
    private String extractErrorCode(AuthenticationException exception) {
        if (exception instanceof OAuth2AuthenticationException oauth2Exception) {
            OAuth2Error error = oauth2Exception.getError();
            return error != null ? error.getErrorCode() : null;
        }
        return null;
    }
}
