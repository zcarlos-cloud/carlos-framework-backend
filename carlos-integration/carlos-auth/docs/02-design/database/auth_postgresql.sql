-- ============================================
-- Carlos Framework - carlos-auth 模块
-- PostgreSQL 建表脚本
-- 数据库: PostgreSQL 14+
-- 生成日期: 2026-07-03
-- 说明:
--   1. 从 docs/db/postgresql/03-carlos-auth.sql 迁移；
--   2. 按最新 AppClient 实体补充门户字段：app_desc/app_type/app_category_id/homepage_url/display_order/portal_visible；
--   3. 新增 app_category、app_role_access 表，与 AppCategory、AppRoleAccess 实体对齐；
--   4. 删除旧的 MySQL oauth2.sql / portal.sql。
-- ============================================

-- ----------------------------
-- 1. 应用客户端表
-- ----------------------------
DROP TABLE IF EXISTS app_client CASCADE;
CREATE TABLE app_client
(
    id                        BIGINT       NOT NULL,
    app_key                   VARCHAR(64)  NOT NULL,
    app_name                  VARCHAR(64)  NOT NULL,
    app_logo                  TEXT,
    app_desc                  VARCHAR(255),
    app_type                  SMALLINT     NOT NULL DEFAULT 1,
    app_category_id           BIGINT,
    homepage_url              VARCHAR(500),
    display_order             INTEGER               DEFAULT 0,
    portal_visible            BOOLEAN      NOT NULL DEFAULT TRUE,
    app_secret                VARCHAR(100) NOT NULL,
    client_secret_expires_at  TIMESTAMP    NOT NULL,
    client_issued_at          TIMESTAMP    NOT NULL,
    authentication_methods    VARCHAR(100) NOT NULL,
    authorization_grant_types VARCHAR(100) NOT NULL,
    scopes                    VARCHAR(100)          DEFAULT NULL,
    redirect_uris             VARCHAR(255)          DEFAULT NULL,
    client_settings           TEXT,
    token_settings            TEXT,
    client_status             SMALLINT     NOT NULL DEFAULT 1,
    is_deleted                SMALLINT     NOT NULL DEFAULT 0,
    create_by                 BIGINT                DEFAULT NULL,
    create_time               TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    update_by                 BIGINT                DEFAULT NULL,
    update_time               TIMESTAMP,
    PRIMARY KEY (id)
);
COMMENT ON TABLE app_client IS '应用客户端信息';
COMMENT ON COLUMN app_client.id IS '主键';
COMMENT ON COLUMN app_client.app_key IS 'appKey';
COMMENT ON COLUMN app_client.app_name IS '应用名称';
COMMENT ON COLUMN app_client.app_logo IS '应用logo';
COMMENT ON COLUMN app_client.app_desc IS '应用描述';
COMMENT ON COLUMN app_client.app_type IS '应用类型(1内部应用 2外部应用 3移动应用)';
COMMENT ON COLUMN app_client.app_category_id IS '应用分类ID';
COMMENT ON COLUMN app_client.homepage_url IS '应用首页地址(完整URL)';
COMMENT ON COLUMN app_client.display_order IS '门户展示排序(越小越靠前)';
COMMENT ON COLUMN app_client.portal_visible IS '门户可见(0隐藏 1可见)';
COMMENT ON COLUMN app_client.app_secret IS '应用密钥';
COMMENT ON COLUMN app_client.client_secret_expires_at IS '应用密钥到期时间';
COMMENT ON COLUMN app_client.client_issued_at IS '应用发行时间';
COMMENT ON COLUMN app_client.authentication_methods IS '认证方式';
COMMENT ON COLUMN app_client.authorization_grant_types IS 'grant_type';
COMMENT ON COLUMN app_client.scopes IS 'scopes';
COMMENT ON COLUMN app_client.redirect_uris IS '重定向地址';
COMMENT ON COLUMN app_client.client_settings IS 'client设置 key:value;';
COMMENT ON COLUMN app_client.token_settings IS 'token设置 key:value;';
COMMENT ON COLUMN app_client.client_status IS '客户端状态：1启用 2禁用 3已注销';
COMMENT ON COLUMN app_client.is_deleted IS '删除状态';
COMMENT ON COLUMN app_client.create_by IS '创建人';
COMMENT ON COLUMN app_client.create_time IS '创建时间';
COMMENT ON COLUMN app_client.update_by IS '更新人';
COMMENT ON COLUMN app_client.update_time IS '更新时间';
CREATE UNIQUE INDEX uk_app_client_key ON app_client (app_key);


-- ----------------------------
-- 2. 应用分类表（Portal 应用门户）
-- ----------------------------
DROP TABLE IF EXISTS app_category CASCADE;
CREATE TABLE app_category
(
    id            BIGINT      NOT NULL,
    category_code VARCHAR(32) NOT NULL,
    category_name VARCHAR(64) NOT NULL,
    category_icon VARCHAR(128),
    sort_order    INTEGER              DEFAULT 0,
    is_deleted    SMALLINT    NOT NULL DEFAULT 0,
    create_by     BIGINT,
    create_time   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by     BIGINT,
    update_time   TIMESTAMP,
    PRIMARY KEY (id)
);
COMMENT ON TABLE app_category IS '应用分类（应用门户）';
COMMENT ON COLUMN app_category.id IS '主键';
COMMENT ON COLUMN app_category.category_code IS '分类编码';
COMMENT ON COLUMN app_category.category_name IS '分类名称';
COMMENT ON COLUMN app_category.category_icon IS '分类图标';
COMMENT ON COLUMN app_category.sort_order IS '排序(越小越靠前)';
COMMENT ON COLUMN app_category.is_deleted IS '逻辑删除: 0-否 1-是';
COMMENT ON COLUMN app_category.create_by IS '创建者编号';
COMMENT ON COLUMN app_category.create_time IS '创建时间';
COMMENT ON COLUMN app_category.update_by IS '更新者编号';
COMMENT ON COLUMN app_category.update_time IS '更新时间';
CREATE UNIQUE INDEX uk_app_category_code ON app_category (category_code);


-- ----------------------------
-- 3. 应用角色访问控制表（Portal 应用门户）
-- 规则：app_role_access 中无记录的应用 = 所有登录用户可见
--       有记录的应用 = 仅匹配 role_code 的用户可见
-- ----------------------------
DROP TABLE IF EXISTS app_role_access CASCADE;
CREATE TABLE app_role_access
(
    id          BIGINT      NOT NULL,
    app_id      BIGINT      NOT NULL,
    role_code   VARCHAR(64) NOT NULL,
    create_by   BIGINT,
    create_time TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);
COMMENT ON TABLE app_role_access IS '应用角色访问控制（应用门户）';
COMMENT ON COLUMN app_role_access.id IS '主键';
COMMENT ON COLUMN app_role_access.app_id IS '应用ID(关联app_client.id)';
COMMENT ON COLUMN app_role_access.role_code IS '角色编码';
COMMENT ON COLUMN app_role_access.create_by IS '创建人';
COMMENT ON COLUMN app_role_access.create_time IS '创建时间';
CREATE INDEX idx_app_role_access_app ON app_role_access (app_id);
CREATE INDEX idx_app_role_access_role ON app_role_access (role_code);
CREATE UNIQUE INDEX uk_app_role_access ON app_role_access (app_id, role_code);


-- ----------------------------
-- 4. 开放API分类表
-- ----------------------------
DROP TABLE IF EXISTS open_api_category CASCADE;
CREATE TABLE open_api_category
(
    id            BIGINT      NOT NULL,
    parent_id     BIGINT      NOT NULL,
    category_code VARCHAR(32) NOT NULL,
    category_name VARCHAR(64) NOT NULL,
    is_enabled    SMALLINT    NOT NULL DEFAULT 1,
    is_deleted    SMALLINT    NOT NULL DEFAULT 0,
    create_by     CHAR(32),
    create_time   TIMESTAMP,
    update_by     CHAR(32),
    update_time   TIMESTAMP,
    PRIMARY KEY (id)
);
COMMENT ON TABLE open_api_category IS '开放API分类';
COMMENT ON COLUMN open_api_category.id IS '主键';
COMMENT ON COLUMN open_api_category.parent_id IS '父级ID';
COMMENT ON COLUMN open_api_category.category_code IS '分类编码';
COMMENT ON COLUMN open_api_category.category_name IS '分类名称';
COMMENT ON COLUMN open_api_category.is_enabled IS '是否启用: 0-禁用 1-启用';
COMMENT ON COLUMN open_api_category.is_deleted IS '逻辑删除: 0-否 1-是';
COMMENT ON COLUMN open_api_category.create_by IS '创建者编号';
COMMENT ON COLUMN open_api_category.create_time IS '创建时间';
COMMENT ON COLUMN open_api_category.update_by IS '更新者编号';
COMMENT ON COLUMN open_api_category.update_time IS '更新时间';
CREATE INDEX idx_open_api_cat_parent ON open_api_category (parent_id);
CREATE UNIQUE INDEX uk_open_api_cat_code ON open_api_category (parent_id, category_code);


-- ----------------------------
-- 5. 开放接口表
-- ----------------------------
DROP TABLE IF EXISTS resource_open_api CASCADE;
CREATE TABLE resource_open_api
(
    id              BIGINT       NOT NULL,
    category_id     BIGINT       NOT NULL,
    api_name        VARCHAR(64)  NOT NULL,
    api_path        VARCHAR(255) NOT NULL,
    method          VARCHAR(8)   NOT NULL,
    permission_key  VARCHAR(64)  NOT NULL,
    rate_limit      INTEGER               DEFAULT 100,
    status          SMALLINT              DEFAULT 1,
    require_sign    SMALLINT              DEFAULT 1,
    description     TEXT,
    version         VARCHAR(16)           DEFAULT 'v1',
    request_sample  TEXT,
    response_sample TEXT,
    is_enabled      SMALLINT     NOT NULL DEFAULT 1,
    is_deleted      SMALLINT     NOT NULL DEFAULT 0,
    create_by       CHAR(32),
    create_time     TIMESTAMP,
    update_by       CHAR(32),
    update_time     TIMESTAMP,
    PRIMARY KEY (id)
);
COMMENT ON TABLE resource_open_api IS '开放接口信息';
COMMENT ON COLUMN resource_open_api.id IS '主键';
COMMENT ON COLUMN resource_open_api.category_id IS '所属分类ID';
COMMENT ON COLUMN resource_open_api.api_name IS 'API名称';
COMMENT ON COLUMN resource_open_api.api_path IS 'API路径';
COMMENT ON COLUMN resource_open_api.method IS 'HTTP方法';
COMMENT ON COLUMN resource_open_api.permission_key IS '权限标识';
COMMENT ON COLUMN resource_open_api.rate_limit IS '每分钟调用上限';
COMMENT ON COLUMN resource_open_api.status IS '状态: 0-禁用 1-启用';
COMMENT ON COLUMN resource_open_api.require_sign IS '是否需要签名验证(0否 1是)';
COMMENT ON COLUMN resource_open_api.description IS '描述';
COMMENT ON COLUMN resource_open_api.version IS '版本号';
COMMENT ON COLUMN resource_open_api.request_sample IS '请求示例';
COMMENT ON COLUMN resource_open_api.response_sample IS '响应示例';
COMMENT ON COLUMN resource_open_api.is_enabled IS '是否启用: 0-禁用 1-启用';
COMMENT ON COLUMN resource_open_api.is_deleted IS '逻辑删除: 0-否 1-是';
COMMENT ON COLUMN resource_open_api.create_by IS '创建者编号';
COMMENT ON COLUMN resource_open_api.create_time IS '创建时间';
COMMENT ON COLUMN resource_open_api.update_by IS '更新者编号';
COMMENT ON COLUMN resource_open_api.update_time IS '更新时间';
CREATE INDEX idx_resource_open_api_cat ON resource_open_api (category_id);


-- ----------------------------
-- 6. 授权范围分类表
-- ----------------------------
DROP TABLE IF EXISTS resource_scope_category CASCADE;
CREATE TABLE resource_scope_category
(
    id            BIGINT      NOT NULL,
    category_code VARCHAR(32) NOT NULL,
    category_name VARCHAR(64) NOT NULL,
    is_enabled    SMALLINT    NOT NULL DEFAULT 1,
    is_deleted    SMALLINT    NOT NULL DEFAULT 0,
    create_by     CHAR(32),
    create_time   TIMESTAMP,
    update_by     CHAR(32),
    update_time   TIMESTAMP,
    PRIMARY KEY (id)
);
COMMENT ON TABLE resource_scope_category IS '授权范围分类';
COMMENT ON COLUMN resource_scope_category.id IS '主键';
COMMENT ON COLUMN resource_scope_category.category_code IS '分类编码';
COMMENT ON COLUMN resource_scope_category.category_name IS '分类名称';
COMMENT ON COLUMN resource_scope_category.is_enabled IS '是否启用: 0-禁用 1-启用';
COMMENT ON COLUMN resource_scope_category.is_deleted IS '逻辑删除: 0-否 1-是';
COMMENT ON COLUMN resource_scope_category.create_by IS '创建者编号';
COMMENT ON COLUMN resource_scope_category.create_time IS '创建时间';
COMMENT ON COLUMN resource_scope_category.update_by IS '更新者编号';
COMMENT ON COLUMN resource_scope_category.update_time IS '更新时间';


-- ----------------------------
-- 7. 授权范围详情表
-- ----------------------------
DROP TABLE IF EXISTS resource_scope_info CASCADE;
CREATE TABLE resource_scope_info
(
    id            BIGINT       NOT NULL,
    category_id   BIGINT       NOT NULL,
    scope_code    VARCHAR(64)  NOT NULL,
    scope_name    VARCHAR(128) NOT NULL,
    description   TEXT,
    parent_scope  VARCHAR(64),
    is_sensitive  SMALLINT              DEFAULT 0,
    require_audit SMALLINT              DEFAULT 1,
    status        SMALLINT              DEFAULT 1,
    is_deleted    SMALLINT     NOT NULL DEFAULT 0,
    create_by     CHAR(32),
    create_time   TIMESTAMP,
    update_by     CHAR(32),
    update_time   TIMESTAMP,
    PRIMARY KEY (id)
);
COMMENT ON TABLE resource_scope_info IS '权限范围';
COMMENT ON COLUMN resource_scope_info.id IS '主键';
COMMENT ON COLUMN resource_scope_info.category_id IS '所属分类ID';
COMMENT ON COLUMN resource_scope_info.scope_code IS '范围编码';
COMMENT ON COLUMN resource_scope_info.scope_name IS '范围名称';
COMMENT ON COLUMN resource_scope_info.description IS '描述';
COMMENT ON COLUMN resource_scope_info.parent_scope IS '上级范围编码';
COMMENT ON COLUMN resource_scope_info.is_sensitive IS '是否敏感权限(0否 1是)';
COMMENT ON COLUMN resource_scope_info.require_audit IS '是否需要人工审核(0否 1是)';
COMMENT ON COLUMN resource_scope_info.status IS '状态: 0-禁用 1-启用';
COMMENT ON COLUMN resource_scope_info.is_deleted IS '逻辑删除: 0-否 1-是';
COMMENT ON COLUMN resource_scope_info.create_by IS '创建者编号';
COMMENT ON COLUMN resource_scope_info.create_time IS '创建时间';
COMMENT ON COLUMN resource_scope_info.update_by IS '更新者编号';
COMMENT ON COLUMN resource_scope_info.update_time IS '更新时间';
CREATE INDEX idx_resource_scope_cat ON resource_scope_info (category_id);


-- ----------------------------
-- 8. 授权范围API关联表
-- ----------------------------
DROP TABLE IF EXISTS resource_scope_api CASCADE;
CREATE TABLE resource_scope_api
(
    id          BIGINT    NOT NULL,
    scope_id    BIGINT    NOT NULL,
    api_id      BIGINT    NOT NULL,
    create_by   BIGINT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);
COMMENT ON TABLE resource_scope_api IS '授权范围关联api';
COMMENT ON COLUMN resource_scope_api.id IS '主键';
COMMENT ON COLUMN resource_scope_api.scope_id IS '授权范围ID';
COMMENT ON COLUMN resource_scope_api.api_id IS 'API ID';
COMMENT ON COLUMN resource_scope_api.create_by IS '创建者';
COMMENT ON COLUMN resource_scope_api.create_time IS '创建时间';
CREATE INDEX idx_resource_scope_api_scope ON resource_scope_api (scope_id);
CREATE INDEX idx_resource_scope_api_api ON resource_scope_api (api_id);
CREATE UNIQUE INDEX uk_resource_scope_api ON resource_scope_api (scope_id, api_id);


-- ----------------------------
-- 9. 应用授权范围关联表
-- ----------------------------
DROP TABLE IF EXISTS app_client_scope CASCADE;
CREATE TABLE app_client_scope
(
    id          BIGINT    NOT NULL,
    app_id      BIGINT    NOT NULL,
    scope_id    BIGINT    NOT NULL,
    status      SMALLINT           DEFAULT 1,
    create_by   BIGINT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);
COMMENT ON TABLE app_client_scope IS '应用关联授权';
COMMENT ON COLUMN app_client_scope.id IS '主键';
COMMENT ON COLUMN app_client_scope.app_id IS '应用ID';
COMMENT ON COLUMN app_client_scope.scope_id IS '授权范围ID';
COMMENT ON COLUMN app_client_scope.status IS '状态: 0-禁用 1-启用';
COMMENT ON COLUMN app_client_scope.create_by IS '创建者';
COMMENT ON COLUMN app_client_scope.create_time IS '创建时间';
CREATE INDEX idx_app_client_scope_scope ON app_client_scope (scope_id);
CREATE INDEX idx_app_client_scope_app ON app_client_scope (app_id);
CREATE UNIQUE INDEX uk_app_client_scope ON app_client_scope (scope_id, app_id);


-- ----------------------------
-- 10. 可信设备表
-- ----------------------------
DROP TABLE IF EXISTS auth_trusted_device CASCADE;
CREATE TABLE auth_trusted_device
(
    id BIGSERIAL NOT NULL,
    user_id            BIGINT      NOT NULL,
    device_fingerprint VARCHAR(64) NOT NULL,
    device_name        VARCHAR(100),
    ip_address         VARCHAR(40),
    location           VARCHAR(100),
    user_agent         VARCHAR(500),
    browser            VARCHAR(50),
    is_mobile          BOOLEAN,
    trusted_time       TIMESTAMP,
    last_used_time     TIMESTAMP,
    create_time        TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time        TIMESTAMP,
    deleted            SMALLINT    NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);
COMMENT ON TABLE auth_trusted_device IS '可信设备';
COMMENT ON COLUMN auth_trusted_device.id IS '设备ID';
COMMENT ON COLUMN auth_trusted_device.user_id IS '用户ID';
COMMENT ON COLUMN auth_trusted_device.device_fingerprint IS '设备指纹（MD5）';
COMMENT ON COLUMN auth_trusted_device.device_name IS '设备名称（如：Chrome on Windows）';
COMMENT ON COLUMN auth_trusted_device.ip_address IS 'IP地址';
COMMENT ON COLUMN auth_trusted_device.location IS '地理位置';
COMMENT ON COLUMN auth_trusted_device.user_agent IS 'User-Agent';
COMMENT ON COLUMN auth_trusted_device.browser IS '浏览器名称';
COMMENT ON COLUMN auth_trusted_device.is_mobile IS '是否为移动设备';
COMMENT ON COLUMN auth_trusted_device.trusted_time IS '添加为可信时间';
COMMENT ON COLUMN auth_trusted_device.last_used_time IS '最后使用时间';
COMMENT ON COLUMN auth_trusted_device.create_time IS '创建时间';
COMMENT ON COLUMN auth_trusted_device.update_time IS '更新时间';
COMMENT ON COLUMN auth_trusted_device.deleted IS '逻辑删除';
CREATE INDEX idx_auth_trusted_device_user ON auth_trusted_device (user_id);


-- ----------------------------
-- 11. MFA恢复码表
-- ----------------------------
DROP TABLE IF EXISTS auth_mfa_recovery_code CASCADE;
CREATE TABLE auth_mfa_recovery_code
(
    id BIGSERIAL NOT NULL,
    user_id     BIGINT      NOT NULL,
    code        VARCHAR(32) NOT NULL,
    used        BOOLEAN              DEFAULT FALSE,
    used_time   TIMESTAMP,
    create_time TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);
COMMENT ON TABLE auth_mfa_recovery_code IS 'MFA备用恢复码';
COMMENT ON COLUMN auth_mfa_recovery_code.id IS '恢复码ID';
COMMENT ON COLUMN auth_mfa_recovery_code.user_id IS '用户ID';
COMMENT ON COLUMN auth_mfa_recovery_code.code IS '恢复码';
COMMENT ON COLUMN auth_mfa_recovery_code.used IS '是否已使用';
COMMENT ON COLUMN auth_mfa_recovery_code.used_time IS '使用时间';
COMMENT ON COLUMN auth_mfa_recovery_code.create_time IS '创建时间';
CREATE INDEX idx_auth_mfa_code_user ON auth_mfa_recovery_code (user_id);


-- ----------------------------
-- 12. 安全告警表
-- ----------------------------
DROP TABLE IF EXISTS security_alert CASCADE;
CREATE TABLE security_alert
(
    id BIGSERIAL NOT NULL,
    user_id      BIGINT,
    username     VARCHAR(64),
    alert_type   VARCHAR(50),
    severity     VARCHAR(20),
    title        VARCHAR(200),
    content      TEXT,
    ip_address   VARCHAR(40),
    location     VARCHAR(100),
    user_agent   VARCHAR(500),
    handled      BOOLEAN           DEFAULT FALSE,
    handled_time TIMESTAMP,
    remark       VARCHAR(500),
    create_time  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);
COMMENT ON TABLE security_alert IS '安全告警';
COMMENT ON COLUMN security_alert.id IS '告警ID';
COMMENT ON COLUMN security_alert.user_id IS '用户ID';
COMMENT ON COLUMN security_alert.username IS '用户名';
COMMENT ON COLUMN security_alert.alert_type IS '告警类型';
COMMENT ON COLUMN security_alert.severity IS '告警级别';
COMMENT ON COLUMN security_alert.title IS '告警标题';
COMMENT ON COLUMN security_alert.content IS '告警内容';
COMMENT ON COLUMN security_alert.ip_address IS 'IP地址';
COMMENT ON COLUMN security_alert.location IS '地理位置';
COMMENT ON COLUMN security_alert.user_agent IS 'User-Agent';
COMMENT ON COLUMN security_alert.handled IS '是否已处理';
COMMENT ON COLUMN security_alert.handled_time IS '处理时间';
COMMENT ON COLUMN security_alert.remark IS '备注';
COMMENT ON COLUMN security_alert.create_time IS '创建时间';
CREATE INDEX idx_security_alert_user ON security_alert (user_id);
CREATE INDEX idx_security_alert_handled ON security_alert (handled);


-- ----------------------------
-- 13. Passkey 凭据表
-- ----------------------------
DROP TABLE IF EXISTS auth_passkey_credential CASCADE;
CREATE TABLE auth_passkey_credential
(
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT       NOT NULL,
    credential_id       BYTEA        NOT NULL,
    credential_id_b64   VARCHAR(255) NOT NULL,
    public_key_cose     BYTEA        NOT NULL,
    sign_count          BIGINT       NOT NULL DEFAULT 0,
    rp_id               VARCHAR(255) NOT NULL,
    user_handle         BYTEA,
    user_handle_b64     VARCHAR(255),
    aaguid              BYTEA,
    attestation_type    VARCHAR(50),
    device_name         VARCHAR(255),
    transports          VARCHAR(255),
    is_discoverable     SMALLINT              DEFAULT 0,
    is_backup_eligible  SMALLINT              DEFAULT 0,
    is_backup_state     SMALLINT              DEFAULT 0,
    last_used_time      TIMESTAMP,
    create_time         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP,
    CONSTRAINT uk_passkey_credential_id_b64 UNIQUE (credential_id_b64)
);
COMMENT ON TABLE auth_passkey_credential IS 'WebAuthn/Passkey 凭据';
COMMENT ON COLUMN auth_passkey_credential.id IS '主键';
COMMENT ON COLUMN auth_passkey_credential.user_id IS '用户ID';
COMMENT ON COLUMN auth_passkey_credential.credential_id IS '凭据ID（二进制）';
COMMENT ON COLUMN auth_passkey_credential.credential_id_b64 IS '凭据ID（base64url，用于查询）';
COMMENT ON COLUMN auth_passkey_credential.public_key_cose IS 'COSE 格式公钥';
COMMENT ON COLUMN auth_passkey_credential.sign_count IS '签名计数器';
COMMENT ON COLUMN auth_passkey_credential.rp_id IS 'RP ID';
COMMENT ON COLUMN auth_passkey_credential.user_handle IS '用户句柄';
COMMENT ON COLUMN auth_passkey_credential.aaguid IS '认证器 AAGUID';
COMMENT ON COLUMN auth_passkey_credential.attestation_type IS '证明类型';
COMMENT ON COLUMN auth_passkey_credential.device_name IS '设备名称';
COMMENT ON COLUMN auth_passkey_credential.transports IS '传输方式 JSON 数组';
COMMENT ON COLUMN auth_passkey_credential.is_discoverable IS '是否可发现凭据：0-否 1-是';
COMMENT ON COLUMN auth_passkey_credential.is_backup_eligible IS '是否可备份：0-否 1-是';
COMMENT ON COLUMN auth_passkey_credential.is_backup_state IS '当前是否已备份：0-否 1-是';
COMMENT ON COLUMN auth_passkey_credential.last_used_time IS '最后使用时间';
COMMENT ON COLUMN auth_passkey_credential.create_time IS '创建时间';
COMMENT ON COLUMN auth_passkey_credential.update_time IS '更新时间';
CREATE INDEX idx_passkey_credential_user ON auth_passkey_credential (user_id);


-- ----------------------------
-- 14. 初始化数据
-- ----------------------------
-- 应用分类
INSERT INTO app_category (id, category_code, category_name, category_icon, sort_order)
VALUES (1, 'admin', '管理工具', 'carbon:settings', 0),
       (2, 'business', '业务应用', 'carbon:application', 1),
       (3, 'develop', '开发工具', 'carbon:code', 2);

-- Portal 客户端样例（public client，PKCE，无 secret）
INSERT INTO app_client (id, app_key, app_name, app_desc, app_type, app_category_id,
                        homepage_url, display_order, portal_visible,
                        app_secret, client_secret_expires_at, client_issued_at,
                        authentication_methods, authorization_grant_types,
                        scopes, redirect_uris, client_settings, token_settings,
                        client_status, is_deleted, create_time)
VALUES (1001, 'carlos-framework-portal', 'Carlos 应用门户', '统一应用入口与 SSO 单点登录', 1, 1,
        'http://localhost:5200', 0, 1,
        '', '2099-12-31 23:59:59', CURRENT_TIMESTAMP,
        'public_key', 'authorization_code',
        'openid profile', 'http://localhost:5200/oauth2/callback',
        '{"require_proof_key":true,"require_authorization_consent":false}',
        '{"access_token_time_to_live":"2h","refresh_token_time_to_live":"7d"}',
        1, 0, CURRENT_TIMESTAMP);
