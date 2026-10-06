package com.carlos.auth.log;

import com.carlos.auth.audit.AuditLogPublisher;
import com.carlos.audit.api.pojo.param.ApiAuditLogMainParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * <p>
 * 登录审计日志异步发送器
 * </p>
 *
 * <p>独立的 Bean，使 {@link Async} 注解通过代理生效（同类自调用不经过代理，注解无效）。
 * 参数必须由调用方在请求线程内构建完成后再传入——异步线程中
 * {@code HttpServletRequest} 代理与 {@code RequestContextHolder} 均不可用。</p>
 *
 * @author carlos
 * @since 3.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginAuditLogSender {

    private final AuditLogPublisher auditLogPublisher;

    /**
     * 异步发送登录审计日志
     *
     * @param param 已构建完成的审计日志参数
     */
    @Async("virtualTaskExecutor")
    public void send(ApiAuditLogMainParam param) {
        try {
            auditLogPublisher.publish(param);
            log.debug("Login audit log sent: user={}, type={}, status={}",
                param.getPrincipalName(), param.getLogType(), param.getLogStatus());
        } catch (Exception e) {
            log.error("Failed to send login audit log", e);
        }
    }
}
