package com.carlos.audit.disruptor;

import com.carlos.audit.api.pojo.enums.AuditLogBizChannelEnum;
import com.carlos.audit.api.pojo.enums.AuditLogPrincipalTypeEnum;
import com.carlos.audit.api.pojo.enums.AuditLogStatusEnum;
import com.carlos.audit.clickhouse.ClickHouseBatchWriter;
import com.carlos.audit.convert.AuditLogMainConvert;
import com.carlos.audit.manager.AuditLogConfigManager;
import com.carlos.audit.pojo.dto.AuditLogConfigDTO;
import com.carlos.audit.pojo.dto.AuditLogMainDTO;
import com.carlos.disruptor.core.DisruptorEvent;
import com.carlos.disruptor.core.DisruptorEventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * <p>
 * 审计日志 Disruptor 事件处理器
 * </p>
 *
 * @author Carlos
 * @date 2026-03-06
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogEventHandler implements DisruptorEventHandler<AuditLogMainDTO> {

    private final ClickHouseBatchWriter batchWriter;
    private final AuditLogConfigManager logConfigManager;

    @Override
    public void onEvent(DisruptorEvent<AuditLogMainDTO> event, long sequence, boolean endOfBatch) throws Exception {
        AuditLogMainDTO original = event.getData();

        if (original == null) {
            log.warn("收到空的审计日志事件，序列号: {}", sequence);
            return;
        }

        // 复制新对象，避免修改上游传入的原始对象
        AuditLogMainDTO auditLog = AuditLogMainConvert.INSTANCE.copy(original);
        event.setData(auditLog);

        try {
            // 1. 数据清洗与 enrich
            enrichAuditLog(auditLog);

            // 2. 应用动态配置
            applyConfig(auditLog);

            // 3. 风险评级
            calculateRiskLevel(auditLog);

            // 4. 加入批量写入队列
            batchWriter.add(auditLog);

            log.debug("审计日志事件处理成功，序列号: {}, 类型: {}", sequence, auditLog.getLogType());
        } catch (Exception e) {
            log.error("处理审计日志事件失败，序列号: {}", sequence, e);
            throw e; // 抛出异常让 Disruptor 的异常处理器处理
        }
    }

    /**
     * 数据清洗与 enrich
     */
    private void enrichAuditLog(AuditLogMainDTO auditLog) {
        // 设置服务器时间（如果未设置）
        if (auditLog.getServerTime() == null) {
            auditLog.setServerTime(LocalDateTime.now());
        }

        // 设置事件日期（如果未设置）
        if (auditLog.getEventDate() == null && auditLog.getServerTime() != null) {
            auditLog.setEventDate(auditLog.getServerTime().toLocalDate());
        }

        // 确保有日志 Schema 版本
        if (auditLog.getLogSchemaVersion() == null) {
            auditLog.setLogSchemaVersion(3);
        }

        // 确保有状态
        if (auditLog.getLogStatus() == null) {
            auditLog.setLogStatus(AuditLogStatusEnum.SUCCESS);
        }

        // 设置创建时间
        if (auditLog.getCreatedTime() == null) {
            auditLog.setCreatedTime(LocalDateTime.now());
        }
    }

    /**
     * 应用动态配置（采样、保留期、字段裁剪）
     */
    private void applyConfig(AuditLogMainDTO auditLog) {
        AuditLogConfigDTO config = logConfigManager.getEffectiveConfig(
            auditLog.getLogType(), auditLog.getTenantId());
        if (config == null) {
            return;
        }

        // 根据保留天数设置数据保留截止日期
        Integer retentionDays = config.getRetentionDays();
        if (retentionDays != null && retentionDays > 0) {
            LocalDate eventDate = auditLog.getEventDate();
            if (eventDate == null) {
                eventDate = LocalDate.now();
            }
            auditLog.setRetentionDeadline(eventDate.plusDays(retentionDays));
        }

        // 不存储数据变更详情时清空相关字段
        if (Boolean.FALSE.equals(config.getStoreDataChange())) {
            auditLog.setHasDataChange(null);
            auditLog.setEntityClass(null);
            auditLog.setTableName(null);
            auditLog.setChangeSummary(null);
            auditLog.setChangedFieldCount(null);
            auditLog.setOldData(null);
            auditLog.setNewData(null);
            auditLog.setOldDataCompressed(null);
            auditLog.setNewDataCompressed(null);
        }

        // 不存储技术上下文时清空相关字段
        if (Boolean.FALSE.equals(config.getStoreTechnical())) {
            auditLog.setDbQueryCount(null);
            auditLog.setDbQueryTimeMs(null);
            auditLog.setExternalCallCount(null);
            auditLog.setExternalCallTimeMs(null);
            auditLog.setCustomMetrics(null);
        }
    }

    /**
     * 计算风险等级（多维度综合评估）
     *
     * 风险评估模型:
     * 1. 基础风险分 (0-60 分) - 基于操作类型
     * 2. 状态风险分 (0-20 分) - 基于操作结果
     * 3. 主体风险分 (0-10 分) - 基于操作主体类型
     * 4. 渠道风险分 (0-10 分) - 基于业务渠道
     *
     * 风险等级划分:
     * 0-20:   低风险      (绿色)
     * 21-40:  较低风险    (蓝色)
     * 41-60:  中风险      (黄色)
     * 61-80:  较高风险    (橙色)
     * 81-100: 高风险      (红色)
     */
    private void calculateRiskLevel(AuditLogMainDTO auditLog) {
        int baseRisk = calculateBaseRisk(auditLog);
        int stateRisk = calculateStateRisk(auditLog);
        int principalRisk = calculatePrincipalRisk(auditLog);
        int channelRisk = calculateChannelRisk(auditLog);

        int riskLevel = baseRisk + stateRisk + principalRisk + channelRisk;

        // 确保风险等级在 0-100 范围内
        riskLevel = Math.max(0, Math.min(100, riskLevel));

        // 如果注解指定了风险等级，取最大值
        if (auditLog.getRiskLevel() != null && auditLog.getRiskLevel() > riskLevel) {
            riskLevel = auditLog.getRiskLevel();
        }

        auditLog.setRiskLevel(riskLevel);
    }

    /**
     * 1. 基础风险分 (0-60 分) - 基于操作类型
     */
    private int calculateBaseRisk(AuditLogMainDTO auditLog) {
        String logType = auditLog.getLogType();
        if (logType == null) {
            return 10;
        }

        return switch (logType) {
            // ==================== 极高风险操作 (50-60 分) ====================
            case "DATA_DELETE", "DATA_BATCH_DELETE" -> 60;
            case "PERMISSION_CHANGE", "ROLE_ASSIGN" -> 55;
            case "SYSTEM_CONFIG_CHANGE", "DATABASE_CONFIG_CHANGE" -> 55;

            // ==================== 高风险操作 (40-50 分) ====================
            case "ORDER_PAY", "ORDER_REFUND" -> 50;
            case "USER_PASSWORD_RESET", "USER_PASSWORD_CHANGE" -> 45;
            case "CONFIG_CHANGE", "BUSINESS_CONFIG_CHANGE" -> 45;
            case "DATA_EXPORT_SENSITIVE", "DATA_IMPORT_CRITICAL" -> 45;
            case "APPROVAL_REJECT", "APPROVAL_CANCEL" -> 40;

            // ==================== 中高风险操作 (30-40 分) ====================
            case "DATA_CREATE", "DATA_UPDATE" -> 35;
            case "DATA_EXPORT", "DATA_IMPORT" -> 35;
            case "FILE_UPLOAD", "FILE_DELETE" -> 35;
            case "USER_CREATE", "USER_UPDATE" -> 30;
            case "ORDER_CREATE", "ORDER_UPDATE" -> 30;
            case "APPROVAL_PASS" -> 30;

            // ==================== 中低风险操作 (20-30 分) ====================
            case "DATA_QUERY" -> 25;
            case "FILE_DOWNLOAD" -> 25;
            case "USER_LOGIN" -> 20;
            case "USER_LOGOUT" -> 20;
            case "PROFILE_UPDATE" -> 20;

            // ==================== 低风险操作 (10-20 分) ====================
            default -> 10;
        };
    }

    /**
     * 2. 状态风险分 (0-20 分) - 基于操作结果
     */
    private int calculateStateRisk(AuditLogMainDTO auditLog) {
        AuditLogStatusEnum state = auditLog.getLogStatus();
        if (state == null) {
            return 0;
        }

        return switch (state) {
            case FAIL -> 20;
            case TIMEOUT -> 15;
            case PARTIAL_SUCCESS -> 10;
            case PENDING -> 5;
            case SUCCESS -> 0;
        };
    }

    /**
     * 3. 主体风险分 (0-10 分) - 基于操作主体类型
     */
    private int calculatePrincipalRisk(AuditLogMainDTO auditLog) {
        AuditLogPrincipalTypeEnum principalType = auditLog.getPrincipalType();
        if (principalType == null) {
            return 5;
        }

        return switch (principalType) {
            case ANONYMOUS -> 10;
            case SERVICE -> 8;
            case SYSTEM -> 5;
            case USER -> 0;
        };
    }

    /**
     * 4. 渠道风险分 (0-10 分) - 基于业务渠道
     */
    private int calculateChannelRisk(AuditLogMainDTO auditLog) {
        AuditLogBizChannelEnum bizChannel = auditLog.getBizChannel();
        if (bizChannel == null) {
            return 5;
        }

        return switch (bizChannel) {
            case OPEN_API -> 10;
            case BACKGROUND -> 8;
            case MINI_PROGRAM -> 6;
            case H5 -> 5;
            case APP -> 3;
            case WEB -> 0;
            case DESKTOP -> 0;
        };
    }

    @Override
    public String getHandlerName() {
        return "AuditLogEventHandler";
    }
}
