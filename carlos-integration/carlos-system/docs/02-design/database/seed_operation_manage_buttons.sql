-- ============================================================================
-- 「操作管理」菜单按钮操作播种脚本（幂等）
-- ----------------------------------------------------------------------------
-- 背景：「操作管理」菜单（sys_menu.route_name = 'manage_operation'）的
--       新增 / 编辑 / 删除按钮需要按钮级权限点（op_type=1），前端通过
--       hasAuth('manage:operation:*') 控制按钮显隐。
--
-- 约定：perm_code 为「模块:功能:操作」三段式，menu_id 取自 sys_menu 中
--       route_name='manage_operation' 的记录，auto_scan=false（手工定义，
--       非扫描生成）。
--
-- 幂等：sys_operation.perm_code 上有 WHERE is_deleted = 0 的部分唯一索引，
--       本脚本按 (perm_code, is_deleted = 0) 判重，已存在则不重复插入，
--       可重复执行。
-- ============================================================================

INSERT INTO sys_operation (id, op_type, menu_id, op_name, perm_code,
                           order_num, auto_scan, op_status, is_deleted, create_time)
SELECT v.id        AS id,
       1           AS op_type,
       m.id        AS menu_id,
       v.op_name   AS op_name,
       v.perm_code AS perm_code,
       v.order_num AS order_num,
       false       AS auto_scan,
       1           AS op_status,
       0           AS is_deleted,
       now()       AS create_time
FROM (VALUES (2400000000000000101::bigint, '新增操作', 'manage:operation:create', 1),
             (2400000000000000102::bigint, '编辑操作', 'manage:operation:update', 2),
             (2400000000000000103::bigint, '删除操作', 'manage:operation:delete', 3))
         AS v(id, op_name, perm_code, order_num)
         JOIN sys_menu m ON m.route_name = 'manage_operation' AND m.is_deleted = 0
WHERE NOT EXISTS (SELECT 1
                  FROM sys_operation o
                  WHERE o.is_deleted = 0
                    AND o.perm_code = v.perm_code);
