-- ============================================================================
-- 开发工具平台（carlos-devkit）菜单与权限点种子脚本（幂等）
-- ----------------------------------------------------------------------------
-- 来源：《DESIGN-20261006-开发工具平台功能设计》第 6.1 节 / 第 4.4 节
--
-- 约定：
--   * 菜单 component 三层规则：顶层目录 = layout.base；叶子 = view.{route_name}；
--     本模块无三级菜单，故不涉及中间层 NULL；
--   * menu_category：1=目录 2=菜单；menu_level：1=一级 2=二级；
--   * 打开页面所需的 sys_menu 行必须与前端 src/views/devkit/ 下的目录结构一致
--     （elegant-router 按 views 目录生成 route_name / 路由组件映射）；
--   * sys_menu.meta.roles 是死字段（动态路由下不生效），权限一律靠 sys_operation.perm_code；
--   * 超管（UserContext.isSuperAdmin）不走权限过滤，脚本执行后即见；
--     非超管需在「角色管理」中授权对应权限点。
--
-- 幂等：按 route_name / perm_code 判重，可重复执行。
-- id 使用 2110xxxxxxxxxxxxxxx 固定号段（2110 = 开发工具平台），避免与雪花 ID 冲突。
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. 一级目录：开发工具
-- ----------------------------------------------------------------------------
INSERT INTO sys_menu (id, parent_id, menu_name, route_path, route_name, icon, icon_type, meta, component,
                      menu_status, menu_level, order_num, hide_in_menu, menu_category, client_type,
                      i18n_key, keep_alive, constant, multi_tab, is_deleted, create_time, update_time)
SELECT 2110000000000000001,
       0,
       '开发工具',
       '/devkit',
       'devkit',
       'carbon:tools',
       1,
       '{"title":"devkit","i18nKey":"route.devkit","icon":"carbon:tools","order":21}',
       'layout.base',
       1,
       1,
       21,
       false,
       1,
       'ALL',
       'route.devkit',
       false,
       false,
       false,
       0,
       now(),
       now()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE route_name = 'devkit' AND is_deleted = 0);

-- ----------------------------------------------------------------------------
-- 2. 二级菜单：数据源管理 / 代码生成器 / 工程生成器 / 模板管理 / 模板编辑器 / 生成记录
-- ----------------------------------------------------------------------------
INSERT INTO sys_menu (id, parent_id, menu_name, route_path, route_name, icon, icon_type, meta, component,
                      menu_status, menu_level, order_num, hide_in_menu, menu_category, client_type,
                      i18n_key, keep_alive, constant, multi_tab, is_deleted, create_time, update_time)
SELECT v.id,
       p.id,
       v.menu_name,
       v.route_path,
       v.route_name,
       v.icon,
       1,
       v.meta,
       v.component,
       1,
       2,
       v.order_num,
       v.hide_in_menu,
       2,
       'ALL',
       v.i18n_key,
       v.keep_alive,
       false,
       false,
       0,
       now(),
       now()
FROM (VALUES
          (2110000000000000101::bigint, '数据源管理', '/devkit/datasource', 'devkit_datasource',
           'carbon:data-base', 'view.devkit_datasource',
           '{"title":"devkit_datasource","i18nKey":"route.devkit_datasource","icon":"carbon:data-base","order":1,"keepAlive":true}',
           'route.devkit_datasource', true, false, 1),
          (2110000000000000102::bigint, '代码生成器', '/devkit/codegen', 'devkit_codegen',
           'carbon:code', 'view.devkit_codegen',
           '{"title":"devkit_codegen","i18nKey":"route.devkit_codegen","icon":"carbon:code","order":2,"keepAlive":true}',
           'route.devkit_codegen', true, false, 2),
          (2110000000000000103::bigint, '工程生成器', '/devkit/project', 'devkit_project',
           'carbon:application', 'view.devkit_project',
           '{"title":"devkit_project","i18nKey":"route.devkit_project","icon":"carbon:application","order":3,"keepAlive":true}',
           'route.devkit_project', true, false, 3),
          (2110000000000000104::bigint, '模板管理', '/devkit/template', 'devkit_template',
           'carbon:template', 'view.devkit_template',
           '{"title":"devkit_template","i18nKey":"route.devkit_template","icon":"carbon:template","order":4,"keepAlive":true}',
           'route.devkit_template', true, false, 4),
          (2110000000000000105::bigint, '模板编辑器', '/devkit/template-editor', 'devkit_template-editor',
           'carbon:edit', 'view.devkit_template-editor',
           '{"title":"devkit_template-editor","i18nKey":"route.devkit_template-editor","icon":"carbon:edit","order":5,"hideInMenu":true,"activeMenu":"devkit_template"}',
           'route.devkit_template-editor', true, true, 5),
          (2110000000000000106::bigint, '生成记录', '/devkit/record', 'devkit_record',
           'carbon:document', 'view.devkit_record',
           '{"title":"devkit_record","i18nKey":"route.devkit_record","icon":"carbon:document","order":6,"keepAlive":true}',
           'route.devkit_record', true, false, 6))
         AS v(id, menu_name, route_path, route_name, icon, component, meta, i18n_key, keep_alive,
              hide_in_menu, order_num)
         CROSS JOIN (SELECT id FROM sys_menu WHERE route_name = 'devkit' AND is_deleted = 0 LIMIT 1) p
WHERE NOT EXISTS (SELECT 1 FROM sys_menu m WHERE m.route_name = v.route_name AND m.is_deleted = 0);

-- ----------------------------------------------------------------------------
-- 3. 权限点（sys_operation，op_type=1 按钮/接口类，挂到对应菜单）
--    perm_code 形态 {menu}:{action}，与后端 @PreAuthorize("hasPermission('devkit:xxx','yyy')") 对应
-- ----------------------------------------------------------------------------
INSERT INTO sys_operation (id, op_type, menu_id, op_name, perm_code, order_num, auto_scan, op_status,
                           is_deleted, create_time)
SELECT v.id,
       1,
       m.id,
       v.op_name,
       v.perm_code,
       v.order_num,
       false,
       1,
       0,
       now()
FROM (VALUES
          -- 数据源管理
          (2110000000000001001::bigint, 'devkit_datasource', '数据源-查看', 'devkit:datasource:view', 1),
          (2110000000000001002::bigint, 'devkit_datasource', '数据源-新增', 'devkit:datasource:add', 2),
          (2110000000000001003::bigint, 'devkit_datasource', '数据源-编辑', 'devkit:datasource:edit', 3),
          (2110000000000001004::bigint, 'devkit_datasource', '数据源-删除（高危）', 'devkit:datasource:delete', 4),
          (2110000000000001005::bigint, 'devkit_datasource', '数据源-测试连接', 'devkit:datasource:test', 5),
          -- 代码生成器
          (2110000000000001101::bigint, 'devkit_codegen', '代码生成-查看', 'devkit:codegen:view', 1),
          (2110000000000001102::bigint, 'devkit_codegen', '代码生成-预览', 'devkit:codegen:preview', 2),
          (2110000000000001103::bigint, 'devkit_codegen', '代码生成-生成下载', 'devkit:codegen:generate', 3),
          -- 工程生成器
          (2110000000000001201::bigint, 'devkit_project', '工程生成-查看', 'devkit:project:view', 1),
          (2110000000000001202::bigint, 'devkit_project', '工程生成-预览', 'devkit:project:preview', 2),
          (2110000000000001203::bigint, 'devkit_project', '工程生成-生成下载', 'devkit:project:generate', 3),
          -- 模板管理 / 编辑器
          (2110000000000001301::bigint, 'devkit_template', '模板-查看', 'devkit:template:view', 1),
          (2110000000000001302::bigint, 'devkit_template', '模板组-新增', 'devkit:template:add', 2),
          (2110000000000001303::bigint, 'devkit_template', '模板-编辑（含在线编辑正文）', 'devkit:template:edit', 3),
          (2110000000000001304::bigint, 'devkit_template', '模板-删除（高危）', 'devkit:template:delete', 4),
          (2110000000000001305::bigint, 'devkit_template', '模板-版本回滚（高危）', 'devkit:template:rollback', 5),
          -- 生成记录
          (2110000000000001401::bigint, 'devkit_record', '生成记录-查看', 'devkit:record:view', 1),
          (2110000000000001402::bigint, 'devkit_record', '生成记录-删除', 'devkit:record:delete', 2))
         AS v(id, route_name, op_name, perm_code, order_num)
         JOIN sys_menu m ON m.route_name = v.route_name AND m.is_deleted = 0
WHERE NOT EXISTS (SELECT 1 FROM sys_operation o WHERE o.perm_code = v.perm_code AND o.is_deleted = 0);
