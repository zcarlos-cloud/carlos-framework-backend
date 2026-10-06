package com.carlos.audit.api.log;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.carlos.audit.api.pojo.enums.AuditLogAuthProviderEnum;
import com.carlos.audit.api.pojo.enums.AuditLogAuthTypeEnum;
import com.carlos.audit.api.pojo.enums.AuditLogBizChannelEnum;
import com.carlos.audit.api.pojo.enums.AuditLogCategoryEnum;
import com.carlos.audit.api.pojo.enums.AuditLogPrincipalTypeEnum;
import com.carlos.audit.api.pojo.enums.AuditLogStatusEnum;
import com.carlos.audit.api.pojo.param.ApiAuditLogMainParam;
import com.carlos.log.entity.OperationLog;
import org.springframework.core.convert.converter.Converter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * {@link OperationLog} → {@link ApiAuditLogMainParam} 转换器
 *
 * <h2>为什么与 carlos-audit-bus 的同名转换器并存</h2>
 * <p>
 * carlos-audit-bus 里已有 {@code AuditLogStorage.AuditLogConverter}（目标类型是该模块私有的
 * {@code AuditLogMainDTO}）。本类<b>刻意不复用它</b>：那是线上审计写入路径（有存量数据、
 * 有 {@code AuditLogStorageTest} / {@code AuditLogPipelineIntegrationTest} 兜底），
 * 而对它做「抽出共享」的改动会让本功能与既有链路的风险耦合在一起。
 * </p>
 * <p>
 * <b>代价与不变量</b>：两处映射规则必须保持一致 —— 同一次操作无论由哪个服务产生、
 * 走哪条链路，落库后的审计行语义应当相同。修改任一侧的枚举回退策略、必填兜底或
 * dynamicExtras 折叠方式时，<b>必须同步另一侧</b>。合并两者是后续独立改动的目标，
 * 不属于本功能范围。
 * </p>
 *
 * <h2>为什么需要 {@code ensureDefaults}</h2>
 * <p>
 * {@code AuditLogMqConsumer} 不执行 Jakarta Bean Validation，而
 * {@link ApiAuditLogMainParam} 对 {@code serverTime} / {@code eventDate} / {@code eventTime} /
 * {@code category} / {@code logType} / {@code principalId} / {@code operation} 声明了
 * {@code @NotNull} / {@code @NotBlank}。生产端必须自己补齐，否则消费端会写入语义残缺的行。
 * </p>
 *
 * <h2>关于脱敏</h2>
 * <p>
 * 本类<b>不做</b> {@code targetSnapshot} / {@code oldData} / {@code newData} / {@code dynamicExtras}
 * 的脱敏：这些字段由 {@code @Log} 注解路径下的 {@code LogAspect} 恒不填充，
 * 脱敏在此处是空操作。请求参数的脱敏由 {@code LogAspect} 依 {@code @Log#excludeParams()}
 * 在序列化时剔除（那个位置才是唯一有效的 —— 事后脱敏做不到「不落库」）。
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
public class OperationLogParamConverter implements Converter<OperationLog, ApiAuditLogMainParam> {

    /** 默认保留期限（天），与 AuditLogConfigManager 的 createDefaultConfig 保持一致 */
    private static final int DEFAULT_RETENTION_DAYS = 90;

    /** 默认 Schema 版本 */
    private static final int DEFAULT_SCHEMA_VERSION = 1;

    @Override
    public ApiAuditLogMainParam convert(OperationLog source) {
        if (source == null) {
            return null;
        }

        ApiAuditLogMainParam target = new ApiAuditLogMainParam();

        // ==================== 时间与基础信息 ====================
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

        // ==================== 主体信息 ====================
        target.setPrincipalId(source.getPrincipalId());
        target.setPrincipalName(source.getPrincipalName());
        target.setTenantId(source.getTenantId());
        target.setDeptId(source.getDeptId());
        target.setDeptName(source.getDeptName());
        target.setDeptPath(source.getDeptPath());
        target.setPrincipalType(mapPrincipalType(source.getPrincipalType()));

        // ==================== 目标对象 ====================
        target.setTargetType(source.getTargetType());
        target.setTargetId(source.getTargetId());
        target.setTargetName(source.getTargetName());
        target.setTargetSnapshot(source.getTargetSnapshot());

        // ==================== 执行状态 ====================
        target.setLogStatus(mapLogStatus(source.getLogStatus()));
        target.setResultCode(source.getResultCode());
        target.setResultMessage(source.getResultMessage());
        target.setApprovalComment(source.getApprovalComment());

        // ==================== 请求信息 ====================
        target.setClientIp(source.getClientIp());
        target.setClientPort(source.getClientPort());
        target.setServerIp(source.getServerIp());
        target.setUserAgent(source.getUserAgent());
        target.setDeviceFingerprint(source.getDeviceFingerprint());

        // ==================== 地理位置 ====================
        target.setLocationCountry(source.getLocationCountry());
        target.setLocationProvince(source.getLocationProvince());
        target.setLocationCity(source.getLocationCity());
        target.setLocationLat(source.getLocationLat());
        target.setLocationLon(source.getLocationLon());

        // ==================== 认证授权 ====================
        target.setAuthType(mapAuthType(source.getAuthType()));
        target.setAuthProvider(mapAuthProvider(source.getAuthProvider()));
        target.setRoles(source.getRoles());
        target.setPermissions(source.getPermissions());

        // ==================== 业务信息 ====================
        target.setBizChannel(mapBizChannel(source.getBizChannel()));
        target.setBizScene(source.getBizScene());
        target.setBizOrderNo(source.getBizOrderNo());
        target.setRelatedBizIds(source.getRelatedBizIds());
        target.setMonetaryAmount(source.getMonetaryAmount());
        target.setProcessId(source.getProcessId());
        target.setApproverId(source.getApproverId());

        // ==================== 批量操作 ====================
        target.setBatchId(source.getBatchId());
        target.setBatchIndex(source.getBatchIndex());
        target.setBatchTotal(source.getBatchTotal());
        target.setTaskId(source.getTaskId());

        // ==================== 数据变更 ====================
        target.setHasDataChange(source.getHasDataChange());
        target.setEntityClass(source.getEntityClass());
        target.setTableName(source.getTableName());
        target.setChangeSummary(source.getChangeSummary());
        target.setChangedFieldCount(source.getChangedFieldCount());
        target.setOldData(source.getOldData());
        target.setNewData(source.getNewData());
        target.setOldDataCompressed(source.getOldDataCompressed());
        target.setNewDataCompressed(source.getNewDataCompressed());

        // ==================== 追踪信息 ====================
        target.setTraceId(source.getTraceId());
        target.setSpanId(source.getSpanId());
        target.setParentSpanId(source.getParentSpanId());
        target.setTracePath(source.getTracePath());

        // ==================== 技术上下文 ====================
        target.setDbQueryCount(source.getDbQueryCount());
        target.setDbQueryTimeMs(source.getDbQueryTimeMs());
        target.setExternalCallCount(source.getExternalCallCount());
        target.setExternalCallTimeMs(source.getExternalCallTimeMs());
        target.setCustomMetrics(source.getCustomMetrics());

        // ==================== 请求响应数据 ====================
        target.setRequestPayloadRef(source.getRequestPayloadRef());
        target.setResponsePayloadRef(source.getResponsePayloadRef());
        // 刻意不映射 payloadStorageType：它是 String，而消费侧 AuditLogMainConvert 会对它做
        // 无保护的 Enum.valueOf(AuditLogPayloadStorageTypeEnum)，且该枚举在 carlos-audit-bus 里，
        // 本模块无法校验。传一个非法值出去 = 制造一条消费端抛异常的毒消息。
        // @Log 路径恒不填充该字段，不损失任何信息；将来若确有需要，应先把枚举提升到 api 模块。

        // ==================== 应用信息 ====================
        target.setAppName(source.getAppName());
        target.setAppVersion(source.getAppVersion());
        target.setCluster(source.getCluster());
        target.setHostName(source.getHostName());

        // ==================== 标签信息 ====================
        target.setTagKeys(source.getTagKeys());
        target.setTagValues(source.getTagValues());

        // ==================== 附件信息 ====================
        target.setAttachmentCount(source.getAttachmentCount());
        target.setAttachmentTypes(source.getAttachmentTypes());
        target.setAttachmentTotalSize(source.getAttachmentTotalSize());
        target.setFirstAttachmentRef(source.getFirstAttachmentRef());
        target.setAttachmentRefs(source.getAttachmentRefs());

        // ==================== 扩展信息 ====================
        target.setDynamicTags(source.getDynamicTags());
        // OperationLog 特有的四个字段在 param 上没有对应列，折叠进 dynamicExtras 保留可查性
        target.setDynamicExtras(mergeExtraFields(source));

        // ==================== 默认值兜底 ====================
        ensureDefaults(target);

        return target;
    }

    /**
     * 把 {@link OperationLog} 特有、{@link ApiAuditLogMainParam} 无对应列的字段折叠进 dynamicExtras
     * <p>
     * 与 carlos-audit-bus 的同名实现保持同样的输出形状（JSON 对象），但改用
     * {@code cn.hutool.json} 构建而非手工拼字符串 —— 手工拼接要自己处理转义，是缺陷的常见来源。
     * </p>
     * <p>
     * 已有 {@code dynamicExtras} 时原样保留：那是一段不透明的 JSON，无法无损合并，
     * 而覆盖它会破坏既有写入方的约定。
     * </p>
     *
     * @param source 操作日志
     * @return JSON 字符串；无内容时返回 null
     */
    private String mergeExtraFields(OperationLog source) {
        if (StrUtil.isNotBlank(source.getDynamicExtras())) {
            return source.getDynamicExtras();
        }

        JSONObject extras = JSONUtil.createObj();
        putIfNotBlank(extras, "exception", source.getException());
        putIfNotBlank(extras, "exceptionClass", source.getExceptionClass());
        putIfNotBlank(extras, "url", source.getUrl());
        putIfNotBlank(extras, "httpMethod", source.getHttpMethod());

        return extras.isEmpty() ? null : extras.toString();
    }

    private void putIfNotBlank(JSONObject target, String key, String value) {
        if (StrUtil.isNotBlank(value)) {
            target.set(key, value);
        }
    }

    /**
     * 补齐消费侧必填字段的默认值
     * <p>
     * 消费端不做 Bean Validation，缺字段会写入语义残缺的审计行。
     * </p>
     */
    private void ensureDefaults(ApiAuditLogMainParam target) {
        LocalDateTime now = LocalDateTime.now();

        if (target.getServerTime() == null) {
            target.setServerTime(now);
        }
        if (target.getEventTime() == null) {
            target.setEventTime(target.getServerTime());
        }
        if (target.getEventDate() == null) {
            target.setEventDate(target.getEventTime().toLocalDate());
        }
        if (target.getRetentionDeadline() == null) {
            target.setRetentionDeadline(LocalDate.now().plusDays(DEFAULT_RETENTION_DAYS));
        }
        if (target.getLogSchemaVersion() == null) {
            target.setLogSchemaVersion(DEFAULT_SCHEMA_VERSION);
        }
        if (target.getCategory() == null) {
            target.setCategory(AuditLogCategoryEnum.BUSINESS);
        }
        if (target.getLogStatus() == null) {
            target.setLogStatus(AuditLogStatusEnum.SUCCESS);
        }
        if (target.getBizChannel() == null) {
            target.setBizChannel(AuditLogBizChannelEnum.WEB);
        }
        if (target.getCreatedTime() == null) {
            target.setCreatedTime(now);
        }
        // 主体：@Log 在无登录身份时给出 ANONYMOUS 且 principalId 为空，
        // 但消费侧 schema 要求 principalId 非空，故兜底为 "anonymous"
        if (target.getPrincipalType() == null) {
            target.setPrincipalType(AuditLogPrincipalTypeEnum.ANONYMOUS);
        }
        if (target.getPrincipalId() == null || target.getPrincipalId().isBlank()) {
            target.setPrincipalId(String.valueOf(target.getPrincipalType()));
        }
        // 顺序要紧：operation 的兜底值取自 logType，故 logType 必须先规整
        if (target.getLogType() == null || target.getLogType().isBlank()) {
            target.setLogType("UNKNOWN");
        }
        if (target.getOperation() == null || target.getOperation().isBlank()) {
            target.setOperation(target.getLogType());
        }
    }

    // ==================== 枚举映射 ====================
    // 约定：名称非法或为空时回退到安全默认值（绝不抛出）。
    // 审计链路的价值在于「宁可多记一条残缺的，也不要因为一个野值丢掉整条记录」。

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
}
