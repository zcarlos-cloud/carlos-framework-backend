-- =============================================================================
-- sys_favorite 通用用户收藏（用户偏好数据，按 user_id 强隔离）
-- 归属：carlos-system（系统域，表前缀 sys_）
-- 说明：
--   1. id 为雪花 ID（应用层 IdType.ASSIGN_ID 分配），不使用 BIGSERIAL；
--   2. 物理删除，无 is_deleted 列：收藏是用户偏好数据，高频增删，物理删除避免
--      逻辑删除与唯一索引 (user_id, target_type, target_id) 的纠缠（参照 sys_news_read）；
--   3. tenant_id 仅为数据列（carlos-system 暂无租户拦截器），为后续多租户预留；
--   4. 唯一索引 uk_sys_favorite 防同用户同类型重复收藏，add 幂等兜底并发；
--   5. target_type 约定小写字母/数字/中划线（如 quick-nav、sys-news），
--      target_id 为业务侧不透明字符串（路由 key、公告 id 等）。
-- 脚本幂等，可重复执行。
-- =============================================================================

CREATE TABLE IF NOT EXISTS sys_favorite (
    id          BIGINT       PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    target_type VARCHAR(64)  NOT NULL,               -- 业务类型：quick-nav、sys-news 等
    target_id   VARCHAR(128) NOT NULL,               -- 目标标识：路由 key、公告 id 等
    sort_no     INT          NOT NULL DEFAULT 0,     -- 同 user_id+target_type 内排序（replace 按数组顺序 1..n 写入）
    tenant_id   BIGINT       NOT NULL DEFAULT 0,     -- 预留（system 模块暂无租户拦截器）
    create_by   BIGINT,
    create_time TIMESTAMP    NOT NULL DEFAULT now(),
    update_by   BIGINT,
    update_time TIMESTAMP    NOT NULL DEFAULT now()
);

COMMENT ON TABLE  sys_favorite IS '通用用户收藏';
COMMENT ON COLUMN sys_favorite.user_id IS '用户ID（数据按用户隔离）';
COMMENT ON COLUMN sys_favorite.target_type IS '业务类型：小写字母/数字/中划线，如 quick-nav、sys-news';
COMMENT ON COLUMN sys_favorite.target_id IS '目标标识：路由 key、公告 id 等业务侧字符串';
COMMENT ON COLUMN sys_favorite.sort_no IS '同 user_id+target_type 内排序，replace 按数组顺序 1..n 写入';
COMMENT ON COLUMN sys_favorite.tenant_id IS '租户ID（预留，system 模块暂无租户拦截器）';

-- 同用户同类型同目标唯一，add 幂等兜底并发重复添加
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_favorite ON sys_favorite (user_id, target_type, target_id);

-- 我的收藏列表（按类型过滤 + 排序）
CREATE INDEX IF NOT EXISTS idx_sys_favorite_user_type ON sys_favorite (user_id, target_type, sort_no);
