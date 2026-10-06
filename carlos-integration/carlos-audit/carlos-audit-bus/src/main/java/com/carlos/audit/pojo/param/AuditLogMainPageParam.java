package com.carlos.audit.pojo.param;


import com.carlos.core.param.ParamPage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;


/**
 * <p>
 * 审计日志宽主表（合并数据变更、技术上下文、标签、附件，保留7天热数据） 列表查询参数封装
 * </p>
 *
 * @author Carlos
 * @date 2026年3月6日 下午9:31:12
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@Schema(description = "审计日志宽主表（合并数据变更、技术上下文、标签、附件，保留7天热数据）列表查询参数")
public class AuditLogMainPageParam extends ParamPage {
    @Schema(description = "服务器时间，毫秒精度，CK分区键")
    private LocalDateTime serverTime;
    @Schema(description = "事件日期，冗余字段，方便分区")
    private LocalDate eventDate;
    @Schema(description = "客户端时间")
    private LocalDateTime clientTime;
    @Schema(description = "事件实际发生时间")
    private LocalDateTime eventTime;
    @Schema(description = "操作耗时，毫秒")
    private Integer durationMs;
    @Schema(description = "数据保留截止日期，TTL删除用")
    private LocalDate retentionDeadline;
    @Schema(description = "日志Schema版本，用于兼容性处理")
    private Integer logSchemaVersion;
    @Schema(description = "大类：SECURITY-安全/BUSINESS-业务/SYSTEM-系统/AUDIT-审计")
    private String category;
    @Schema(description = "细类：USER_LOGIN-登录/ORDER_PAY-支付/DATA_EXPORT-数据导出等")
    private String logType;
    @Schema(description = "风险等级 0-100，0为无风险，100为极高风险")
    private Integer riskLevel;
    @Schema(description = "操作主体ID，用户ID或服务账号")
    private String principalId;
    @Schema(description = "主体类型：USER-用户/SERVICE-服务/SYSTEM-系统/ANONYMOUS-匿名")
    private String principalType;
    @Schema(description = "主体名称，冗余存储避免关联查询")
    private String principalName;
    @Schema(description = "租户ID，SaaS多租户隔离字段")
    private String tenantId;
    @Schema(description = "部门ID")
    private String deptId;
    @Schema(description = "部门名称，冗余存储")
    private String deptName;
    @Schema(description = "部门层级路径，如：1/12/156/，方便层级查询")
    private String deptPath;
    @Schema(description = "对象类型：ORDER-订单/USER-用户/CONFIG-配置/DATA-数据")
    private String targetType;
    @Schema(description = "对象唯一标识")
    private String targetId;
    @Schema(description = "对象显示名称，冗余存储")
    private String targetName;
    @Schema(description = "对象关键信息摘要，JSON格式存储关键字段")
    private String targetSnapshot;
    @Schema(description = "状态：SUCCESS-成功/FAIL-失败/PENDING-处理中/TIMEOUT-超时/PARTIAL_SUCCESS-部分成功")
    private String logStatus;
    @Schema(description = "业务结果码，用于细分错误类型")
    private String resultCode;
    @Schema(description = "结果描述，前500字符")
    private String resultMessage;
    @Schema(description = "操作描述，人工可读的操作说明")
    private String operation;
    @Schema(description = "审批意见，工作流用")
    private String approvalComment;
    @Schema(description = "客户端IP，支持IPv6")
    private String clientIp;
    @Schema(description = "客户端端口")
    private Integer clientPort;
    @Schema(description = "处理服务器IP")
    private String serverIp;
    @Schema(description = "浏览器UA，用于设备识别")
    private String userAgent;
    @Schema(description = "设备指纹，唯一标识设备")
    private String deviceFingerprint;
    @Schema(description = "国家")
    private String locationCountry;
    @Schema(description = "省份")
    private String locationProvince;
    @Schema(description = "城市")
    private String locationCity;
    @Schema(description = "纬度")
    private BigDecimal locationLat;
    @Schema(description = "经度")
    private BigDecimal locationLon;
    @Schema(description = "认证方式：PASSWORD-密码/SMS-短信/OAUTH2-OAuth2/LDAP-LDAP/CERT-证书")
    private String authType;
    @Schema(description = "认证源：LOCAL-本地/WECHAT-微信/DINGTALK-钉钉")
    private String authProvider;
    @Schema(description = "当前角色列表，JSON数组")
    private String roles;
    @Schema(description = "当前权限列表，JSON数组")
    private String permissions;
    @Schema(description = "业务渠道：WEB-网页/APP-移动应用/MINI_PROGRAM-小程序/OPEN_API-开放接口")
    private String bizChannel;
    @Schema(description = "业务场景，如：订单创建-ORDER_CREATE")
    private String bizScene;
    @Schema(description = "业务订单号，用于关联订单")
    private String bizOrderNo;
    @Schema(description = "关联业务ID列表，JSON数组")
    private String relatedBizIds;
    @Schema(description = "涉及金额，精确到分")
    private BigDecimal monetaryAmount;
    @Schema(description = "流程实例ID，工作流引擎生成")
    private String processId;
    @Schema(description = "批量操作批次号，UUID")
    private String batchId;
    @Schema(description = "批次内序号，从0开始")
    private Integer batchIndex;
    @Schema(description = "批次总数")
    private Integer batchTotal;
    @Schema(description = "任务ID")
    private String taskId;
    @Schema(description = "审批人ID")
    private String approverId;
    @Schema(description = "是否有数据变更详情：0-否/1-是")
    private Boolean hasDataChange;
    @Schema(description = "实体类名，如：com.example.Order")
    private String entityClass;
    @Schema(description = "数据库表名，如：t_order")
    private String tableName;
    @Schema(description = "变更摘要，人工可读的变更说明")
    private String changeSummary;
    @Schema(description = "变更字段数量")
    private Integer changedFieldCount;
    @Schema(description = "变更前完整数据，JSON格式")
    private String oldData;
    @Schema(description = "变更后完整数据，JSON格式")
    private String newData;
    @Schema(description = "旧数据是否压缩：0-否/1-是")
    private Boolean oldDataCompressed;
    @Schema(description = "新数据是否压缩：0-否/1-是")
    private Boolean newDataCompressed;
    @Schema(description = "全链路追踪ID，如SkyWalking TraceId")
    private String traceId;
    @Schema(description = "当前Span ID")
    private String spanId;
    @Schema(description = "父Span ID")
    private String parentSpanId;
    @Schema(description = "调用链路节点，JSON数组")
    private String tracePath;
    @Schema(description = "数据库查询次数")
    private Integer dbQueryCount;
    @Schema(description = "数据库查询耗时，毫秒")
    private Integer dbQueryTimeMs;
    @Schema(description = "外部接口调用次数")
    private Integer externalCallCount;
    @Schema(description = "外部接口耗时，毫秒")
    private Integer externalCallTimeMs;
    @Schema(description = "自定义指标，JSON格式")
    private String customMetrics;
    @Schema(description = "请求数据OSS引用")
    private String requestPayloadRef;
    @Schema(description = "响应数据OSS引用")
    private String responsePayloadRef;
    @Schema(description = "存储类型：OSS/S3/MINIO/DB")
    private String payloadStorageType;
    @Schema(description = "应用名，如：order-service")
    private String appName;
    @Schema(description = "应用版本，如：1.2.3")
    private String appVersion;
    @Schema(description = "集群标识，如：prod-cluster-1")
    private String cluster;
    @Schema(description = "主机名")
    private String hostName;
    @Schema(description = "标签键数组，如：['env','module']")
    private String tagKeys;
    @Schema(description = "标签值数组，如：['prod','order']")
    private String tagValues;
    @Schema(description = "附件数量")
    private Integer attachmentCount;
    @Schema(description = "附件类型数组，如：['IMAGE','PDF']")
    private String attachmentTypes;
    @Schema(description = "附件总大小，字节")
    private Long attachmentTotalSize;
    @Schema(description = "第一个附件的OSS引用，快速预览")
    private String firstAttachmentRef;
    @Schema(description = "所有附件引用数组，JSON格式")
    private String attachmentRefs;
    @Schema(description = "业务标签，JSON格式")
    private String dynamicTags;
    @Schema(description = "业务扩展字段，JSON格式")
    private String dynamicExtras;
    @Schema(description = "记录创建时间")
    private LocalDateTime createdTime;
    @Schema(description = "记录更新时间")
    private LocalDateTime updatedTime;
    @Schema(description = "开始时间")
    private LocalDateTime start;

    @Schema(description = "结束时间")
    private LocalDateTime end;
}
