-- =============================================================================
-- sys_sequence_rule 业务编码/序列号规则（S5 序列号生成器）
-- 归属：carlos-system（系统域，表前缀 sys_）
-- 说明：
--   1. id 为雪花 ID（应用层 IdType.ASSIGN_ID 分配），不使用 BIGSERIAL；
--   2. 逻辑删除（is_deleted）；rule_code 唯一索引带 WHERE is_deleted = 0 的部分条件；
--   3. 计数真相源是 Redis 键 system:sequence:{rule_code}[:{period}]（原子 INCRBY，Lua 保证播种/溢出预检/自增/设过期同批）；
--      current_value / current_period 是「数据库兜底水位」：
--        - Redis 正常时，每次生成后按前向推进/跨日期段条件回写，保持与 Redis 计数同量级；
--        - Redis 不可用时，按 (current_value, current_period) 做 CAS 自增兜底，保证不重号；
--        - 因此该列不是「实时计数展示值」，前台展示会滞后最多一次生成。
--      ⚠️ 计数键目前不含租户维度：carlos-system 暂未接入租户拦截器，tenant_id 恒为 0，
--      多租户启用后必须把 tenant_id 拼进 Redis 键，否则不同租户的同名规则会共用同一个流水。
--   4. 编码结构：prefix + datePattern 渲染结果 + separator + 左补零流水；
--   5. reset_cycle：1-永不重置 2-按日 3-按月 4-按年。**无日期段（date_pattern 为空）时只能取 1**，
--      否则编码内没有周期标识、跨周期流水归零会产生重复编码（应用层已强制校验）；
--      日期段存在时重置周期不得短于日期段粒度（应用层以 20986 拒绝）：编码里只有月份段却要求按日重置，
--      实际重置发生在月切换时，配置与意图不符。注意理由不是「键会中途过期」—— TTL 取
--      max(重置周期, 日期段周期×2) 后键必然覆盖整个日期段，此处纯粹是拒绝自相矛盾的配置。
--   6. tenant_id 仅为数据列（carlos-system 暂无租户拦截器），为后续多租户预留；
--   7. **删除规则即放弃计数水位**：删规则会连带清理其 Redis 计数键，重建同 rule_code 的规则会从 1
--      重新编号，与删除前已发出的编码重复。调整编码格式请用「改配置」而不是删了重加：
--      改日期段后新格式的键与水位天然失效（日期段串长度必然不同），新格式自动从 1 开始。
-- 脚本幂等，可重复执行。
-- =============================================================================

CREATE TABLE IF NOT EXISTS sys_sequence_rule
(
    id             BIGINT       PRIMARY KEY,
    rule_code      VARCHAR(64)  NOT NULL,
    rule_name      VARCHAR(128) NOT NULL,
    prefix         VARCHAR(32),
    date_pattern   VARCHAR(32),
    separator      VARCHAR(8)   NOT NULL DEFAULT '',
    seq_length     INT          NOT NULL DEFAULT 6,
    step           INT          NOT NULL DEFAULT 1,
    reset_cycle    SMALLINT     NOT NULL DEFAULT 2,
    current_value  BIGINT       NOT NULL DEFAULT 0,
    current_period VARCHAR(32)  NOT NULL DEFAULT '',
    tenant_id      BIGINT       NOT NULL DEFAULT 0,
    create_by      BIGINT,
    create_time    TIMESTAMP    NOT NULL DEFAULT now(),
    update_by      BIGINT,
    update_time    TIMESTAMP    NOT NULL DEFAULT now(),
    is_deleted     SMALLINT     NOT NULL DEFAULT 0
);

COMMENT ON TABLE sys_sequence_rule IS '业务编码/序列号规则';
COMMENT ON COLUMN sys_sequence_rule.rule_code IS '规则编码（业务标识，如 order_no，创建后不可修改）';
COMMENT ON COLUMN sys_sequence_rule.rule_name IS '规则名称';
COMMENT ON COLUMN sys_sequence_rule.prefix IS '编码前缀（如 PO）';
COMMENT ON COLUMN sys_sequence_rule.date_pattern IS '日期段格式：yyyy/yyyyMM/yyyyMMdd/yyyyMMddHH/yyyyMMddHHmmss，空表示不含日期段';
COMMENT ON COLUMN sys_sequence_rule.separator IS '日期段与流水之间的分隔符（如 -）';
COMMENT ON COLUMN sys_sequence_rule.seq_length IS '流水位数（左补零）';
COMMENT ON COLUMN sys_sequence_rule.step IS '单次自增步长';
COMMENT ON COLUMN sys_sequence_rule.reset_cycle IS '重置周期：1-永不重置 2-按日 3-按月 4-按年';
COMMENT ON COLUMN sys_sequence_rule.current_value IS '数据库兜底计数水位（真相源在 Redis，仅供兜底与排查）';
COMMENT ON COLUMN sys_sequence_rule.current_period IS '兜底计数所属日期段';
COMMENT ON COLUMN sys_sequence_rule.tenant_id IS '租户ID（预留）';

CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_sequence_rule_code ON sys_sequence_rule (rule_code) WHERE is_deleted = 0;

-- 取值域约束：与前端表单、后端 Bean Validation、领域校验三层同口径。
-- 用 DO 块而非 `ALTER TABLE ... ADD CONSTRAINT`：表在早期版本已建过，重复执行不能报错。
DO
$$
    BEGIN
        IF NOT EXISTS(SELECT 1 FROM pg_constraint
                      WHERE conname = 'ck_sys_sequence_rule_seq_length'
                        AND conrelid = 'sys_sequence_rule'::regclass) THEN
            ALTER TABLE sys_sequence_rule
                ADD CONSTRAINT ck_sys_sequence_rule_seq_length CHECK (seq_length BETWEEN 1 AND 12);
        END IF;
        IF NOT EXISTS(SELECT 1 FROM pg_constraint
                      WHERE conname = 'ck_sys_sequence_rule_step'
                        AND conrelid = 'sys_sequence_rule'::regclass) THEN
            ALTER TABLE sys_sequence_rule
                ADD CONSTRAINT ck_sys_sequence_rule_step CHECK (step BETWEEN 1 AND 10000);
        END IF;
        IF NOT EXISTS(SELECT 1 FROM pg_constraint
                      WHERE conname = 'ck_sys_sequence_rule_reset_cycle'
                        AND conrelid = 'sys_sequence_rule'::regclass) THEN
            ALTER TABLE sys_sequence_rule
                ADD CONSTRAINT ck_sys_sequence_rule_reset_cycle CHECK (reset_cycle BETWEEN 1 AND 4);
        END IF;
        IF NOT EXISTS(SELECT 1 FROM pg_constraint
                      WHERE conname = 'ck_sys_sequence_rule_current_value'
                        AND conrelid = 'sys_sequence_rule'::regclass) THEN
            ALTER TABLE sys_sequence_rule
                ADD CONSTRAINT ck_sys_sequence_rule_current_value CHECK (current_value >= 0);
        END IF;
    END
$$;
