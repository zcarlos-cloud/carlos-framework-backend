-- ============================================================================
-- 系统监控模块 字典种子脚本（sys_dict / sys_dict_item，幂等）
-- ----------------------------------------------------------------------------
-- 来源：《DESIGN-20260930-系统监控模块库表设计》4.1 节
-- 内容：7 个字典及其全部字典项
--   monitor_instance_type  监控实例类型（7 项）
--   monitor_env            环境（3 项）
--   monitor_alarm_level    告警级别（3 项）
--   monitor_alarm_status   告警状态（3 项）
--   monitor_notify_channel 告警通知渠道（3 项）
--   monitor_report_type    分析报告类型（2 项）
--   monitor_mask_type      脱敏类型（5 项）
--
-- 约定：type=2（字符串类型，对齐 DictTypeEnum.STRING）；sys_dict 无 is_deleted 列，
--       按 dict_code 判重；sys_dict_item 按 (dict_id, item_code) 判重；可重复执行。
--       id 使用 1913xxxx 固定大数号段（13=监控模块编码），避免与雪花 ID 及其他种子冲突。
-- ============================================================================

-- ----------------------------
-- 1. 字典主表
-- ----------------------------
INSERT INTO sys_dict (id, dict_name, dict_code, description, type, create_time)
SELECT v.id, v.dict_name, v.dict_code, v.description, 2, now()
FROM (VALUES (191300000000000001::bigint, '监控实例类型', 'monitor_instance_type', '监控实例类型：Redis 三形态/关系库/ClickHouse'),
             (191300000000000002::bigint, '监控环境', 'monitor_env', '监控实例所属环境：dev/test/prod'),
             (191300000000000003::bigint, '监控告警级别', 'monitor_alarm_level', '告警级别：INFO/WARN/CRITICAL'),
             (191300000000000004::bigint, '监控告警状态', 'monitor_alarm_status', '告警状态：FIRING/CONFIRMED/RECOVERED'),
             (191300000000000005::bigint, '监控告警通知渠道', 'monitor_notify_channel', '告警通知渠道：SITE/MAIL/DINGTALK（对接 carlos-message）'),
             (191300000000000006::bigint, '监控分析报告类型', 'monitor_report_type', 'Redis 分析报告类型：BIGKEY/HOTKEY'),
             (191300000000000007::bigint, '监控脱敏类型', 'monitor_mask_type', '数据管理脱敏类型：FULL/PHONE/EMAIL/NAME/CUSTOM'))
         AS v(id, dict_name, dict_code, description)
WHERE NOT EXISTS (SELECT 1
                  FROM sys_dict d
                  WHERE d.dict_code = v.dict_code);

-- ----------------------------
-- 2. 字典项：monitor_instance_type 监控实例类型
-- ----------------------------
INSERT INTO sys_dict_item (id, dict_id, item_name, item_code, description, order_num, is_enable, create_time)
SELECT v.id, d.id, v.item_name, v.item_code, v.description, v.order_num, true, now()
FROM (VALUES (191300000000001001::bigint, 'Redis单机', 'REDIS_STANDALONE', 'Redis 单机实例', 1),
             (191300000000001002::bigint, 'Redis集群', 'REDIS_CLUSTER', 'Redis Cluster 集群', 2),
             (191300000000001003::bigint, 'Redis哨兵', 'REDIS_SENTINEL', 'Redis 哨兵模式', 3),
             (191300000000001004::bigint, 'PostgreSQL', 'POSTGRESQL', 'PostgreSQL 数据库', 4),
             (191300000000001005::bigint, 'MySQL', 'MYSQL', 'MySQL 数据库', 5),
             (191300000000001006::bigint, '达梦数据库', 'DM', '达梦数据库（DM8）', 6),
             (191300000000001007::bigint, 'ClickHouse', 'CLICKHOUSE', 'ClickHouse 列式数据库', 7))
         AS v(id, item_name, item_code, description, order_num)
         JOIN sys_dict d ON d.dict_code = 'monitor_instance_type'
WHERE NOT EXISTS (SELECT 1
                  FROM sys_dict_item i
                  WHERE i.dict_id = d.id
                    AND i.item_code = v.item_code);

-- ----------------------------
-- 3. 字典项：monitor_env 环境
-- ----------------------------
INSERT INTO sys_dict_item (id, dict_id, item_name, item_code, description, order_num, is_enable, create_time)
SELECT v.id, d.id, v.item_name, v.item_code, v.description, v.order_num, true, now()
FROM (VALUES (191300000000002001::bigint, '开发环境', 'dev', '开发环境', 1),
             (191300000000002002::bigint, '测试环境', 'test', '测试环境', 2),
             (191300000000002003::bigint, '生产环境', 'prod', '生产环境', 3))
         AS v(id, item_name, item_code, description, order_num)
         JOIN sys_dict d ON d.dict_code = 'monitor_env'
WHERE NOT EXISTS (SELECT 1
                  FROM sys_dict_item i
                  WHERE i.dict_id = d.id
                    AND i.item_code = v.item_code);

-- ----------------------------
-- 4. 字典项：monitor_alarm_level 告警级别
-- ----------------------------
INSERT INTO sys_dict_item (id, dict_id, item_name, item_code, description, order_num, is_enable, create_time)
SELECT v.id, d.id, v.item_name, v.item_code, v.description, v.order_num, true, now()
FROM (VALUES (191300000000003001::bigint, '提示', 'INFO', '提示级告警', 1),
             (191300000000003002::bigint, '警告', 'WARN', '警告级告警', 2),
             (191300000000003003::bigint, '严重', 'CRITICAL', '严重级告警', 3))
         AS v(id, item_name, item_code, description, order_num)
         JOIN sys_dict d ON d.dict_code = 'monitor_alarm_level'
WHERE NOT EXISTS (SELECT 1
                  FROM sys_dict_item i
                  WHERE i.dict_id = d.id
                    AND i.item_code = v.item_code);

-- ----------------------------
-- 5. 字典项：monitor_alarm_status 告警状态
-- ----------------------------
INSERT INTO sys_dict_item (id, dict_id, item_name, item_code, description, order_num, is_enable, create_time)
SELECT v.id, d.id, v.item_name, v.item_code, v.description, v.order_num, true, now()
FROM (VALUES (191300000000004001::bigint, '触发中', 'FIRING', '告警触发中（未确认未恢复）', 1),
             (191300000000004002::bigint, '已确认', 'CONFIRMED', '告警已人工确认', 2),
             (191300000000004003::bigint, '已恢复', 'RECOVERED', '指标恢复正常，告警关闭', 3))
         AS v(id, item_name, item_code, description, order_num)
         JOIN sys_dict d ON d.dict_code = 'monitor_alarm_status'
WHERE NOT EXISTS (SELECT 1
                  FROM sys_dict_item i
                  WHERE i.dict_id = d.id
                    AND i.item_code = v.item_code);

-- ----------------------------
-- 6. 字典项：monitor_notify_channel 告警通知渠道
-- ----------------------------
INSERT INTO sys_dict_item (id, dict_id, item_name, item_code, description, order_num, is_enable, create_time)
SELECT v.id, d.id, v.item_name, v.item_code, v.description, v.order_num, true, now()
FROM (VALUES (191300000000005001::bigint, '站内信', 'SITE', '平台站内信通知', 1),
             (191300000000005002::bigint, '邮件', 'MAIL', '邮件通知', 2),
             (191300000000005003::bigint, '钉钉', 'DINGTALK', '钉钉机器人通知', 3))
         AS v(id, item_name, item_code, description, order_num)
         JOIN sys_dict d ON d.dict_code = 'monitor_notify_channel'
WHERE NOT EXISTS (SELECT 1
                  FROM sys_dict_item i
                  WHERE i.dict_id = d.id
                    AND i.item_code = v.item_code);

-- ----------------------------
-- 7. 字典项：monitor_report_type 分析报告类型
-- ----------------------------
INSERT INTO sys_dict_item (id, dict_id, item_name, item_code, description, order_num, is_enable, create_time)
SELECT v.id, d.id, v.item_name, v.item_code, v.description, v.order_num, true, now()
FROM (VALUES (191300000000006001::bigint, '大Key报告', 'BIGKEY', 'Redis 大 key 分析报告', 1),
             (191300000000006002::bigint, '热Key报告', 'HOTKEY', 'Redis 热 key 分析报告', 2))
         AS v(id, item_name, item_code, description, order_num)
         JOIN sys_dict d ON d.dict_code = 'monitor_report_type'
WHERE NOT EXISTS (SELECT 1
                  FROM sys_dict_item i
                  WHERE i.dict_id = d.id
                    AND i.item_code = v.item_code);

-- ----------------------------
-- 8. 字典项：monitor_mask_type 脱敏类型
-- ----------------------------
INSERT INTO sys_dict_item (id, dict_id, item_name, item_code, description, order_num, is_enable, create_time)
SELECT v.id, d.id, v.item_name, v.item_code, v.description, v.order_num, true, now()
FROM (VALUES (191300000000007001::bigint, '全脱敏', 'FULL', '全部内容替换为 ***', 1),
             (191300000000007002::bigint, '手机号', 'PHONE', '手机号中间四位脱敏', 2),
             (191300000000007003::bigint, '邮箱', 'EMAIL', '邮箱账号部分脱敏', 3),
             (191300000000007004::bigint, '姓名', 'NAME', '姓名保留姓氏脱敏', 4),
             (191300000000007005::bigint, '自定义', 'CUSTOM', '按 mask_pattern 正则模板脱敏', 5))
         AS v(id, item_name, item_code, description, order_num)
         JOIN sys_dict d ON d.dict_code = 'monitor_mask_type'
WHERE NOT EXISTS (SELECT 1
                  FROM sys_dict_item i
                  WHERE i.dict_id = d.id
                    AND i.item_code = v.item_code);
