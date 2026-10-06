package com.carlos.audit.pojo.entity;


import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.carlos.audit.api.pojo.enums.*;
import com.carlos.audit.pojo.enums.AuditLogPayloadStorageTypeEnum;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * <p>
 * 审计日志宽主表（合并数据变更、技术上下文、标签、附件，保留7天热数据） 数据源对象
 * </p>
 *
 * @author Carlos
 * @date 2026年3月6日 下午9:31:12
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("audit_log_main")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditLogMain extends Model<AuditLogMain> implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 主键，雪花ID
     */
    @TableId(type = IdType.ASSIGN_ID, value = "id")
    private Long id;
    /**
     * 服务器时间，毫秒精度，CK分区键
     */
    @TableField(value = "server_time")
    private LocalDateTime serverTime;
    /**
     * 事件日期，冗余字段，方便分区
     */
    @TableField(value = "event_date")
    private LocalDate eventDate;
    /**
     * 客户端时间
     */
    @TableField(value = "client_time")
    private LocalDateTime clientTime;
    /**
     * 事件实际发生时间
     */
    @TableField(value = "event_time")
    private LocalDateTime eventTime;
    /**
     * 操作耗时，毫秒
     */
    @TableField(value = "duration_ms")
    private Integer durationMs;
    /**
     * 数据保留截止日期，TTL删除用
     */
    @TableField(value = "retention_deadline")
    private LocalDate retentionDeadline;
    /**
     * 日志Schema版本，用于兼容性处理
     */
    @TableField(value = "log_schema_version")
    private Integer logSchemaVersion;
    /**
     * 大类：SECURITY-安全/BUSINESS-业务/SYSTEM-系统/AUDIT-审计
     */
    @TableField(value = "category")
    private AuditLogCategoryEnum category;
    /**
     * 细类：自定义日志类型，如 USER_LOGIN、ORDER_PAY、DATA_EXPORT 等
     * 建议格式：业务域_操作，如 ORDER_CREATE、INVOICE_DELETE
     */
    @TableField(value = "log_type")
    private String logType;
    /**
     * 风险等级 0-100，0为无风险，100为极高风险
     */
    @TableField(value = "risk_level")
    private Integer riskLevel;
    /**
     * 操作主体ID，用户ID或服务账号
     */
    @TableField(value = "principal_id")
    private String principalId;
    /**
     * 主体类型：USER-用户/SERVICE-服务/SYSTEM-系统/ANONYMOUS-匿名
     */
    @TableField(value = "principal_type")
    private AuditLogPrincipalTypeEnum principalType;
    /**
     * 主体名称，冗余存储避免关联查询
     */
    @TableField(value = "principal_name")
    private String principalName;
    /**
     * 租户ID，SaaS多租户隔离字段
     */
    @TableField(value = "tenant_id")
    private String tenantId;
    /**
     * 部门ID
     */
    @TableField(value = "dept_id")
    private String deptId;
    /**
     * 部门名称，冗余存储
     */
    @TableField(value = "dept_name")
    private String deptName;
    /**
     * 部门层级路径，如：1/12/156/，方便层级查询
     */
    @TableField(value = "dept_path")
    private String deptPath;
    /**
     * 对象类型：ORDER-订单/USER-用户/CONFIG-配置/DATA-数据
     */
    @TableField(value = "target_type")
    private String targetType;
    /**
     * 对象唯一标识
     */
    @TableField(value = "target_id")
    private String targetId;
    /**
     * 对象显示名称，冗余存储
     */
    @TableField(value = "target_name")
    private String targetName;
    /**
     * 对象关键信息摘要，JSON格式存储关键字段
     */
    @TableField(value = "target_snapshot")
    private String targetSnapshot;
    /**
     * 状态：SUCCESS-成功/FAIL-失败/PENDING-处理中/TIMEOUT-超时/PARTIAL_SUCCESS-部分成功
     */
    @TableField(value = "log_status")
    private AuditLogStatusEnum logStatus;
    /**
     * 业务结果码，用于细分错误类型
     */
    @TableField(value = "result_code")
    private String resultCode;
    /**
     * 结果描述，前500字符
     */
    @TableField(value = "result_message")
    private String resultMessage;
    /**
     * 操作描述，人工可读的操作说明
     */
    @TableField(value = "operation")
    private String operation;
    /**
     * 审批意见，工作流用
     */
    @TableField(value = "approval_comment")
    private String approvalComment;
    /**
     * 客户端IP，支持IPv6
     */
    @TableField(value = "client_ip")
    private String clientIp;
    /**
     * 客户端端口
     */
    @TableField(value = "client_port")
    private Integer clientPort;
    /**
     * 处理服务器IP
     */
    @TableField(value = "server_ip")
    private String serverIp;
    /**
     * 浏览器UA，用于设备识别
     */
    @TableField(value = "user_agent")
    private String userAgent;
    /**
     * 设备指纹，唯一标识设备
     */
    @TableField(value = "device_fingerprint")
    private String deviceFingerprint;
    /**
     * 国家
     */
    @TableField(value = "location_country")
    private String locationCountry;
    /**
     * 省份
     */
    @TableField(value = "location_province")
    private String locationProvince;
    /**
     * 城市
     */
    @TableField(value = "location_city")
    private String locationCity;
    /**
     * 纬度
     */
    @TableField(value = "location_lat")
    private BigDecimal locationLat;
    /**
     * 经度
     */
    @TableField(value = "location_lon")
    private BigDecimal locationLon;
    /**
     * 认证方式：PASSWORD-密码/SMS-短信/OAUTH2-OAuth2/LDAP-LDAP/CERT-证书
     */
    @TableField(value = "auth_type")
    private AuditLogAuthTypeEnum authType;
    /**
     * 认证源：LOCAL-本地/WECHAT-微信/DINGTALK-钉钉
     */
    @TableField(value = "auth_provider")
    private AuditLogAuthProviderEnum authProvider;
    /**
     * 当前角色列表，JSON数组
     */
    @TableField(value = "roles")
    private String roles;
    /**
     * 当前权限列表，JSON数组
     */
    @TableField(value = "permissions")
    private String permissions;
    /**
     * 业务渠道：WEB-网页/APP-移动应用/MINI_PROGRAM-小程序/OPEN_API-开放接口
     */
    @TableField(value = "biz_channel")
    private AuditLogBizChannelEnum bizChannel;
    /**
     * 业务场景，如：订单创建-ORDER_CREATE
     */
    @TableField(value = "biz_scene")
    private String bizScene;
    /**
     * 业务订单号，用于关联订单
     */
    @TableField(value = "biz_order_no")
    private String bizOrderNo;
    /**
     * 关联业务ID列表，JSON数组
     */
    @TableField(value = "related_biz_ids")
    private String relatedBizIds;
    /**
     * 涉及金额，精确到分
     */
    @TableField(value = "monetary_amount")
    private BigDecimal monetaryAmount;
    /**
     * 流程实例ID，工作流引擎生成
     */
    @TableField(value = "process_id")
    private String processId;
    /**
     * 批量操作批次号，UUID
     */
    @TableField(value = "batch_id")
    private String batchId;
    /**
     * 批次内序号，从0开始
     */
    @TableField(value = "batch_index")
    private Integer batchIndex;
    /**
     * 批次总数
     */
    @TableField(value = "batch_total")
    private Integer batchTotal;
    /**
     * 任务ID
     */
    @TableField(value = "task_id")
    private String taskId;
    /**
     * 审批人ID
     */
    @TableField(value = "approver_id")
    private String approverId;
    /**
     * 是否有数据变更详情：0-否/1-是
     */
    @TableField(value = "has_data_change")
    private Boolean hasDataChange;
    /**
     * 实体类名，如：com.example.Order
     */
    @TableField(value = "entity_class")
    private String entityClass;
    /**
     * 数据库表名，如：t_order
     */
    @TableField(value = "table_name")
    private String tableName;
    /**
     * 变更摘要，人工可读的变更说明
     */
    @TableField(value = "change_summary")
    private String changeSummary;
    /**
     * 变更字段数量
     */
    @TableField(value = "changed_field_count")
    private Integer changedFieldCount;
    /**
     * 变更前完整数据，JSON格式
     */
    @TableField(value = "old_data")
    private String oldData;
    /**
     * 变更后完整数据，JSON格式
     */
    @TableField(value = "new_data")
    private String newData;
    /**
     * 旧数据是否压缩：0-否/1-是
     */
    @TableField(value = "old_data_compressed")
    private Boolean oldDataCompressed;
    /**
     * 新数据是否压缩：0-否/1-是
     */
    @TableField(value = "new_data_compressed")
    private Boolean newDataCompressed;
    /**
     * 全链路追踪ID，如SkyWalking TraceId
     */
    @TableField(value = "trace_id")
    private String traceId;
    /**
     * 当前Span ID
     */
    @TableField(value = "span_id")
    private String spanId;
    /**
     * 父Span ID
     */
    @TableField(value = "parent_span_id")
    private String parentSpanId;
    /**
     * 调用链路节点，JSON数组
     */
    @TableField(value = "trace_path")
    private String tracePath;
    /**
     * 数据库查询次数
     */
    @TableField(value = "db_query_count")
    private Integer dbQueryCount;
    /**
     * 数据库查询耗时，毫秒
     */
    @TableField(value = "db_query_time_ms")
    private Integer dbQueryTimeMs;
    /**
     * 外部接口调用次数
     */
    @TableField(value = "external_call_count")
    private Integer externalCallCount;
    /**
     * 外部接口耗时，毫秒
     */
    @TableField(value = "external_call_time_ms")
    private Integer externalCallTimeMs;
    /**
     * 自定义指标，JSON格式
     */
    @TableField(value = "custom_metrics")
    private String customMetrics;
    /**
     * 请求数据OSS引用
     */
    @TableField(value = "request_payload_ref")
    private String requestPayloadRef;
    /**
     * 响应数据OSS引用
     */
    @TableField(value = "response_payload_ref")
    private String responsePayloadRef;
    /**
     * 存储类型：OSS/S3/MINIO/DB
     */
    @TableField(value = "payload_storage_type")
    private AuditLogPayloadStorageTypeEnum payloadStorageType;
    /**
     * 应用名，如：order-service
     */
    @TableField(value = "app_name")
    private String appName;
    /**
     * 应用版本，如：1.2.3
     */
    @TableField(value = "app_version")
    private String appVersion;
    /**
     * 集群标识，如：prod-cluster-1
     */
    @TableField(value = "cluster")
    private String cluster;
    /**
     * 主机名
     */
    @TableField(value = "host_name")
    private String hostName;
    /**
     * 标签键数组，如：['env','module']
     */
    @TableField(value = "tag_keys")
    private String tagKeys;
    /**
     * 标签值数组，如：['prod','order']
     */
    @TableField(value = "tag_values")
    private String tagValues;
    /**
     * 附件数量
     */
    @TableField(value = "attachment_count")
    private Integer attachmentCount;
    /**
     * 附件类型数组，如：['IMAGE','PDF']
     */
    @TableField(value = "attachment_types")
    private String attachmentTypes;
    /**
     * 附件总大小，字节
     */
    @TableField(value = "attachment_total_size")
    private Long attachmentTotalSize;
    /**
     * 第一个附件的OSS引用，快速预览
     */
    @TableField(value = "first_attachment_ref")
    private String firstAttachmentRef;
    /**
     * 所有附件引用数组，JSON格式
     */
    @TableField(value = "attachment_refs")
    private String attachmentRefs;
    /**
     * 业务标签，JSON格式
     */
    @TableField(value = "dynamic_tags")
    private String dynamicTags;
    /**
     * 业务扩展字段，JSON格式
     */
    @TableField(value = "dynamic_extras")
    private String dynamicExtras;
    /**
     * 记录创建时间
     */
    @TableField(value = "created_time")
    private LocalDateTime createdTime;
    /**
     * 记录更新时间
     */
    @TableField(value = "updated_time")
    private LocalDateTime updatedTime;
    /**
     * 逻辑删除：0-未删除/1-已删除
     */
    @TableLogic
    @TableField(value = "deleted")
    private Boolean deleted;

}
