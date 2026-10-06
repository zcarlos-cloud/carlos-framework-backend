-- ============================================
-- Carlos Framework - carlos-audit 模块
-- PostgreSQL 建表脚本
-- 数据库: PostgreSQL 14+
-- 生成日期: 2026-07-03
-- 说明: 从 docs/db/postgresql/04-carlos-audit.sql 迁移，采用宽主表设计
-- ============================================

-- ----------------------------
-- 1. 审计日志宽主表
-- ----------------------------
DROP TABLE IF EXISTS audit_log_main CASCADE;
CREATE TABLE audit_log_main
(
    id                    BIGINT       NOT NULL,
    server_time           TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    event_date            DATE         NOT NULL DEFAULT CURRENT_DATE,
    client_time           TIMESTAMP(3),
    event_time            TIMESTAMP(3),
    duration_ms           INTEGER,
    retention_deadline    DATE         NOT NULL,
    log_schema_version    SMALLINT     NOT NULL DEFAULT 3,
    category              VARCHAR(20)  NOT NULL,
    log_type              VARCHAR(50)  NOT NULL,
    risk_level            SMALLINT     NOT NULL DEFAULT 0,
    principal_id          VARCHAR(64)  NOT NULL,
    principal_type        VARCHAR(20)  NOT NULL DEFAULT 'USER',
    principal_name        VARCHAR(100),
    tenant_id             VARCHAR(64)  NOT NULL DEFAULT '0',
    dept_id               VARCHAR(64),
    dept_name             VARCHAR(64),
    dept_path             VARCHAR(500),
    target_type           VARCHAR(50),
    target_id             VARCHAR(64),
    target_name           VARCHAR(200),
    target_snapshot JSONB,
    log_status            VARCHAR(20)  NOT NULL DEFAULT 'SUCCESS',
    result_code           VARCHAR(50),
    result_message        VARCHAR(500),
    operation             VARCHAR(200) NOT NULL,
    approval_comment      VARCHAR(500),
    client_ip             VARCHAR(40),
    client_port           INTEGER,
    server_ip             VARCHAR(40),
    user_agent            VARCHAR(500),
    device_fingerprint    VARCHAR(64),
    location_country      VARCHAR(50),
    location_province     VARCHAR(50),
    location_city         VARCHAR(50),
    location_lat          NUMERIC(10, 8),
    location_lon          NUMERIC(11, 8),
    auth_type             VARCHAR(20),
    auth_provider         VARCHAR(30),
    roles JSONB,
    permissions JSONB,
    biz_channel           VARCHAR(20),
    biz_scene             VARCHAR(50),
    biz_order_no          VARCHAR(64),
    related_biz_ids JSONB,
    monetary_amount       NUMERIC(18, 4),
    process_id            VARCHAR(64),
    batch_id              VARCHAR(64),
    batch_index           INTEGER,
    batch_total           INTEGER,
    task_id               VARCHAR(64),
    approver_id           VARCHAR(64),
    has_data_change       BOOLEAN     NOT NULL DEFAULT FALSE,
    entity_class          VARCHAR(200),
    table_name            VARCHAR(100),
    change_summary        VARCHAR(500),
    changed_field_count   INTEGER,
    old_data JSONB,
    new_data JSONB,
    old_data_compressed   BOOLEAN     NOT NULL DEFAULT FALSE,
    new_data_compressed   BOOLEAN     NOT NULL DEFAULT FALSE,
    trace_id              VARCHAR(64),
    span_id               VARCHAR(32),
    parent_span_id        VARCHAR(32),
    trace_path JSONB,
    db_query_count        INTEGER,
    db_query_time_ms      INTEGER,
    external_call_count   INTEGER,
    external_call_time_ms INTEGER,
    custom_metrics JSONB,
    request_payload_ref   VARCHAR(500),
    response_payload_ref  VARCHAR(500),
    payload_storage_type  VARCHAR(20),
    app_name              VARCHAR(50),
    app_version           VARCHAR(30),
    cluster               VARCHAR(50),
    host_name             VARCHAR(100),
    tag_keys JSONB,
    tag_values JSONB,
    attachment_count      SMALLINT              DEFAULT 0,
    attachment_types JSONB,
    attachment_total_size BIGINT,
    first_attachment_ref  VARCHAR(500),
    attachment_refs JSONB,
    dynamic_tags JSONB,
    dynamic_extras JSONB,
    created_time          TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_time          TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    deleted               SMALLINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);
COMMENT ON TABLE audit_log_main IS '审计日志宽主表（合并数据变更、技术上下文、标签、附件）';
COMMENT ON COLUMN audit_log_main.id IS '主键，雪花ID';
COMMENT ON COLUMN audit_log_main.server_time IS '服务器时间，毫秒精度';
COMMENT ON COLUMN audit_log_main.event_date IS '事件日期，冗余字段，方便分区';
COMMENT ON COLUMN audit_log_main.client_time IS '客户端时间';
COMMENT ON COLUMN audit_log_main.event_time IS '事件实际发生时间';
COMMENT ON COLUMN audit_log_main.duration_ms IS '操作耗时，毫秒';
COMMENT ON COLUMN audit_log_main.retention_deadline IS '数据保留截止日期，TTL删除用';
COMMENT ON COLUMN audit_log_main.log_schema_version IS '日志Schema版本，用于兼容性处理';
COMMENT ON COLUMN audit_log_main.category IS '大类：SECURITY-安全/BUSINESS-业务/SYSTEM-系统/AUDIT-审计';
COMMENT ON COLUMN audit_log_main.log_type IS '细类：USER_LOGIN-登录/ORDER_PAY-支付/DATA_EXPORT-数据导出等';
COMMENT ON COLUMN audit_log_main.risk_level IS '风险等级 0-100';
COMMENT ON COLUMN audit_log_main.principal_id IS '操作主体ID';
COMMENT ON COLUMN audit_log_main.principal_type IS '主体类型：USER-用户/SERVICE-服务/SYSTEM-系统/ANONYMOUS-匿名';
COMMENT ON COLUMN audit_log_main.principal_name IS '主体名称，冗余存储避免关联查询';
COMMENT ON COLUMN audit_log_main.tenant_id IS '租户ID，SaaS多租户隔离字段';
COMMENT ON COLUMN audit_log_main.dept_id IS '部门ID';
COMMENT ON COLUMN audit_log_main.dept_name IS '部门名称，冗余存储';
COMMENT ON COLUMN audit_log_main.dept_path IS '部门层级路径，如：1/12/156/，方便层级查询';
COMMENT ON COLUMN audit_log_main.target_type IS '对象类型：ORDER-订单/USER-用户/CONFIG-配置/DATA-数据';
COMMENT ON COLUMN audit_log_main.target_id IS '对象唯一标识';
COMMENT ON COLUMN audit_log_main.target_name IS '对象显示名称，冗余存储';
COMMENT ON COLUMN audit_log_main.target_snapshot IS '对象关键信息摘要，JSON格式';
COMMENT ON COLUMN audit_log_main.log_status IS '状态：SUCCESS-成功/FAIL-失败/PENDING-处理中/TIMEOUT-超时';
COMMENT ON COLUMN audit_log_main.result_code IS '业务结果码，用于细分错误类型';
COMMENT ON COLUMN audit_log_main.result_message IS '结果描述，前500字符';
COMMENT ON COLUMN audit_log_main.operation IS '操作描述，人工可读的操作说明';
COMMENT ON COLUMN audit_log_main.approval_comment IS '审批意见，工作流用';
COMMENT ON COLUMN audit_log_main.client_ip IS '客户端IP，支持IPv6';
COMMENT ON COLUMN audit_log_main.client_port IS '客户端端口';
COMMENT ON COLUMN audit_log_main.server_ip IS '处理服务器IP';
COMMENT ON COLUMN audit_log_main.user_agent IS '浏览器UA，用于设备识别';
COMMENT ON COLUMN audit_log_main.device_fingerprint IS '设备指纹，唯一标识设备';
COMMENT ON COLUMN audit_log_main.location_country IS '国家';
COMMENT ON COLUMN audit_log_main.location_province IS '省份';
COMMENT ON COLUMN audit_log_main.location_city IS '城市';
COMMENT ON COLUMN audit_log_main.location_lat IS '纬度';
COMMENT ON COLUMN audit_log_main.location_lon IS '经度';
COMMENT ON COLUMN audit_log_main.auth_type IS '认证方式：PASSWORD-密码/SMS-短信/OAUTH2-OAuth2/LDAP-LDAP/CERT-证书';
COMMENT ON COLUMN audit_log_main.auth_provider IS '认证源：LOCAL-本地/WECHAT-微信/DINGTALK-钉钉';
COMMENT ON COLUMN audit_log_main.roles IS '当前角色列表，JSON数组';
COMMENT ON COLUMN audit_log_main.permissions IS '当前权限列表，JSON数组';
COMMENT ON COLUMN audit_log_main.biz_channel IS '业务渠道：WEB-网页/APP-移动应用/MINI_PROGRAM-小程序/OPEN_API-开放接口';
COMMENT ON COLUMN audit_log_main.biz_scene IS '业务场景，如：订单创建-ORDER_CREATE';
COMMENT ON COLUMN audit_log_main.biz_order_no IS '业务订单号，用于关联订单';
COMMENT ON COLUMN audit_log_main.related_biz_ids IS '关联业务ID列表，JSON数组';
COMMENT ON COLUMN audit_log_main.monetary_amount IS '涉及金额，精确到分';
COMMENT ON COLUMN audit_log_main.process_id IS '流程实例ID，工作流引擎生成';
COMMENT ON COLUMN audit_log_main.batch_id IS '批量操作批次号，UUID';
COMMENT ON COLUMN audit_log_main.batch_index IS '批次内序号，从0开始';
COMMENT ON COLUMN audit_log_main.batch_total IS '批次总数';
COMMENT ON COLUMN audit_log_main.task_id IS '任务ID';
COMMENT ON COLUMN audit_log_main.approver_id IS '审批人ID';
COMMENT ON COLUMN audit_log_main.has_data_change IS '是否有数据变更详情：0-否/1-是';
COMMENT ON COLUMN audit_log_main.entity_class IS '实体类名，如：com.example.Order';
COMMENT ON COLUMN audit_log_main.table_name IS '数据库表名，如：t_order';
COMMENT ON COLUMN audit_log_main.change_summary IS '变更摘要，人工可读的变更说明';
COMMENT ON COLUMN audit_log_main.changed_field_count IS '变更字段数量';
COMMENT ON COLUMN audit_log_main.old_data IS '变更前完整数据，JSON格式';
COMMENT ON COLUMN audit_log_main.new_data IS '变更后完整数据，JSON格式';
COMMENT ON COLUMN audit_log_main.old_data_compressed IS '旧数据是否压缩：0-否/1-是';
COMMENT ON COLUMN audit_log_main.new_data_compressed IS '新数据是否压缩：0-否/1-是';
COMMENT ON COLUMN audit_log_main.trace_id IS '全链路追踪ID，如SkyWalking TraceId';
COMMENT ON COLUMN audit_log_main.span_id IS '当前Span ID';
COMMENT ON COLUMN audit_log_main.parent_span_id IS '父Span ID';
COMMENT ON COLUMN audit_log_main.trace_path IS '调用链路节点，JSON数组';
COMMENT ON COLUMN audit_log_main.db_query_count IS '数据库查询次数';
COMMENT ON COLUMN audit_log_main.db_query_time_ms IS '数据库查询耗时，毫秒';
COMMENT ON COLUMN audit_log_main.external_call_count IS '外部接口调用次数';
COMMENT ON COLUMN audit_log_main.external_call_time_ms IS '外部接口耗时，毫秒';
COMMENT ON COLUMN audit_log_main.custom_metrics IS '自定义指标，JSON格式';
COMMENT ON COLUMN audit_log_main.request_payload_ref IS '请求数据OSS引用';
COMMENT ON COLUMN audit_log_main.response_payload_ref IS '响应数据OSS引用';
COMMENT ON COLUMN audit_log_main.payload_storage_type IS '存储类型：OSS/S3/MINIO/DB';
COMMENT ON COLUMN audit_log_main.app_name IS '应用名，如：order-service';
COMMENT ON COLUMN audit_log_main.app_version IS '应用版本，如：1.2.3';
COMMENT ON COLUMN audit_log_main.cluster IS '集群标识，如：prod-cluster-1';
COMMENT ON COLUMN audit_log_main.host_name IS '主机名';
COMMENT ON COLUMN audit_log_main.tag_keys IS '标签键数组，如：["env","module"]';
COMMENT ON COLUMN audit_log_main.tag_values IS '标签值数组，如：["prod","order"]';
COMMENT ON COLUMN audit_log_main.attachment_count IS '附件数量';
COMMENT ON COLUMN audit_log_main.attachment_types IS '附件类型数组，如：["IMAGE","PDF"]';
COMMENT ON COLUMN audit_log_main.attachment_total_size IS '附件总大小，字节';
COMMENT ON COLUMN audit_log_main.first_attachment_ref IS '第一个附件的OSS引用，快速预览';
COMMENT ON COLUMN audit_log_main.attachment_refs IS '所有附件引用数组，JSON格式';
COMMENT ON COLUMN audit_log_main.dynamic_tags IS '业务标签，JSON格式';
COMMENT ON COLUMN audit_log_main.dynamic_extras IS '业务扩展字段，JSON格式';
COMMENT ON COLUMN audit_log_main.created_time IS '记录创建时间';
COMMENT ON COLUMN audit_log_main.updated_time IS '记录更新时间';
COMMENT ON COLUMN audit_log_main.deleted IS '逻辑删除：0-未删除/1-已删除';

-- 核心查询索引
CREATE INDEX idx_audit_main_tenant_cat_time ON audit_log_main (tenant_id, category, server_time);
CREATE INDEX idx_audit_main_principal_time ON audit_log_main (principal_id, server_time);
CREATE INDEX idx_audit_main_target ON audit_log_main (target_type, target_id, server_time);
CREATE INDEX idx_audit_main_log_type_time ON audit_log_main (log_type, server_time);

-- 业务查询索引
CREATE INDEX idx_audit_main_biz_order ON audit_log_main (biz_order_no, server_time);
CREATE INDEX idx_audit_main_batch ON audit_log_main (batch_id, batch_index);
CREATE INDEX idx_audit_main_process ON audit_log_main (process_id);
CREATE INDEX idx_audit_main_trace ON audit_log_main (trace_id);

-- 安全风控索引
CREATE INDEX idx_audit_main_ip_time ON audit_log_main (client_ip, server_time);
CREATE INDEX idx_audit_main_device ON audit_log_main (device_fingerprint, server_time);
CREATE INDEX idx_audit_main_risk ON audit_log_main (risk_level, server_time);

-- 时间范围查询
CREATE INDEX idx_audit_main_retention ON audit_log_main (retention_deadline);
CREATE INDEX idx_audit_main_server_time ON audit_log_main (server_time);

-- 复合覆盖索引
CREATE INDEX idx_audit_main_cover_principal ON audit_log_main (principal_id, principal_type, tenant_id, server_time,
                                                               operation, log_status);


-- ----------------------------
-- 2. 审计日志归档记录表
-- ----------------------------
DROP TABLE IF EXISTS audit_log_archive_record CASCADE;
CREATE TABLE audit_log_archive_record
(
    id              BIGINT       NOT NULL,
    archive_id      VARCHAR(64)  NOT NULL,
    archive_date    DATE         NOT NULL,
    start_time      TIMESTAMP    NOT NULL,
    end_time        TIMESTAMP    NOT NULL,
    record_count    BIGINT       NOT NULL,
    file_size_bytes BIGINT       NOT NULL,
    storage_path    VARCHAR(500) NOT NULL,
    storage_type    VARCHAR(20)  NOT NULL,
    verify_checksum VARCHAR(64),
    archive_status  VARCHAR(32)  NOT NULL DEFAULT 'SUCCESS',
    created_time    TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id)
);
COMMENT ON TABLE audit_log_archive_record IS '审计日志归档记录（管理冷数据归档）';
COMMENT ON COLUMN audit_log_archive_record.id IS '主键';
COMMENT ON COLUMN audit_log_archive_record.archive_id IS '归档批次ID，UUID';
COMMENT ON COLUMN audit_log_archive_record.archive_date IS '归档日期';
COMMENT ON COLUMN audit_log_archive_record.start_time IS '归档数据起始时间';
COMMENT ON COLUMN audit_log_archive_record.end_time IS '归档数据结束时间';
COMMENT ON COLUMN audit_log_archive_record.record_count IS '归档记录数';
COMMENT ON COLUMN audit_log_archive_record.file_size_bytes IS '归档文件大小，字节';
COMMENT ON COLUMN audit_log_archive_record.storage_path IS '存储路径';
COMMENT ON COLUMN audit_log_archive_record.storage_type IS '存储类型：OSS/S3/LOCAL';
COMMENT ON COLUMN audit_log_archive_record.verify_checksum IS '校验和，MD5或SHA256';
COMMENT ON COLUMN audit_log_archive_record.archive_status IS '状态：SUCCESS-成功/FAILED-失败/PROCESSING-处理中';
COMMENT ON COLUMN audit_log_archive_record.created_time IS '创建时间';
CREATE UNIQUE INDEX uk_audit_archive_date ON audit_log_archive_record (archive_date);
CREATE INDEX idx_audit_archive_time ON audit_log_archive_record (start_time, end_time);


-- ----------------------------
-- 3. 审计日志配置表
-- ----------------------------
DROP TABLE IF EXISTS audit_log_config CASCADE;
CREATE TABLE audit_log_config
(
    id                BIGINT        NOT NULL,
    log_type          VARCHAR(50)   NOT NULL,
    retention_days    INTEGER       NOT NULL DEFAULT 90,
    sampling_rate     NUMERIC(3, 2) NOT NULL DEFAULT 1.00,
    async_write       BOOLEAN      NOT NULL DEFAULT TRUE,
    store_data_change BOOLEAN      NOT NULL DEFAULT TRUE,
    store_technical   BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted           SMALLINT      NOT NULL DEFAULT 0,
    tenant_id         VARCHAR(64)   NOT NULL DEFAULT '0',
    create_by         BIGINT,
    create_time       TIMESTAMP(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_by         BIGINT,
    update_time       TIMESTAMP(3),
    PRIMARY KEY (id)
);
COMMENT ON TABLE audit_log_config IS '审计日志配置（动态TTL与采样策略）';
COMMENT ON COLUMN audit_log_config.id IS '主键';
COMMENT ON COLUMN audit_log_config.log_type IS '日志类型，如：USER_LOGIN、ORDER_PAY 等';
COMMENT ON COLUMN audit_log_config.retention_days IS '保留天数';
COMMENT ON COLUMN audit_log_config.sampling_rate IS '采样率 0.00-1.00';
COMMENT ON COLUMN audit_log_config.async_write IS '是否异步写入：0-同步/1-异步';
COMMENT ON COLUMN audit_log_config.store_data_change IS '是否存储数据变更：0-否/1-是';
COMMENT ON COLUMN audit_log_config.store_technical IS '是否存储技术上下文：0-否/1-是';
COMMENT ON COLUMN audit_log_config.deleted IS '逻辑删除：0-未删除/1-已删除';
COMMENT ON COLUMN audit_log_config.tenant_id IS '租户ID，0表示系统级配置';
COMMENT ON COLUMN audit_log_config.create_by IS '创建者编号';
COMMENT ON COLUMN audit_log_config.create_time IS '创建时间';
COMMENT ON COLUMN audit_log_config.update_by IS '更新者编号';
COMMENT ON COLUMN audit_log_config.update_time IS '更新时间';
CREATE UNIQUE INDEX uk_audit_config_tenant_type ON audit_log_config (tenant_id, log_type);
