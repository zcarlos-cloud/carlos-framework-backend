-- ----------------------------
-- Table structure for test
-- ----------------------------
DROP TABLE IF EXISTS test;
CREATE TABLE test
(
    id          varchar(32) NOT NULL COMMENT '主键ID',
    test_1      varchar(32) NOT NULL COMMENT '模板字段1',
    test_2      varchar(32) NOT NULL COMMENT '模板字段2',
    test_3      varchar(32) NOT NULL COMMENT '模板字段3',
    test_4      varchar(32) NOT NULL COMMENT '模板字段4',
    is_deleted  tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除，0：未删除，1：已删除',
    create_by   char(32) DEFAULT NULL COMMENT '创建者编号',
    create_time datetime DEFAULT NULL COMMENT '创建时间',
    update_by   char(32) DEFAULT NULL COMMENT '更新者编号',
    update_time datetime DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8
  COLLATE = utf8_general_ci COMMENT = '模板表名'
  ROW_FORMAT = DYNAMIC;


