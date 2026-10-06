-- =============================================================================
-- sys_sequence_rule 种子：字典编码规则（dict_code）
--
-- 背景：
--   SysDictService#initWithTxt 原先用 `int dictCode = 25` 在内存里递增拼 "025"、"026" …，
--   起点与库中存量毫无关联（换一套环境、或漏改一次就是重号）。改造后该处改走序列号生成器，
--   本脚本负责把规则本身建出来 —— **规则不存在时 nextCode 会抛
--   SYS_SEQUENCE_RULE_NOT_FOUND、txt 导入整体失败**，故本脚本须先于该功能上线执行。
--
-- 规则口径：
--   prefix         ''     无前缀
--   date_pattern   ''     无日期段（字典编码是全局流水，不随日期重置）
--   separator      ''     无分隔符
--   seq_length     3      左补零到 3 位，即 025 / 026 / …
--   step           1
--   reset_cycle    1      永不重置（无日期段时只允许取 1，否则跨周期归零必然重号）
--   current_value  24     **水位播种**：Redis 计数键播种时会抬到该值，
--                         因而第一次 nextCode 得到 25 → "025"，与改造前的起点一致。
--   current_period ''     与「无日期段」对应
--
-- ⚠️ 水位怎么调（两件事都容易搞反，务必读完）：
--   1. **水位必须 ≥ 本环境 sys_dict 现存的最大 dict_code**。改小（含「想从 001 重新开始」
--      而把 24 改成 0）会让生成器重新发出已存在的编码，addDict 的 count 检查会以
--      SYS_DICT_ALREADY_EXISTS 拒绝，**txt 导入在第一个字典就中断**。这不是跳号，是撞号。
--   2. **"把下面的 24 改大" 对本脚本无效**：脚本末尾带 `WHERE NOT EXISTS` 守卫，
--      规则行一旦存在，整条 INSERT 就是 no-op，改字面量不产生任何效果。
--      要抬水位请直接改库（Redis 计数键已存在时 Lua 播种取 max(现值, 水位)，故不会回退）：
--          UPDATE sys_sequence_rule SET current_value = <新值> WHERE rule_code = 'dict_code';
--      改完如果 Redis 键还在（进程没重启、键没过期），生效的是 Redis 里更大的那个计数；
--      想让它立刻从新水位起步，需同时删掉 Redis 键 system:sequence:dict_code 再重启。
--
-- ⚠️ 与改造前的差异（不是完全等价，是有意收紧）：
--   seq_length=3 把「1000 即溢出」钉死了。改造前 padPre(dictCode, 3) 到 1000 会**静默**变成
--   4 位继续发；改造后由生成器抛 SYS_SEQUENCE_OVERFLOW。定长才不破坏下游按 3 位解析的假设，
--   所以这是修好的行为，但确实不是「行为等价」—— 字典数量接近 999 的环境请先把 seq_length 调大。
--
-- 说明：
--   1. current_value 只是**数据库兜底水位**，计数真相源是 Redis 键 system:sequence:dict_code。
--   2. 脚本幂等：守卫同时判 id 与 rule_code，重复执行不报错、不覆盖。
--      注意「建过 → 在页面上删掉（逻辑删除）→ 重跑本脚本」不会重新插入（id 已占用），
--      此时 nextCode 仍会报规则不存在，需手工恢复：
--          UPDATE sys_sequence_rule SET is_deleted = 0 WHERE id = 2100619971863248910;
--      （若期间又手工建过同 rule_code 的规则，先删掉那条，否则会撞 uk_sys_sequence_rule_code。）
--   3. id 为固定雪花字面量（本表不使用 BIGSERIAL，id 由应用层 IdType.ASSIGN_ID 分配），
--      效果等价于在「系统管理 → 编码规则」页面上手工建一条同样的规则。
-- =============================================================================

INSERT INTO sys_sequence_rule (id, rule_code, rule_name, prefix, date_pattern, separator, seq_length, step,
                               reset_cycle, current_value, current_period, tenant_id,
                               create_by, create_time, update_by, update_time, is_deleted)
SELECT 2100619971863248910, 'dict_code', '字典编码', '', '', '', 3, 1, 1, 24, '', 0,
       NULL, now(), NULL, now(), 0
WHERE NOT EXISTS (SELECT 1
                  FROM sys_sequence_rule
                  WHERE id = 2100619971863248910
                     OR (rule_code = 'dict_code' AND is_deleted = 0));
