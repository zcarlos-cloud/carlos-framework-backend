-- ============================================================================
-- 系统监控模块 PostgreSQL 元数据表 DDL
-- ----------------------------------------------------------------------------
-- 来源：《DESIGN-20260930-系统监控模块库表设计》第 2 节（原样提取）
-- 目标库：PostgreSQL 14+（框架主库）
-- 表清单（12 张）：
--   1  monitor_instance_group        实例分组
--   2  monitor_group_auth            分组-角色授权
--   3  monitor_instance              监控实例注册
--   4  monitor_instance_app_rel      实例-应用关联
--   5  monitor_data_mask_rule        敏感列/字段脱敏规则
--   6  monitor_alarm_rule            告警规则
--   7  monitor_alarm_record          告警记录
--   8  monitor_alarm_silence         告警静默
--   9  monitor_bigkey_report         大 key/热 key 报告主表
--   10 monitor_bigkey_report_item    报告明细
--   11 monitor_db_capacity_snapshot  库表容量每日快照
--   12 monitor_sql_history           SQL 控制台执行历史
-- ============================================================================

-- ----------------------------
-- 1. 实例分组
-- ----------------------------
DROP TABLE IF EXISTS monitor_instance_group CASCADE;
CREATE TABLE monitor_instance_group
(
    id          BIGINT       NOT NULL,
    group_name  VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    order_num   INT          NOT NULL DEFAULT 0,
    create_by   BIGINT,
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by   BIGINT,
    update_time TIMESTAMP,
    is_deleted  SMALLINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);
COMMENT ON TABLE monitor_instance_group IS '监控实例分组';
COMMENT ON COLUMN monitor_instance_group.id IS '主键（雪花ID）';
COMMENT ON COLUMN monitor_instance_group.group_name IS '分组名称';
COMMENT ON COLUMN monitor_instance_group.description IS '分组描述';
COMMENT ON COLUMN monitor_instance_group.order_num IS '排序号';
COMMENT ON COLUMN monitor_instance_group.create_by IS '创建人';
COMMENT ON COLUMN monitor_instance_group.create_time IS '创建时间';
COMMENT ON COLUMN monitor_instance_group.update_by IS '修改人';
COMMENT ON COLUMN monitor_instance_group.update_time IS '修改时间';
COMMENT ON COLUMN monitor_instance_group.is_deleted IS '逻辑删除：0正常 1删除';
-- 部分唯一索引：与 MyBatis-Plus 逻辑删除兼容，已删除行不占用唯一键（下同）
CREATE UNIQUE INDEX uk_monitor_instance_group_name ON monitor_instance_group (group_name) WHERE is_deleted = 0;

-- ----------------------------
-- 2. 分组-角色授权
-- ----------------------------
DROP TABLE IF EXISTS monitor_group_auth CASCADE;
CREATE TABLE monitor_group_auth
(
    id          BIGINT    NOT NULL,
    group_id    BIGINT    NOT NULL,
    role_id     BIGINT    NOT NULL,
    create_by   BIGINT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by   BIGINT,
    update_time TIMESTAMP,
    is_deleted  SMALLINT  NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);
COMMENT ON TABLE monitor_group_auth IS '监控分组-角色授权';
COMMENT ON COLUMN monitor_group_auth.id IS '主键（雪花ID）';
COMMENT ON COLUMN monitor_group_auth.group_id IS '分组ID';
COMMENT ON COLUMN monitor_group_auth.role_id IS '角色ID（逻辑关联 org 角色）';
CREATE UNIQUE INDEX uk_monitor_group_auth ON monitor_group_auth (group_id, role_id) WHERE is_deleted = 0;
CREATE INDEX idx_monitor_group_auth_role ON monitor_group_auth (role_id);

-- ----------------------------
-- 3. 监控实例
-- ----------------------------
DROP TABLE IF EXISTS monitor_instance CASCADE;
CREATE TABLE monitor_instance
(
    id                BIGINT       NOT NULL,
    instance_name     VARCHAR(100) NOT NULL,
    instance_type     VARCHAR(30)  NOT NULL,
    env               VARCHAR(20)  NOT NULL DEFAULT 'prod',
    group_id          BIGINT,
    addresses         VARCHAR(1000) NOT NULL,
    db_name           VARCHAR(100),
    db_index          INT,
    username          VARCHAR(100),
    password_cipher   VARCHAR(500),
    ext_config        VARCHAR(2000),
    collect_enabled   BOOLEAN      NOT NULL DEFAULT TRUE,
    collect_interval  INT          NOT NULL DEFAULT 30,
    connect_timeout   INT          NOT NULL DEFAULT 3000,
    socket_timeout    INT          NOT NULL DEFAULT 5000,
    manage_enabled    BOOLEAN      NOT NULL DEFAULT TRUE,
    sql_write_enabled BOOLEAN      NOT NULL DEFAULT FALSE,
    health_status     VARCHAR(20)  NOT NULL DEFAULT 'UNKNOWN',
    last_collect_time TIMESTAMP,
    remark            VARCHAR(500),
    create_by         BIGINT,
    create_time       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by         BIGINT,
    update_time       TIMESTAMP,
    is_deleted        SMALLINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);
COMMENT ON TABLE monitor_instance IS '监控实例（中间件/数据库注册）';
COMMENT ON COLUMN monitor_instance.id IS '主键（雪花ID）';
COMMENT ON COLUMN monitor_instance.instance_name IS '实例名称';
COMMENT ON COLUMN monitor_instance.instance_type IS '实例类型（字典 monitor_instance_type）：REDIS_STANDALONE/REDIS_CLUSTER/REDIS_SENTINEL/POSTGRESQL/MYSQL/DM/CLICKHOUSE';
COMMENT ON COLUMN monitor_instance.env IS '环境（字典 monitor_env）：dev/test/prod';
COMMENT ON COLUMN monitor_instance.group_id IS '所属分组ID';
COMMENT ON COLUMN monitor_instance.addresses IS '连接地址，host:port 逗号分隔（cluster/sentinel 多节点）';
COMMENT ON COLUMN monitor_instance.db_name IS '数据库名（DB类型必填）';
COMMENT ON COLUMN monitor_instance.db_index IS 'Redis 逻辑库编号（standalone 用，cluster 忽略）';
COMMENT ON COLUMN monitor_instance.username IS '连接账号';
COMMENT ON COLUMN monitor_instance.password_cipher IS '连接密码（SM4 密文，接口永不回显）';
COMMENT ON COLUMN monitor_instance.ext_config IS '扩展配置JSON：ssl/sentinel masterName/拓扑刷新等';
COMMENT ON COLUMN monitor_instance.collect_enabled IS '采集开关';
COMMENT ON COLUMN monitor_instance.collect_interval IS '采集间隔（秒，10~600）';
COMMENT ON COLUMN monitor_instance.connect_timeout IS '连接超时（毫秒）';
COMMENT ON COLUMN monitor_instance.socket_timeout IS '读写超时（毫秒）';
COMMENT ON COLUMN monitor_instance.manage_enabled IS '数据管理入口开关（关闭则仅监控）';
COMMENT ON COLUMN monitor_instance.sql_write_enabled IS 'SQL 写模式开关（默认关闭，prod 实例需管理员显式开启）';
COMMENT ON COLUMN monitor_instance.health_status IS '健康状态冗余：UNKNOWN/UP/DOWN（由采集结果回写）';
COMMENT ON COLUMN monitor_instance.last_collect_time IS '最近成功采集时间';
COMMENT ON COLUMN monitor_instance.remark IS '备注';
CREATE UNIQUE INDEX uk_monitor_instance_name_env ON monitor_instance (instance_name, env) WHERE is_deleted = 0;
CREATE INDEX idx_monitor_instance_type ON monitor_instance (instance_type, env);
CREATE INDEX idx_monitor_instance_group ON monitor_instance (group_id);

-- ----------------------------
-- 4. 实例-应用关联
-- ----------------------------
DROP TABLE IF EXISTS monitor_instance_app_rel CASCADE;
CREATE TABLE monitor_instance_app_rel
(
    id          BIGINT       NOT NULL,
    instance_id BIGINT       NOT NULL,
    app_name    VARCHAR(100) NOT NULL,
    create_by   BIGINT,
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by   BIGINT,
    update_time TIMESTAMP,
    is_deleted  SMALLINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);
COMMENT ON TABLE monitor_instance_app_rel IS '监控实例-应用关联（多对多）';
COMMENT ON COLUMN monitor_instance_app_rel.id IS '主键（雪花ID）';
COMMENT ON COLUMN monitor_instance_app_rel.instance_id IS '实例ID';
COMMENT ON COLUMN monitor_instance_app_rel.app_name IS '应用名（微服务 spring.application.name）';
CREATE UNIQUE INDEX uk_monitor_instance_app ON monitor_instance_app_rel (instance_id, app_name) WHERE is_deleted = 0;
CREATE INDEX idx_monitor_instance_app_name ON monitor_instance_app_rel (app_name);

-- ----------------------------
-- 5. 数据脱敏规则
-- ----------------------------
DROP TABLE IF EXISTS monitor_data_mask_rule CASCADE;
CREATE TABLE monitor_data_mask_rule
(
    id           BIGINT       NOT NULL,
    instance_id  BIGINT,
    target_type  VARCHAR(20)  NOT NULL DEFAULT 'DB',
    schema_name  VARCHAR(100),
    table_name   VARCHAR(100),
    column_name  VARCHAR(100) NOT NULL,
    mask_type    VARCHAR(30)  NOT NULL,
    mask_pattern VARCHAR(200),
    enabled      BOOLEAN      NOT NULL DEFAULT TRUE,
    create_by    BIGINT,
    create_time  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by    BIGINT,
    update_time  TIMESTAMP,
    is_deleted   SMALLINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);
COMMENT ON TABLE monitor_data_mask_rule IS '数据管理脱敏规则';
COMMENT ON COLUMN monitor_data_mask_rule.id IS '主键（雪花ID）';
COMMENT ON COLUMN monitor_data_mask_rule.instance_id IS '实例ID，NULL 表示全局规则';
COMMENT ON COLUMN monitor_data_mask_rule.target_type IS '作用目标：DB=库表列，REDIS=按 key pattern（此时 table_name 存 pattern）';
COMMENT ON COLUMN monitor_data_mask_rule.schema_name IS '模式名（DB）';
COMMENT ON COLUMN monitor_data_mask_rule.table_name IS '表名 / Redis key pattern';
COMMENT ON COLUMN monitor_data_mask_rule.column_name IS '列名 / hash field 名';
COMMENT ON COLUMN monitor_data_mask_rule.mask_type IS '脱敏类型：FULL/PHONE/EMAIL/NAME/CUSTOM';
COMMENT ON COLUMN monitor_data_mask_rule.mask_pattern IS 'CUSTOM 时的正则与替换模板';
COMMENT ON COLUMN monitor_data_mask_rule.enabled IS '启用开关';
CREATE INDEX idx_monitor_data_mask_instance ON monitor_data_mask_rule (instance_id, target_type);

-- ----------------------------
-- 6. 告警规则
-- ----------------------------
DROP TABLE IF EXISTS monitor_alarm_rule CASCADE;
CREATE TABLE monitor_alarm_rule
(
    id              BIGINT        NOT NULL,
    rule_name       VARCHAR(100)  NOT NULL,
    metric_source   VARCHAR(20)   NOT NULL,
    target_type     VARCHAR(20)   NOT NULL DEFAULT 'INSTANCE',
    target_id       BIGINT,
    metric_name     VARCHAR(100)  NOT NULL,
    condition_op    VARCHAR(10)   NOT NULL,
    threshold_value DECIMAL(18,4) NOT NULL,
    duration_count  INT           NOT NULL DEFAULT 1,
    alarm_level     VARCHAR(20)   NOT NULL DEFAULT 'WARN',
    notify_channels VARCHAR(200)  NOT NULL DEFAULT 'SITE',
    notify_targets  VARCHAR(1000),
    enabled         BOOLEAN       NOT NULL DEFAULT TRUE,
    remark          VARCHAR(500),
    create_by       BIGINT,
    create_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP,
    is_deleted      SMALLINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);
COMMENT ON TABLE monitor_alarm_rule IS '告警规则';
COMMENT ON COLUMN monitor_alarm_rule.id IS '主键（雪花ID）';
COMMENT ON COLUMN monitor_alarm_rule.rule_name IS '规则名称';
COMMENT ON COLUMN monitor_alarm_rule.metric_source IS '指标来源：REDIS/DB/SERVICE';
COMMENT ON COLUMN monitor_alarm_rule.target_type IS '作用范围：INSTANCE=单实例，GROUP=分组，ALL=全部';
COMMENT ON COLUMN monitor_alarm_rule.target_id IS '作用对象ID（target_type=ALL 时为 NULL）';
COMMENT ON COLUMN monitor_alarm_rule.metric_name IS '指标名（如 redis.memory.used_percent）';
COMMENT ON COLUMN monitor_alarm_rule.condition_op IS '比较符：GT/GTE/LT/LTE/EQ/NE/CHANGE_RATE_GT/CHANGE_RATE_LT（环比变化率%）';
COMMENT ON COLUMN monitor_alarm_rule.threshold_value IS '阈值';
COMMENT ON COLUMN monitor_alarm_rule.duration_count IS '连续触发周期数（防抖动）';
COMMENT ON COLUMN monitor_alarm_rule.alarm_level IS '告警级别（字典 monitor_alarm_level）：INFO/WARN/CRITICAL';
COMMENT ON COLUMN monitor_alarm_rule.notify_channels IS '通知渠道逗号分隔：SITE/MAIL/DINGTALK（对接 carlos-message）';
COMMENT ON COLUMN monitor_alarm_rule.notify_targets IS '通知目标JSON：{"userIds":[],"roleIds":[]}';
COMMENT ON COLUMN monitor_alarm_rule.enabled IS '启用开关';
CREATE INDEX idx_monitor_alarm_rule_target ON monitor_alarm_rule (metric_source, target_type, target_id);

-- ----------------------------
-- 7. 告警记录
-- ----------------------------
DROP TABLE IF EXISTS monitor_alarm_record CASCADE;
CREATE TABLE monitor_alarm_record
(
    id                 BIGINT        NOT NULL,
    rule_id            BIGINT        NOT NULL,
    rule_name          VARCHAR(100)  NOT NULL,
    instance_id        BIGINT        NOT NULL,
    instance_name      VARCHAR(100)  NOT NULL,
    metric_name        VARCHAR(100)  NOT NULL,
    metric_value       DECIMAL(18,4),
    threshold_value    DECIMAL(18,4),
    alarm_level        VARCHAR(20)   NOT NULL,
    alarm_status       VARCHAR(20)   NOT NULL DEFAULT 'FIRING',
    alarm_content      VARCHAR(2000),
    first_trigger_time TIMESTAMP     NOT NULL,
    last_trigger_time  TIMESTAMP     NOT NULL,
    recover_time       TIMESTAMP,
    notify_status      VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    confirm_by         BIGINT,
    confirm_time       TIMESTAMP,
    handle_remark      VARCHAR(1000),
    create_by          BIGINT,
    create_time        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by          BIGINT,
    update_time        TIMESTAMP,
    is_deleted         SMALLINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);
COMMENT ON TABLE monitor_alarm_record IS '告警记录';
COMMENT ON COLUMN monitor_alarm_record.id IS '主键（雪花ID）';
COMMENT ON COLUMN monitor_alarm_record.rule_id IS '规则ID（冗余 rule_name 防规则改名失联）';
COMMENT ON COLUMN monitor_alarm_record.instance_id IS '实例ID（冗余 instance_name）';
COMMENT ON COLUMN monitor_alarm_record.metric_value IS '触发时指标值';
COMMENT ON COLUMN monitor_alarm_record.alarm_status IS '状态：FIRING=触发中，CONFIRMED=已确认，RECOVERED=已恢复';
COMMENT ON COLUMN monitor_alarm_record.alarm_content IS '告警内容（通知文案）';
COMMENT ON COLUMN monitor_alarm_record.first_trigger_time IS '首次触发时间';
COMMENT ON COLUMN monitor_alarm_record.last_trigger_time IS '最近触发时间（未恢复重复触发时刷新）';
COMMENT ON COLUMN monitor_alarm_record.recover_time IS '恢复时间';
COMMENT ON COLUMN monitor_alarm_record.notify_status IS '通知状态：PENDING/SENT/FAILED/SILENCED';
COMMENT ON COLUMN monitor_alarm_record.confirm_by IS '确认人';
COMMENT ON COLUMN monitor_alarm_record.handle_remark IS '处理备注';
CREATE INDEX idx_monitor_alarm_record_status ON monitor_alarm_record (alarm_status, alarm_level);
CREATE INDEX idx_monitor_alarm_record_instance ON monitor_alarm_record (instance_id, create_time);
CREATE INDEX idx_monitor_alarm_record_rule ON monitor_alarm_record (rule_id, alarm_status);

-- ----------------------------
-- 8. 告警静默
-- ----------------------------
DROP TABLE IF EXISTS monitor_alarm_silence CASCADE;
CREATE TABLE monitor_alarm_silence
(
    id            BIGINT       NOT NULL,
    rule_id       BIGINT       NOT NULL,
    instance_id   BIGINT,
    silence_start TIMESTAMP    NOT NULL,
    silence_end   TIMESTAMP    NOT NULL,
    reason        VARCHAR(500),
    create_by     BIGINT,
    create_time   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by     BIGINT,
    update_time   TIMESTAMP,
    is_deleted    SMALLINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);
COMMENT ON TABLE monitor_alarm_silence IS '告警静默';
COMMENT ON COLUMN monitor_alarm_silence.id IS '主键（雪花ID）';
COMMENT ON COLUMN monitor_alarm_silence.rule_id IS '规则ID';
COMMENT ON COLUMN monitor_alarm_silence.instance_id IS '实例ID，NULL 表示该规则下全部实例静默';
COMMENT ON COLUMN monitor_alarm_silence.silence_start IS '静默开始时间';
COMMENT ON COLUMN monitor_alarm_silence.silence_end IS '静默结束时间';
COMMENT ON COLUMN monitor_alarm_silence.reason IS '静默原因';
CREATE INDEX idx_monitor_alarm_silence_rule ON monitor_alarm_silence (rule_id, silence_end);

-- ----------------------------
-- 9. Redis 大key/热key 分析报告
-- ----------------------------
DROP TABLE IF EXISTS monitor_bigkey_report CASCADE;
CREATE TABLE monitor_bigkey_report
(
    id            BIGINT       NOT NULL,
    instance_id   BIGINT       NOT NULL,
    report_type   VARCHAR(20)  NOT NULL,
    trigger_type  VARCHAR(20)  NOT NULL DEFAULT 'MANUAL',
    scan_status   VARCHAR(20)  NOT NULL DEFAULT 'RUNNING',
    scanned_keys  BIGINT       NOT NULL DEFAULT 0,
    sample_rate   INT          NOT NULL DEFAULT 100,
    start_time    TIMESTAMP    NOT NULL,
    end_time      TIMESTAMP,
    error_msg     VARCHAR(1000),
    create_by     BIGINT,
    create_time   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by     BIGINT,
    update_time   TIMESTAMP,
    is_deleted    SMALLINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);
COMMENT ON TABLE monitor_bigkey_report IS 'Redis 大key/热key 分析报告';
COMMENT ON COLUMN monitor_bigkey_report.id IS '主键（雪花ID）';
COMMENT ON COLUMN monitor_bigkey_report.instance_id IS 'Redis 实例ID';
COMMENT ON COLUMN monitor_bigkey_report.report_type IS '报告类型：BIGKEY/HOTKEY';
COMMENT ON COLUMN monitor_bigkey_report.trigger_type IS '触发方式：MANUAL/SCHEDULED';
COMMENT ON COLUMN monitor_bigkey_report.scan_status IS '扫描状态：RUNNING/SUCCESS/FAILED';
COMMENT ON COLUMN monitor_bigkey_report.scanned_keys IS '已扫描 key 数量';
COMMENT ON COLUMN monitor_bigkey_report.sample_rate IS '采样率（%），100=全量 SCAN';
COMMENT ON COLUMN monitor_bigkey_report.start_time IS '开始时间';
COMMENT ON COLUMN monitor_bigkey_report.end_time IS '结束时间';
COMMENT ON COLUMN monitor_bigkey_report.error_msg IS '失败原因';
CREATE INDEX idx_monitor_bigkey_report_instance ON monitor_bigkey_report (instance_id, report_type, create_time);

-- ----------------------------
-- 10. Redis 大key/热key 报告明细
-- ----------------------------
DROP TABLE IF EXISTS monitor_bigkey_report_item CASCADE;
CREATE TABLE monitor_bigkey_report_item
(
    id            BIGINT       NOT NULL,
    report_id     BIGINT       NOT NULL,
    node_address  VARCHAR(100),
    redis_key     VARCHAR(500) NOT NULL,
    key_type      VARCHAR(20)  NOT NULL,
    memory_bytes  BIGINT,
    element_count BIGINT,
    ttl_seconds   BIGINT,
    access_freq   BIGINT,
    estimated     BOOLEAN      NOT NULL DEFAULT FALSE,
    create_time   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted    SMALLINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);
COMMENT ON TABLE monitor_bigkey_report_item IS 'Redis 大key/热key 报告明细';
COMMENT ON COLUMN monitor_bigkey_report_item.id IS '主键（雪花ID）';
COMMENT ON COLUMN monitor_bigkey_report_item.report_id IS '报告ID';
COMMENT ON COLUMN monitor_bigkey_report_item.node_address IS '节点地址（cluster 模式标识 key 所在分片）';
COMMENT ON COLUMN monitor_bigkey_report_item.redis_key IS 'key 名';
COMMENT ON COLUMN monitor_bigkey_report_item.key_type IS '类型：string/hash/list/set/zset/stream';
COMMENT ON COLUMN monitor_bigkey_report_item.memory_bytes IS '内存占用（字节，MEMORY USAGE）';
COMMENT ON COLUMN monitor_bigkey_report_item.element_count IS '元素数量';
COMMENT ON COLUMN monitor_bigkey_report_item.ttl_seconds IS 'TTL（秒），-1 永不过期';
COMMENT ON COLUMN monitor_bigkey_report_item.access_freq IS '访问频次（热key报告，OBJECT FREQ / MONITOR 采样计数）';
COMMENT ON COLUMN monitor_bigkey_report_item.estimated IS '内存是否为估算值（Redis < 4.0 无 MEMORY USAGE 时按元素长度估算）';
CREATE INDEX idx_monitor_bigkey_item_report ON monitor_bigkey_report_item (report_id, memory_bytes);

-- ----------------------------
-- 11. 数据库表容量每日快照
-- ----------------------------
DROP TABLE IF EXISTS monitor_db_capacity_snapshot CASCADE;
CREATE TABLE monitor_db_capacity_snapshot
(
    id               BIGINT       NOT NULL,
    instance_id      BIGINT       NOT NULL,
    schema_name      VARCHAR(100) NOT NULL,
    table_name       VARCHAR(200) NOT NULL,
    table_rows       BIGINT,
    table_size_bytes BIGINT,
    index_size_bytes BIGINT,
    snapshot_date    DATE         NOT NULL,
    create_time      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted       SMALLINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);
COMMENT ON TABLE monitor_db_capacity_snapshot IS '数据库表容量每日快照';
COMMENT ON COLUMN monitor_db_capacity_snapshot.id IS '主键（雪花ID）';
COMMENT ON COLUMN monitor_db_capacity_snapshot.instance_id IS '数据库实例ID';
COMMENT ON COLUMN monitor_db_capacity_snapshot.schema_name IS '模式名';
COMMENT ON COLUMN monitor_db_capacity_snapshot.table_name IS '表名';
COMMENT ON COLUMN monitor_db_capacity_snapshot.table_rows IS '行数（估计值）';
COMMENT ON COLUMN monitor_db_capacity_snapshot.table_size_bytes IS '表大小（字节）';
COMMENT ON COLUMN monitor_db_capacity_snapshot.index_size_bytes IS '索引大小（字节）';
COMMENT ON COLUMN monitor_db_capacity_snapshot.snapshot_date IS '快照日期';
CREATE UNIQUE INDEX uk_monitor_capacity ON monitor_db_capacity_snapshot (instance_id, schema_name, table_name, snapshot_date);

-- ----------------------------
-- 12. SQL 控制台执行历史
-- ----------------------------
DROP TABLE IF EXISTS monitor_sql_history CASCADE;
CREATE TABLE monitor_sql_history
(
    id            BIGINT       NOT NULL,
    instance_id   BIGINT       NOT NULL,
    execute_by    BIGINT       NOT NULL,
    sql_text      VARCHAR(5000) NOT NULL,
    sql_type      VARCHAR(20)  NOT NULL,
    exec_status   VARCHAR(20)  NOT NULL,
    block_reason  VARCHAR(500),
    affected_rows INT,
    result_rows   INT,
    duration_ms   BIGINT,
    client_ip     VARCHAR(50),
    execute_time  TIMESTAMP    NOT NULL,
    create_time   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted    SMALLINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);
COMMENT ON TABLE monitor_sql_history IS 'SQL 控制台执行历史（脱敏后留档）';
COMMENT ON COLUMN monitor_sql_history.id IS '主键（雪花ID）';
COMMENT ON COLUMN monitor_sql_history.instance_id IS '数据库实例ID';
COMMENT ON COLUMN monitor_sql_history.execute_by IS '执行人（用户ID）';
COMMENT ON COLUMN monitor_sql_history.sql_text IS '执行 SQL（参数已脱敏，截断 5000）';
COMMENT ON COLUMN monitor_sql_history.sql_type IS 'SQL 类型：SELECT/EXPLAIN/UPDATE/DELETE';
COMMENT ON COLUMN monitor_sql_history.exec_status IS '执行结果：SUCCESS/FAILED/BLOCKED';
COMMENT ON COLUMN monitor_sql_history.block_reason IS '被拦截原因（exec_status=BLOCKED）';
COMMENT ON COLUMN monitor_sql_history.affected_rows IS '影响行数';
COMMENT ON COLUMN monitor_sql_history.result_rows IS '返回行数';
COMMENT ON COLUMN monitor_sql_history.duration_ms IS '执行耗时（毫秒）';
COMMENT ON COLUMN monitor_sql_history.client_ip IS '来源 IP';
COMMENT ON COLUMN monitor_sql_history.execute_time IS '执行时间';
CREATE INDEX idx_monitor_sql_history_instance ON monitor_sql_history (instance_id, execute_time);
CREATE INDEX idx_monitor_sql_history_user ON monitor_sql_history (execute_by, execute_time);
