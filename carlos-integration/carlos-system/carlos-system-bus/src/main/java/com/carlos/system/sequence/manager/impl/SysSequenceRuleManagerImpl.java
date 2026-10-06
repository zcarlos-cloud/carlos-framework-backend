package com.carlos.system.sequence.manager.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseServiceImpl;
import com.carlos.datasource.pagination.MybatisPage;
import com.carlos.datasource.pagination.PageInfo;
import com.carlos.redis.util.RedisUtil;
import com.carlos.system.api.ApiSequence;
import com.carlos.system.enums.SystemErrorCode;
import com.carlos.system.sequence.convert.SysSequenceRuleConvert;
import com.carlos.system.sequence.mapper.SysSequenceRuleMapper;
import com.carlos.system.sequence.manager.SysSequenceRuleManager;
import com.carlos.system.sequence.pojo.dto.SequenceBatch;
import com.carlos.system.sequence.pojo.dto.SysSequenceRuleDTO;
import com.carlos.system.sequence.pojo.entity.SysSequenceRule;
import com.carlos.system.sequence.pojo.param.SysSequenceRulePageParam;
import com.carlos.system.sequence.pojo.vo.SysSequenceRuleVO;
import com.carlos.system.sequence.support.SequenceCodeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 业务编码/序列号规则 Manager 实现
 * </p>
 *
 * <p>计数策略：</p>
 * <ol>
 *   <li>正常态：Redis 键 {@code system:sequence:{ruleCode}[:{period}]} 原子自增。播种、溢出预检、自增、
 *       首次设过期四件事在**同一个 Lua 脚本**内完成（见 {@link #INCR_SCRIPT}）；</li>
 *   <li>播种：脚本内以「数据库水位（同一日期段时）」为下限，即 {@code max(Redis 现值, 水位)} 再自增。
 *       这是 Redis 丢数据 / 降级后恢复不重号的关键 —— 否则计数器会从 0 重来，与已发出的编码整段重复。
 *       ⚠️ 该保证的前提是**水位回写成功**：回写是尽力而为的（见 {@link #syncCounterBestEffort}），
 *       若回写持续失败期间 Redis 又丢数据，仍可能重发一段（无 outbox 无法彻底消除，属已知取舍）；</li>
 *   <li>生成后把水位（current_value / current_period）**严格前向**地尽力回写数据库：只在「日期段前进」
 *       或「同日期段且水位更小」时写入，绝不回退；</li>
 *   <li>Redis 不可用：降级为数据库 CAS 自增（读写改重试，单行更新），该期间发出的号段同样唯一，
 *       且水位会推进到 Redis 计数之上，Redis 恢复后由第 2 条播种收敛。</li>
 * </ol>
 *
 * <p><b>保证边界</b>：同一规则、同一日期段内不会重号（Redis 正常、Redis 降级、以及两者交替的
 * 场景均成立）；允许跳号（降级窗口、进程重启丢号段、人工删键后由水位播种抬升）。</p>
 *
 * @author carlos
 * @since 3.0.0
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class SysSequenceRuleManagerImpl extends BaseServiceImpl<SysSequenceRuleMapper, SysSequenceRule>
    implements SysSequenceRuleManager {

    /**
     * 数据库兜底自增的最大重试次数（CAS 冲突时重试）
     */
    private static final int MAX_DB_RETRY = 5;

    /**
     * Lua 的溢出哨兵返回值
     *
     * <p>用返回值而非抛 Lua 错误：Redis 侧拒绝自增是原子的，脚本报错虽然也会中断执行，
     * 但异常信息会混进「Redis 不可用」的降级分支（见 {@link #next}），语义上分不开。</p>
     */
    private static final long OVERFLOW_SENTINEL = -1L;

    /**
     * 原子「播种 + 溢出预检 + 自增 + 首次设过期」的 Lua 脚本
     *
     * <p>逐条说明（ARGV 全部走字符串序列化器，见 {@code RedisUtil.lua(String, List, Class, String...)}）：</p>
     * <ul>
     *   <li>ARGV[1] 本次自增量（count × step）、ARGV[2] TTL 秒（≤0 表示不过期）、
     *       ARGV[3] 数据库水位下限、ARGV[4] 本位数允许的最大流水值；</li>
     *   <li>播种：键不存在或小于水位时把基准抬到水位。{@code SET} 会清掉 TTL，
     *       所以过期判断放在自增之后用 {@code TTL < 0} 兜（而不是靠"值等于本次增量"来判定首次）；</li>
     *   <li>溢出预检：{@code base + delta > maxSeq} 直接返回哨兵，**既不自增也不播种** ——
     *       先自增再校验会让越界规则每次调用都把计数往前推，一条配错的规则会被永久打死；
     *       播种也同样要挡在预检之后：溢出分支里 SET 出来的键走不到 EXPIRE，
     *       会留下一个永不过期的孤儿键（键名含日期段，不污染其它周期，但白占内存）；</li>
     *   <li>预检之后的播种用 {@code current ~= base} 判断，命中即 SET（被 SET 过的键在同一脚本内
     *       必然被自增，故不会出现"播种了但没自增"的裸键）。</li>
     *   <li>三步拆开会有"自增成功但过期未设置"的窗口，导致键永久驻留，故必须同批执行。</li>
     * </ul>
     */
    private static final String INCR_SCRIPT = """
        local key = KEYS[1]
        local delta = tonumber(ARGV[1])
        local ttl = tonumber(ARGV[2])
        local watermark = tonumber(ARGV[3])
        local maxSeq = tonumber(ARGV[4])

        local current = tonumber(redis.call('GET', key))
        local base = current
        if base == nil or base < watermark then
            base = watermark
        end
        if base + delta > maxSeq then
            return -1
        end

        if current ~= base then
            redis.call('SET', key, base)
        end
        local value = redis.call('INCRBY', key, delta)
        if ttl > 0 and redis.call('TTL', key) < 0 then
            redis.call('EXPIRE', key, ttl)
        end
        return value
        """;

    @Override
    public boolean add(SysSequenceRuleDTO dto) {
        SysSequenceRule entity = SysSequenceRuleConvert.INSTANCE.toDO(dto);
        boolean success = save(entity);
        if (!success) {
            log.warn("Insert 'SysSequenceRule' data fail, entity:{}", entity);
            return false;
        }
        dto.setId(entity.getId());
        return true;
    }

    @Override
    public boolean delete(Serializable id) {
        SysSequenceRule entity = getBaseMapper().selectById(id);
        if (entity == null) {
            return false;
        }
        boolean success = removeById(id);
        if (success) {
            clearCounters(entity.getRuleCode());
        }
        return success;
    }

    @Override
    public boolean modify(SysSequenceRuleDTO dto) {
        SysSequenceRule entity = SysSequenceRuleConvert.INSTANCE.toDO(dto);
        boolean success = updateById(entity);
        if (!success) {
            log.warn("Update 'SysSequenceRule' data fail, entity:{}", entity);
        }
        return success;
    }

    @Override
    public SysSequenceRuleDTO getDtoById(Long id) {
        SysSequenceRule entity = getBaseMapper().selectById(id);
        return SysSequenceRuleConvert.INSTANCE.toDTO(entity);
    }

    @Override
    public SysSequenceRuleDTO getByCode(String ruleCode) {
        LambdaQueryChainWrapper<SysSequenceRule> wrapper = lambdaQuery()
            .eq(SysSequenceRule::getRuleCode, ruleCode);
        SysSequenceRule entity = wrapper.one();
        return SysSequenceRuleConvert.INSTANCE.toDTO(entity);
    }

    @Override
    public Paging<SysSequenceRuleVO> getPage(SysSequenceRulePageParam param) {
        PageInfo<SysSequenceRule> page = page(pageInfo(param), buildQueryWrapper(param));
        return MybatisPage.convert(page, SysSequenceRuleConvert.INSTANCE::toVO);
    }

    @Override
    public boolean existsByCode(String ruleCode) {
        return lambdaQuery().eq(SysSequenceRule::getRuleCode, ruleCode).exists();
    }

    @Override
    public void clearCounters(String ruleCode) {
        try {
            // 无日期段的规则是精确键，有日期段的是「精确前缀 + :{period}」——两者分开清理。
            // ⚠️ 不能用 `规则前缀 + "*"`：那会连带删掉 ruleCode 为其前缀的**其它规则**的计数键，
            // 对方下次自增从 1 开始、与历史编码整段重号（如删 `order` 命中 `order_no`）。
            RedisUtil.delete(SequenceCodeFormatter.redisKey(ruleCode, ""));
            RedisUtil.deletePattern(SequenceCodeFormatter.redisKeyPattern(ruleCode));
        } catch (RuntimeException e) {
            log.warn("清理序列号计数键失败：ruleCode={}", ruleCode, e);
        }
    }

    @Override
    public SequenceBatch next(SysSequenceRuleDTO rule, int count) {
        if (count < 1 || count > ApiSequence.MAX_BATCH_SIZE) {
            throw SystemErrorCode.SYS_PARAM_SEQUENCE_BATCH_SIZE_INVALID.exception();
        }

        int step = rule.getStep() == null ? 1 : rule.getStep();
        String period = SequenceCodeFormatter.resolvePeriod(rule.getDatePattern(), LocalDateTime.now());
        long delta = (long) count * step;
        String key = SequenceCodeFormatter.redisKey(rule.getRuleCode(), period);
        long ttlSeconds = SequenceCodeFormatter.resolveTtlSeconds(rule.getResetCycle(), rule.getDatePattern());
        long maxSeq = SequenceCodeFormatter.modulus(rule.getSeqLength()) - 1L;
        long watermark = resolveWatermark(rule, period);

        Long end = null;
        try {
            end = RedisUtil.lua(INCR_SCRIPT, List.of(key), Long.class,
                String.valueOf(delta), String.valueOf(ttlSeconds),
                String.valueOf(watermark), String.valueOf(maxSeq));
        } catch (RuntimeException e) {
            // 记异常本身（带堆栈），否则排障时只剩一句 message
            log.warn("Redis 计数不可用，降级为数据库兜底自增：ruleCode={}, key={}", rule.getRuleCode(), key, e);
        }
        if (end == null) {
            return nextFromDb(rule, count, step, period, maxSeq);
        }
        if (end == OVERFLOW_SENTINEL) {
            throw SystemErrorCode.SYS_SEQUENCE_OVERFLOW.exception();
        }

        syncCounterBestEffort(rule, period, end);
        return buildBatch(end, count, step, period);
    }

    /**
     * 构建分页查询条件
     */
    private LambdaQueryWrapper<SysSequenceRule> buildQueryWrapper(SysSequenceRulePageParam param) {
        LambdaQueryWrapper<SysSequenceRule> wrapper = queryWrapper();
        wrapper.like(StrUtil.isNotBlank(param.getRuleCode()), SysSequenceRule::getRuleCode, param.getRuleCode());
        wrapper.like(StrUtil.isNotBlank(param.getRuleName()), SysSequenceRule::getRuleName, param.getRuleName());
        wrapper.orderByDesc(SysSequenceRule::getCreateTime);
        return wrapper;
    }

    /**
     * 组装号段（段首 = 段内第一个**真正被分配**的流水值，见 {@link SequenceCodeFormatter#firstSeq}）
     */
    private SequenceBatch buildBatch(long end, int count, int step, String period) {
        return new SequenceBatch()
            .setStart(SequenceCodeFormatter.firstSeq(end, count, step))
            .setEnd(end)
            .setCount(count)
            .setPeriod(period);
    }

    /**
     * 数据库水位下限：仅当水位属于**同一个日期段**时才可用作播种值
     *
     * <p>跨日期段（含首次生成）必须从 0 开始，否则新周期会接着上个周期的流水继续编号。</p>
     */
    private long resolveWatermark(SysSequenceRuleDTO rule, String period) {
        if (rule.getCurrentValue() == null || !period.equals(rule.getCurrentPeriod())) {
            return 0L;
        }
        return rule.getCurrentValue();
    }

    /**
     * 尽力回写数据库计数水位（失败不影响生成）
     *
     * <p>条件是**严格前向**的：{@code current_period < period}
     * 或 {@code current_period = period 且 current_value < value}。
     * 跨日期段时不能让「迟到的旧周期请求」把水位写小 —— 那会让 Redis 挂掉后的兜底路径
     * 按旧日期段从头发号。</p>
     *
     * <p>同日期段内 {@code current_period} 是定长串，字典序即时间序，故可直接比较。</p>
     */
    private void syncCounterBestEffort(SysSequenceRuleDTO rule, String period, long value) {
        try {
            LambdaUpdateWrapper<SysSequenceRule> wrapper = updateWrapper();
            wrapper.eq(SysSequenceRule::getId, rule.getId())
                .set(SysSequenceRule::getCurrentValue, value)
                .set(SysSequenceRule::getCurrentPeriod, period)
                // 显式 set：wrapper 更新且 entity 为 null 时不会触发 FieldFill.UPDATE
                .set(SysSequenceRule::getUpdateTime, LocalDateTime.now())
                .and(w -> w.lt(SysSequenceRule::getCurrentPeriod, period)
                    .or(inner -> inner.eq(SysSequenceRule::getCurrentPeriod, period)
                        .lt(SysSequenceRule::getCurrentValue, value)));
            getBaseMapper().update(null, wrapper);
        } catch (RuntimeException e) {
            log.warn("回写序列号计数水位失败（不影响编码生成）：ruleCode={}, value={}", rule.getRuleCode(), value, e);
        }
    }

    /**
     * 数据库 CAS 兜底自增（Redis 不可用时）
     *
     * <p>读 → 算 → 以「读到的旧值」为条件更新，冲突则重读重试；耗尽后抛
     * {@code SYS_SEQUENCE_GENERATE_FAILED}，绝不返回可能重复的号段。</p>
     *
     * <p>溢出预检与 Lua 脚本**同口径**，且同样在写库之前：这里若先推进水位再报错，
     * 会把这轮越界区间里那些**合法且从未发出**的号永久烧掉（如 9950 + 100 → 10050 越过
     * 9999，9951~9999 全丢），且水位越界后 Redis 恢复时会以此为播种下限，此后每次生成都溢出，
     * 规则被无故打死 —— Lua 路径下同样的请求只会被整体拒绝、计数原地不动。</p>
     */
    private SequenceBatch nextFromDb(SysSequenceRuleDTO rule, int count, int step, String period, long maxSeq) {
        long delta = (long) count * step;
        for (int i = 0; i < MAX_DB_RETRY; i++) {
            SysSequenceRule current = getBaseMapper().selectById(rule.getId());
            if (current == null) {
                throw SystemErrorCode.SYS_SEQUENCE_RULE_NOT_FOUND.exception();
            }
            long base = period.equals(current.getCurrentPeriod())
                ? (current.getCurrentValue() == null ? 0L : current.getCurrentValue())
                : 0L;
            long nextValue = base + delta;
            if (nextValue > maxSeq) {
                throw SystemErrorCode.SYS_SEQUENCE_OVERFLOW.exception();
            }

            LambdaUpdateWrapper<SysSequenceRule> wrapper = updateWrapper();
            wrapper.eq(SysSequenceRule::getId, rule.getId())
                .set(SysSequenceRule::getCurrentValue, nextValue)
                .set(SysSequenceRule::getCurrentPeriod, period)
                .set(SysSequenceRule::getUpdateTime, LocalDateTime.now());
            // 两列在 DDL 里都是 NOT NULL，isNull 分支只是防御性写法（表结构变更时不至于拼出 `= NULL`）
            if (current.getCurrentValue() == null) {
                wrapper.isNull(SysSequenceRule::getCurrentValue);
            } else {
                wrapper.eq(SysSequenceRule::getCurrentValue, current.getCurrentValue());
            }
            if (current.getCurrentPeriod() == null) {
                wrapper.isNull(SysSequenceRule::getCurrentPeriod);
            } else {
                wrapper.eq(SysSequenceRule::getCurrentPeriod, current.getCurrentPeriod());
            }

            if (getBaseMapper().update(null, wrapper) > 0) {
                return buildBatch(nextValue, count, step, period);
            }
        }
        log.error("数据库兜底自增连续 {} 次 CAS 冲突，放弃生成：ruleCode={}", MAX_DB_RETRY, rule.getRuleCode());
        throw SystemErrorCode.SYS_SEQUENCE_GENERATE_FAILED.exception();
    }

}
