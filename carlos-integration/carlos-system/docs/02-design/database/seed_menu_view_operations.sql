-- ============================================================================
-- 菜单「查看」权限点播种脚本（幂等）
-- ----------------------------------------------------------------------------
-- 背景：菜单可见性由「角色被授权的操作所属菜单」派生（见 carlos-system
--       RouteService.getUserRoutes 的过滤逻辑）。因此每个需要按角色控制可见性的
--       菜单，至少要有一条属于自己的操作（sys_operation）。
--
-- 约定：每个启用菜单生成一条 op_type=1 的「查看」操作，
--       perm_code = <sys_menu.route_name 的 '_' 换成 ':'>:view，
--       即「模块:功能:操作」三段式（如 route_name='org_user' -> 'org:user:view'），
--       menu_id 指向该菜单。
--
-- 说明：API 类权限点（op_type=2）由 /sys/operation/scan 扫描 @PreAuthorize 的权限码生成
--       （见 ResourceScanService#resolvePermCode）；未声明 @PreAuthorize 的端点才按路径兜底。
--       本脚本只补「菜单查看」这一类，二者互补。
--       脚本可重复执行：已存在的 (menu_id, perm_code) 不会重复插入。
-- ============================================================================

INSERT INTO sys_operation (id, op_type, menu_id, op_name, perm_code,
                           order_num, auto_scan, op_status, is_deleted, create_time)
SELECT 2300000000000000000 + row_number() OVER (ORDER BY m.id)::bigint AS id,
       1                                                               AS op_type,
       m.id                                                            AS menu_id,
       m.menu_name || '查看'                                            AS op_name,
       replace(m.route_name, '_', ':') || ':view'                       AS perm_code,
       0                                                               AS order_num,
       false                                                           AS auto_scan,
       1                                                               AS op_status,
       0                                                               AS is_deleted,
       now()                                                           AS create_time
FROM sys_menu m
WHERE m.is_deleted = 0
  AND m.menu_status = 1
  AND m.route_name IS NOT NULL
  AND m.route_name <> ''
  AND NOT EXISTS (SELECT 1
                  FROM sys_operation o
                  WHERE o.is_deleted = 0
                    AND o.menu_id = m.id
                    AND o.perm_code = replace(m.route_name, '_', ':') || ':view');
