-- ============================================================================
-- 系统监控模块 权限点种子脚本（sys_operation，幂等）
-- ----------------------------------------------------------------------------
-- 来源：《DESIGN-20260930-系统监控模块库表设计》4.2 节（24 个权限点，
--       含 4.3 节菜单结构中的 monitor:dashboard:view）
--
-- 约定：监控菜单由「菜单管理」页手工登记（见库表设计 4.3 节，无菜单种子 SQL），
--       因此本脚本按 op_type=2（API 类权限点）播种，menu_id 置空、auto_scan=false；
--       菜单登记后可在操作管理页将权限点挂载到对应菜单（按钮类 op_type=1）。
--       带「（高危）」的权限点对应需 confirm-token 二次确认的操作。
--
-- 幂等：sys_operation.perm_code 上有 WHERE is_deleted = 0 的部分唯一索引，
--       本脚本按 (perm_code, is_deleted = 0) 判重，已存在则不重复插入，可重复执行。
--       id 使用 1913xxxx 固定大数号段（13=监控模块编码），避免与雪花 ID 及其他种子冲突。
-- ============================================================================

INSERT INTO sys_operation (id, op_type, menu_id, op_name, perm_code,
                           order_num, auto_scan, op_status, is_deleted, create_time)
SELECT v.id        AS id,
       2           AS op_type,
       NULL        AS menu_id,
       v.op_name   AS op_name,
       v.perm_code AS perm_code,
       v.order_num AS order_num,
       false       AS auto_scan,
       1           AS op_status,
       0           AS is_deleted,
       now()       AS create_time
FROM (VALUES
          -- 监控大盘
          (191300000000100001::bigint, '监控大盘-查看', 'monitor:dashboard:view', 1),
          -- 实例管理
          (191300000000100101::bigint, '监控实例-查看', 'monitor:instance:view', 101),
          (191300000000100102::bigint, '监控实例-新增', 'monitor:instance:add', 102),
          (191300000000100103::bigint, '监控实例-编辑', 'monitor:instance:edit', 103),
          (191300000000100104::bigint, '监控实例-删除', 'monitor:instance:delete', 104),
          (191300000000100105::bigint, '监控实例-测试连接', 'monitor:instance:test', 105),
          -- Redis 监控与数据管理
          (191300000000100201::bigint, 'Redis监控-查看', 'monitor:redis:view', 201),
          (191300000000100202::bigint, 'Redis数据-查看', 'monitor:redis:data:view', 202),
          (191300000000100203::bigint, 'Redis数据-编辑', 'monitor:redis:data:edit', 203),
          (191300000000100204::bigint, 'Redis数据-删除（高危）', 'monitor:redis:data:delete', 204),
          (191300000000100205::bigint, 'Redis数据-批量删除（高危）', 'monitor:redis:data:batch-delete', 205),
          (191300000000100206::bigint, 'Redis分析-大key/热key扫描', 'monitor:redis:analysis', 206),
          -- 数据库监控与数据管理
          (191300000000100301::bigint, '数据库监控-查看', 'monitor:db:view', 301),
          (191300000000100302::bigint, '数据库数据-查看', 'monitor:db:data:view', 302),
          (191300000000100303::bigint, 'SQL控制台-只读执行', 'monitor:db:sql:readonly', 303),
          (191300000000100304::bigint, 'SQL控制台-写执行（高危）', 'monitor:db:sql:write', 304),
          (191300000000100305::bigint, '数据库-终止会话（高危）', 'monitor:db:session:kill', 305),
          (191300000000100306::bigint, '数据库-容量分析', 'monitor:db:capacity', 306),
          -- 微服务监控
          (191300000000100401::bigint, '服务监控-查看', 'monitor:service:view', 401),
          (191300000000100402::bigint, '服务监控-日志级别调整（高危）', 'monitor:service:logger', 402),
          -- 告警中心
          (191300000000100501::bigint, '告警-查看', 'monitor:alarm:view', 501),
          (191300000000100502::bigint, '告警规则-维护', 'monitor:alarm:rule', 502),
          (191300000000100503::bigint, '告警-确认处理', 'monitor:alarm:confirm', 503),
          (191300000000100504::bigint, '告警-静默设置', 'monitor:alarm:silence', 504))
         AS v(id, op_name, perm_code, order_num)
WHERE NOT EXISTS (SELECT 1
                  FROM sys_operation o
                  WHERE o.is_deleted = 0
                    AND o.perm_code = v.perm_code);
