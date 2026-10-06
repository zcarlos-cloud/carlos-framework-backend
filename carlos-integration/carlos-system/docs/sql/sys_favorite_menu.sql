-- =============================================================================
-- 收藏管理菜单（系统管理 → 收藏管理）
-- 归属：carlos-system（系统域）
-- 说明：
--   1. sys_menu 一行：route_name=manage_favorite，组件 view.manage_favorite
--      （前端 src/views/manage/favorite/index.vue，elegant-router 按 views 目录
--      自动生成路由与组件映射，菜单 component 写死 view.<route_name> 即可）；
--   2. sys_operation 一行「查看」权限点 manage:favorite:view（op_type=1），
--      供非超管角色在角色管理中授权——菜单可见性由「角色被授权操作所属菜单」派生；
--      超管（UserContext.isSuperAdmin）不走权限过滤，脚本执行后即见；
--   3. 脚本幂等，可重复执行。
-- =============================================================================

INSERT INTO sys_menu (id, parent_id, menu_name, route_path, route_name, icon, icon_type, meta, component,
                      menu_status, menu_level, order_num, hide_in_menu, menu_category, client_type,
                      i18n_key, keep_alive, constant, multi_tab, is_deleted, create_time, update_time)
SELECT 2102720000000000001,
       2075214528458522653,
       '收藏管理',
       '/manage/favorite',
       'manage_favorite',
       'carbon:star',
       1,
       '{"title":"manage_favorite","i18nKey":"route.manage_favorite","icon":"carbon:star","order":20,"roles":["R_ADMIN"],"keepAlive":true}',
       'view.manage_favorite',
       1,
       2,
       20,
       false,
       2,
       'ALL',
       'route.manage_favorite',
       true,
       false,
       false,
       0,
       now(),
       now()
WHERE NOT EXISTS (SELECT 1
                  FROM sys_menu
                  WHERE route_name = 'manage_favorite'
                    AND is_deleted = 0);

INSERT INTO sys_operation (id, op_type, menu_id, op_name, perm_code, order_num, auto_scan, op_status,
                           is_deleted, create_time)
SELECT 2300000000000090001,
       1,
       m.id,
       '收藏管理查看',
       'manage:favorite:view',
       0,
       false,
       1,
       0,
       now()
FROM sys_menu m
WHERE m.route_name = 'manage_favorite'
  AND m.is_deleted = 0
  AND NOT EXISTS (SELECT 1
                  FROM sys_operation o
                  WHERE o.menu_id = m.id
                    AND o.perm_code = 'manage:favorite:view'
                    AND o.is_deleted = 0);
