-- =============================================================================
-- org_tenant 租户表（S2 租户管理）
-- 归属：carlos-org（组织域，表前缀 org_）
-- 说明：
--   1. id 为雪花 ID（应用层 IdType.ASSIGN_ID 分配），不使用 BIGSERIAL；
--   2. id = 0 保留为平台租户（存量数据 tenant_id 默认 0，与 OrgTenantLineHandler 对齐）；
--   3. tenant_code 唯一性采用部分唯一索引（仅未删除行），与逻辑删除共存；
--   4. org_tenant 自身不做租户隔离（无 tenant_id 列，不得加入 TENANT_TABLES）。
-- 脚本幂等，可重复执行。
-- =============================================================================

CREATE TABLE IF NOT EXISTS org_tenant (
    id            BIGINT       PRIMARY KEY,
    tenant_code   VARCHAR(32)  NOT NULL,
    tenant_name   VARCHAR(128) NOT NULL,
    contact_name  VARCHAR(64),
    contact_phone VARCHAR(32),
    tenant_status SMALLINT     NOT NULL DEFAULT 1,    -- 1-正常 2-停用（OrgTenantStateEnum）
    expire_time   TIMESTAMP,                          -- NULL 表示永不过期
    remark        VARCHAR(512),
    create_by     BIGINT,
    create_time   TIMESTAMP    NOT NULL DEFAULT now(),
    update_by     BIGINT,
    update_time   TIMESTAMP    NOT NULL DEFAULT now(),
    is_deleted    SMALLINT     NOT NULL DEFAULT 0     -- 0-未删除 1-已删除
);

COMMENT ON TABLE  org_tenant IS '租户表（0 号租户保留为平台租户）';
COMMENT ON COLUMN org_tenant.tenant_code IS '租户编码（登录时输入，租户内唯一标识）';
COMMENT ON COLUMN org_tenant.tenant_status IS '租户状态：1-正常 2-停用';
COMMENT ON COLUMN org_tenant.expire_time IS '过期时间，NULL 表示永不过期';

-- 租户编码部分唯一索引（仅未删除行参与唯一约束）
CREATE UNIQUE INDEX IF NOT EXISTS uk_org_tenant_code
    ON org_tenant (tenant_code) WHERE is_deleted = 0;

-- 平台租户种子数据（id=0，保留）
INSERT INTO org_tenant (id, tenant_code, tenant_name, tenant_status, remark, create_by, create_time, update_time)
VALUES (0, 'PLATFORM', '平台租户', 1, '平台租户（保留，id=0，存量数据默认归属）', 1, now(), now())
ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- S2 配套：org_user 租户内唯一约束（代码评审 M1）
-- 背景：init-admin / 新增用户的账号与手机查重是 check-then-insert，并发下可能产生
--       同租户重复账号；这里用部分唯一索引在数据库层兜底。
-- 注意：若存量数据存在同租户重复 account/phone，以下语句会报错，需先清理脏数据。
--       （2026-09-22 已核查线上库无重复，可直接执行。）
-- =============================================================================
CREATE UNIQUE INDEX IF NOT EXISTS uk_org_user_tenant_account
    ON org_user (tenant_id, account) WHERE is_deleted = 0;

CREATE UNIQUE INDEX IF NOT EXISTS uk_org_user_tenant_phone
    ON org_user (tenant_id, phone) WHERE is_deleted = 0 AND phone IS NOT NULL AND phone <> '';
