-- ============================================
-- Carlos Framework - carlos-message 模块
-- PostgreSQL 建表脚本
-- 数据库: PostgreSQL 14+
-- 生成日期: 2026-07-03
-- 说明: 从 docs/db/postgresql/05-carlos-message.sql 迁移并整合
-- ============================================

-- ----------------------------
-- 1. 消息类型表
-- ----------------------------
DROP TABLE IF EXISTS message_type CASCADE;
CREATE TABLE message_type
(
    id          BIGINT      NOT NULL,
    type_code   VARCHAR(32) NOT NULL,
    type_name   VARCHAR(32) NOT NULL,
    is_enabled  BOOLEAN    NOT NULL DEFAULT TRUE,
    is_deleted  SMALLINT    NOT NULL DEFAULT 0,
    create_by   BIGINT,
    create_time TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by   BIGINT,
    update_time TIMESTAMP,
    PRIMARY KEY (id)
);
COMMENT ON TABLE message_type IS '消息类型';
COMMENT ON COLUMN message_type.id IS '主键ID';
COMMENT ON COLUMN message_type.type_code IS '类型编码';
COMMENT ON COLUMN message_type.type_name IS '类型名称';
COMMENT ON COLUMN message_type.is_enabled IS '是否启用: 0-禁用 1-启用';
COMMENT ON COLUMN message_type.is_deleted IS '逻辑删除: 0-否 1-是';
COMMENT ON COLUMN message_type.create_by IS '创建者编号';
COMMENT ON COLUMN message_type.create_time IS '创建时间';
COMMENT ON COLUMN message_type.update_by IS '更新者编号';
COMMENT ON COLUMN message_type.update_time IS '更新时间';
CREATE UNIQUE INDEX uk_message_type_code ON message_type (type_code);

-- 消息类型初始化数据
INSERT INTO message_type (id, type_code, type_name, is_enabled)
VALUES (1, 'TASK_NOTIFY', '任务通知', 1),
       (2, 'VERIFY_CODE', '验证码', 1),
       (3, 'SYSTEM_NOTIFY', '系统通知', 1),
       (4, 'APPROVAL_REMIND', '审批提醒', 1),
       (5, 'MARKETING_PUSH', '营销推送', 1);


-- ----------------------------
-- 2. 消息模板表
-- ----------------------------
DROP TABLE IF EXISTS message_template CASCADE;
CREATE TABLE message_template
(
    id               BIGINT      NOT NULL,
    type_id          BIGINT      NOT NULL,
    template_code    VARCHAR(50) NOT NULL,
    template_name    VARCHAR(50) NOT NULL,
    title_template   VARCHAR(100)         DEFAULT NULL,
    content_template TEXT        NOT NULL,
    param_schema JSONB,
    channel_config JSONB,
    is_enabled       BOOLEAN    NOT NULL DEFAULT FALSE,
    is_deleted       SMALLINT    NOT NULL DEFAULT 0,
    create_by        BIGINT,
    create_time      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by        BIGINT,
    update_time      TIMESTAMP,
    PRIMARY KEY (id)
);
COMMENT ON TABLE message_template IS '消息模板';
COMMENT ON COLUMN message_template.id IS '主键ID';
COMMENT ON COLUMN message_template.type_id IS '消息类型ID';
COMMENT ON COLUMN message_template.template_code IS '模板编码';
COMMENT ON COLUMN message_template.template_name IS '模板名称';
COMMENT ON COLUMN message_template.title_template IS '标题模板';
COMMENT ON COLUMN message_template.content_template IS '模板内容';
COMMENT ON COLUMN message_template.param_schema IS '参数定义JSON';
COMMENT ON COLUMN message_template.channel_config IS '渠道特殊配置';
COMMENT ON COLUMN message_template.is_enabled IS '是否启用: 0-禁用 1-启用 2-草稿';
COMMENT ON COLUMN message_template.is_deleted IS '逻辑删除';
COMMENT ON COLUMN message_template.create_by IS '创建者编号';
COMMENT ON COLUMN message_template.create_time IS '创建时间';
COMMENT ON COLUMN message_template.update_by IS '更新者编号';
COMMENT ON COLUMN message_template.update_time IS '更新时间';
CREATE UNIQUE INDEX uk_message_template_code ON message_template (template_code);
CREATE INDEX idx_message_template_type ON message_template (type_id);
CREATE INDEX idx_message_template_enabled ON message_template (is_enabled);


-- ----------------------------
-- 3. 消息渠道配置表
-- ----------------------------
DROP TABLE IF EXISTS message_channel CASCADE;
CREATE TABLE message_channel
(
    id               BIGINT      NOT NULL,
    channel_code     VARCHAR(32) NOT NULL,
    channel_name     VARCHAR(32) NOT NULL,
    channel_type     SMALLINT    NOT NULL DEFAULT 1,
    provider         VARCHAR(32),
    channel_config JSONB NOT NULL,
    rate_limit_qps   INTEGER              DEFAULT 100,
    rate_limit_burst INTEGER              DEFAULT 200,
    retry_times      SMALLINT             DEFAULT 3,
    retry_interval   INTEGER              DEFAULT 1000,
    weight           INTEGER              DEFAULT 100,
    is_enabled       BOOLEAN    NOT NULL DEFAULT TRUE,
    is_deleted       SMALLINT    NOT NULL DEFAULT 0,
    create_by        BIGINT,
    create_time      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by        BIGINT,
    update_time      TIMESTAMP,
    PRIMARY KEY (id)
);
COMMENT ON TABLE message_channel IS '消息渠道配置';
COMMENT ON COLUMN message_channel.id IS '主键ID';
COMMENT ON COLUMN message_channel.channel_code IS '渠道编码';
COMMENT ON COLUMN message_channel.channel_name IS '渠道名称';
COMMENT ON COLUMN message_channel.channel_type IS '渠道类型: 1-短信 2-邮件 3-钉钉 4-企业微信 5-站内信';
COMMENT ON COLUMN message_channel.provider IS '服务商编码';
COMMENT ON COLUMN message_channel.channel_config IS '渠道配置JSON';
COMMENT ON COLUMN message_channel.rate_limit_qps IS '每秒最大请求数';
COMMENT ON COLUMN message_channel.rate_limit_burst IS '突发流量限制';
COMMENT ON COLUMN message_channel.retry_times IS '最大重试次数';
COMMENT ON COLUMN message_channel.retry_interval IS '重试间隔(ms)';
COMMENT ON COLUMN message_channel.weight IS '权重';
COMMENT ON COLUMN message_channel.is_enabled IS '是否启用: 0-禁用 1-启用 2-故障';
COMMENT ON COLUMN message_channel.is_deleted IS '逻辑删除';
COMMENT ON COLUMN message_channel.create_by IS '创建者编号';
COMMENT ON COLUMN message_channel.create_time IS '创建时间';
COMMENT ON COLUMN message_channel.update_by IS '更新者编号';
COMMENT ON COLUMN message_channel.update_time IS '更新时间';
CREATE UNIQUE INDEX uk_message_channel_code ON message_channel (channel_code);
CREATE INDEX idx_message_channel_type ON message_channel (channel_type);
CREATE INDEX idx_message_channel_enabled ON message_channel (is_enabled);


-- ----------------------------
-- 4. 消息记录表（核心表）
-- ----------------------------
DROP TABLE IF EXISTS message_record CASCADE;
CREATE TABLE message_record
(
    id               BIGINT      NOT NULL,
    message_id       VARCHAR(64) NOT NULL,
    template_code    VARCHAR(32) NOT NULL,
    template_params JSONB,
    message_type     VARCHAR(32) NOT NULL,
    message_title    VARCHAR(255),
    message_content  TEXT        NOT NULL,
    sender_id        VARCHAR(32) NOT NULL,
    sender_name      VARCHAR(64),
    sender_system    VARCHAR(50) NOT NULL,
    feedback_type    VARCHAR(16),
    feedback_content VARCHAR(255),
    priority         SMALLINT    NOT NULL DEFAULT 3,
    valid_until      TIMESTAMP,
    total_count      INTEGER              DEFAULT 0,
    success_count    INTEGER              DEFAULT 0,
    fail_count       INTEGER              DEFAULT 0,
    extras JSONB,
    create_by        BIGINT,
    create_time      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by        BIGINT,
    update_time      TIMESTAMP,
    PRIMARY KEY (id)
);
COMMENT ON TABLE message_record IS '消息记录表';
COMMENT ON COLUMN message_record.id IS '主键ID';
COMMENT ON COLUMN message_record.message_id IS '消息唯一标识';
COMMENT ON COLUMN message_record.template_code IS '模板编码';
COMMENT ON COLUMN message_record.template_params IS '模板参数JSON';
COMMENT ON COLUMN message_record.message_type IS '消息类型编码';
COMMENT ON COLUMN message_record.message_title IS '消息标题';
COMMENT ON COLUMN message_record.message_content IS '消息内容';
COMMENT ON COLUMN message_record.sender_id IS '发送人ID';
COMMENT ON COLUMN message_record.sender_name IS '发送人名称';
COMMENT ON COLUMN message_record.sender_system IS '发送系统标识';
COMMENT ON COLUMN message_record.feedback_type IS '操作反馈类型';
COMMENT ON COLUMN message_record.feedback_content IS '操作反馈内容';
COMMENT ON COLUMN message_record.priority IS '优先级: 1-紧急 2-高 3-普通 4-低';
COMMENT ON COLUMN message_record.valid_until IS '消息有效期';
COMMENT ON COLUMN message_record.total_count IS '总接收人数';
COMMENT ON COLUMN message_record.success_count IS '成功发送数';
COMMENT ON COLUMN message_record.fail_count IS '失败发送数';
COMMENT ON COLUMN message_record.extras IS '扩展信息';
COMMENT ON COLUMN message_record.create_by IS '创建人';
COMMENT ON COLUMN message_record.create_time IS '创建时间';
COMMENT ON COLUMN message_record.update_by IS '更新人';
COMMENT ON COLUMN message_record.update_time IS '更新时间';
CREATE UNIQUE INDEX uk_message_record_id ON message_record (message_id);
CREATE INDEX idx_message_record_template ON message_record (template_code);
CREATE INDEX idx_message_record_type ON message_record (message_type);
CREATE INDEX idx_message_record_sender ON message_record (sender_id);
CREATE INDEX idx_message_record_system ON message_record (sender_system);
CREATE INDEX idx_message_record_priority ON message_record (priority);
CREATE INDEX idx_message_record_time ON message_record (create_time);


-- ----------------------------
-- 5. 消息接收人表
-- ----------------------------
DROP TABLE IF EXISTS message_receiver CASCADE;
CREATE TABLE message_receiver
(
    id                 BIGINT      NOT NULL,
    message_id         VARCHAR(64) NOT NULL,
    channel_code       VARCHAR(32) NOT NULL,
    receiver_id        VARCHAR(64) NOT NULL,
    receiver_type      SMALLINT             DEFAULT 1,
    receiver_number    VARCHAR(128),
    receiver_audience  VARCHAR(64),
    channel_message_id VARCHAR(64),
    status             SMALLINT    NOT NULL DEFAULT 0,
    fail_count         SMALLINT             DEFAULT 0,
    fail_reason        VARCHAR(512),
    schedule_time      TIMESTAMP,
    send_time          TIMESTAMP,
    deliver_time       TIMESTAMP,
    read_time          TIMESTAMP,
    callback_url       VARCHAR(512),
    extras JSONB,
    create_by          BIGINT,
    create_time        TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by          BIGINT,
    update_time        TIMESTAMP,
    PRIMARY KEY (id)
);
COMMENT ON TABLE message_receiver IS '消息接收人表';
COMMENT ON COLUMN message_receiver.id IS '主键ID';
COMMENT ON COLUMN message_receiver.message_id IS '消息ID';
COMMENT ON COLUMN message_receiver.channel_code IS '渠道编码';
COMMENT ON COLUMN message_receiver.receiver_id IS '接收者ID';
COMMENT ON COLUMN message_receiver.receiver_type IS '接收人类型: 1-用户 2-部门 3-角色';
COMMENT ON COLUMN message_receiver.receiver_number IS '接收地址';
COMMENT ON COLUMN message_receiver.receiver_audience IS '接收者设备标识';
COMMENT ON COLUMN message_receiver.channel_message_id IS '渠道返回的消息ID';
COMMENT ON COLUMN message_receiver.status IS '状态: 0-待发送 1-发送中 2-已发送 3-送达 4-已读 5-失败 6-撤回';
COMMENT ON COLUMN message_receiver.fail_count IS '失败次数';
COMMENT ON COLUMN message_receiver.fail_reason IS '失败原因';
COMMENT ON COLUMN message_receiver.schedule_time IS '定时发送时间';
COMMENT ON COLUMN message_receiver.send_time IS '实际发送时间';
COMMENT ON COLUMN message_receiver.deliver_time IS '送达时间';
COMMENT ON COLUMN message_receiver.read_time IS '阅读时间';
COMMENT ON COLUMN message_receiver.callback_url IS '回调URL';
COMMENT ON COLUMN message_receiver.extras IS '扩展信息';
COMMENT ON COLUMN message_receiver.create_by IS '创建人';
COMMENT ON COLUMN message_receiver.create_time IS '创建时间';
COMMENT ON COLUMN message_receiver.update_by IS '更新人';
COMMENT ON COLUMN message_receiver.update_time IS '更新时间';
CREATE INDEX idx_message_receiver_msg ON message_receiver (message_id);
CREATE INDEX idx_message_receiver_channel ON message_receiver (channel_code);
CREATE INDEX idx_message_receiver_receiver ON message_receiver (receiver_id);
CREATE INDEX idx_message_receiver_status ON message_receiver (status);
CREATE INDEX idx_message_receiver_rs ON message_receiver (receiver_id, status);
CREATE INDEX idx_message_receiver_cs ON message_receiver (channel_code, status);
CREATE INDEX idx_message_receiver_ms ON message_receiver (message_id, status);
CREATE INDEX idx_message_receiver_schedule ON message_receiver (schedule_time, status);
CREATE INDEX idx_message_receiver_time ON message_receiver (create_time);


-- ----------------------------
-- 6. 消息发送日志表
-- ----------------------------
DROP TABLE IF EXISTS message_send_log CASCADE;
CREATE TABLE message_send_log
(
    id            BIGINT      NOT NULL,
    message_id    VARCHAR(64) NOT NULL,
    receiver_id   BIGINT      NOT NULL,
    channel_code  VARCHAR(32) NOT NULL,
    request_param TEXT,
    response_data TEXT,
    is_success    BOOLEAN    NOT NULL DEFAULT FALSE,
    error_code    VARCHAR(32),
    error_message VARCHAR(512),
    cost_time     INTEGER,
    create_time   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);
COMMENT ON TABLE message_send_log IS '消息发送日志';
COMMENT ON COLUMN message_send_log.id IS '主键ID';
COMMENT ON COLUMN message_send_log.message_id IS '消息ID';
COMMENT ON COLUMN message_send_log.receiver_id IS '接收人记录ID';
COMMENT ON COLUMN message_send_log.channel_code IS '渠道编码';
COMMENT ON COLUMN message_send_log.request_param IS '请求参数';
COMMENT ON COLUMN message_send_log.response_data IS '响应数据';
COMMENT ON COLUMN message_send_log.is_success IS '是否成功: 0-失败 1-成功';
COMMENT ON COLUMN message_send_log.error_code IS '错误码';
COMMENT ON COLUMN message_send_log.error_message IS '错误信息';
COMMENT ON COLUMN message_send_log.cost_time IS '耗时(ms)';
COMMENT ON COLUMN message_send_log.create_time IS '创建时间';
CREATE INDEX idx_message_send_log_msg ON message_send_log (message_id);
CREATE INDEX idx_message_send_log_receiver ON message_send_log (receiver_id);
CREATE INDEX idx_message_send_log_channel ON message_send_log (channel_code);
CREATE INDEX idx_message_send_log_success ON message_send_log (is_success);
CREATE INDEX idx_message_send_log_time ON message_send_log (create_time);
