-- =============================================================================
-- sys_news 公告/站内通知 + sys_news_read 已读记录（S3 公告）
-- 归属：carlos-system（系统域，表前缀 sys_）
-- 说明：
--   1. id 为雪花 ID（应用层 IdType.ASSIGN_ID 分配），不使用 BIGSERIAL；
--   2. sys_news 逻辑删除（is_deleted）；sys_news_read 物理删除，无删除标记；
--   3. tenant_id 仅为数据列（carlos-system 暂无租户拦截器），为后续多租户预留；
--   4. sys_news_read 唯一约束 (news_id, user_id) 防重复已读。
-- 脚本幂等，可重复执行。
-- =============================================================================

CREATE TABLE IF NOT EXISTS sys_news (
    id            BIGINT       PRIMARY KEY,
    title         VARCHAR(256) NOT NULL,
    news_type     SMALLINT     NOT NULL DEFAULT 1,    -- 1-公告 2-通知 3-更新日志（SysNewsTypeEnum）
    content       TEXT         NOT NULL,
    publish_scope SMALLINT     NOT NULL DEFAULT 1,    -- 1-全员 2-指定角色（SysNewsScopeEnum）
    scope_role_ids VARCHAR(512),                      -- JSON 数组（如 [100,200]），publish_scope=2 时生效
    is_top        BOOLEAN      NOT NULL DEFAULT FALSE,
    status        SMALLINT     NOT NULL DEFAULT 1,    -- 1-草稿 2-已发布 3-已下线（SysNewsStatusEnum）
    publish_time  TIMESTAMP,                          -- 发布时间，发布动作时写入
    expire_time   TIMESTAMP,                          -- NULL 表示永不过期
    tenant_id     BIGINT       NOT NULL DEFAULT 0,    -- 预留（system 模块暂无租户拦截器）
    create_by     BIGINT,
    create_time   TIMESTAMP    NOT NULL DEFAULT now(),
    update_by     BIGINT,
    update_time   TIMESTAMP    NOT NULL DEFAULT now(),
    is_deleted    SMALLINT     NOT NULL DEFAULT 0
);

COMMENT ON TABLE  sys_news IS '公告/站内通知';
COMMENT ON COLUMN sys_news.news_type IS '类型：1-公告 2-通知 3-更新日志';
COMMENT ON COLUMN sys_news.publish_scope IS '发布范围：1-全员 2-指定角色';
COMMENT ON COLUMN sys_news.scope_role_ids IS '指定角色的角色ID JSON 数组，publish_scope=2 时生效';
COMMENT ON COLUMN sys_news.status IS '状态：1-草稿 2-已发布 3-已下线';

-- 管理端列表查询
CREATE INDEX IF NOT EXISTS idx_sys_news_status ON sys_news (status, publish_time) WHERE is_deleted = 0;

-- 已读记录（用户维度，物理删除）
CREATE TABLE IF NOT EXISTS sys_news_read (
    id         BIGINT    PRIMARY KEY,
    news_id    BIGINT    NOT NULL,
    user_id    BIGINT    NOT NULL,
    read_time  TIMESTAMP NOT NULL DEFAULT now(),
    tenant_id  BIGINT    NOT NULL DEFAULT 0
);

COMMENT ON TABLE sys_news_read IS '公告已读记录（用户维度）';

-- 同一用户同一公告只记一条已读
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_news_read ON sys_news_read (news_id, user_id);

-- 用户维度查未读
CREATE INDEX IF NOT EXISTS idx_sys_news_read_user ON sys_news_read (user_id, news_id);
