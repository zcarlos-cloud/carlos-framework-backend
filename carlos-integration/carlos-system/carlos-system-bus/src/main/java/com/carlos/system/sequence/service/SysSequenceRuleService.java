package com.carlos.system.sequence.service;

import cn.hutool.core.util.StrUtil;
import com.carlos.system.api.ApiSequence;
import com.carlos.system.enums.SystemErrorCode;
import com.carlos.system.pojo.ao.SequenceBatchAO;
import com.carlos.system.sequence.manager.SysSequenceRuleManager;
import com.carlos.system.sequence.pojo.dto.SequenceBatch;
import com.carlos.system.sequence.pojo.dto.SysSequenceRuleDTO;
import com.carlos.system.sequence.pojo.enums.SequenceDatePatternEnum;
import com.carlos.system.sequence.pojo.enums.SequenceResetCycleEnum;
import com.carlos.system.sequence.support.SequenceCodeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * <p>
 * 业务编码/序列号规则 业务服务
 * </p>
 *
 * <p>规则维护（校验、唯一性、编排）在这里；计数自增属于数据层原子操作，在 Manager 层。</p>
 *
 * <p><b>更新语义</b>：字符串字段 {@code null} = 沿用原值，空串 = 清空（与 MyBatis-Plus 的
 * NOT_NULL 更新策略一致，见 {@code SysSequenceRuleUpdateParam}）。校验一律在「原值 + 本次改动」
 * 合并出的**完整视图**上进行 —— 只校验半个对象会让任何局部更新（如只改名称）都被跨字段规则误拒。</p>
 *
 * @author carlos
 * @since 3.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysSequenceRuleService {

    private final SysSequenceRuleManager sequenceRuleManager;

    /**
     * 新增规则
     *
     * @param dto 规则数据
     */
    public void addRule(SysSequenceRuleDTO dto) {
        validateRule(dto);
        if (sequenceRuleManager.existsByCode(dto.getRuleCode())) {
            throw SystemErrorCode.SYS_SEQUENCE_RULE_CODE_EXISTS.exception();
        }
        if (!sequenceRuleManager.add(dto)) {
            throw SystemErrorCode.SYS_SEQUENCE_SAVE_FAILED.exception();
        }
        log.info("新增编码规则成功：ruleCode={}", dto.getRuleCode());
    }

    /**
     * 更新规则（规则编码不可修改）
     *
     * <p><b>改日期段时不做任何「重置计数」的动作</b>（既不重置数据库水位，也不清理 Redis 计数键）：
     * 这是刻意的，因为「清计数」与「配置生效」之间必然存在窗口，而窗口两边各有一条重号路径：</p>
     * <ul>
     *   <li>先改配置、后清键：窗口内已用**新**规则生成的请求给新周期键播了种并发出 {@code X-0001}，
     *       随后清键把它删掉，下一个请求又从 1 开始 ⇒ 新格式重号；</li>
     *   <li>水位一并置零：窗口内读到**旧**规则的迟到请求会因「水位日期段不匹配」从 0 播种，
     *       发出当天早已发过的 {@code X-0001} ⇒ 旧格式重号；且把日期段改回去也会重号。</li>
     * </ul>
     *
     * <p>而这两件事本就**不需要**：日期段一改，新格式渲染出的日期段串与旧串长度必然不同
     * （枚举内五种格式长度 0/4/6/8/10/14 互不相同），因此新键与旧键不可能同名，
     * 数据库水位也因「日期段不匹配」自动失效 ⇒ 新格式天然从 1 开始，旧键由 TTL 自然回收
     * （唯一不过期的是「原规则无日期段」时留下的一个孤儿键，无读取入口，可接受）。
     * 「缩位」的出路同样由此天然成立，见 {@link #checkWatermarkWithinSeqLength}。</p>
     *
     * @param dto 规则数据
     */
    public void updateRule(SysSequenceRuleDTO dto) {
        SysSequenceRuleDTO existing = sequenceRuleManager.getDtoById(dto.getId());
        if (existing == null) {
            throw SystemErrorCode.SYS_SEQUENCE_RULE_NOT_FOUND.exception();
        }
        validateRule(mergeForValidate(existing, dto));
        if (!sequenceRuleManager.modify(dto)) {
            throw SystemErrorCode.SYS_SEQUENCE_SAVE_FAILED.exception();
        }
        log.info("更新编码规则成功：ruleCode={}", existing.getRuleCode());
    }

    /**
     * 批量删除规则（同时清理各自 Redis 计数键）
     *
     * <p>删规则即放弃其计数：重建同编码规则会从 1 重新编号，与删除前已发出的编码重复，
     * 故规则编码应该用「改配置」而不是「删了重加」来调整。</p>
     *
     * @param ids 规则ID集合
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteRules(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (Long id : ids) {
            sequenceRuleManager.delete(id);
        }
    }

    /**
     * 生成下一个编码（供 Feign 调用）
     *
     * @param ruleCode 规则编码
     * @return 业务编码
     */
    public String nextCode(String ruleCode) {
        SysSequenceRuleDTO rule = getRuleOrThrow(ruleCode);
        SequenceBatch batch = sequenceRuleManager.next(rule, 1);
        return formatCode(rule, batch.getEnd(), batch.getPeriod());
    }

    /**
     * 批量预取号段（供 Feign 调用）
     *
     * @param ruleCode 规则编码
     * @param count    预取数量
     * @return 号段信息
     */
    public SequenceBatchAO nextBatch(String ruleCode, int count) {
        if (count < 1 || count > ApiSequence.MAX_BATCH_SIZE) {
            throw SystemErrorCode.SYS_PARAM_SEQUENCE_BATCH_SIZE_INVALID.exception();
        }
        SysSequenceRuleDTO rule = getRuleOrThrow(ruleCode);
        SequenceBatch batch = sequenceRuleManager.next(rule, count);
        return new SequenceBatchAO()
            .setStartCode(formatCode(rule, batch.getStart(), batch.getPeriod()))
            .setEndCode(formatCode(rule, batch.getEnd(), batch.getPeriod()))
            .setCount(batch.getCount())
            .setStep(rule.getStep() == null ? 1 : rule.getStep())
            .setPeriod(batch.getPeriod());
    }

    /**
     * 按规则编码取规则，不存在则抛异常
     */
    private SysSequenceRuleDTO getRuleOrThrow(String ruleCode) {
        SysSequenceRuleDTO rule = sequenceRuleManager.getByCode(ruleCode);
        if (rule == null) {
            // 规则编码来自调用方，只落日志不回显进响应，避免错误消息成为「编码是否存在」的探测口
            log.warn("编码规则不存在：ruleCode={}", ruleCode);
            throw SystemErrorCode.SYS_SEQUENCE_RULE_NOT_FOUND.exception();
        }
        return rule;
    }

    /**
     * 格式化编码
     */
    private String formatCode(SysSequenceRuleDTO rule, long seq, String period) {
        return SequenceCodeFormatter.format(rule.getPrefix(), period, rule.getSeparator(), seq, rule.getSeqLength());
    }

    /**
     * 合并出「原值 + 本次改动」的完整视图（null = 沿用原值，空串 = 清空）
     *
     * <p>规则编码不在更新参数中，直接沿用原值。</p>
     */
    private SysSequenceRuleDTO mergeForValidate(SysSequenceRuleDTO existing, SysSequenceRuleDTO dto) {
        return new SysSequenceRuleDTO()
            .setId(dto.getId())
            .setRuleCode(existing.getRuleCode())
            .setRuleName(dto.getRuleName() == null ? existing.getRuleName() : dto.getRuleName())
            .setPrefix(dto.getPrefix() == null ? existing.getPrefix() : dto.getPrefix())
            .setDatePattern(dto.getDatePattern() == null ? existing.getDatePattern() : dto.getDatePattern())
            .setSeparator(dto.getSeparator() == null ? existing.getSeparator() : dto.getSeparator())
            .setSeqLength(dto.getSeqLength() == null ? existing.getSeqLength() : dto.getSeqLength())
            .setStep(dto.getStep() == null ? existing.getStep() : dto.getStep())
            .setResetCycle(dto.getResetCycle() == null ? existing.getResetCycle() : dto.getResetCycle())
            .setCurrentValue(existing.getCurrentValue())
            .setCurrentPeriod(existing.getCurrentPeriod());
    }

    /**
     * 规则校验（在完整视图上进行）
     *
     * <p>四条跨字段不变式，缺一条都会产出重复编码或让规则「永久打不出号」：</p>
     * <ol>
     *   <li>日期段必须在白名单内（否则等于把任意格式化串交给 DateTimeFormatter）；</li>
     *   <li>重置周期必须是合法枚举值 —— 越界编码会被解析成 null，而 null 在 NOT_NULL 更新策略下
     *       会被**静默丢弃**、落到列默认值 2（按日），与用户意图不符；</li>
     *   <li>无日期段 ⇒ 只能「永不重置」，否则跨周期流水归零必然重号；有日期段 ⇒ 重置周期不得短于
     *       日期段粒度（编码里只有月份段却要求按日重置时，重置其实发生在月切换时，配置与意图不符）。</li>
     *   <li>步长与当前水位都必须落在位数上限内 —— 越界后每次生成都会溢出，规则被永久打死。</li>
     * </ol>
     *
     * @param rule 完整视图（更新时为合并结果）
     */
    private void validateRule(SysSequenceRuleDTO rule) {
        String datePattern = rule.getDatePattern();
        if (!SequenceDatePatternEnum.isSupported(datePattern)) {
            throw SystemErrorCode.SYS_SEQUENCE_DATE_PATTERN_INVALID.exception();
        }
        SequenceResetCycleEnum resetCycle = rule.getResetCycle();
        if (resetCycle == null) {
            throw SystemErrorCode.SYS_PARAM_SEQUENCE_RESET_CYCLE_INVALID.exception();
        }
        if (StrUtil.isBlank(datePattern)) {
            if (resetCycle != SequenceResetCycleEnum.NEVER) {
                throw SystemErrorCode.SYS_SEQUENCE_RESET_WITHOUT_DATE_PATTERN.exception();
            }
        } else if (!SequenceCodeFormatter.isCycleCompatible(resetCycle, datePattern)) {
            throw SystemErrorCode.SYS_SEQUENCE_CYCLE_SHORTER_THAN_PERIOD.exception();
        }

        if (rule.getSeqLength() == null) {
            throw SystemErrorCode.SYS_SEQUENCE_OVERFLOW.exception("流水位数不能为空");
        }
        long modulus = SequenceCodeFormatter.modulus(rule.getSeqLength());
        int step = rule.getStep() == null ? 1 : rule.getStep();
        if (step < 1) {
            // 步长为 0 时 INCRBY 0 恒返回同一值，每次生成都发同一个编码；
            // Param 层已有 @Min(1)、DDL 也有 CHECK，这里兜的是库中存量脏数据与内部直接调用
            throw SystemErrorCode.SYS_PARAM_SEQUENCE_STEP_INVALID.exception();
        }
        if (step >= modulus) {
            throw SystemErrorCode.SYS_SEQUENCE_OVERFLOW.exception("步长必须小于流水位数的取值上限（10^流水位数）");
        }
        checkWatermarkWithinSeqLength(rule, datePattern, modulus);
    }

    /**
     * 校验「当前计数水位」仍能被新的流水位数容纳
     *
     * <p>水位只在与**当前日期段**一致时才是有效约束：上个周期的水位早就不参与编号了，
     * 拿它拦缩位是误报。日期段变更时新老周期串结构必然不同（枚举内的 pattern 渲染结果
     * 长度各不相同），故此处天然不适用于「改日期段顺带缩位」的场景。</p>
     */
    private void checkWatermarkWithinSeqLength(SysSequenceRuleDTO rule, String datePattern, long modulus) {
        Long watermark = rule.getCurrentValue();
        if (watermark == null || watermark <= 0 || watermark < modulus) {
            return;
        }
        if (!SequenceCodeFormatter.resolvePeriod(datePattern, LocalDateTime.now()).equals(rule.getCurrentPeriod())) {
            return;
        }
        throw SystemErrorCode.SYS_SEQUENCE_OVERFLOW.exception(
            "当前流水已超出新位数上限，请改用更大的流水位数，或变更日期段以重置计数");
    }

}
