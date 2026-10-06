package com.carlos.audit.clickhouse;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * ClickHouse JSONEachRow 查询结果行（读取模型）
 *
 * <p>与 {@link ClickHouseBatchWriter.ClickHouseRow} 字段结构保持一致，
 * 但独立为读取模型，避免暴露写入器的内部序列化实现。</p>
 *
 * @author Carlos
 * @date 2026-06-12
 */
@Data
public class ClickHouseQueryRow {

    // 1. 主键与时间体系
    @JsonProperty("server_time")
    private String serverTime;
    @JsonProperty("event_date")
    private String eventDate;
    @JsonProperty("id")
    private Long id;
    @JsonProperty("client_time")
    private String clientTime;
    @JsonProperty("event_time")
    private String eventTime;
    @JsonProperty("duration_ms")
    private Integer durationMs;
    @JsonProperty("retention_deadline")
    private String retentionDeadline;

    // 2. 分类与版本
    @JsonProperty("log_schema_version")
    private Integer logSchemaVersion;
    @JsonProperty("category")
    private String category;
    @JsonProperty("log_type")
    private String logType;
    @JsonProperty("risk_level")
    private Integer riskLevel;

    // 3. 操作者体系
    @JsonProperty("principal_id")
    private String principalId;
    @JsonProperty("principal_type")
    private String principalType;
    @JsonProperty("principal_name")
    private String principalName;
    @JsonProperty("tenant_id")
    private String tenantId;
    @JsonProperty("dept_id")
    private String deptId;
    @JsonProperty("dept_name")
    private String deptName;
    @JsonProperty("dept_path")
    private String deptPath;

    // 4. 目标对象体系
    @JsonProperty("target_type")
    private String targetType;
    @JsonProperty("target_id")
    private String targetId;
    @JsonProperty("target_name")
    private String targetName;
    @JsonProperty("target_snapshot")
    private String targetSnapshot;

    // 5. 操作结果
    @JsonProperty("approval_comment")
    private String approvalComment;
    @JsonProperty("log_status")
    private String logStatus;
    @JsonProperty("result_code")
    private String resultCode;
    @JsonProperty("result_message")
    private String resultMessage;
    @JsonProperty("operation")
    private String operation;

    // 6. 网络与设备
    @JsonProperty("client_ip")
    private String clientIp;
    @JsonProperty("client_port")
    private Integer clientPort;
    @JsonProperty("server_ip")
    private String serverIp;
    @JsonProperty("user_agent")
    private String userAgent;
    @JsonProperty("device_fingerprint")
    private String deviceFingerprint;
    @JsonProperty("location_country")
    private String locationCountry;
    @JsonProperty("location_province")
    private String locationProvince;
    @JsonProperty("location_city")
    private String locationCity;
    @JsonProperty("location_lat")
    private BigDecimal locationLat;
    @JsonProperty("location_lon")
    private BigDecimal locationLon;

    // 7. 认证与权限
    @JsonProperty("auth_type")
    private String authType;
    @JsonProperty("auth_provider")
    private String authProvider;
    @JsonProperty("roles")
    private String roles;
    @JsonProperty("permissions")
    private String permissions;

    // 8. 业务上下文
    @JsonProperty("biz_channel")
    private String bizChannel;
    @JsonProperty("biz_scene")
    private String bizScene;
    @JsonProperty("biz_order_no")
    private String bizOrderNo;
    @JsonProperty("related_biz_ids")
    private String relatedBizIds;
    @JsonProperty("monetary_amount")
    private BigDecimal monetaryAmount;
    @JsonProperty("process_id")
    private String processId;

    // 9. 批量操作
    @JsonProperty("batch_id")
    private String batchId;
    @JsonProperty("batch_index")
    private Integer batchIndex;
    @JsonProperty("batch_total")
    private Integer batchTotal;

    // 10. 审批工作流
    @JsonProperty("task_id")
    private String taskId;
    @JsonProperty("approver_id")
    private String approverId;

    // 11. 数据变更详情
    @JsonProperty("has_data_change")
    private Boolean hasDataChange;
    @JsonProperty("entity_class")
    private String entityClass;
    @JsonProperty("table_name")
    private String tableName;
    @JsonProperty("change_summary")
    private String changeSummary;
    @JsonProperty("changed_field_count")
    private Integer changedFieldCount;
    @JsonProperty("old_data")
    private String oldData;
    @JsonProperty("new_data")
    private String newData;
    @JsonProperty("old_data_compressed")
    private Boolean oldDataCompressed;
    @JsonProperty("new_data_compressed")
    private Boolean newDataCompressed;

    // 12. 技术上下文
    @JsonProperty("trace_id")
    private String traceId;
    @JsonProperty("span_id")
    private String spanId;
    @JsonProperty("parent_span_id")
    private String parentSpanId;
    @JsonProperty("trace_path")
    private String tracePath;
    @JsonProperty("db_query_count")
    private Integer dbQueryCount;
    @JsonProperty("db_query_time_ms")
    private Integer dbQueryTimeMs;
    @JsonProperty("external_call_count")
    private Integer externalCallCount;
    @JsonProperty("external_call_time_ms")
    private Integer externalCallTimeMs;
    @JsonProperty("custom_metrics")
    private String customMetrics;

    // Payload 存储引用
    @JsonProperty("request_payload_ref")
    private String requestPayloadRef;
    @JsonProperty("response_payload_ref")
    private String responsePayloadRef;
    @JsonProperty("payload_storage_type")
    private String payloadStorageType;

    // 环境信息
    @JsonProperty("app_name")
    private String appName;
    @JsonProperty("app_version")
    private String appVersion;
    @JsonProperty("cluster")
    private String cluster;
    @JsonProperty("host_name")
    private String hostName;

    // 13. 动态标签（ClickHouse Array 列）
    @JsonProperty("tag_keys")
    private List<String> tagKeys;
    @JsonProperty("tag_values")
    private List<String> tagValues;

    // 14. 附件元数据
    @JsonProperty("attachment_count")
    private Integer attachmentCount;
    @JsonProperty("attachment_types")
    private List<String> attachmentTypes;
    @JsonProperty("attachment_total_size")
    private Long attachmentTotalSize;
    @JsonProperty("first_attachment_ref")
    private String firstAttachmentRef;
    @JsonProperty("attachment_refs")
    private String attachmentRefs;

    // 15. 动态扩展
    @JsonProperty("dynamic_tags")
    private String dynamicTags;
    @JsonProperty("dynamic_extras")
    private String dynamicExtras;

    // 16. 系统字段
    @JsonProperty("created_time")
    private String createdTime;
    @JsonProperty("updated_time")
    private String updatedTime;
}
