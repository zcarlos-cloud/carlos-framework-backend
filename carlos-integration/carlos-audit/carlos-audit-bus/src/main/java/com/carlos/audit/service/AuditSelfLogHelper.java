package com.carlos.audit.service;

import cn.hutool.core.util.StrUtil;
import com.carlos.boot.web.request.RequestInfo;
import com.carlos.boot.web.request.WebRequestContext;
import com.carlos.json.JsonUtils;
import com.carlos.log.entity.OperationLog;
import com.carlos.security.auth.UserContext;
import com.carlos.security.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 审计模块自审计日志构造器
 *
 * <p>用于在审计配置、归档等内部操作成功后，生成 {@link OperationLog} 供 {@link com.carlos.audit.storage.AuditLogStorage} 记录</p>
 *
 * @author Carlos
 * @date 2026-06-12
 */
@Slf4j
public final class AuditSelfLogHelper {

    private static final String CATEGORY_AUDIT = "AUDIT";
    private static final String PRINCIPAL_TYPE_SYSTEM = "SYSTEM";
    private static final String PRINCIPAL_TYPE_USER = "USER";
    private static final String LOG_STATUS_SUCCESS = "SUCCESS";

    private AuditSelfLogHelper() {
    }

    /**
     * 构造审计模块自审计日志
     *
     * @param logType        日志类型，如 AUDIT_CONFIG_CHANGE
     * @param operation      操作描述
     * @param targetType     目标对象类型，如 AUDIT_CONFIG
     * @param targetId       目标对象标识
     * @param targetName     目标对象名称
     * @param targetSnapshot 目标对象快照（DTO）
     * @param tenantId       租户ID
     * @return OperationLog
     */
    public static OperationLog build(String logType, String operation, String targetType,
                                     String targetId, String targetName, Object targetSnapshot, String tenantId) {
        OperationLog operationLog = new OperationLog();
        LocalDateTime now = LocalDateTime.now();
        operationLog.setServerTime(now);
        operationLog.setEventDate(LocalDate.now());
        operationLog.setEventTime(now);
        operationLog.setCategory(CATEGORY_AUDIT);
        operationLog.setLogType(logType);
        operationLog.setOperation(operation);
        operationLog.setRiskLevel(30);
        operationLog.setLogStatus(LOG_STATUS_SUCCESS);
        operationLog.setTargetType(targetType);
        operationLog.setTargetId(targetId);
        operationLog.setTargetName(targetName);
        operationLog.setTargetSnapshot(toJsonSnapshot(targetSnapshot));
        operationLog.setTenantId(StrUtil.blankToDefault(tenantId, "0"));
        fillPrincipal(operationLog);
        fillRequestContext(operationLog);
        return operationLog;
    }

    private static void fillPrincipal(OperationLog operationLog) {
        try {
            UserContext ctx = SecurityUtils.getCurrentUser();
            if (ctx != null) {
                operationLog.setPrincipalId(ctx.getUserId() != null ? ctx.getUserId().toString() : "system");
                operationLog.setPrincipalName(StrUtil.blankToDefault(ctx.getUsername(), "system"));
                operationLog.setPrincipalType(PRINCIPAL_TYPE_USER);
                if (ctx.getTenantId() != null) {
                    operationLog.setTenantId(ctx.getTenantId().toString());
                }
                if (ctx.getDeptId() != null) {
                    operationLog.setDeptId(ctx.getDeptId().toString());
                }
                return;
            }
        } catch (Exception e) {
            log.debug("获取当前请求用户上下文失败，使用系统账号: {}", e.getMessage());
        }
        operationLog.setPrincipalId("system");
        operationLog.setPrincipalName("system");
        operationLog.setPrincipalType(PRINCIPAL_TYPE_SYSTEM);
    }

    private static void fillRequestContext(OperationLog operationLog) {
        try {
            RequestInfo requestInfo = WebRequestContext.getRequestInfo();
            if (requestInfo != null) {
                operationLog.setClientIp(requestInfo.getIp());
                operationLog.setTraceId(requestInfo.getRequestId());
            }
            // 从请求头获取应用名称
            HttpServletRequest request = WebRequestContext.getCurrentRequest();
            String appName = request.getHeader("X-App-Name");
            if (StrUtil.isNotBlank(appName)) {
                operationLog.setAppName(appName);
            }
        } catch (Exception e) {
            log.debug("获取当前请求上下文失败: {}", e.getMessage());
        }
    }

    private static String toJsonSnapshot(Object snapshot) {
        if (snapshot == null) {
            return null;
        }
        try {
            return JsonUtils.toJson(snapshot);
        } catch (Exception e) {
            log.warn("自审计快照序列化失败: {}", e.getMessage());
            return String.valueOf(snapshot);
        }
    }
}
