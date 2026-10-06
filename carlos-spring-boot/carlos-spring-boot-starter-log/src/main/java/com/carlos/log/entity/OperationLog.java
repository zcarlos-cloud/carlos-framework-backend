package com.carlos.log.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 操作日志实体
 * <p>
 * 通用操作日志模型，字段与审计日志（AuditLogMainDTO）保持一致。
 * 支持存储到多种存储介质（MySQL、ClickHouse、Elasticsearch 等）。
 * <p>
 * 字段设计遵循宽表模型，按功能分组管理。
 *
 * @author carlos
 * @since 3.0.0
 */
@Data
@Accessors(chain = true)
public class OperationLog {

    // ==================== 基础信息 ====================

    /** 服务器时间，毫秒精度 */
    private LocalDateTime serverTime;

    /** 事件日期，冗余字段，方便分区 */
    private LocalDate eventDate;

    /** 客户端时间 */
    private LocalDateTime clientTime;

    /** 事件实际发生时间 */
    private LocalDateTime eventTime;

    /** 操作耗时，毫秒 */
    private Integer durationMs;

    /** 数据保留截止日期，TTL删除用 */
    private LocalDate retentionDeadline;

    /** 日志Schema版本，用于兼容性处理 */
    private Integer logSchemaVersion;

    /** 大类：SECURITY-安全/BUSINESS-业务/SYSTEM-系统/AUDIT-审计 */
    private String category;

    /** 细类：自定义日志类型，如 USER_LOGIN、ORDER_PAY、DATA_EXPORT 等 */
    private String logType;

    /** 风险等级 0-100，0为无风险，100为极高风险 */
    private Integer riskLevel;

    /** 操作描述，人工可读的操作说明 */
    private String operation;

    // ==================== 主体信息（操作人） ====================

    /** 操作主体ID，用户ID或服务账号 */
    private String principalId;

    /** 主体类型：USER-用户/SERVICE-服务/SYSTEM-系统/ANONYMOUS-匿名 */
    private String principalType;

    /** 主体名称，冗余存储避免关联查询 */
    private String principalName;

    /** 租户ID，SaaS多租户隔离字段 */
    private String tenantId;

    /** 部门ID */
    private String deptId;

    /** 部门名称，冗余存储 */
    private String deptName;

    /** 部门层级路径，如：1/12/156/，方便层级查询 */
    private String deptPath;

    // ==================== 目标对象信息 ====================

    /** 对象类型：ORDER-订单/USER-用户/CONFIG-配置/DATA-数据 */
    private String targetType;

    /** 对象唯一标识 */
    private String targetId;

    /** 对象显示名称，冗余存储 */
    private String targetName;

    /** 对象关键信息摘要，JSON格式存储关键字段 */
    private String targetSnapshot;

    // ==================== 执行状态 ====================

    /** 状态：SUCCESS-成功/FAIL-失败/PENDING-处理中/TIMEOUT-超时/PARTIAL_SUCCESS-部分成功 */
    private String logStatus;

    /** 业务结果码，用于细分错误类型 */
    private String resultCode;

    /** 结果描述，前500字符 */
    private String resultMessage;

    /** 审批意见，工作流用 */
    private String approvalComment;

    // ==================== 异常信息 ====================

    /** 异常信息 */
    private String exception;

    /** 异常类型 */
    private String exceptionClass;

    // ==================== 请求信息 ====================

    /** 请求URL */
    private String url;

    /** HTTP请求方法（GET/POST/PUT/DELETE等） */
    private String httpMethod;

    /** 客户端IP，支持IPv6 */
    private String clientIp;

    /** 客户端端口 */
    private Integer clientPort;

    /** 处理服务器IP */
    private String serverIp;

    /** 浏览器UA，用于设备识别 */
    private String userAgent;

    /** 设备指纹，唯一标识设备 */
    private String deviceFingerprint;

    // ==================== 地理位置信息 ====================

    /** 国家 */
    private String locationCountry;

    /** 省份 */
    private String locationProvince;

    /** 城市 */
    private String locationCity;

    /** 纬度 */
    private BigDecimal locationLat;

    /** 经度 */
    private BigDecimal locationLon;

    // ==================== 认证授权信息 ====================

    /** 认证方式：PASSWORD-密码/SMS-短信/OAUTH2-OAuth2/LDAP-LDAP/CERT-证书 */
    private String authType;

    /** 认证源：LOCAL-本地/WECHAT-微信/DINGTALK-钉钉 */
    private String authProvider;

    /** 当前角色列表，JSON数组 */
    private String roles;

    /** 当前权限列表，JSON数组 */
    private String permissions;

    // ==================== 业务信息 ====================

    /** 业务渠道：WEB-网页/APP-移动应用/MINI_PROGRAM-小程序/OPEN_API-开放接口 */
    private String bizChannel;

    /** 业务场景，如：订单创建-ORDER_CREATE */
    private String bizScene;

    /** 业务订单号，用于关联订单 */
    private String bizOrderNo;

    /** 关联业务ID列表，JSON数组 */
    private String relatedBizIds;

    /** 涉及金额，精确到分 */
    private BigDecimal monetaryAmount;

    /** 流程实例ID，工作流引擎生成 */
    private String processId;

    /** 审批人ID */
    private String approverId;

    // ==================== 批量操作信息 ====================

    /** 批量操作批次号，UUID */
    private String batchId;

    /** 批次内序号，从0开始 */
    private Integer batchIndex;

    /** 批次总数 */
    private Integer batchTotal;

    /** 任务ID */
    private String taskId;

    // ==================== 数据变更信息 ====================

    /** 是否有数据变更详情：0-否/1-是 */
    private Boolean hasDataChange;

    /** 实体类名，如：com.example.Order */
    private String entityClass;

    /** 数据库表名，如：t_order */
    private String tableName;

    /** 变更摘要，人工可读的变更说明 */
    private String changeSummary;

    /** 变更字段数量 */
    private Integer changedFieldCount;

    /** 变更前完整数据，JSON格式 */
    private String oldData;

    /** 变更后完整数据，JSON格式 */
    private String newData;

    /** 旧数据是否压缩：0-否/1-是 */
    private Boolean oldDataCompressed;

    /** 新数据是否压缩：0-否/1-是 */
    private Boolean newDataCompressed;

    // ==================== 追踪信息 ====================

    /** 全链路追踪ID，如SkyWalking TraceId */
    private String traceId;

    /** 当前Span ID */
    private String spanId;

    /** 父Span ID */
    private String parentSpanId;

    /** 调用链路节点，JSON数组 */
    private String tracePath;

    // ==================== 技术上下文 ====================

    /** 数据库查询次数 */
    private Integer dbQueryCount;

    /** 数据库查询耗时，毫秒 */
    private Integer dbQueryTimeMs;

    /** 外部接口调用次数 */
    private Integer externalCallCount;

    /** 外部接口耗时，毫秒 */
    private Integer externalCallTimeMs;

    /** 自定义指标，JSON格式 */
    private String customMetrics;

    // ==================== 请求响应数据 ====================

    /** 请求参数，JSON格式 */
    private String requestPayloadRef;

    /** 响应数据，JSON格式 */
    private String responsePayloadRef;

    /** 存储类型：OSS/S3/MINIO/DB */
    private String payloadStorageType;

    // ==================== 应用信息 ====================

    /** 应用名，如：order-service */
    private String appName;

    /** 应用版本，如：1.2.3 */
    private String appVersion;

    /** 环境标识，如：dev/test/prod */
    private String env;

    /** 集群标识，如：prod-cluster-1 */
    private String cluster;

    /** 主机名 */
    private String hostName;

    // ==================== 标签信息 ====================

    /** 标签键数组，如：['env','module'] */
    private String tagKeys;

    /** 标签值数组，如：['prod','order'] */
    private String tagValues;

    // ==================== 附件信息 ====================

    /** 附件数量 */
    private Integer attachmentCount;

    /** 附件类型数组，如：['IMAGE','PDF'] */
    private String attachmentTypes;

    /** 附件总大小，字节 */
    private Long attachmentTotalSize;

    /** 第一个附件的OSS引用，快速预览 */
    private String firstAttachmentRef;

    /** 所有附件引用数组，JSON格式 */
    private String attachmentRefs;

    // ==================== 扩展信息 ====================

    /** 业务标签，JSON格式 */
    private String dynamicTags;

    /** 业务扩展字段，JSON格式 */
    private String dynamicExtras;

    // ==================== 时间信息 ====================

    /** 记录创建时间 */
    private LocalDateTime createdTime;

    /** 记录更新时间 */
    private LocalDateTime updatedTime;

    // ==================== 便捷方法 ====================

    /**
     * 快速判断是否成功
     */
    public boolean isSuccess() {
        return "SUCCESS".equals(logStatus);
    }

    /**
     * 快速判断是否失败
     */
    public boolean isFail() {
        return "FAIL".equals(logStatus);
    }

    /**
     * 快速判断是否高风险
     */
    public boolean isHighRisk() {
        return riskLevel != null && riskLevel >= 70;
    }
}
