-- ============================================
-- Carlos Framework - carlos-system 模块
-- PostgreSQL 建表脚本
-- 数据库: PostgreSQL 14+
-- 生成日期: 2026-07-03
-- 说明: 从 docs/db/postgresql/01-carlos-system.sql 迁移并整合为模块级规范 SQL
-- ============================================

-- ----------------------------
-- 1. 服务注册表
-- ----------------------------
DROP TABLE IF EXISTS sys_service_register CASCADE;
CREATE TABLE sys_service_register
(
    id             VARCHAR(32) NOT NULL,
    service_code   VARCHAR(32) NOT NULL,
    service_name   VARCHAR(32) NOT NULL,
    service_key    VARCHAR(32) NOT NULL,
    service_remark VARCHAR(255)         DEFAULT NULL,
    state          VARCHAR(12) NOT NULL,
    online_num     INTEGER              DEFAULT 0,
    is_deleted     SMALLINT    NOT NULL DEFAULT 0,
    create_by      CHAR(32)             DEFAULT NULL,
    create_time    TIMESTAMP            DEFAULT NULL,
    update_by      CHAR(32)             DEFAULT NULL,
    update_time    TIMESTAMP            DEFAULT NULL,
    PRIMARY KEY (id)
);
COMMENT ON TABLE sys_service_register IS '服务注册信息';
COMMENT ON COLUMN sys_service_register.id IS '主键ID';
COMMENT ON COLUMN sys_service_register.service_code IS '服务编码';
COMMENT ON COLUMN sys_service_register.service_name IS '服务名称';
COMMENT ON COLUMN sys_service_register.service_key IS '服务秘钥';
COMMENT ON COLUMN sys_service_register.service_remark IS '服务描述';
COMMENT ON COLUMN sys_service_register.state IS '服务状态';
COMMENT ON COLUMN sys_service_register.online_num IS '在线服务数';
COMMENT ON COLUMN sys_service_register.is_deleted IS '逻辑删除，0：未删除，1：已删除';
COMMENT ON COLUMN sys_service_register.create_by IS '创建者编号';
COMMENT ON COLUMN sys_service_register.create_time IS '创建时间';
COMMENT ON COLUMN sys_service_register.update_by IS '更新者编号';
COMMENT ON COLUMN sys_service_register.update_time IS '更新时间';


-- ----------------------------
-- 2. 行政区域表
-- ----------------------------
DROP TABLE IF EXISTS sys_region CASCADE;
CREATE TABLE sys_region
(
    id                   BIGINT       NOT NULL,
    region_code          VARCHAR(50)  NOT NULL,
    region_name          VARCHAR(100) NOT NULL,
    parent_code          VARCHAR(50),
    parents              VARCHAR(500),
    remark               VARCHAR(500),
    region_type          VARCHAR(10),
    region_area          NUMERIC(20, 6),
    region_people_number BIGINT,
    region_level         INTEGER,
    region_leader        VARCHAR(50),
    extend               BIGINT,
    longitude            NUMERIC(20, 10),
    latitude             NUMERIC(20, 10),
    bound_left           NUMERIC(20, 10),
    bound_right          NUMERIC(20, 10),
    bound_bottom         NUMERIC(20, 10),
    bound_top            NUMERIC(20, 10),
    gis_oid              BIGINT,
    is_deleted           SMALLINT     NOT NULL DEFAULT 0,
    create_by            BIGINT,
    create_time          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by            BIGINT,
    update_time          TIMESTAMP,
    order_num            INTEGER      NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);
COMMENT ON TABLE sys_region IS '行政区域划分';
COMMENT ON COLUMN sys_region.id IS '自增ID';
COMMENT ON COLUMN sys_region.region_code IS '行政区域编码';
COMMENT ON COLUMN sys_region.region_name IS '行政区域名称';
COMMENT ON COLUMN sys_region.parent_code IS '上级行政区域编码';
COMMENT ON COLUMN sys_region.parents IS '父级code集合 ,分隔';
COMMENT ON COLUMN sys_region.remark IS '备注';
COMMENT ON COLUMN sys_region.region_type IS '区域类型(0：村下级，1：社区/村，2：街道/乡镇，3：区/县，4：市，5：省/直辖市)';
COMMENT ON COLUMN sys_region.region_area IS '区域面积(KM2)';
COMMENT ON COLUMN sys_region.region_people_number IS '区域人口';
COMMENT ON COLUMN sys_region.region_level IS '区域级别';
COMMENT ON COLUMN sys_region.region_leader IS '区域负责人';
COMMENT ON COLUMN sys_region.extend IS '扩展字段值（网格：1、综合网格；2、专属网格；3、镇街网格；4村社区网格；5、基础网格）（社区：1、村社区；2、镇街社区；）';
COMMENT ON COLUMN sys_region.longitude IS '经度';
COMMENT ON COLUMN sys_region.latitude IS '纬度';
COMMENT ON COLUMN sys_region.bound_left IS '行政区左边界经度';
COMMENT ON COLUMN sys_region.bound_right IS '行政区右边界经度';
COMMENT ON COLUMN sys_region.bound_bottom IS '行政区下边界纬度';
COMMENT ON COLUMN sys_region.bound_top IS '行政区上边界纬度';
COMMENT ON COLUMN sys_region.gis_oid IS 'gis标绘信息，大于0整数代表已标绘';
COMMENT ON COLUMN sys_region.is_deleted IS '删除标记(0.未删除 1.已删除)';
COMMENT ON COLUMN sys_region.create_by IS '创建人';
COMMENT ON COLUMN sys_region.create_time IS '创建时间';
COMMENT ON COLUMN sys_region.update_by IS '更新人';
COMMENT ON COLUMN sys_region.update_time IS '更新时间';
COMMENT ON COLUMN sys_region.order_num IS '排序';


-- ----------------------------
-- 3. 系统字典表
-- ----------------------------
DROP TABLE IF EXISTS sys_dict CASCADE;
CREATE TABLE sys_dict
(
    id          BIGINT       NOT NULL,
    dict_name   VARCHAR(100) NOT NULL,
    dict_code   VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    type        SMALLINT,
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP,
    PRIMARY KEY (id)
);
COMMENT ON TABLE sys_dict IS '系统字典';
COMMENT ON COLUMN sys_dict.id IS '主键';
COMMENT ON COLUMN sys_dict.dict_name IS '字典名称';
COMMENT ON COLUMN sys_dict.dict_code IS '字典编码';
COMMENT ON COLUMN sys_dict.description IS '描述';
COMMENT ON COLUMN sys_dict.type IS '字典类型 数字类型 字符类型';
COMMENT ON COLUMN sys_dict.create_time IS '创建时间';
COMMENT ON COLUMN sys_dict.update_time IS '更新时间';


-- ----------------------------
-- 4. 字典详情表
-- ----------------------------
DROP TABLE IF EXISTS sys_dict_item CASCADE;
CREATE TABLE sys_dict_item
(
    id          BIGINT       NOT NULL,
    dict_id     BIGINT       NOT NULL,
    item_name   VARCHAR(100) NOT NULL,
    item_code   VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    order_num   INTEGER,
    is_enable   BOOLEAN,
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP,
    PRIMARY KEY (id)
);
COMMENT ON TABLE sys_dict_item IS '系统字典详情';
COMMENT ON COLUMN sys_dict_item.id IS '主键';
COMMENT ON COLUMN sys_dict_item.dict_id IS '字典id';
COMMENT ON COLUMN sys_dict_item.item_name IS '字典项值';
COMMENT ON COLUMN sys_dict_item.item_code IS '字典项key';
COMMENT ON COLUMN sys_dict_item.description IS '描述';
COMMENT ON COLUMN sys_dict_item.order_num IS '排序';
COMMENT ON COLUMN sys_dict_item.is_enable IS '状态（1启用 0不启用）';
COMMENT ON COLUMN sys_dict_item.create_time IS '创建时间';
COMMENT ON COLUMN sys_dict_item.update_time IS '更新时间';
CREATE INDEX idx_dict_item_dict_id ON sys_dict_item (dict_id);


-- ----------------------------
-- 5. 功能模块表
-- ----------------------------
DROP TABLE IF EXISTS sys_module CASCADE;
CREATE TABLE sys_module
(
    id            BIGINT       NOT NULL,
    parent_id     BIGINT,
    module_name   VARCHAR(100) NOT NULL,
    module_code   VARCHAR(100) NOT NULL,
    order_num     INTEGER,
    module_status BOOLEAN,
    is_deleted    SMALLINT     NOT NULL DEFAULT 0,
    create_by     BIGINT,
    create_time   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by     BIGINT,
    update_time   TIMESTAMP,
    PRIMARY KEY (id)
);
COMMENT ON TABLE sys_module IS '功能模块';
COMMENT ON COLUMN sys_module.id IS '主键ID';
COMMENT ON COLUMN sys_module.parent_id IS '父级ID';
COMMENT ON COLUMN sys_module.module_name IS '模块名称';
COMMENT ON COLUMN sys_module.module_code IS '模块编码';
COMMENT ON COLUMN sys_module.order_num IS '排序';
COMMENT ON COLUMN sys_module.module_status IS '状态';
COMMENT ON COLUMN sys_module.is_deleted IS '逻辑删除';
COMMENT ON COLUMN sys_module.create_by IS '创建者';
COMMENT ON COLUMN sys_module.create_time IS '创建时间';
COMMENT ON COLUMN sys_module.update_by IS '更新者';
COMMENT ON COLUMN sys_module.update_time IS '更新时间';


-- ----------------------------
-- 6. 统一操作表（按钮 + API）
-- ----------------------------
DROP TABLE IF EXISTS sys_operation CASCADE;
CREATE TABLE sys_operation
(
    id          BIGINT       NOT NULL,
    op_type     SMALLINT     NOT NULL DEFAULT 1,
    menu_id     BIGINT,
    op_name     VARCHAR(100) NOT NULL,
    perm_code   VARCHAR(100) NOT NULL,
    icon        VARCHAR(100),
    order_num   INTEGER               DEFAULT 0,
    api_path    VARCHAR(500),
    api_method  VARCHAR(10),
    auto_scan   BOOLEAN              DEFAULT FALSE,
    op_status   SMALLINT              DEFAULT 1,
    is_deleted  SMALLINT              DEFAULT 0,
    create_by   BIGINT,
    update_by   BIGINT,
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP,
    PRIMARY KEY (id)
);
COMMENT ON TABLE sys_operation IS '统一操作表：按钮+API';
COMMENT ON COLUMN sys_operation.id IS '主键';
COMMENT ON COLUMN sys_operation.op_type IS '操作类型：1=按钮, 2=API';
COMMENT ON COLUMN sys_operation.menu_id IS '所属菜单ID（按钮必填，API可选）';
COMMENT ON COLUMN sys_operation.op_name IS '操作名称，如"新增用户"';
COMMENT ON COLUMN sys_operation.perm_code IS '权限码，如 user:create';
COMMENT ON COLUMN sys_operation.icon IS '图标（仅按钮）';
COMMENT ON COLUMN sys_operation.order_num IS '排序';
COMMENT ON COLUMN sys_operation.api_path IS 'API路径（仅API）';
COMMENT ON COLUMN sys_operation.api_method IS 'HTTP方法（仅API）';
COMMENT ON COLUMN sys_operation.auto_scan IS '是否自动扫描生成：0否 1是（仅API）';
COMMENT ON COLUMN sys_operation.op_status IS '状态：0禁用 1启用';
COMMENT ON COLUMN sys_operation.is_deleted IS '逻辑删除';
COMMENT ON COLUMN sys_operation.create_by IS '创建人';
COMMENT ON COLUMN sys_operation.update_by IS '修改人';
COMMENT ON COLUMN sys_operation.create_time IS '创建时间';
COMMENT ON COLUMN sys_operation.update_time IS '修改时间';
-- 唯一约束必须是**部分索引**：SysOperation 实体带 @TableLogic（逻辑删除），
-- 若不加 WHERE，软删掉的按钮权限仍会占用 perm_code，
-- 导致「编辑一次带按钮权限的菜单」必然主键冲突报 500
-- （updateMenu 会先 deleteByMenuIdAndOpType 软删、再按传入的 buttons 重建）
CREATE UNIQUE INDEX uk_sys_operation_perm_code ON sys_operation (perm_code) WHERE is_deleted = 0;
CREATE INDEX idx_sys_operation_menu ON sys_operation (menu_id);
CREATE INDEX idx_sys_operation_type ON sys_operation (op_type);
CREATE INDEX idx_sys_operation_status ON sys_operation (op_status);
CREATE INDEX idx_sys_operation_auto_scan ON sys_operation (auto_scan);


-- ----------------------------
-- 7. 系统菜单表
-- ----------------------------
DROP TABLE IF EXISTS sys_menu CASCADE;
CREATE TABLE sys_menu
(
    id                 BIGINT       NOT NULL,
    parent_id          BIGINT,
    menu_name          VARCHAR(100) NOT NULL,
    route_path         VARCHAR(200),
    route_name         VARCHAR(100),
    icon               VARCHAR(100),
    icon_type          SMALLINT,
    meta               TEXT,
    component          VARCHAR(200),
    url                VARCHAR(500),
    menu_status        SMALLINT,
    remark             VARCHAR(500),
    menu_level         INTEGER,
    order_num          INTEGER,
    hide_in_menu       BOOLEAN,
    menu_category      SMALLINT,
    client_type        VARCHAR(20),
    i18n_key           VARCHAR(100),
    keep_alive         BOOLEAN,
    constant           BOOLEAN,
    href               VARCHAR(500),
    active_menu        VARCHAR(100),
    multi_tab          BOOLEAN,
    fixed_index_in_tab INTEGER,
    route_query        TEXT,
    create_by          BIGINT,
    update_by          BIGINT,
    is_deleted         SMALLINT     NOT NULL DEFAULT 0,
    create_time        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time        TIMESTAMP,
    PRIMARY KEY (id)
);
COMMENT ON TABLE sys_menu IS '系统菜单';
COMMENT ON COLUMN sys_menu.id IS '主键';
COMMENT ON COLUMN sys_menu.parent_id IS '父级ID';
COMMENT ON COLUMN sys_menu.menu_name IS '菜单名称';
COMMENT ON COLUMN sys_menu.route_path IS '前端路由path';
COMMENT ON COLUMN sys_menu.route_name IS '路由name，前端匹配用';
COMMENT ON COLUMN sys_menu.icon IS '前端图标';
COMMENT ON COLUMN sys_menu.icon_type IS '图标类型：1=iconify, 2=local';
COMMENT ON COLUMN sys_menu.meta IS '菜单配置（JSON扩展）';
COMMENT ON COLUMN sys_menu.component IS '目标组件';
COMMENT ON COLUMN sys_menu.url IS '请求地址';
COMMENT ON COLUMN sys_menu.menu_status IS '状态：1=启用, 2=禁用';
COMMENT ON COLUMN sys_menu.remark IS '备注';
COMMENT ON COLUMN sys_menu.menu_level IS '菜单级数';
COMMENT ON COLUMN sys_menu.order_num IS '菜单排序';
COMMENT ON COLUMN sys_menu.hide_in_menu IS '是否在菜单中隐藏';
COMMENT ON COLUMN sys_menu.menu_category IS '菜单类别：1=目录, 2=菜单';
COMMENT ON COLUMN sys_menu.client_type IS '客户端类型：PC/MOBILE/ALL';
COMMENT ON COLUMN sys_menu.i18n_key IS '国际化key';
COMMENT ON COLUMN sys_menu.keep_alive IS '是否缓存页面';
COMMENT ON COLUMN sys_menu.constant IS '是否常量路由';
COMMENT ON COLUMN sys_menu.href IS '外链地址';
COMMENT ON COLUMN sys_menu.active_menu IS '高亮菜单路由名';
COMMENT ON COLUMN sys_menu.multi_tab IS '是否多标签页';
COMMENT ON COLUMN sys_menu.fixed_index_in_tab IS '标签页固定索引';
COMMENT ON COLUMN sys_menu.route_query IS '路由query参数（JSON）';
COMMENT ON COLUMN sys_menu.create_by IS '创建人';
COMMENT ON COLUMN sys_menu.update_by IS '修改人';
COMMENT ON COLUMN sys_menu.is_deleted IS '逻辑删除';
COMMENT ON COLUMN sys_menu.create_time IS '创建时间';
COMMENT ON COLUMN sys_menu.update_time IS '修改时间';


-- ----------------------------
-- 8. 国际化消息表
-- ----------------------------
DROP TABLE IF EXISTS sys_i18n_message CASCADE;
CREATE TABLE sys_i18n_message
(
    id          BIGINT       NOT NULL,
    lang        VARCHAR(20)  NOT NULL,
    module_id   BIGINT,
    msg_key     VARCHAR(200) NOT NULL,
    msg_value   TEXT         NOT NULL,
    description VARCHAR(500),
    msg_status  BOOLEAN,
    create_by   BIGINT,
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by   BIGINT,
    update_time TIMESTAMP,
    PRIMARY KEY (id)
);
COMMENT ON TABLE sys_i18n_message IS '国际化消息';
COMMENT ON COLUMN sys_i18n_message.id IS '主键ID';
COMMENT ON COLUMN sys_i18n_message.lang IS '语言标识';
COMMENT ON COLUMN sys_i18n_message.module_id IS '所属模块ID';
COMMENT ON COLUMN sys_i18n_message.msg_key IS '消息键';
COMMENT ON COLUMN sys_i18n_message.msg_value IS '消息值';
COMMENT ON COLUMN sys_i18n_message.description IS '描述说明';
COMMENT ON COLUMN sys_i18n_message.msg_status IS '状态：true启用 false禁用';
COMMENT ON COLUMN sys_i18n_message.create_by IS '创建者';
COMMENT ON COLUMN sys_i18n_message.create_time IS '创建时间';
COMMENT ON COLUMN sys_i18n_message.update_by IS '更新者';
COMMENT ON COLUMN sys_i18n_message.update_time IS '更新时间';
CREATE INDEX idx_i18n_module_key ON sys_i18n_message (module_id, msg_key);
CREATE INDEX idx_i18n_lang ON sys_i18n_message (lang);
-- 唯一约束：同语言 + 同消息键 不可重复
-- 覆盖模型下 module_id 不参与唯一性（它只是可选的分类标签，覆盖行一律为 NULL）
-- 无需 WHERE is_deleted = 0：本表实体未标 @TableLogic，删除是物理删除
-- （对比 sys_operation，那个实体带 @TableLogic，它的唯一索引才需要部分索引）
CREATE UNIQUE INDEX uk_i18n_lang_msg_key ON sys_i18n_message (lang, msg_key);


-- ----------------------------
-- 9. 系统配置表
-- ----------------------------
DROP TABLE IF EXISTS sys_config CASCADE;
CREATE TABLE sys_config
(
    id            BIGINT       NOT NULL,
    config_name   VARCHAR(100) NOT NULL,
    config_code   VARCHAR(100) NOT NULL,
    config_value  TEXT,
    value_type    SMALLINT,
    config_status BOOLEAN,
    remark        VARCHAR(500),
    create_by     BIGINT,
    create_time   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by     BIGINT,
    update_time   TIMESTAMP,
    PRIMARY KEY (id)
);
COMMENT ON TABLE sys_config IS '系统配置';
COMMENT ON COLUMN sys_config.id IS '参数Id';
COMMENT ON COLUMN sys_config.config_name IS '参数名称';
COMMENT ON COLUMN sys_config.config_code IS '参数编码';
COMMENT ON COLUMN sys_config.config_value IS '参数键值';
COMMENT ON COLUMN sys_config.value_type IS '值类型';
COMMENT ON COLUMN sys_config.config_status IS '状态';
COMMENT ON COLUMN sys_config.remark IS '备注';
COMMENT ON COLUMN sys_config.create_by IS '创建者';
COMMENT ON COLUMN sys_config.create_time IS '创建时间';
COMMENT ON COLUMN sys_config.update_by IS '更新者';
COMMENT ON COLUMN sys_config.update_time IS '更新时间';
CREATE UNIQUE INDEX uk_sys_config_code ON sys_config (config_code);


-- ----------------------------
-- 10. 文件上传记录表
-- ----------------------------
DROP TABLE IF EXISTS sys_upload_record CASCADE;
CREATE TABLE sys_upload_record
(
    id              BIGINT       NOT NULL,
    group_id        VARCHAR(100),
    repository_name VARCHAR(100),
    repository_url  VARCHAR(500),
    original_name   VARCHAR(200),
    uses            VARCHAR(100),
    upload_status   VARCHAR(20),
    file_size       BIGINT,
    content_type    VARCHAR(100),
    file_extension  VARCHAR(20),
    storage_type    VARCHAR(20)  NOT NULL DEFAULT 'MINIO',
    create_by       BIGINT,
    create_time     TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP(6),
    is_deleted      SMALLINT              DEFAULT 0,
    deleted_time    TIMESTAMP(6),
    PRIMARY KEY (id)
);
COMMENT ON TABLE sys_upload_record IS '文件上传记录';
COMMENT ON COLUMN sys_upload_record.id IS '主键';
COMMENT ON COLUMN sys_upload_record.group_id IS '文件分组';
COMMENT ON COLUMN sys_upload_record.repository_name IS '文件库名称';
COMMENT ON COLUMN sys_upload_record.repository_url IS '文件库地址';
COMMENT ON COLUMN sys_upload_record.original_name IS '源文件名';
COMMENT ON COLUMN sys_upload_record.uses IS '文件用途';
COMMENT ON COLUMN sys_upload_record.upload_status IS '上传状态 成功 失败';
COMMENT ON COLUMN sys_upload_record.file_size IS '文件大小（字节）';
COMMENT ON COLUMN sys_upload_record.content_type IS 'MIME类型';
COMMENT ON COLUMN sys_upload_record.file_extension IS '文件扩展名';
COMMENT ON COLUMN sys_upload_record.storage_type IS '存储类型 MINIO/OSS/LOCAL';
COMMENT ON COLUMN sys_upload_record.create_by IS '上传人';
COMMENT ON COLUMN sys_upload_record.create_time IS '上传时间';
COMMENT ON COLUMN sys_upload_record.update_by IS '更新人';
COMMENT ON COLUMN sys_upload_record.update_time IS '更新时间';
COMMENT ON COLUMN sys_upload_record.is_deleted IS '逻辑删除 0=正常 1=已删除';
COMMENT ON COLUMN sys_upload_record.deleted_time IS '删除时间';
CREATE INDEX idx_upload_record_composite ON sys_upload_record (create_by, create_time) WHERE is_deleted = 0;
CREATE INDEX idx_upload_record_content_type ON sys_upload_record (content_type) WHERE is_deleted = 0;
CREATE INDEX idx_upload_record_create_by ON sys_upload_record (create_by) WHERE is_deleted = 0;
CREATE INDEX idx_upload_record_create_time ON sys_upload_record (create_time) WHERE is_deleted = 0;
CREATE INDEX idx_upload_record_group_id ON sys_upload_record (group_id) WHERE is_deleted = 0;
CREATE INDEX idx_upload_record_storage_type ON sys_upload_record (storage_type) WHERE is_deleted = 0;
CREATE INDEX idx_upload_record_uses ON sys_upload_record (uses) WHERE is_deleted = 0;


-- ----------------------------
-- 11. 文件存储配置表
-- ----------------------------
DROP TABLE IF EXISTS sys_file_storage_config CASCADE;
CREATE TABLE sys_file_storage_config
(
    id                BIGINT       NOT NULL,
    config_name       VARCHAR(100) NOT NULL,
    storage_type      VARCHAR(20)  NOT NULL,
    endpoint          VARCHAR(255),
    access_key        VARCHAR(500),
    secret_key        VARCHAR(500),
    bucket_name       VARCHAR(100),
    region            VARCHAR(50),
    path_style_access BOOLEAN               DEFAULT TRUE,
    local_base_path   VARCHAR(255),
    public_endpoint   VARCHAR(255),
    is_default        BOOLEAN               DEFAULT FALSE,
    config_status     BOOLEAN              DEFAULT TRUE,
    remark            VARCHAR(500),
    create_by         BIGINT,
    create_time       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by         BIGINT,
    update_time       TIMESTAMP,
    is_deleted        SMALLINT              DEFAULT 0,
    deleted_time      TIMESTAMP,
    PRIMARY KEY (id)
);
COMMENT ON TABLE sys_file_storage_config IS '文件存储配置';
COMMENT ON COLUMN sys_file_storage_config.id IS '主键';
COMMENT ON COLUMN sys_file_storage_config.config_name IS '配置名称';
COMMENT ON COLUMN sys_file_storage_config.storage_type IS '存储类型 S3/LOCAL';
COMMENT ON COLUMN sys_file_storage_config.endpoint IS 'S3端点地址';
COMMENT ON COLUMN sys_file_storage_config.access_key IS '访问密钥（加密存储）';
COMMENT ON COLUMN sys_file_storage_config.secret_key IS '秘密密钥（加密存储）';
COMMENT ON COLUMN sys_file_storage_config.bucket_name IS '默认Bucket';
COMMENT ON COLUMN sys_file_storage_config.region IS 'S3区域';
COMMENT ON COLUMN sys_file_storage_config.path_style_access IS '路径样式访问';
COMMENT ON COLUMN sys_file_storage_config.local_base_path IS '本地存储基础路径';
COMMENT ON COLUMN sys_file_storage_config.public_endpoint IS '公共访问端点';
COMMENT ON COLUMN sys_file_storage_config.is_default IS '是否默认配置';
COMMENT ON COLUMN sys_file_storage_config.config_status IS '状态 1启用 0禁用';
COMMENT ON COLUMN sys_file_storage_config.remark IS '备注';
COMMENT ON COLUMN sys_file_storage_config.create_by IS '创建人';
COMMENT ON COLUMN sys_file_storage_config.create_time IS '创建时间';
COMMENT ON COLUMN sys_file_storage_config.update_by IS '更新人';
COMMENT ON COLUMN sys_file_storage_config.update_time IS '更新时间';
COMMENT ON COLUMN sys_file_storage_config.is_deleted IS '逻辑删除 0=正常 1=已删除';
COMMENT ON COLUMN sys_file_storage_config.deleted_time IS '删除时间';
CREATE UNIQUE INDEX uk_storage_config_name ON sys_file_storage_config (config_name) WHERE is_deleted = 0;
CREATE INDEX idx_storage_config_default ON sys_file_storage_config (is_default) WHERE is_deleted = 0;
