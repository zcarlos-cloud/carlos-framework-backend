-- ============================================
-- Carlos Framework - carlos-org 模块
-- PostgreSQL 建表脚本
-- 数据库: PostgreSQL 14+
-- 生成日期: 2026-07-03
-- 说明: 从 docs/db/postgresql/02-carlos-org.sql 迁移，并按最新 Entity 修正字段名
-- 关键修正:
--   org_user.state -> user_status
--   org_department.state -> dept_status
--   org_department.sort -> order_num
--   org_position_level.state -> position_level_status
--   org_position_role.state -> position_role_status
-- ============================================

-- ----------------------------
-- 1. 系统用户表
-- ----------------------------
DROP TABLE IF EXISTS org_user CASCADE;
CREATE TABLE org_user
(
    id              BIGINT      NOT NULL,
    account         VARCHAR(20) NOT NULL,
    nickname        VARCHAR(20) NOT NULL,
    pwd VARCHAR(128) NOT NULL,
    identify        VARCHAR(18),
    phone           VARCHAR(11),
    address         VARCHAR(100),
    gender          SMALLINT,
    email           VARCHAR(30),
    avatar          VARCHAR(200),
    description     VARCHAR(200),
    user_status     SMALLINT,
    main_dept_id    BIGINT,
    login_time      TIMESTAMP,
    login_ip        VARCHAR(50) NOT NULL DEFAULT '0',
    login_count     INTEGER     NOT NULL DEFAULT 0,
    version         INTEGER     NOT NULL DEFAULT 0,
    pwd_last_modify TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted      SMALLINT    NOT NULL DEFAULT 0,
    create_by       BIGINT,
    create_time     TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP,
    tenant_id       BIGINT,
    PRIMARY KEY (id)
);
COMMENT ON TABLE org_user IS '系统用户';
COMMENT ON COLUMN org_user.id IS '主键';
COMMENT ON COLUMN org_user.account IS '用户名';
COMMENT ON COLUMN org_user.nickname IS '昵称';
COMMENT ON COLUMN org_user.pwd IS '密码';
COMMENT ON COLUMN org_user.identify IS '证件号码';
COMMENT ON COLUMN org_user.phone IS '手机号码';
COMMENT ON COLUMN org_user.address IS '详细地址';
COMMENT ON COLUMN org_user.gender IS '性别，0：保密, 1：男，2：女';
COMMENT ON COLUMN org_user.email IS '邮箱';
COMMENT ON COLUMN org_user.avatar IS '头像';
COMMENT ON COLUMN org_user.description IS '备注';
COMMENT ON COLUMN org_user.user_status IS '状态，0：禁用，1：启用，2：锁定';
COMMENT ON COLUMN org_user.main_dept_id IS '主部门id';
COMMENT ON COLUMN org_user.login_time IS '最后登录时间';
COMMENT ON COLUMN org_user.login_ip IS '最后登录ip';
COMMENT ON COLUMN org_user.login_count IS '登录次数';
COMMENT ON COLUMN org_user.version IS '版本';
COMMENT ON COLUMN org_user.pwd_last_modify IS '密码最后修改时间';
COMMENT ON COLUMN org_user.is_deleted IS '逻辑删除，0：未删除，1：已删除';
COMMENT ON COLUMN org_user.create_by IS '创建者';
COMMENT ON COLUMN org_user.create_time IS '创建时间';
COMMENT ON COLUMN org_user.update_by IS '修改者';
COMMENT ON COLUMN org_user.update_time IS '修改时间';
COMMENT ON COLUMN org_user.tenant_id IS '租户id';
CREATE INDEX idx_org_user_main_dept ON org_user (main_dept_id, user_status);


-- ----------------------------
-- 2. 部门表
-- ----------------------------
DROP TABLE IF EXISTS org_department CASCADE;
CREATE TABLE org_department
(
    id          BIGINT       NOT NULL,
    parent_id   BIGINT       NOT NULL DEFAULT 0,
    dept_name   VARCHAR(64)  NOT NULL,
    dept_code   VARCHAR(64),
    path        VARCHAR(500) NOT NULL,
    leader_id   BIGINT       NOT NULL,
    dept_status SMALLINT     NOT NULL DEFAULT 1,
    order_num   INTEGER      NOT NULL DEFAULT 0,
    level       INTEGER      NOT NULL DEFAULT 0,
    description VARCHAR(200),
    version     INTEGER      NOT NULL DEFAULT 0,
    is_deleted  SMALLINT     NOT NULL DEFAULT 0,
    create_by   BIGINT       NOT NULL,
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by   BIGINT,
    update_time TIMESTAMP,
    tenant_id   BIGINT,
    PRIMARY KEY (id)
);
COMMENT ON TABLE org_department IS '部门';
COMMENT ON COLUMN org_department.id IS '主键';
COMMENT ON COLUMN org_department.parent_id IS '父id';
COMMENT ON COLUMN org_department.dept_name IS '部门名称';
COMMENT ON COLUMN org_department.dept_code IS '部门编号';
COMMENT ON COLUMN org_department.path IS '部门路径';
COMMENT ON COLUMN org_department.leader_id IS '负责人id';
COMMENT ON COLUMN org_department.dept_status IS '状态，0：禁用，1：启用';
COMMENT ON COLUMN org_department.order_num IS '排序';
COMMENT ON COLUMN org_department.level IS '层级';
COMMENT ON COLUMN org_department.description IS '备注';
COMMENT ON COLUMN org_department.version IS '版本';
COMMENT ON COLUMN org_department.is_deleted IS '逻辑删除';
COMMENT ON COLUMN org_department.create_by IS '创建者';
COMMENT ON COLUMN org_department.create_time IS '创建时间';
COMMENT ON COLUMN org_department.update_by IS '修改者';
COMMENT ON COLUMN org_department.update_time IS '修改时间';
COMMENT ON COLUMN org_department.tenant_id IS '租户id';
CREATE UNIQUE INDEX uk_org_dept_code ON org_department (dept_code);
CREATE INDEX idx_org_dept_path ON org_department (path);


-- ----------------------------
-- 3. 系统角色表
-- ----------------------------
DROP TABLE IF EXISTS org_role CASCADE;
CREATE TABLE org_role
(
    id          BIGINT       NOT NULL,
    role_name   VARCHAR(40)  NOT NULL,
    role_code   VARCHAR(100),
    role_type   SMALLINT     NOT NULL DEFAULT 1,
    data_scope  SMALLINT     NOT NULL DEFAULT 1,
    role_status SMALLINT     NOT NULL DEFAULT 0,
    description VARCHAR(200) NOT NULL DEFAULT '',
    version     INTEGER      NOT NULL DEFAULT 0,
    is_deleted  SMALLINT     NOT NULL DEFAULT 0,
    create_by   BIGINT,
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by   BIGINT,
    update_time TIMESTAMP,
    tenant_id   BIGINT,
    PRIMARY KEY (id)
);
COMMENT ON TABLE org_role IS '系统角色';
COMMENT ON COLUMN org_role.id IS '主键';
COMMENT ON COLUMN org_role.role_name IS '角色名称';
COMMENT ON COLUMN org_role.role_code IS '角色唯一编码';
COMMENT ON COLUMN org_role.role_type IS '角色类型，1：系统角色, 2: 自定义角色';
COMMENT ON COLUMN org_role.data_scope IS '数据范围，1：全部, 2: 本部及子部门, 3:仅本部门, 4:仅本人, 5:自定义规则';
COMMENT ON COLUMN org_role.role_status IS '角色状态，0：禁用, 1: 启用';
COMMENT ON COLUMN org_role.description IS '备注';
COMMENT ON COLUMN org_role.version IS '版本';
COMMENT ON COLUMN org_role.is_deleted IS '逻辑删除';
COMMENT ON COLUMN org_role.create_by IS '创建者';
COMMENT ON COLUMN org_role.create_time IS '创建时间';
COMMENT ON COLUMN org_role.update_by IS '修改者';
COMMENT ON COLUMN org_role.update_time IS '修改时间';
COMMENT ON COLUMN org_role.tenant_id IS '租户id';


-- ----------------------------
-- 4. 用户部门关联表
-- ----------------------------
DROP TABLE IF EXISTS org_user_department CASCADE;
CREATE TABLE org_user_department
(
    id           BIGINT    NOT NULL,
    user_id      BIGINT    NOT NULL,
    dept_id      BIGINT    NOT NULL,
    is_main_dept BOOLEAN   NOT NULL DEFAULT FALSE,
    create_by    BIGINT,
    create_time  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);
COMMENT ON TABLE org_user_department IS '用户部门';
COMMENT ON COLUMN org_user_department.id IS '主键';
COMMENT ON COLUMN org_user_department.user_id IS '用户id';
COMMENT ON COLUMN org_user_department.dept_id IS '部门id';
COMMENT ON COLUMN org_user_department.is_main_dept IS '是否为主部门';
COMMENT ON COLUMN org_user_department.create_by IS '创建者';
COMMENT ON COLUMN org_user_department.create_time IS '创建时间';
CREATE UNIQUE INDEX uk_org_user_dept ON org_user_department (user_id, dept_id);
CREATE INDEX idx_org_user_dept_user ON org_user_department (user_id);
CREATE INDEX idx_org_user_dept_dept ON org_user_department (dept_id);


-- ----------------------------
-- 5. 用户角色关联表
-- ----------------------------
DROP TABLE IF EXISTS org_user_role CASCADE;
CREATE TABLE org_user_role
(
    id          BIGINT    NOT NULL,
    user_id     BIGINT    NOT NULL,
    role_id     BIGINT    NOT NULL,
    dept_id     BIGINT,
    expire_time TIMESTAMP,
    is_deleted  SMALLINT  NOT NULL DEFAULT 0,
    create_by   BIGINT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tenant_id   BIGINT,
    PRIMARY KEY (id)
);
COMMENT ON TABLE org_user_role IS '用户角色';
COMMENT ON COLUMN org_user_role.id IS '主键';
COMMENT ON COLUMN org_user_role.user_id IS '用户id';
COMMENT ON COLUMN org_user_role.role_id IS '角色id';
COMMENT ON COLUMN org_user_role.dept_id IS '角色生效的部门id';
COMMENT ON COLUMN org_user_role.expire_time IS '失效时间';
COMMENT ON COLUMN org_user_role.is_deleted IS '逻辑删除，0：未删除，1：已删除';
COMMENT ON COLUMN org_user_role.create_by IS '创建者';
COMMENT ON COLUMN org_user_role.create_time IS '创建时间';
COMMENT ON COLUMN org_user_role.tenant_id IS '租户id';
CREATE INDEX idx_org_user_role_user ON org_user_role (user_id);
CREATE INDEX idx_org_user_role_role ON org_user_role (role_id);


-- ----------------------------
-- 6. 部门角色关联表
-- ----------------------------
DROP TABLE IF EXISTS org_department_role CASCADE;
CREATE TABLE org_department_role
(
    id              BIGINT    NOT NULL,
    dept_id         BIGINT    NOT NULL,
    role_id         BIGINT    NOT NULL,
    version         INTEGER   NOT NULL DEFAULT 0,
    is_default_role BOOLEAN  NOT NULL DEFAULT FALSE,
    create_by       BIGINT,
    create_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP,
    tenant_id       BIGINT,
    PRIMARY KEY (id)
);
COMMENT ON TABLE org_department_role IS '部门角色';
COMMENT ON COLUMN org_department_role.id IS '主键';
COMMENT ON COLUMN org_department_role.dept_id IS '部门id';
COMMENT ON COLUMN org_department_role.role_id IS '角色id';
COMMENT ON COLUMN org_department_role.version IS '版本';
COMMENT ON COLUMN org_department_role.is_default_role IS '是否为默认角色';
COMMENT ON COLUMN org_department_role.create_by IS '创建者';
COMMENT ON COLUMN org_department_role.create_time IS '创建时间';
COMMENT ON COLUMN org_department_role.update_time IS '修改时间';
COMMENT ON COLUMN org_department_role.tenant_id IS '租户id';
CREATE UNIQUE INDEX uk_org_dept_role ON org_department_role (dept_id, role_id);


-- ----------------------------
-- 7. 用户导入临时表
-- ----------------------------
DROP TABLE IF EXISTS org_user_import CASCADE;
CREATE TABLE org_user_import
(
    id          BIGINT      NOT NULL,
    account     VARCHAR(20) NOT NULL,
    realname    VARCHAR(20) NOT NULL,
    identify    VARCHAR(18),
    phone       VARCHAR(11) NOT NULL,
    role        VARCHAR(16),
    dept        VARCHAR(200),
    dept_type   VARCHAR(255),
    region_code VARCHAR(50),
    gender      VARCHAR(10),
    email       VARCHAR(30),
    PRIMARY KEY (id)
);
COMMENT ON TABLE org_user_import IS '用户导入';
COMMENT ON COLUMN org_user_import.id IS '主键';
COMMENT ON COLUMN org_user_import.account IS '用户名';
COMMENT ON COLUMN org_user_import.realname IS '真实姓名';
COMMENT ON COLUMN org_user_import.identify IS '证件号码';
COMMENT ON COLUMN org_user_import.phone IS '手机号码';
COMMENT ON COLUMN org_user_import.role IS '角色';
COMMENT ON COLUMN org_user_import.dept IS '部门';
COMMENT ON COLUMN org_user_import.dept_type IS '部门类型';
COMMENT ON COLUMN org_user_import.region_code IS '行政区域编码';
COMMENT ON COLUMN org_user_import.gender IS '性别';
COMMENT ON COLUMN org_user_import.email IS '邮箱';


-- ----------------------------
-- 8. 用户对接映射表
-- ----------------------------
DROP TABLE IF EXISTS org_docking_mapping CASCADE;
CREATE TABLE org_docking_mapping
(
    id           BIGINT      NOT NULL,
    system_id    BIGINT      NOT NULL,
    target_id    BIGINT      NOT NULL,
    target_code  VARCHAR(50) NOT NULL,
    docking_type SMALLINT,
    is_deleted   SMALLINT    NOT NULL DEFAULT 0,
    create_by    BIGINT,
    create_time  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by    BIGINT,
    update_time  TIMESTAMP,
    PRIMARY KEY (id)
);
COMMENT ON TABLE org_docking_mapping IS '用户信息对接关联表';
COMMENT ON COLUMN org_docking_mapping.id IS '主键ID';
COMMENT ON COLUMN org_docking_mapping.system_id IS '系统数据id';
COMMENT ON COLUMN org_docking_mapping.target_id IS '目标系统id';
COMMENT ON COLUMN org_docking_mapping.target_code IS '目标系统标识';
COMMENT ON COLUMN org_docking_mapping.docking_type IS '对接类型';
COMMENT ON COLUMN org_docking_mapping.is_deleted IS '逻辑删除，0：未删除，1：已删除';
COMMENT ON COLUMN org_docking_mapping.create_by IS '创建者编号';
COMMENT ON COLUMN org_docking_mapping.create_time IS '创建时间';
COMMENT ON COLUMN org_docking_mapping.update_by IS '更新者编号';
COMMENT ON COLUMN org_docking_mapping.update_time IS '更新时间';


-- ----------------------------
-- 9. 角色操作权限关联表
-- ----------------------------
DROP TABLE IF EXISTS org_role_operation CASCADE;
CREATE TABLE org_role_operation
(
    id           BIGINT    NOT NULL,
    role_id      BIGINT    NOT NULL,
    operation_id BIGINT    NOT NULL,
    tenant_id    BIGINT,
    create_by    BIGINT,
    create_time  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);
COMMENT ON TABLE org_role_operation IS '角色-操作权限关联表';
COMMENT ON COLUMN org_role_operation.id IS '主键';
COMMENT ON COLUMN org_role_operation.role_id IS '角色ID';
COMMENT ON COLUMN org_role_operation.operation_id IS '操作ID';
COMMENT ON COLUMN org_role_operation.tenant_id IS '租户ID';
COMMENT ON COLUMN org_role_operation.create_by IS '创建者';
COMMENT ON COLUMN org_role_operation.create_time IS '创建时间';
CREATE UNIQUE INDEX uk_org_role_operation ON org_role_operation (role_id, operation_id);
CREATE INDEX idx_org_role_op_role ON org_role_operation (role_id);
CREATE INDEX idx_org_role_op_op ON org_role_operation (operation_id);


-- ----------------------------
-- 10. 岗位类别表（职系）
-- ----------------------------
DROP TABLE IF EXISTS org_position_category CASCADE;
CREATE TABLE org_position_category
(
    id                       BIGINT      NOT NULL,
    category_code            VARCHAR(50) NOT NULL,
    category_name            VARCHAR(50) NOT NULL,
    category_type            SMALLINT    NOT NULL DEFAULT 1,
    description              VARCHAR(200),
    order_num                INTEGER     NOT NULL DEFAULT 0,
    position_category_status SMALLINT    NOT NULL DEFAULT 1,
    version                  INTEGER     NOT NULL DEFAULT 0,
    is_deleted               SMALLINT    NOT NULL DEFAULT 0,
    tenant_id                BIGINT,
    create_by                BIGINT,
    create_time              TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by                BIGINT,
    update_time              TIMESTAMP,
    PRIMARY KEY (id)
);
COMMENT ON TABLE org_position_category IS '岗位类别表（职系）';
COMMENT ON COLUMN org_position_category.id IS '主键';
COMMENT ON COLUMN org_position_category.category_code IS '类别编码，如：M、T、P、S';
COMMENT ON COLUMN org_position_category.category_name IS '类别名称，如：管理系、技术系、专业系、销售系';
COMMENT ON COLUMN org_position_category.category_type IS '类别类型：1管理通道，2专业通道，3双通道';
COMMENT ON COLUMN org_position_category.description IS '类别描述';
COMMENT ON COLUMN org_position_category.order_num IS '排序';
COMMENT ON COLUMN org_position_category.position_category_status IS '状态：0禁用，1启用';
COMMENT ON COLUMN org_position_category.version IS '乐观锁版本号';
COMMENT ON COLUMN org_position_category.is_deleted IS '逻辑删除：0正常，1删除';
COMMENT ON COLUMN org_position_category.tenant_id IS '租户ID';
COMMENT ON COLUMN org_position_category.create_by IS '创建者';
COMMENT ON COLUMN org_position_category.create_time IS '创建时间';
COMMENT ON COLUMN org_position_category.update_by IS '修改者';
COMMENT ON COLUMN org_position_category.update_time IS '修改时间';
CREATE UNIQUE INDEX uk_org_pos_cat_code ON org_position_category (tenant_id, category_code);
CREATE INDEX idx_org_pos_cat_type_state ON org_position_category (category_type, position_category_status);


-- ----------------------------
-- 11. 职级表
-- ----------------------------
DROP TABLE IF EXISTS org_position_level CASCADE;
CREATE TABLE org_position_level
(
    id                    BIGINT      NOT NULL,
    category_id           BIGINT      NOT NULL,
    level_code            VARCHAR(20) NOT NULL,
    level_name            VARCHAR(50) NOT NULL,
    level_seq             INTEGER     NOT NULL,
    level_group           VARCHAR(20),
    min_salary            NUMERIC(12, 2),
    max_salary            NUMERIC(12, 2),
    stock_level           SMALLINT             DEFAULT 0,
    description           VARCHAR(200),
    requirements          TEXT,
    position_level_status SMALLINT    NOT NULL DEFAULT 1,
    version               INTEGER     NOT NULL DEFAULT 0,
    is_deleted            SMALLINT    NOT NULL DEFAULT 0,
    tenant_id             BIGINT,
    create_by             BIGINT,
    create_time           TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by             BIGINT,
    update_time           TIMESTAMP,
    PRIMARY KEY (id)
);
COMMENT ON TABLE org_position_level IS '职级表';
COMMENT ON COLUMN org_position_level.id IS '主键';
COMMENT ON COLUMN org_position_level.category_id IS '所属职系ID';
COMMENT ON COLUMN org_position_level.level_code IS '职级编码，如：T1、T2-1、M3';
COMMENT ON COLUMN org_position_level.level_name IS '职级名称，如：初级工程师、高级工程师、部门经理';
COMMENT ON COLUMN org_position_level.level_seq IS '职级序列号，用于排序和比较';
COMMENT ON COLUMN org_position_level.level_group IS '职级分组：初级、中级、高级、专家、资深专家';
COMMENT ON COLUMN org_position_level.min_salary IS '薪资范围下限';
COMMENT ON COLUMN org_position_level.max_salary IS '薪资范围上限';
COMMENT ON COLUMN org_position_level.stock_level IS '股权激励等级：0无，1部分，2全额';
COMMENT ON COLUMN org_position_level.description IS '职级描述';
COMMENT ON COLUMN org_position_level.requirements IS '晋升要求（JSON格式）';
COMMENT ON COLUMN org_position_level.position_level_status IS '状态：0禁用，1启用';
COMMENT ON COLUMN org_position_level.version IS '乐观锁版本号';
COMMENT ON COLUMN org_position_level.is_deleted IS '逻辑删除：0正常，1删除';
COMMENT ON COLUMN org_position_level.tenant_id IS '租户ID';
COMMENT ON COLUMN org_position_level.create_by IS '创建者';
COMMENT ON COLUMN org_position_level.create_time IS '创建时间';
COMMENT ON COLUMN org_position_level.update_by IS '修改者';
COMMENT ON COLUMN org_position_level.update_time IS '修改时间';
CREATE UNIQUE INDEX uk_org_pos_level_code ON org_position_level (tenant_id, level_code);
CREATE UNIQUE INDEX uk_org_pos_level_cat_seq ON org_position_level (category_id, level_seq);
CREATE INDEX idx_org_pos_level_group ON org_position_level (level_group, position_level_status);


-- ----------------------------
-- 12. 岗位表
-- ----------------------------
DROP TABLE IF EXISTS org_position CASCADE;
CREATE TABLE org_position
(
    id               BIGINT      NOT NULL,
    position_code    VARCHAR(50) NOT NULL,
    position_name    VARCHAR(50) NOT NULL,
    position_short   VARCHAR(20),
    category_id      BIGINT      NOT NULL,
    default_level_id BIGINT,
    level_range      VARCHAR(100),
    position_type    SMALLINT    NOT NULL DEFAULT 1,
    position_nature  SMALLINT    NOT NULL DEFAULT 1,
    department_id    BIGINT,
    parent_id        BIGINT      NOT NULL DEFAULT 0,
    headcount        INTEGER              DEFAULT 1,
    current_count    INTEGER     NOT NULL DEFAULT 0,
    vacancy_count    INTEGER     NOT NULL DEFAULT 0,
    duty             TEXT,
    requirement      TEXT,
    qualification    TEXT,
    kpi_indicator    TEXT,
    cost_center      VARCHAR(50),
    location         VARCHAR(100),
    salary_grade     VARCHAR(20),
    position_status  SMALLINT    NOT NULL DEFAULT 1,
    version          INTEGER     NOT NULL DEFAULT 0,
    is_deleted       SMALLINT    NOT NULL DEFAULT 0,
    tenant_id        BIGINT,
    create_by        BIGINT,
    create_time      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by        BIGINT,
    update_time      TIMESTAMP,
    PRIMARY KEY (id)
);
COMMENT ON TABLE org_position IS '岗位表';
COMMENT ON COLUMN org_position.id IS '主键';
COMMENT ON COLUMN org_position.position_code IS '岗位编码，租户内唯一';
COMMENT ON COLUMN org_position.position_name IS '岗位名称';
COMMENT ON COLUMN org_position.position_short IS '岗位简称';
COMMENT ON COLUMN org_position.category_id IS '所属职系ID';
COMMENT ON COLUMN org_position.default_level_id IS '默认职级ID（新人入职默认职级）';
COMMENT ON COLUMN org_position.level_range IS '职级范围，如：T1-T5，支持的范围';
COMMENT ON COLUMN org_position.position_type IS '岗位类型：1标准岗，2虚拟岗，3兼职岗';
COMMENT ON COLUMN org_position.position_nature IS '岗位性质：1全职，2兼职，3实习，4外包';
COMMENT ON COLUMN org_position.department_id IS '所属部门ID（标准岗必填，虚拟岗可为空）';
COMMENT ON COLUMN org_position.parent_id IS '上级岗位ID，用于汇报线，0表示无上级';
COMMENT ON COLUMN org_position.headcount IS '编制人数，0表示不限';
COMMENT ON COLUMN org_position.current_count IS '当前在岗人数（冗余，实时计算）';
COMMENT ON COLUMN org_position.vacancy_count IS '空缺人数（冗余，实时计算）';
COMMENT ON COLUMN org_position.duty IS '岗位职责（富文本）';
COMMENT ON COLUMN org_position.requirement IS '任职要求（富文本）';
COMMENT ON COLUMN org_position.qualification IS '任职资格（JSON格式，支持扩展）';
COMMENT ON COLUMN org_position.kpi_indicator IS '绩效考核指标（JSON格式）';
COMMENT ON COLUMN org_position.cost_center IS '成本中心编码，对接财务系统';
COMMENT ON COLUMN org_position.location IS '工作地点，如：北京、上海、远程';
COMMENT ON COLUMN org_position.salary_grade IS '薪等，如：P5、P6';
COMMENT ON COLUMN org_position.position_status IS '状态：0禁用，1启用，2冻结（暂停招聘）';
COMMENT ON COLUMN org_position.version IS '乐观锁版本号';
COMMENT ON COLUMN org_position.is_deleted IS '逻辑删除：0正常，1删除';
COMMENT ON COLUMN org_position.tenant_id IS '租户ID';
COMMENT ON COLUMN org_position.create_by IS '创建者';
COMMENT ON COLUMN org_position.create_time IS '创建时间';
COMMENT ON COLUMN org_position.update_by IS '修改者';
COMMENT ON COLUMN org_position.update_time IS '修改时间';
CREATE UNIQUE INDEX uk_org_position_code ON org_position (tenant_id, position_code);
CREATE INDEX idx_org_position_category ON org_position (category_id, position_status);
CREATE INDEX idx_org_position_dept ON org_position (department_id, position_type, position_status);
CREATE INDEX idx_org_position_parent ON org_position (parent_id);
CREATE INDEX idx_org_position_level ON org_position (default_level_id);


-- ----------------------------
-- 13. 岗位角色关联表
-- ----------------------------
DROP TABLE IF EXISTS org_position_role CASCADE;
CREATE TABLE org_position_role
(
    id                   BIGINT    NOT NULL,
    position_id          BIGINT    NOT NULL,
    role_id              BIGINT    NOT NULL,
    is_default           BOOLEAN  NOT NULL DEFAULT FALSE,
    position_role_status SMALLINT  NOT NULL DEFAULT 1,
    create_by            BIGINT,
    create_time          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by            BIGINT,
    update_time          TIMESTAMP,
    tenant_id            BIGINT,
    PRIMARY KEY (id)
);
COMMENT ON TABLE org_position_role IS '岗位角色关联表（岗位默认权限配置）';
COMMENT ON COLUMN org_position_role.id IS '主键';
COMMENT ON COLUMN org_position_role.position_id IS '岗位ID';
COMMENT ON COLUMN org_position_role.role_id IS '角色ID';
COMMENT ON COLUMN org_position_role.is_default IS '是否默认角色：1是（入职自动获得），0否（可选）';
COMMENT ON COLUMN org_position_role.position_role_status IS '状态：0停用，1启用';
COMMENT ON COLUMN org_position_role.create_by IS '创建者';
COMMENT ON COLUMN org_position_role.create_time IS '创建时间';
COMMENT ON COLUMN org_position_role.update_by IS '修改者';
COMMENT ON COLUMN org_position_role.update_time IS '修改时间';
COMMENT ON COLUMN org_position_role.tenant_id IS '租户ID';
CREATE UNIQUE INDEX uk_org_pos_role ON org_position_role (position_id, role_id);
CREATE INDEX idx_org_pos_role_default ON org_position_role (position_id, is_default, position_role_status);


-- ----------------------------
-- 14. 用户岗位职级关联表
-- ----------------------------
DROP TABLE IF EXISTS org_user_position CASCADE;
CREATE TABLE org_user_position
(
    id                 BIGINT    NOT NULL,
    user_id            BIGINT    NOT NULL,
    position_id        BIGINT    NOT NULL,
    level_id           BIGINT    NOT NULL,
    department_id      BIGINT    NOT NULL,
    is_main            BOOLEAN  NOT NULL DEFAULT FALSE,
    position_status    SMALLINT  NOT NULL DEFAULT 1,
    appoint_type       SMALLINT  NOT NULL DEFAULT 1,
    appoint_date       DATE      NOT NULL,
    probation_end      DATE,
    dimission_date     DATE,
    cost_ratio         NUMERIC(3, 2)      DEFAULT 1.00,
    report_to          BIGINT,
    dotted_report      BIGINT,
    work_location      VARCHAR(100),
    contract_type      SMALLINT           DEFAULT 1,
    performance_rating VARCHAR(10),
    salary_level       VARCHAR(20),
    stock_grant        BOOLEAN           DEFAULT FALSE,
    version            INTEGER   NOT NULL DEFAULT 0,
    is_deleted         SMALLINT  NOT NULL DEFAULT 0,
    tenant_id          BIGINT,
    create_by          BIGINT,
    create_time        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by          BIGINT,
    update_time        TIMESTAMP,
    PRIMARY KEY (id)
);
COMMENT ON TABLE org_user_position IS '用户岗位职级关联表（核心任职信息）';
COMMENT ON COLUMN org_user_position.id IS '主键';
COMMENT ON COLUMN org_user_position.user_id IS '用户ID';
COMMENT ON COLUMN org_user_position.position_id IS '岗位ID';
COMMENT ON COLUMN org_user_position.level_id IS '职级ID（具体的职级）';
COMMENT ON COLUMN org_user_position.department_id IS '任职部门ID（岗位可能跨部门，此处记录实际任职部门）';
COMMENT ON COLUMN org_user_position.is_main IS '是否主岗位：1是，0否（兼职/兼岗）';
COMMENT ON COLUMN org_user_position.position_status IS '任职状态：1在职，2试用期，3待入职，4待离职，5已卸任';
COMMENT ON COLUMN org_user_position.appoint_type IS '任职方式：1任命，2竞聘，3委派，4借调';
COMMENT ON COLUMN org_user_position.appoint_date IS '任职日期';
COMMENT ON COLUMN org_user_position.probation_end IS '试用期结束日期';
COMMENT ON COLUMN org_user_position.dimission_date IS '卸任日期';
COMMENT ON COLUMN org_user_position.cost_ratio IS '成本分摊比例（兼职岗使用，如0.5表示50%）';
COMMENT ON COLUMN org_user_position.report_to IS '汇报对象（直接上级）user_id';
COMMENT ON COLUMN org_user_position.dotted_report IS '虚线汇报对象（矩阵管理）user_id';
COMMENT ON COLUMN org_user_position.work_location IS '实际工作地点（可覆盖岗位默认地点）';
COMMENT ON COLUMN org_user_position.contract_type IS '合同类型：1劳动合同，2劳务合同，3实习协议，4外包合同';
COMMENT ON COLUMN org_user_position.performance_rating IS '最近绩效等级：A/B/C/D';
COMMENT ON COLUMN org_user_position.salary_level IS '实际薪级（可能高于岗位默认）';
COMMENT ON COLUMN org_user_position.stock_grant IS '是否有股权激励：0无，1有';
COMMENT ON COLUMN org_user_position.version IS '乐观锁版本号';
COMMENT ON COLUMN org_user_position.is_deleted IS '逻辑删除：0正常，1删除';
COMMENT ON COLUMN org_user_position.tenant_id IS '租户ID';
COMMENT ON COLUMN org_user_position.create_by IS '创建者';
COMMENT ON COLUMN org_user_position.create_time IS '创建时间';
COMMENT ON COLUMN org_user_position.update_by IS '修改者';
COMMENT ON COLUMN org_user_position.update_time IS '修改时间';
CREATE UNIQUE INDEX uk_org_user_pos ON org_user_position (user_id, position_id, department_id);
CREATE INDEX idx_org_user_pos_main ON org_user_position (user_id, is_main, position_status);
CREATE INDEX idx_org_user_pos_pos ON org_user_position (position_id, position_status);
CREATE INDEX idx_org_user_pos_dept ON org_user_position (department_id, position_status);
CREATE INDEX idx_org_user_pos_report ON org_user_position (report_to);
CREATE INDEX idx_org_user_pos_level ON org_user_position (level_id);


-- ----------------------------
-- 15. 岗位变更历史表
-- ----------------------------
DROP TABLE IF EXISTS org_position_history CASCADE;
CREATE TABLE org_position_history
(
    id              BIGINT       NOT NULL,
    user_id         BIGINT       NOT NULL,
    change_type     SMALLINT     NOT NULL,
    old_position_id BIGINT,
    new_position_id BIGINT,
    old_level_id    BIGINT,
    new_level_id    BIGINT,
    old_dept_id     BIGINT,
    new_dept_id     BIGINT,
    old_salary      NUMERIC(12, 2),
    new_salary      NUMERIC(12, 2),
    change_reason   VARCHAR(200) NOT NULL,
    change_date     DATE         NOT NULL,
    approval_no     VARCHAR(50),
    attachments     TEXT,
    remark          VARCHAR(500),
    operator_id     BIGINT       NOT NULL,
    operator_name   VARCHAR(20)  NOT NULL,
    tenant_id       BIGINT,
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);
COMMENT ON TABLE org_position_history IS '岗位变更历史表';
COMMENT ON COLUMN org_position_history.id IS '主键';
COMMENT ON COLUMN org_position_history.user_id IS '用户ID';
COMMENT ON COLUMN org_position_history.change_type IS '变更类型：1入职，2晋升，3降职，4转岗，5兼职，6卸任，7离职';
COMMENT ON COLUMN org_position_history.old_position_id IS '原岗位ID';
COMMENT ON COLUMN org_position_history.new_position_id IS '新岗位ID';
COMMENT ON COLUMN org_position_history.old_level_id IS '原职级ID';
COMMENT ON COLUMN org_position_history.new_level_id IS '新职级ID';
COMMENT ON COLUMN org_position_history.old_dept_id IS '原部门ID';
COMMENT ON COLUMN org_position_history.new_dept_id IS '新部门ID';
COMMENT ON COLUMN org_position_history.old_salary IS '原薪资';
COMMENT ON COLUMN org_position_history.new_salary IS '新薪资';
COMMENT ON COLUMN org_position_history.change_reason IS '变更原因';
COMMENT ON COLUMN org_position_history.change_date IS '变更生效日期';
COMMENT ON COLUMN org_position_history.approval_no IS '审批单号';
COMMENT ON COLUMN org_position_history.attachments IS '附件（JSON数组，存储文件URL）';
COMMENT ON COLUMN org_position_history.remark IS '备注';
COMMENT ON COLUMN org_position_history.operator_id IS '操作人ID';
COMMENT ON COLUMN org_position_history.operator_name IS '操作人姓名（冗余）';
COMMENT ON COLUMN org_position_history.tenant_id IS '租户ID';
COMMENT ON COLUMN org_position_history.create_time IS '创建时间';
CREATE INDEX idx_org_pos_hist_user ON org_position_history (user_id, change_date);
CREATE INDEX idx_org_pos_hist_type ON org_position_history (change_type, create_time);
CREATE INDEX idx_org_pos_hist_approval ON org_position_history (approval_no);
