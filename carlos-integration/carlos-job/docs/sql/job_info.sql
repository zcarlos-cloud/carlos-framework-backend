-- ==========================================
-- carlos-job 定时任务元数据表（控制面）
-- 说明：Quartz(QRTZ_* 表)是执行面，本表是管理面元数据与策略配置的唯一事实源
-- ==========================================

CREATE TABLE IF NOT EXISTS job_info
(
    id               BIGSERIAL PRIMARY KEY,
    job_name         VARCHAR(128) NOT NULL,
    job_group        VARCHAR(64)  NOT NULL DEFAULT 'DEFAULT',
    service_name     VARCHAR(64)  NOT NULL,
    handler_name     VARCHAR(128) NOT NULL,
    schedule_type    SMALLINT     NOT NULL DEFAULT 1,        -- 1-CRON 2-固定间隔 3-一次性
    schedule_conf    VARCHAR(128) NOT NULL,                  -- cron 表达式 / 间隔毫秒数 / 触发时间(ISO)
    params           TEXT,
    description      VARCHAR(250),
    misfire_strategy SMALLINT     NOT NULL DEFAULT 0,        -- 0-智能策略 1-忽略过期 2-立即补跑一次 3-不补跑
    concurrent_mode  SMALLINT     NOT NULL DEFAULT 1,        -- 1-串行 2-并行
    timeout_seconds  INTEGER      NOT NULL DEFAULT 0,        -- 任务超时(秒)，0-不限制
    fail_retry_count INTEGER      NOT NULL DEFAULT 0,        -- 失败重试次数，0-不重试
    author           VARCHAR(64),
    alarm_receivers  VARCHAR(500),                           -- 报警接收人，多个逗号分隔
    status           VARCHAR(16)  NOT NULL DEFAULT 'NORMAL', -- Quartz 触发器状态: NORMAL/PAUSED 等
    create_by        BIGINT,
    create_time      TIMESTAMP    NOT NULL DEFAULT now(),
    update_by        BIGINT,
    update_time      TIMESTAMP,
    is_deleted       SMALLINT     NOT NULL DEFAULT 0         -- 逻辑删除: 0-否 1-是
);

-- 部分唯一索引：仅未删除行参与唯一约束，逻辑删除后可用同名重建
CREATE UNIQUE INDEX IF NOT EXISTS uk_job_info_name_group ON job_info (job_name, job_group) WHERE is_deleted = 0;

COMMENT ON TABLE job_info IS '定时任务元数据(控制面)';
COMMENT ON COLUMN job_info.job_name IS '任务名称(分组内唯一)';
COMMENT ON COLUMN job_info.job_group IS '任务分组';
COMMENT ON COLUMN job_info.service_name IS '目标服务名(Nacos 注册名)';
COMMENT ON COLUMN job_info.handler_name IS '任务处理器名称(JobHandler Bean 名称)';
COMMENT ON COLUMN job_info.schedule_type IS '调度类型: 1-CRON 2-固定间隔 3-一次性';
COMMENT ON COLUMN job_info.schedule_conf IS '调度配置: cron 表达式/固定间隔毫秒/一次性触发时间';
COMMENT ON COLUMN job_info.misfire_strategy IS '调度过期策略: 0-智能策略 1-忽略过期 2-立即补跑一次 3-不补跑';
COMMENT ON COLUMN job_info.concurrent_mode IS '并发模式: 1-串行(集群互斥) 2-并行';
COMMENT ON COLUMN job_info.timeout_seconds IS '任务超时(秒)，0-不限制';
COMMENT ON COLUMN job_info.fail_retry_count IS '失败重试次数，0-不重试';
COMMENT ON COLUMN job_info.author IS '负责人';
COMMENT ON COLUMN job_info.alarm_receivers IS '报警接收人，多个逗号分隔';
COMMENT ON COLUMN job_info.status IS '任务状态(Quartz 触发器状态): NORMAL-正常 PAUSED-暂停';
COMMENT ON COLUMN job_info.is_deleted IS '逻辑删除: 0-否 1-是';

CREATE INDEX IF NOT EXISTS idx_job_info_service ON job_info (service_name);
CREATE INDEX IF NOT EXISTS idx_job_info_author ON job_info (author);
