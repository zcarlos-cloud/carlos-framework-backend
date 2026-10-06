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
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.RequestCache;

import java.io.IOException;

/**
 * 表单登录成功处理器（SSO 链路专用）
 *
 * <p>基于 {@link SavedRequestAwareAuthenticationSuccessHandler}，认证成功后：</p>
 * <ol>
 *   <li>记录审计日志</li>
 *   <li>从 RequestCache 取出原始请求（通常是 /oauth2/authorize）</li>
 *   <li>302 重定向回原始请求，继续授权码流程</li>
 * </ol>
 *
 * <p>这是实现真正 SSO 的关键：表单登录成功后会在 auth 域种下 Session Cookie，
 * 后续访问其他系统的 /oauth2/authorize 时无需再次登录。</p>
 *
 * @author Carlos
 * @version 3.0.0
 * @since 2026-07-01
 */
@Slf4j
public class FormLoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final AuditLogUtil auditLogUtil;
    private final AuditLogPublisher auditLogPublisher;

    public FormLoginSuccessHandler(AuditLogUtil auditLogUtil, AuditLogPublisher auditLogPublisher,
                                   RequestCache requestCache) {
        this.auditLogUtil = auditLogUtil;
        this.auditLogPublisher = auditLogPublisher;
        setRequestCache(requestCache);
        setDefaultTargetUrl("/");
        setAlwaysUseDefaultTargetUrl(false);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, jakarta.servlet.ServletException {
        String principalName = authentication.getName();
        log.info("表单登录成功（SSO）: {}", principalName);

        // 记录审计日志（异步，失败不影响登录）
        try {
            ApiAuditLogMainParam param = auditLogUtil.buildBaseParam();
            param.setCategory(AuditLogCategoryEnum.SECURITY);
            param.setLogType("USER_LOGIN");
            param.setOperation("用户表单登录成功（SSO）");
            // principalId 为 @NotBlank 必填：SSO 表单登录未加载用户ID，用用户名兜底
            param.setPrincipalId(principalName);
            param.setPrincipalName(principalName);
            param.setPrincipalType(AuditLogPrincipalTypeEnum.USER);
            param.setLogStatus(AuditLogStatusEnum.SUCCESS);
            param.setResultCode("00000");
            param.setAuthType(AuditLogAuthTypeEnum.PASSWORD);
            auditLogUtil.populateIpAndLocation(param, request);
            auditLogPublisher.publish(param);
        } catch (Exception e) {
            log.warn("记录表单登录审计日志失败: principalName:{}", principalName, e);
        }

        // 委托父类：从 RequestCache 恢复原始请求（/oauth2/authorize）并重定向
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
