-- ==========================================
-- carlos-job 任务执行日志表
-- ==========================================

CREATE TABLE IF NOT EXISTS job_execution_log
(
    id              BIGSERIAL PRIMARY KEY,
    job_name        VARCHAR(128) NOT NULL,
    job_group       VARCHAR(64)  NOT NULL DEFAULT 'DEFAULT',
    service_name    VARCHAR(64),
    handler_name    VARCHAR(128),
    trigger_type    SMALLINT     NOT NULL DEFAULT 1,   -- 1-cron定时 2-手动触发
    cron_expression VARCHAR(64),
    status          SMALLINT     NOT NULL,             -- 1-成功 2-失败 3-执行中
    start_time      TIMESTAMP    NOT NULL,
    end_time        TIMESTAMP,
    duration_ms     BIGINT,
    result_message  TEXT,
    server_node     VARCHAR(64),
    create_time     TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_job_log_name_time ON job_execution_log (job_name, start_time DESC);
CREATE INDEX IF NOT EXISTS idx_job_log_status ON job_execution_log (status, start_time DESC);

COMMENT ON TABLE job_execution_log IS '定时任务执行日志';
COMMENT ON COLUMN job_execution_log.job_name IS '任务名称';
COMMENT ON COLUMN job_execution_log.job_group IS '任务分组';
COMMENT ON COLUMN job_execution_log.service_name IS '目标服务名';
COMMENT ON COLUMN job_execution_log.handler_name IS '任务处理器名称';
COMMENT ON COLUMN job_execution_log.trigger_type IS '触发类型: 1-cron定时 2-手动触发';
COMMENT ON COLUMN job_execution_log.status IS '执行状态: 1-成功 2-失败 3-执行中';
COMMENT ON COLUMN job_execution_log.server_node IS '执行节点（调度中心实例）';
