-- ============================================================================
-- 开发工具平台（carlos-devkit）元数据库表 DDL
-- ----------------------------------------------------------------------------
-- 来源：《DESIGN-20261006-开发工具平台库表设计》
-- 目标库：PostgreSQL 14+
--
-- 约定（对齐框架既有 monitor_* 系列）：
--   * 表名小写下划线，统一前缀 dev_；
--   * 主键 id BIGINT（MyBatis-Plus ASSIGN_ID 雪花 ID），无统一 BaseEntity，公共字段平铺；
--   * 公共审计字段 create_by / create_time / update_by / update_time；
--   * 逻辑删除 is_deleted SMALLINT NOT NULL DEFAULT 0（部分唯一索引 WHERE is_deleted = 0）；
--   * 不使用外键、不使用存储过程。
--
-- 幂等：全部使用 CREATE TABLE IF NOT EXISTS / CREATE INDEX IF NOT EXISTS，可重复执行。
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. dev_template_group 模板组
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS dev_template_group
(
    id                  BIGINT       NOT NULL,
    group_code          VARCHAR(64)  NOT NULL,
    group_name          VARCHAR(128) NOT NULL,
    description         VARCHAR(512),
    template_type       SMALLINT     NOT NULL,
    tech_stack          VARCHAR(32),
    db_type             VARCHAR(16),
    template_root       VARCHAR(64)  NOT NULL DEFAULT 'template',
    logic_delete_fields VARCHAR(512),
    version_fields      VARCHAR(512),
    common_fields       VARCHAR(1024),
    built_in            BOOLEAN      NOT NULL DEFAULT FALSE,
    status              SMALLINT     NOT NULL DEFAULT 1,
    sort_num            INT          NOT NULL DEFAULT 0,
    remark              VARCHAR(512),
    create_by           BIGINT,
    create_time         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by           BIGINT,
    update_time         TIMESTAMP,
    is_deleted          SMALLINT     NOT NULL DEFAULT 0,
    CONSTRAINT pk_dev_template_group PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_dev_template_group_code
    ON dev_template_group (group_code) WHERE is_deleted = 0;
CREATE INDEX IF NOT EXISTS idx_dev_template_group_type
    ON dev_template_group (template_type, status);

COMMENT ON TABLE dev_template_group IS '开发工具-模板组';
COMMENT ON COLUMN dev_template_group.id IS '主键（雪花 ID）';
COMMENT ON COLUMN dev_template_group.group_code IS '模板组编码，全局唯一，如 cloud-mybatis';
COMMENT ON COLUMN dev_template_group.group_name IS '模板组显示名';
COMMENT ON COLUMN dev_template_group.description IS '描述';
COMMENT ON COLUMN dev_template_group.template_type IS '模板类型：1=代码生成 2=工程生成';
COMMENT ON COLUMN dev_template_group.tech_stack IS '技术栈编码：MYBATIS/DDD_MYBATIS/ELASTICSEARCH/MONGO/SPRINGBOOT/SPRINGCLOUD/OTHER';
COMMENT ON COLUMN dev_template_group.db_type IS '适用数据库：MYSQL/POSTGRESQL/ANY；工程生成可空';
COMMENT ON COLUMN dev_template_group.template_root IS '物化时模板根目录名，默认 template';
COMMENT ON COLUMN dev_template_group.logic_delete_fields IS '逻辑删除字段名清单，逗号分隔';
COMMENT ON COLUMN dev_template_group.version_fields IS '乐观锁版本字段名清单，逗号分隔';
COMMENT ON COLUMN dev_template_group.common_fields IS '通用字段名清单，逗号分隔';
COMMENT ON COLUMN dev_template_group.built_in IS '是否内置模板（内置不可删除，仅可停用）';
COMMENT ON COLUMN dev_template_group.status IS '状态：1=启用 0=停用';
COMMENT ON COLUMN dev_template_group.sort_num IS '排序值，越小越靠前';
COMMENT ON COLUMN dev_template_group.remark IS '备注';
COMMENT ON COLUMN dev_template_group.create_by IS '创建人';
COMMENT ON COLUMN dev_template_group.create_time IS '创建时间';
COMMENT ON COLUMN dev_template_group.update_by IS '更新人';
COMMENT ON COLUMN dev_template_group.update_time IS '更新时间';
COMMENT ON COLUMN dev_template_group.is_deleted IS '逻辑删除：0=正常 1=删除';

-- ----------------------------------------------------------------------------
-- 2. dev_template 模板文件
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS dev_template
(
    id               BIGINT       NOT NULL,
    group_id         BIGINT       NOT NULL,
    file_path        VARCHAR(512) NOT NULL,
    file_name        VARCHAR(256) NOT NULL,
    file_ext         VARCHAR(32),
    file_type        SMALLINT     NOT NULL DEFAULT 1,
    loop_flag        BOOLEAN      NOT NULL DEFAULT FALSE,
    content          TEXT,
    content_encoding VARCHAR(16)  NOT NULL DEFAULT 'TEXT',
    file_size        INT          NOT NULL DEFAULT 0,
    md5              VARCHAR(32),
    sort_num         INT          NOT NULL DEFAULT 0,
    status           SMALLINT     NOT NULL DEFAULT 1,
    remark           VARCHAR(512),
    create_by        BIGINT,
    create_time      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by        BIGINT,
    update_time      TIMESTAMP,
    is_deleted       SMALLINT     NOT NULL DEFAULT 0,
    CONSTRAINT pk_dev_template PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_dev_template_path
    ON dev_template (group_id, file_path) WHERE is_deleted = 0;
CREATE INDEX IF NOT EXISTS idx_dev_template_group ON dev_template (group_id, status);

COMMENT ON TABLE dev_template IS '开发工具-模板文件（含在线编辑正文）';
COMMENT ON COLUMN dev_template.id IS '主键（雪花 ID）';
COMMENT ON COLUMN dev_template.group_id IS '所属模板组 ID（dev_template_group.id）';
COMMENT ON COLUMN dev_template.file_path IS '组内相对路径，禁止 .. / 绝对路径 / 盘符';
COMMENT ON COLUMN dev_template.file_name IS '文件名（路径末段）';
COMMENT ON COLUMN dev_template.file_ext IS '扩展名';
COMMENT ON COLUMN dev_template.file_type IS '文件类型：1=模板(.ftl) 2=静态资源';
COMMENT ON COLUMN dev_template.loop_flag IS '是否循环渲染（文件名含 [for]）';
COMMENT ON COLUMN dev_template.content IS '文件正文；content_encoding=BASE64 时为二进制内容的 Base64';
COMMENT ON COLUMN dev_template.content_encoding IS '正文编码：TEXT=纯文本 BASE64=二进制';
COMMENT ON COLUMN dev_template.file_size IS '正文字节数';
COMMENT ON COLUMN dev_template.md5 IS '正文 MD5，用于变更检测';
COMMENT ON COLUMN dev_template.sort_num IS '同层级排序值';
COMMENT ON COLUMN dev_template.status IS '状态：1=启用 0=停用（停用则不参与生成）';
COMMENT ON COLUMN dev_template.remark IS '备注';
COMMENT ON COLUMN dev_template.create_by IS '创建人';
COMMENT ON COLUMN dev_template.create_time IS '创建时间';
COMMENT ON COLUMN dev_template.update_by IS '更新人';
COMMENT ON COLUMN dev_template.update_time IS '更新时间';
COMMENT ON COLUMN dev_template.is_deleted IS '逻辑删除：0=正常 1=删除';

-- ----------------------------------------------------------------------------
-- 3. dev_template_version 模板版本历史
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS dev_template_version
(
    id               BIGINT       NOT NULL,
    template_id      BIGINT       NOT NULL,
    group_id         BIGINT       NOT NULL,
    version_no       INT          NOT NULL,
    file_path        VARCHAR(512) NOT NULL,
    content          TEXT,
    content_encoding VARCHAR(16)  NOT NULL DEFAULT 'TEXT',
    file_size        INT          NOT NULL DEFAULT 0,
    md5              VARCHAR(32),
    change_log       VARCHAR(512),
    create_by        BIGINT,
    create_time      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted       SMALLINT     NOT NULL DEFAULT 0,
    CONSTRAINT pk_dev_template_version PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_dev_template_version_no
    ON dev_template_version (template_id, version_no) WHERE is_deleted = 0;
CREATE INDEX IF NOT EXISTS idx_dev_template_version_group ON dev_template_version (group_id);

COMMENT ON TABLE dev_template_version IS '开发工具-模板版本历史';
COMMENT ON COLUMN dev_template_version.id IS '主键（雪花 ID）';
COMMENT ON COLUMN dev_template_version.template_id IS '模板 ID（dev_template.id）';
COMMENT ON COLUMN dev_template_version.group_id IS '模板组 ID（冗余，便于按组清理）';
COMMENT ON COLUMN dev_template_version.version_no IS '版本号，同一模板内自增';
COMMENT ON COLUMN dev_template_version.file_path IS '保存时的文件路径快照';
COMMENT ON COLUMN dev_template_version.content IS '该版本的正文快照';
COMMENT ON COLUMN dev_template_version.content_encoding IS '正文编码：TEXT / BASE64';
COMMENT ON COLUMN dev_template_version.file_size IS '正文字节数';
COMMENT ON COLUMN dev_template_version.md5 IS '正文 MD5';
COMMENT ON COLUMN dev_template_version.change_log IS '变更说明';
COMMENT ON COLUMN dev_template_version.create_by IS '保存人';
COMMENT ON COLUMN dev_template_version.create_time IS '保存时间';
COMMENT ON COLUMN dev_template_version.is_deleted IS '逻辑删除：0=正常 1=删除';

-- ----------------------------------------------------------------------------
-- 4. dev_datasource 受管数据源
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS dev_datasource
(
    id              BIGINT       NOT NULL,
    ds_name         VARCHAR(128) NOT NULL,
    ds_code         VARCHAR(64),
    db_type         VARCHAR(16)  NOT NULL,
    host            VARCHAR(128) NOT NULL,
    port            INT          NOT NULL,
    database_name   VARCHAR(128),
    schema_name     VARCHAR(128),
    username        VARCHAR(128),
    password_cipher VARCHAR(512),
    jdbc_params     VARCHAR(1024),
    read_only       BOOLEAN      NOT NULL DEFAULT TRUE,
    status          SMALLINT     NOT NULL DEFAULT 1,
    remark          VARCHAR(512),
    create_by       BIGINT,
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP,
    is_deleted      SMALLINT     NOT NULL DEFAULT 0,
    CONSTRAINT pk_dev_datasource PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_dev_datasource_name
    ON dev_datasource (ds_name) WHERE is_deleted = 0;
CREATE INDEX IF NOT EXISTS idx_dev_datasource_type ON dev_datasource (db_type, status);

COMMENT ON TABLE dev_datasource IS '开发工具-受管数据源（供代码生成器读取表元数据）';
COMMENT ON COLUMN dev_datasource.id IS '主键（雪花 ID）';
COMMENT ON COLUMN dev_datasource.ds_name IS '数据源名称，全局唯一';
COMMENT ON COLUMN dev_datasource.ds_code IS '数据源编码（可空）';
COMMENT ON COLUMN dev_datasource.db_type IS '数据库类型：MYSQL/POSTGRESQL';
COMMENT ON COLUMN dev_datasource.host IS '主机地址';
COMMENT ON COLUMN dev_datasource.port IS '端口';
COMMENT ON COLUMN dev_datasource.database_name IS '库名';
COMMENT ON COLUMN dev_datasource.schema_name IS '默认 schema（PostgreSQL 专用）';
COMMENT ON COLUMN dev_datasource.username IS '用户名';
COMMENT ON COLUMN dev_datasource.password_cipher IS '密码 SM4 密文（EncryptUtil；SM4 未启用时等于明文，生产必须开启）';
COMMENT ON COLUMN dev_datasource.jdbc_params IS '额外 JDBC 参数';
COMMENT ON COLUMN dev_datasource.read_only IS '是否只读数据源（仅取元数据）';
COMMENT ON COLUMN dev_datasource.status IS '状态：1=启用 0=停用';
COMMENT ON COLUMN dev_datasource.remark IS '备注';
COMMENT ON COLUMN dev_datasource.create_by IS '创建人';
COMMENT ON COLUMN dev_datasource.create_time IS '创建时间';
COMMENT ON COLUMN dev_datasource.update_by IS '更新人';
COMMENT ON COLUMN dev_datasource.update_time IS '更新时间';
COMMENT ON COLUMN dev_datasource.is_deleted IS '逻辑删除：0=正常 1=删除';

-- ----------------------------------------------------------------------------
-- 5. dev_generate_record 生成记录
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS dev_generate_record
(
    id                  BIGINT       NOT NULL,
    biz_type            SMALLINT     NOT NULL,
    template_group_id   BIGINT,
    template_group_code VARCHAR(64),
    template_group_name VARCHAR(128),
    ds_id               BIGINT,
    ds_name             VARCHAR(128),
    project_name        VARCHAR(128),
    package_name        VARCHAR(256),
    gen_params          TEXT,
    file_count          INT          NOT NULL DEFAULT 0,
    total_size          BIGINT       NOT NULL DEFAULT 0,
    duration_ms         BIGINT       NOT NULL DEFAULT 0,
    status              SMALLINT     NOT NULL DEFAULT 1,
    error_msg           VARCHAR(1024),
    oss_object_key      VARCHAR(512),
    create_by           BIGINT,
    create_time         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted          SMALLINT     NOT NULL DEFAULT 0,
    CONSTRAINT pk_dev_generate_record PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_dev_generate_record_time ON dev_generate_record (create_time DESC);
CREATE INDEX IF NOT EXISTS idx_dev_generate_record_biz ON dev_generate_record (biz_type, template_group_code);

COMMENT ON TABLE dev_generate_record IS '开发工具-生成记录（产物摘要，不含产物）';
COMMENT ON COLUMN dev_generate_record.id IS '主键（雪花 ID）';
COMMENT ON COLUMN dev_generate_record.biz_type IS '业务类型：1=代码生成 2=工程生成';
COMMENT ON COLUMN dev_generate_record.template_group_id IS '模板组 ID';
COMMENT ON COLUMN dev_generate_record.template_group_code IS '模板组编码快照';
COMMENT ON COLUMN dev_generate_record.template_group_name IS '模板组名称快照';
COMMENT ON COLUMN dev_generate_record.ds_id IS '数据源 ID（代码生成有值）';
COMMENT ON COLUMN dev_generate_record.ds_name IS '数据源名称快照';
COMMENT ON COLUMN dev_generate_record.project_name IS '项目名';
COMMENT ON COLUMN dev_generate_record.package_name IS '包名';
COMMENT ON COLUMN dev_generate_record.gen_params IS '生成参数完整 JSON 快照';
COMMENT ON COLUMN dev_generate_record.file_count IS '产物文件数';
COMMENT ON COLUMN dev_generate_record.total_size IS '产物总字节数';
COMMENT ON COLUMN dev_generate_record.duration_ms IS '生成耗时（毫秒）';
COMMENT ON COLUMN dev_generate_record.status IS '状态：1=成功 0=失败';
COMMENT ON COLUMN dev_generate_record.error_msg IS '失败原因';
COMMENT ON COLUMN dev_generate_record.oss_object_key IS '产物 OSS 对象键（预留，一期不写入）';
COMMENT ON COLUMN dev_generate_record.create_by IS '操作人';
COMMENT ON COLUMN dev_generate_record.create_time IS '生成时间';
COMMENT ON COLUMN dev_generate_record.is_deleted IS '逻辑删除：0=正常 1=删除';
