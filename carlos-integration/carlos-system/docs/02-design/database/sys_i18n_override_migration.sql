-- ============================================
-- sys_i18n_message：从「全量镜像」迁移到「覆盖模型」
-- ============================================
--
-- 背景
-- ----
-- 旧模型：前端语言文件是源，后端表是它的**全量镜像**（用生成器导出）。
--         结果是要维护一条同步管道，而且一旦漏跑就静默不一致
--         （曾发生过：253 个 key 几个月没同步到后端）。
--
-- 新模型：前端语言文件仍然是**完整的 key 目录 + 基线值**（类型安全、离线兜底），
--         后端表**只存被运营改过的那部分 key 的覆盖值**。
--         运行时由 mergeLocaleMessage 合并：拿到的值 = 覆盖值 ?? 基线值。
--
--         于是「同步」这件事本身消失了 —— 没改过就没有行。
--
-- 影响面
-- ------
-- 后端 Java 代码无需改动：
--   * SysI18nCreateParam.moduleId 本来就可选
--   * existsByLangModuleKey 的语义是 (lang, msgKey, module_id 是否为 null)，
--     覆盖行的 module_id 恒为 NULL，恰好就是「同语言同 key 只能有一条」
--
-- 本脚本做两件事
-- --------------
--   1. 把唯一索引从 (lang, COALESCE(module_id,0), msg_key) 改为 (lang, msg_key)
--   2. 清空历史的全量镜像行
--
-- 为什么第 2 步必须做（重要）
-- --------------------------
-- 那批历史行在新模型下会被当成「覆盖值」。而它们的值取自当时的前端文件，
-- 之后前端改过的文案会被这些陈旧的覆盖值**遮蔽**，表现为「改了前端但页面不变」。
-- 清空后一切都回落到前端基线，运营再从零开始按需添加覆盖。
--
-- 清空不会丢任何东西：完整的文案仍在前端的 locales/langs/*.ts 里（且在 git 里）。
--
-- 执行前建议先备份：
--   CREATE TABLE sys_i18n_message_bak_20260917 AS SELECT * FROM sys_i18n_message;
-- ============================================


-- ---------- 0. 备份（可选但建议） ----------
-- CREATE TABLE sys_i18n_message_bak_20260917 AS SELECT * FROM sys_i18n_message;


-- ---------- 1. 防呆：确认没有同 (lang, msg_key) 的重复行 ----------
-- 若有重复，下面的建索引会失败。正常数据下应为 0 行。
-- 之所以不会有重复：msg_key 本身带命名空间前缀（system. / page. 等），
-- 每个 key 只属于一个模块，而旧索引已保证 (lang, module, key) 唯一。
SELECT lang, msg_key, COUNT(*) AS cnt
FROM sys_i18n_message
GROUP BY lang, msg_key
HAVING COUNT(*) > 1;


-- ---------- 2. 换唯一索引 ----------
DROP INDEX IF EXISTS uk_i18n_lang_module_key;

CREATE UNIQUE INDEX uk_i18n_lang_msg_key ON sys_i18n_message (lang, msg_key);

COMMENT ON INDEX uk_i18n_lang_msg_key IS '同语言同消息键唯一（覆盖模型：module_id 不参与唯一性）';

-- 说明：这里不需要 WHERE is_deleted = 0。
-- SysI18nMessage 实体未标注 @TableLogic（也不映射 is_deleted 字段），
-- 删除走的是物理删除，不会留下占位的软删行。
-- 对比 sys_operation：那个实体带 @TableLogic，它的唯一索引必须用部分索引，
-- 否则「删了再建同 perm_code」必然撞唯一约束（该问题目前仍存在，未修）。


-- ---------- 3. 清空历史全量镜像行 ----------
-- 这一步之后，前端文案全部回落到本地基线；
-- 运营在「国际化管理」页面里看到的「当前值」即基线值，改哪条就产生哪条的覆盖行。
DELETE FROM sys_i18n_message;


-- ---------- 4. 校验 ----------
-- 期望：0 行
SELECT COUNT(*) AS remaining FROM sys_i18n_message;
