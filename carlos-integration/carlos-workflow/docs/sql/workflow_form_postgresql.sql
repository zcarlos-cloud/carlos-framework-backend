-- Warm-Flow 1.8.9 flow_form 表（PostgreSQL）
--
-- 为什么单独一个文件：官方 7 表脚本（warm-flow-all.sql / 本目录的 workflow_postgresql.sql）
-- **不包含 flow_form**。该表只存在于仓库的 sql/mysql/v1-upgrade/warm-flow_form.sql，
-- 且文件头标注 "-- TODO form 开发中"，**没有 PostgreSQL / Oracle / 达梦 版本**。
-- 由于本项目要用 Warm-Flow 的内置表单设计器，这里手工补齐 PG 版本。
--
-- ⚠️ 与官方 MySQL 脚本的差异（重要）：
--   官方 MySQL 脚本只有 13 列，**缺 create_by / update_by**；
--   但 1.8.9 的实体 org.dromara.warm.flow.orm.entity.FlowForm 映射了这两个字段：
--       private String createBy;
--       private String updateBy;
--   MyBatis-Plus 会把它们映射为 create_by / update_by 列。缺列会导致
--   flow_form 的 INSERT / SELECT 直接失败（column "create_by" does not exist）。
--   **本文件按实体补齐为 15 列**，与官方脚本的 13 列不同，这是刻意的。
--
-- 列集合来源：warm-flow-mybatis-plus-core-1.8.9-sources.jar 的 FlowForm.java（15 个字段，
-- 其中 id 标 @TableId、delFlag 标 @TableLogic(value="0", delval="1")、
-- createTime/updateTime 标 @TableField(fill=...)，其余为普通字段）。

CREATE TABLE flow_form
(
    id           int8         NOT NULL,
    form_code    varchar(40)  NOT NULL,
    form_name    varchar(100) NOT NULL,
    "version"    varchar(20)  NOT NULL,
    is_publish   int2         NOT NULL DEFAULT 0,
    form_type    int2         NULL     DEFAULT 0,
    form_path    varchar(100) NULL,
    form_content text         NULL,
    ext          varchar(400) NULL,
    create_time  timestamp    NULL,
    create_by    varchar(64)  NULL     DEFAULT '':: character varying,
    update_time  timestamp    NULL,
    update_by    varchar(64)  NULL     DEFAULT '':: character varying,
    del_flag     bpchar(1)    NULL     DEFAULT '0':: character varying,
    tenant_id    varchar(40)  NULL,
    CONSTRAINT flow_form_pkey PRIMARY KEY (id)
);

COMMENT ON TABLE flow_form IS '流程表单表';

COMMENT ON COLUMN flow_form.id IS '主键id';
COMMENT ON COLUMN flow_form.form_code IS '表单编码';
COMMENT ON COLUMN flow_form.form_name IS '表单名称';
COMMENT ON COLUMN flow_form."version" IS '表单版本';
COMMENT ON COLUMN flow_form.is_publish IS '是否发布（0未发布 1已发布 9失效）';
COMMENT ON COLUMN flow_form.form_type IS '表单类型（0内置表单 存 form_content 1外挂表单 存form_path）';
COMMENT ON COLUMN flow_form.form_path IS '表单路径';
COMMENT ON COLUMN flow_form.form_content IS '表单内容';
COMMENT ON COLUMN flow_form.ext IS '表单扩展，用户自行使用';
COMMENT ON COLUMN flow_form.create_time IS '创建时间';
COMMENT ON COLUMN flow_form.create_by IS '创建人（官方 MySQL 脚本缺此列，按实体补齐）';
COMMENT ON COLUMN flow_form.update_time IS '更新时间';
COMMENT ON COLUMN flow_form.update_by IS '更新人（官方 MySQL 脚本缺此列，按实体补齐）';
COMMENT ON COLUMN flow_form.del_flag IS '删除标志';
COMMENT ON COLUMN flow_form.tenant_id IS '租户id';
