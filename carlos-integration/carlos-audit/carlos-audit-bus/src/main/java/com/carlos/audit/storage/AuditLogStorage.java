package com.carlos.audit.storage;

import com.carlos.audit.api.pojo.enums.*;
import com.carlos.audit.clickhouse.ClickHouseBatchWriter;
import com.carlos.audit.manager.AuditLogConfigManager;
import com.carlos.audit.pojo.dto.AuditLogConfigDTO;
import com.carlos.audit.pojo.dto.AuditLogMainDTO;
import com.carlos.audit.pojo.enums.AuditLogPayloadStorageTypeEnum;
import com.carlos.audit.util.SensitiveDataUtil;
import com.carlos.disruptor.core.DisruptorTemplate;
import com.carlos.log.entity.OperationLog;
import com.carlos.log.storage.LogStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 审计日志存储器
 * <p>
 * 实现 LogStorage 接口，将操作日志存储到审计系统（ClickHouse）
 *
 * @author carlos
 * @since 3.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogStorage implements LogStorage {

    private final DisruptorTemplate<AuditLogMainDTO> auditDisruptorTemplate;
    private final ClickHouseBatchWriter batchWriter;
    private final AuditLogConfigManager logConfigManager;
    private final AuditLogConverter converter = new AuditLogConverter();

    @Override
    public boolean store(OperationLog operationLog) {
        if (operationLog == null) {
            return false;
        }

        try {
            AuditLogMainDTO auditLog = converter.convert(operationLog);
            if (auditLog == null) {
                return false;
            }
            AuditLogConfigDTO config = getEffectiveConfig(auditLog);
            if (!shouldSample(config)) {
                log.debug("审计日志被采样过滤，logType={}", auditLog.getLogType());
                return true;
            }
            writeAuditLog(auditLog, config);
            return true;
        } catch (Exception e) {
            log.error("存储审计日志失败", e);
            return false;
        }
    }

    /**
     * 强制存储审计日志，忽略采样配置
     *
     * <p>用于审计模块自审计等关键场景，确保事件不被采样策略丢弃</p>
     *
     * @param operationLog 操作日志
     * @return 是否成功
     */
    public boolean storeWithoutSampling(OperationLog operationLog) {
        if (operationLog == null) {
            return false;
        }

        try {
            AuditLogMainDTO auditLog = converter.convert(operationLog);
            if (auditLog == null) {
                return false;
            }
            writeAuditLog(auditLog, null);
            return true;
        } catch (Exception e) {
            log.error("强制存储审计日志失败", e);
            return false;
        }
    }

    @Override
    public boolean storeBatch(List<OperationLog> logs) {
        if (logs == null || logs.isEmpty()) {
            return true;
        }

        try {
            List<AuditLogMainDTO> asyncLogs = new ArrayList<>(logs.size());
            for (OperationLog operationLog : logs) {
                AuditLogMainDTO auditLog = converter.convert(operationLog);
                if (auditLog == null) {
                    continue;
                }
                AuditLogConfigDTO config = getEffectiveConfig(auditLog);
                if (!shouldSample(config)) {
                    log.debug("批量审计日志被采样过滤，logType={}", auditLog.getLogType());
                    continue;
                }
                if (Boolean.FALSE.equals(config.getAsyncWrite())) {
                    batchWriter.add(auditLog);
                } else {
                    asyncLogs.add(auditLog);
                }
            }
            if (!asyncLogs.isEmpty()) {
                auditDisruptorTemplate.publishEventList(asyncLogs);
            }
            return true;
        } catch (Exception e) {
            log.error("批量存储审计日志失败", e);
            return false;
        }
    }

    private void writeAuditLog(AuditLogMainDTO auditLog, AuditLogConfigDTO config) {
        boolean async = config != null && Boolean.FALSE.equals(config.getAsyncWrite());
        if (async) {
            batchWriter.add(auditLog);
        } else {
            auditDisruptorTemplate.publishEvent(auditLog);
        }
    }

    @Override
    public int getPriority() {
        return 50;
    }

    @Override
    public boolean supportBatch() {
        return true;
    }

    @Override
    public String getName() {
        return "AuditLogStorage(ClickHouse)";
    }

    /**
     * 获取生效的审计日志配置
     */
    private AuditLogConfigDTO getEffectiveConfig(AuditLogMainDTO auditLog) {
        String logType = auditLog.getLogType();
        String tenantId = auditLog.getTenantId();
        return logConfigManager.getEffectiveConfig(logType, tenantId);
    }

    /**
     * 根据采样率决定是否保留当前日志
     */
    private boolean shouldSample(AuditLogConfigDTO config) {
        if (config == null || config.getSamplingRate() == null) {
            return true;
        }
        double rate = config.getSamplingRate().doubleValue();
        if (rate <= 0.0D) {
            return false;
        }
        if (rate >= 1.0D) {
            return true;
        }
        return ThreadLocalRandom.current().nextDouble() < rate;
    }

    /**
     * OperationLog 转 AuditLogMainDTO 转换器
     * <p>
     * 将通用的 String 类型字段转换为审计系统特定的枚举类型
     */
    public static class AuditLogConverter implements Converter<OperationLog, AuditLogMainDTO> {

        @Override
        public AuditLogMainDTO convert(OperationLog source) {
            if (source == null) {
                return null;
            }

            AuditLogMainDTO target = new AuditLogMainDTO();

            // ==================== 基础信息（直接复制）====================
            target.setServerTime(source.getServerTime());
            target.setEventDate(source.getEventDate());
            target.setClientTime(source.getClientTime());
            target.setEventTime(source.getEventTime());
            target.setDurationMs(source.getDurationMs());
            target.setRetentionDeadline(source.getRetentionDeadline());
            target.setLogSchemaVersion(source.getLogSchemaVersion());
            target.setLogType(source.getLogType());
            target.setRiskLevel(source.getRiskLevel());
            target.setOperation(source.getOperation());

            // ==================== 基础信息（枚举映射）====================
            target.setCategory(mapCategory(source.getCategory()));

            // ==================== 主体信息（直接复制）====================
            target.setPrincipalId(source.getPrincipalId());
            target.setPrincipalName(source.getPrincipalName());
            target.setTenantId(source.getTenantId());
            target.setDeptId(source.getDeptId());
            target.setDeptName(source.getDeptName());
            target.setDeptPath(source.getDeptPath());

            // ==================== 主体信息（枚举映射）====================
            target.setPrincipalType(mapPrincipalType(source.getPrincipalType()));

            // ==================== 目标对象（直接复制）====================
            target.setTargetType(source.getTargetType());
            target.setTargetId(source.getTargetId());
            target.setTargetName(source.getTargetName());
            target.setTargetSnapshot(source.getTargetSnapshot());

            // ==================== 执行状态（枚举映射）====================
            target.setLogStatus(mapLogStatus(source.getLogStatus()));
            target.setResultCode(source.getResultCode());
            target.setResultMessage(source.getResultMessage());
            target.setApprovalComment(source.getApprovalComment());

            // ==================== 请求信息（直接复制）====================
            target.setClientIp(source.getClientIp());
            target.setClientPort(source.getClientPort());
            target.setServerIp(source.getServerIp());
            target.setUserAgent(source.getUserAgent());
            target.setDeviceFingerprint(source.getDeviceFingerprint());

            // ==================== 地理位置（直接复制）====================
            target.setLocationCountry(source.getLocationCountry());
            target.setLocationProvince(source.getLocationProvince());
            target.setLocationCity(source.getLocationCity());
            target.setLocationLat(source.getLocationLat());
            target.setLocationLon(source.getLocationLon());

            // ==================== 认证授权（枚举映射）====================
            target.setAuthType(mapAuthType(source.getAuthType()));
            target.setAuthProvider(mapAuthProvider(source.getAuthProvider()));
            target.setRoles(source.getRoles());
            target.setPermissions(source.getPermissions());

            // ==================== 业务信息（直接复制+枚举映射）====================
            target.setBizChannel(mapBizChannel(source.getBizChannel()));
            target.setBizScene(source.getBizScene());
            target.setBizOrderNo(source.getBizOrderNo());
            target.setRelatedBizIds(source.getRelatedBizIds());
            target.setMonetaryAmount(source.getMonetaryAmount());
            target.setProcessId(source.getProcessId());
            target.setApproverId(source.getApproverId());

            // ==================== 批量操作（直接复制）====================
            target.setBatchId(source.getBatchId());
            target.setBatchIndex(source.getBatchIndex());
            target.setBatchTotal(source.getBatchTotal());
            target.setTaskId(source.getTaskId());

            // ==================== 数据变更（直接复制）====================
            target.setHasDataChange(source.getHasDataChange());
            target.setEntityClass(source.getEntityClass());
            target.setTableName(source.getTableName());
            target.setChangeSummary(source.getChangeSummary());
            target.setChangedFieldCount(source.getChangedFieldCount());
            target.setOldData(source.getOldData());
            target.setNewData(source.getNewData());
            target.setOldDataCompressed(source.getOldDataCompressed());
            target.setNewDataCompressed(source.getNewDataCompressed());

            // ==================== 追踪信息（直接复制）====================
            target.setTraceId(source.getTraceId());
            target.setSpanId(source.getSpanId());
            target.setParentSpanId(source.getParentSpanId());
            target.setTracePath(source.getTracePath());

            // ==================== 技术上下文（直接复制）====================
            target.setDbQueryCount(source.getDbQueryCount());
            target.setDbQueryTimeMs(source.getDbQueryTimeMs());
            target.setExternalCallCount(source.getExternalCallCount());
            target.setExternalCallTimeMs(source.getExternalCallTimeMs());
            target.setCustomMetrics(source.getCustomMetrics());

            // ==================== 请求响应数据（直接复制）====================
            target.setRequestPayloadRef(source.getRequestPayloadRef());
            target.setResponsePayloadRef(source.getResponsePayloadRef());
            target.setPayloadStorageType(mapPayloadStorageType(source.getPayloadStorageType()));

            // ==================== 应用信息（直接复制）====================
            target.setAppName(source.getAppName());
            target.setAppVersion(source.getAppVersion());
            target.setCluster(source.getCluster());
            target.setHostName(source.getHostName());

            // ==================== 标签信息（直接复制）====================
            target.setTagKeys(source.getTagKeys());
            target.setTagValues(source.getTagValues());

            // ==================== 附件信息（直接复制）====================
            target.setAttachmentCount(source.getAttachmentCount());
            target.setAttachmentTypes(source.getAttachmentTypes());
            target.setAttachmentTotalSize(source.getAttachmentTotalSize());
            target.setFirstAttachmentRef(source.getFirstAttachmentRef());
            target.setAttachmentRefs(source.getAttachmentRefs());

            // ==================== 扩展信息（直接复制+合并）====================
            target.setDynamicTags(source.getDynamicTags());

            // 将OperationLog特有的字段合并到dynamicExtras
            String dynamicExtras = mergeExtraFields(
                source.getDynamicExtras(),
                source.getException(),
                source.getExceptionClass(),
                source.getUrl(),
                source.getHttpMethod()
            );
            target.setDynamicExtras(dynamicExtras);

            // 对 JSON 格式的敏感字段进行脱敏
            maskSensitiveFields(target);

            // ==================== 时间信息（直接复制）====================
            target.setCreatedTime(source.getCreatedTime());
            target.setUpdatedTime(source.getUpdatedTime());

            // ==================== 默认值处理 ====================
            ensureDefaults(target);

            return target;
        }

        /**
         * 对审计日志中的 JSON 敏感字段脱敏
         */
        private void maskSensitiveFields(AuditLogMainDTO target) {
            target.setTargetSnapshot(SensitiveDataUtil.maskJson(target.getTargetSnapshot()));
            target.setOldData(SensitiveDataUtil.maskJson(target.getOldData()));
            target.setNewData(SensitiveDataUtil.maskJson(target.getNewData()));
            target.setDynamicExtras(SensitiveDataUtil.maskJson(target.getDynamicExtras()));
        }

        /**
         * 确保必填字段有默认值
         */
        private void ensureDefaults(AuditLogMainDTO target) {
            // 确保事件日期
            if (target.getEventDate() == null) {
                target.setEventDate(LocalDate.now());
            }
            // 确保保留期限
            if (target.getRetentionDeadline() == null) {
                target.setRetentionDeadline(LocalDate.now().plusDays(90));
            }
            // 确保 Schema 版本
            if (target.getLogSchemaVersion() == null) {
                target.setLogSchemaVersion(1);
            }
            // 确保类别
            if (target.getCategory() == null) {
                target.setCategory(AuditLogCategoryEnum.BUSINESS);
            }
            // 确保主体类型
            if (target.getPrincipalType() == null) {
                target.setPrincipalType(AuditLogPrincipalTypeEnum.ANONYMOUS);
            }
            // 确保状态
            if (target.getLogStatus() == null) {
                target.setLogStatus(AuditLogStatusEnum.SUCCESS);
            }
            // 确保渠道
            if (target.getBizChannel() == null) {
                target.setBizChannel(AuditLogBizChannelEnum.WEB);
            }
            // 确保创建时间
            if (target.getCreatedTime() == null) {
                target.setCreatedTime(LocalDateTime.now());
            }
        }

        // ==================== 辅助方法 ====================

        /**
         * 将OperationLog特有字段合并到dynamicExtras
         * 简单实现：直接拼接JSON字符串
         */
        private String mergeExtraFields(String originalExtras, String exception,
                                        String exceptionClass, String url, String httpMethod) {
            StringBuilder sb = new StringBuilder();

            // 先复制原始内容（去掉首尾的大括号）
            if (originalExtras != null && !originalExtras.isEmpty()) {
                String trimmed = originalExtras.trim();
                if (trimmed.startsWith("{")) {
                    trimmed = trimmed.substring(1);
                }
                if (trimmed.endsWith("}")) {
                    trimmed = trimmed.substring(0, trimmed.length() - 1);
                }
                if (!trimmed.isEmpty()) {
                    sb.append(trimmed);
                    if (!trimmed.endsWith(",")) {
                        sb.append(",");
                    }
                }
            }

            // 添加特有字段
            boolean hasField = false;
            if (exception != null) {
                sb.append("\"exception\":\"").append(escapeJson(exception)).append("\"");
                hasField = true;
            }
            if (exceptionClass != null) {
                if (hasField) {
                    sb.append(",");
                }
                sb.append("\"exceptionClass\":\"").append(escapeJson(exceptionClass)).append("\"");
                hasField = true;
            }
            if (url != null) {
                if (hasField) {
                    sb.append(",");
                }
                sb.append("\"url\":\"").append(escapeJson(url)).append("\"");
                hasField = true;
            }
            if (httpMethod != null) {
                if (hasField) {
                    sb.append(",");
                }
                sb.append("\"httpMethod\":\"").append(escapeJson(httpMethod)).append("\"");
            }

            if (sb.length() == 0) {
                return null;
            }

            return "{" + sb.toString() + "}";
        }

        /**
         * 简单的JSON字符串转义
         */
        private String escapeJson(String str) {
            if (str == null) {
                return "";
            }
            return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
        }

        // ==================== 枚举映射方法 ====================

        private AuditLogCategoryEnum mapCategory(String category) {
            if (category == null) {
                return null;
            }
            try {
                return AuditLogCategoryEnum.valueOf(category.toUpperCase());
            } catch (IllegalArgumentException e) {
                return AuditLogCategoryEnum.BUSINESS;
            }
        }

        private AuditLogPrincipalTypeEnum mapPrincipalType(String principalType) {
            if (principalType == null) {
                return null;
            }
            try {
                return AuditLogPrincipalTypeEnum.valueOf(principalType.toUpperCase());
            } catch (IllegalArgumentException e) {
                return AuditLogPrincipalTypeEnum.USER;
            }
        }

        private AuditLogStatusEnum mapLogStatus(String logStatus) {
            if (logStatus == null) {
                return null;
            }
            try {
                return AuditLogStatusEnum.valueOf(logStatus.toUpperCase());
            } catch (IllegalArgumentException e) {
                return AuditLogStatusEnum.SUCCESS;
            }
        }

        private AuditLogAuthTypeEnum mapAuthType(String authType) {
            if (authType == null) {
                return null;
            }
            try {
                return AuditLogAuthTypeEnum.valueOf(authType.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        private AuditLogAuthProviderEnum mapAuthProvider(String authProvider) {
            if (authProvider == null) {
                return null;
            }
            try {
                return AuditLogAuthProviderEnum.valueOf(authProvider.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        private AuditLogBizChannelEnum mapBizChannel(String bizChannel) {
            if (bizChannel == null) {
                return null;
            }
            try {
                return AuditLogBizChannelEnum.valueOf(bizChannel.toUpperCase());
            } catch (IllegalArgumentException e) {
                return AuditLogBizChannelEnum.WEB;
            }
        }

        private AuditLogPayloadStorageTypeEnum mapPayloadStorageType(String storageType) {
            if (storageType == null) {
                return null;
            }
            try {
                return AuditLogPayloadStorageTypeEnum.valueOf(storageType.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }
}
