package com.carlos.monitor.audit;

import cn.hutool.core.util.StrUtil;
import com.carlos.audit.api.constant.AuditMqTopics;
import com.carlos.audit.api.pojo.enums.AuditLogCategoryEnum;
import com.carlos.audit.api.pojo.enums.AuditLogPrincipalTypeEnum;
import com.carlos.audit.api.pojo.enums.AuditLogStatusEnum;
import com.carlos.audit.api.pojo.param.ApiAuditLogMainParam;
import com.carlos.mq.core.MqSendOptions;
import com.carlos.mq.core.MqTemplate;
import com.carlos.mq.core.SendResult;
import com.carlos.security.auth.UserContext;
import com.carlos.security.util.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * <p>
 * 监控操作审计上报器（经 audit-api MQ 通道异步上报 carlos-audit）
 * 契约（主题 {@link AuditMqTopics#AUDIT_LOG} + {@link ApiAuditLogMainParam}）来自 audit-api，
 * 不跨模块依赖 audit-bus；上报失败仅记日志，绝不阻断主流程
 * </p>
 *
 * @author Carlos
 * @date 2026年10月3日
 */
@Slf4j
@Component
public class MonitorAuditReporter {

    /**
     * 审计事件
     */
    public record AuditEvent(String action, String targetId, String targetSnapshot,
                             boolean success, String resultMessage, long durationMs,
                             String clientIp, int riskLevel) {
    }

    private final ObjectProvider<MqTemplate> mqTemplateProvider;

    private ExecutorService reportExecutor;

    public MonitorAuditReporter(ObjectProvider<MqTemplate> mqTemplateProvider) {
        this.mqTemplateProvider = mqTemplateProvider;
    }

    @PostConstruct
    public void init() {
        reportExecutor = Executors.newVirtualThreadPerTaskExecutor();
    }

    @PreDestroy
    public void destroy() {
        if (reportExecutor != null) {
            reportExecutor.shutdownNow();
        }
    }

    /**
     * 异步上报审计事件（任何异常只记日志）
     */
    public void report(AuditEvent event) {
        try {
            reportExecutor.submit(() -> doReport(event));
        } catch (Exception e) {
            log.warn("审计上报任务提交失败（已容忍）, action:{}, error:{}", event.action(), e.getMessage());
        }
    }

    private void doReport(AuditEvent event) {
        try {
            MqTemplate mqTemplate = mqTemplateProvider.getIfAvailable();
            if (mqTemplate == null) {
                log.debug("MQ 不可用，审计事件仅落应用日志: action={}, target={}", event.action(), event.targetId());
                return;
            }
            ApiAuditLogMainParam param = buildParam(event);
            SendResult result = mqTemplate.send(AuditMqTopics.AUDIT_LOG, param,
                MqSendOptions.ofKey(event.targetId()));
            if (result == null || !result.isSuccess()) {
                log.warn("审计日志投递未成功（可容忍）：action={}, target={}, status={}",
                    event.action(), event.targetId(), result == null ? null : result.getStatus());
            }
        } catch (Exception e) {
            log.error("审计日志 MQ 投递异常（已降级为丢弃）: action={}", event.action(), e);
        }
    }

    private ApiAuditLogMainParam buildParam(AuditEvent event) {
        ApiAuditLogMainParam param = new ApiAuditLogMainParam();
        LocalDateTime now = LocalDateTime.now();
        param.setServerTime(now);
        param.setEventTime(now);
        param.setEventDate(LocalDate.now());
        param.setLogSchemaVersion(3);
        param.setCategory(AuditLogCategoryEnum.SECURITY);
        param.setLogType(event.action());
        param.setRiskLevel(event.riskLevel());
        param.setLogStatus(event.success() ? AuditLogStatusEnum.SUCCESS : AuditLogStatusEnum.FAIL);
        param.setDurationMs((int) Math.min(event.durationMs(), Integer.MAX_VALUE));
        param.setResultMessage(StrUtil.maxLength(event.resultMessage(), 500));
        param.setOperation("监控数据管理操作：" + event.action());
        param.setAppName("carlos-monitor");

        UserContext user = SecurityUtils.getCurrentUser();
        if (user != null) {
            param.setPrincipalId(String.valueOf(user.getUserId()));
            param.setPrincipalName(user.getUsername());
            param.setPrincipalType(AuditLogPrincipalTypeEnum.USER);
        } else {
            param.setPrincipalId("");
            param.setPrincipalType(AuditLogPrincipalTypeEnum.ANONYMOUS);
        }
        param.setTargetType("REDIS_INSTANCE");
        param.setTargetId(event.targetId());
        param.setTargetSnapshot(event.targetSnapshot());
        param.setClientIp(event.clientIp());
        return param;
    }
}
