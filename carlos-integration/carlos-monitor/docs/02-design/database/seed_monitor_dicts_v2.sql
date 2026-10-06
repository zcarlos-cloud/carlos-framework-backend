-- ============================================================================
-- 系统监控模块 字典种子补充脚本 V2（幂等）
-- ----------------------------------------------------------------------------
-- 背景：N7 微服务监控落地，InstanceTypeEnum 增加 MICROSERVICE（微服务实例由
--       ServiceRegistrySyncTask 从 Nacos 自动登记）。
-- 幂等：按 dict_code / (dict_id, item_code) 判重，可重复执行。
-- ============================================================================

INSERT INTO sys_dict_item (id, dict_id, item_name, item_code, description, order_num, is_enable, create_time)
SELECT v.id, d.id, v.item_name, v.item_code, v.description, v.order_num, true, now()
FROM (VALUES (191300000000001008::bigint, '微服务', 'MICROSERVICE', 'Nacos 自动登记的微服务实例', 8))
         AS v(id, item_name, item_code, description, order_num)
         JOIN sys_dict d ON d.dict_code = 'monitor_instance_type'
WHERE NOT EXISTS (SELECT 1
                  FROM sys_dict_item i
                  WHERE i.dict_id = d.id
                    AND i.item_code = v.item_code);
