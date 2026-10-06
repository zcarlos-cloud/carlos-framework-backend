package com.carlos.system.sequence.service;

import com.carlos.core.exception.BusinessException;
import com.carlos.system.pojo.ao.SequenceBatchAO;
import com.carlos.system.sequence.manager.SysSequenceRuleManager;
import com.carlos.system.sequence.pojo.dto.SequenceBatch;
import com.carlos.system.sequence.pojo.dto.SysSequenceRuleDTO;
import com.carlos.system.sequence.pojo.enums.SequenceResetCycleEnum;
import com.carlos.system.sequence.support.SequenceCodeFormatter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * SysSequenceRuleService 单元测试
 *
 * <p>S5 序列号：规则四条跨字段不变式（日期段白名单、重置周期合法、周期与粒度兼容、
 * 步长与水位不越位）、唯一性、部分更新语义、编排、编码格式化与溢出校验。</p>
 *
 * @author carlos
 */
@ExtendWith(MockitoExtension.class)
class SysSequenceRuleServiceTest {

    @Mock
    private SysSequenceRuleManager sequenceRuleManager;

    @InjectMocks
    private SysSequenceRuleService sequenceRuleService;

    /**
     * 按日重置的常用规则：PO + yyyyMMdd + - + 4 位流水
     */
    private SysSequenceRuleDTO dailyRule() {
        return new SysSequenceRuleDTO()
            .setId(1L)
            .setRuleCode("order_no")
            .setRuleName("订单号")
            .setPrefix("PO")
            .setDatePattern("yyyyMMdd")
            .setSeparator("-")
            .setSeqLength(4)
            .setStep(1)
            .setResetCycle(SequenceResetCycleEnum.DAILY);
    }

    /**
     * 仅含 ID 的部分更新参数（其余字段为 null，表示不改）
     */
    private SysSequenceRuleDTO partialUpdate() {
        return new SysSequenceRuleDTO().setId(1L);
    }

    // ==================== 新增 ====================

    @Test
    @DisplayName("新增规则：无日期段且按周期重置时抛异常")
    void should_throw_when_reset_without_date_pattern() {
        SysSequenceRuleDTO dto = new SysSequenceRuleDTO()
            .setRuleCode("order_no")
            .setRuleName("订单号")
            .setDatePattern(null)
            .setSeqLength(6)
            .setStep(1)
            .setResetCycle(SequenceResetCycleEnum.DAILY);

        assertThrows(BusinessException.class, () -> sequenceRuleService.addRule(dto));
        verify(sequenceRuleManager, never()).add(any());
    }

    @Test
    @DisplayName("新增规则：无日期段 + 永不重置合法")
    void should_add_rule_when_no_date_pattern_with_never_reset() {
        SysSequenceRuleDTO dto = new SysSequenceRuleDTO()
            .setRuleCode("order_no")
            .setRuleName("订单号")
            .setDatePattern("")
            .setSeqLength(6)
            .setStep(1)
            .setResetCycle(SequenceResetCycleEnum.NEVER);
        when(sequenceRuleManager.existsByCode("order_no")).thenReturn(false);
        when(sequenceRuleManager.add(dto)).thenReturn(true);

        sequenceRuleService.addRule(dto);

        verify(sequenceRuleManager).add(dto);
    }

    @Test
    @DisplayName("新增规则：日期段不在白名单时抛异常")
    void should_throw_when_date_pattern_unsupported() {
        SysSequenceRuleDTO dto = dailyRule().setDatePattern("yyyy-MM-dd");

        assertThrows(BusinessException.class, () -> sequenceRuleService.addRule(dto));
        verify(sequenceRuleManager, never()).add(any());
    }

    @Test
    @DisplayName("新增规则：重置周期为空（编码越界解析为 null）时抛异常，不得静默落到列默认值")
    void should_throw_when_reset_cycle_null() {
        SysSequenceRuleDTO dto = dailyRule().setResetCycle(null);

        assertThrows(BusinessException.class, () -> sequenceRuleService.addRule(dto));
        verify(sequenceRuleManager, never()).add(any());
    }

    @Test
    @DisplayName("新增规则：重置周期短于日期段粒度（按月日期段 + 按日重置）时抛异常")
    void should_throw_when_cycle_shorter_than_period() {
        SysSequenceRuleDTO dto = dailyRule().setDatePattern("yyyyMM");

        assertThrows(BusinessException.class, () -> sequenceRuleService.addRule(dto));
        verify(sequenceRuleManager, never()).add(any());
    }

    @Test
    @DisplayName("新增规则：步长不小于流水位数上限时抛异常（否则每次生成都溢出，规则被永久打死）")
    void should_throw_when_step_reaches_modulus() {
        // 2 位流水最多到 99，步长 100 一出手就越界
        SysSequenceRuleDTO dto = dailyRule().setSeqLength(2).setStep(100);

        assertThrows(BusinessException.class, () -> sequenceRuleService.addRule(dto));
        verify(sequenceRuleManager, never()).add(any());
    }

    @Test
    @DisplayName("新增规则：流水位数为空时抛异常")
    void should_throw_when_seq_length_null() {
        SysSequenceRuleDTO dto = dailyRule().setSeqLength(null);

        assertThrows(BusinessException.class, () -> sequenceRuleService.addRule(dto));
        verify(sequenceRuleManager, never()).add(any());
    }

    @Test
    @DisplayName("新增规则：规则编码已存在时抛异常")
    void should_throw_when_rule_code_exists() {
        SysSequenceRuleDTO dto = dailyRule();
        when(sequenceRuleManager.existsByCode("order_no")).thenReturn(true);

        assertThrows(BusinessException.class, () -> sequenceRuleService.addRule(dto));
        verify(sequenceRuleManager, never()).add(any());
    }

    @Test
    @DisplayName("新增规则：写入返回 false 时抛保存失败异常，不得静默成功")
    void should_throw_when_add_returns_false() {
        SysSequenceRuleDTO dto = dailyRule();
        when(sequenceRuleManager.existsByCode("order_no")).thenReturn(false);
        when(sequenceRuleManager.add(dto)).thenReturn(false);

        assertThrows(BusinessException.class, () -> sequenceRuleService.addRule(dto));
    }

    // ==================== 更新 ====================

    @Test
    @DisplayName("更新规则：规则不存在时抛异常")
    void should_throw_when_update_rule_not_found() {
        when(sequenceRuleManager.getDtoById(1L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> sequenceRuleService.updateRule(partialUpdate()));
        verify(sequenceRuleManager, never()).modify(any());
    }

    @Test
    @DisplayName("更新规则：只改名称的部分更新按「原值 + 改动」合并校验后提交")
    void should_update_partial_only_rule_name() {
        SysSequenceRuleDTO dto = partialUpdate().setRuleName("订单号V2");
        when(sequenceRuleManager.getDtoById(1L)).thenReturn(dailyRule());
        when(sequenceRuleManager.modify(dto)).thenReturn(true);

        sequenceRuleService.updateRule(dto);

        verify(sequenceRuleManager).modify(dto);
        // 日期段未变，不该清计数
        verify(sequenceRuleManager, never()).clearCounters(anyString());
    }

    @Test
    @DisplayName("更新规则：部分更新清空日期段时按合并视图校验（原有按日重置必须改为永不重置）")
    void should_throw_when_clearing_date_pattern_keeps_cycle_reset() {
        // 原规则是按日重置，客户端只清日期段：合并后「无日期段 + 按日重置」→ 必须拒绝
        SysSequenceRuleDTO dto = partialUpdate().setDatePattern("");
        when(sequenceRuleManager.getDtoById(1L)).thenReturn(dailyRule());

        assertThrows(BusinessException.class, () -> sequenceRuleService.updateRule(dto));
        verify(sequenceRuleManager, never()).modify(any());
    }

    @Test
    @DisplayName("更新规则：改日期段不动计数键与水位（清键会把并发中新建的新格式键删掉而重号）")
    void should_not_touch_counters_when_date_pattern_changed() {
        // 日粒度改成月粒度，重置周期必须同步改为按月（否则 20986）
        SysSequenceRuleDTO dto = partialUpdate().setDatePattern("yyyyMM").setResetCycle(SequenceResetCycleEnum.MONTHLY);
        when(sequenceRuleManager.getDtoById(1L)).thenReturn(dailyRule());
        when(sequenceRuleManager.modify(dto)).thenReturn(true);

        sequenceRuleService.updateRule(dto);

        verify(sequenceRuleManager).modify(dto);
        verify(sequenceRuleManager, never()).clearCounters(anyString());
    }

    @Test
    @DisplayName("更新规则：步长小于 1 时拒绝（INCRBY 0 会反复发出同一个编码）")
    void should_throw_when_step_invalid_on_update() {
        SysSequenceRuleDTO dto = partialUpdate().setStep(0);
        when(sequenceRuleManager.getDtoById(1L)).thenReturn(dailyRule());

        assertThrows(BusinessException.class, () -> sequenceRuleService.updateRule(dto));
        verify(sequenceRuleManager, never()).modify(any());
    }

    @Test
    @DisplayName("更新规则：当前日期段水位已超出新位数上限时抛异常（不留下打不出号的规则）")
    void should_throw_when_watermark_exceeds_shrunk_seq_length() {
        SysSequenceRuleDTO dto = partialUpdate().setSeqLength(2);
        SysSequenceRuleDTO existing = dailyRule().setCurrentValue(500L)
            .setCurrentPeriod(SequenceCodeFormatter.resolvePeriod("yyyyMMdd", LocalDateTime.now()));
        when(sequenceRuleManager.getDtoById(1L)).thenReturn(existing);

        assertThrows(BusinessException.class, () -> sequenceRuleService.updateRule(dto));
        verify(sequenceRuleManager, never()).modify(any());
    }

    @Test
    @DisplayName("更新规则：水位属于上一周期时不参与缩位校验（旧水位早就不参与编号）")
    void should_skip_watermark_check_when_period_stale() {
        SysSequenceRuleDTO dto = partialUpdate().setSeqLength(2);
        SysSequenceRuleDTO existing = dailyRule().setCurrentValue(500L).setCurrentPeriod("20200101");
        when(sequenceRuleManager.getDtoById(1L)).thenReturn(existing);
        when(sequenceRuleManager.modify(dto)).thenReturn(true);

        sequenceRuleService.updateRule(dto);

        verify(sequenceRuleManager).modify(dto);
    }

    @Test
    @DisplayName("更新规则：写入返回 false 时抛保存失败异常")
    void should_throw_when_modify_returns_false() {
        SysSequenceRuleDTO dto = partialUpdate().setRuleName("订单号V2");
        when(sequenceRuleManager.getDtoById(1L)).thenReturn(dailyRule());
        when(sequenceRuleManager.modify(dto)).thenReturn(false);

        assertThrows(BusinessException.class, () -> sequenceRuleService.updateRule(dto));
    }

    // ==================== 删除 ====================

    @Test
    @DisplayName("删除规则：空集合直接跳过")
    void should_skip_delete_when_ids_empty() {
        sequenceRuleService.deleteRules(Collections.emptySet());
        sequenceRuleService.deleteRules(null);

        verify(sequenceRuleManager, never()).delete(any());
    }

    @Test
    @DisplayName("删除规则：逐个删除并清理计数键")
    void should_delete_each_rule() {
        sequenceRuleService.deleteRules(Set.of(1L, 2L));

        verify(sequenceRuleManager, times(2)).delete(any());
    }

    // ==================== 生成 ====================

    @Test
    @DisplayName("生成编码：规则不存在时抛异常")
    void should_throw_when_next_code_rule_not_found() {
        when(sequenceRuleManager.getByCode("ghost")).thenReturn(null);

        assertThrows(BusinessException.class, () -> sequenceRuleService.nextCode("ghost"));
        verify(sequenceRuleManager, never()).next(any(), anyInt());
    }

    @Test
    @DisplayName("生成编码：按规则格式化为 prefix+period+separator+流水")
    void should_generate_next_code() {
        SysSequenceRuleDTO rule = dailyRule();
        when(sequenceRuleManager.getByCode("order_no")).thenReturn(rule);
        when(sequenceRuleManager.next(rule, 1)).thenReturn(new SequenceBatch()
            .setStart(1L).setEnd(1L).setCount(1).setPeriod("20260923"));

        String code = sequenceRuleService.nextCode("order_no");

        assertEquals("PO20260923-0001", code);
    }

    @Test
    @DisplayName("生成编码：流水超出位数上限时抛异常")
    void should_throw_when_seq_overflow() {
        SysSequenceRuleDTO rule = dailyRule();
        when(sequenceRuleManager.getByCode("order_no")).thenReturn(rule);
        when(sequenceRuleManager.next(rule, 1)).thenReturn(new SequenceBatch()
            .setStart(10000L).setEnd(10000L).setCount(1).setPeriod("20260923"));

        assertThrows(BusinessException.class, () -> sequenceRuleService.nextCode("order_no"));
    }

    @Test
    @DisplayName("批量预取：返回段首/段尾编码、数量、步长与日期段")
    void should_generate_batch() {
        SysSequenceRuleDTO rule = dailyRule();
        when(sequenceRuleManager.getByCode("order_no")).thenReturn(rule);
        when(sequenceRuleManager.next(rule, 3)).thenReturn(new SequenceBatch()
            .setStart(5L).setEnd(7L).setCount(3).setPeriod("20260923"));

        SequenceBatchAO batch = sequenceRuleService.nextBatch("order_no", 3);

        assertEquals("PO20260923-0005", batch.getStartCode());
        assertEquals("PO20260923-0007", batch.getEndCode());
        assertEquals(3, batch.getCount());
        assertEquals(1, batch.getStep());
        assertEquals("20260923", batch.getPeriod());
    }

    @Test
    @DisplayName("批量预取：步长大于 1 时段首/段尾按步长跨度为界，并回传步长")
    void should_generate_batch_with_step_greater_than_one() {
        SysSequenceRuleDTO rule = dailyRule().setStep(10);
        when(sequenceRuleManager.getByCode("order_no")).thenReturn(rule);
        when(sequenceRuleManager.next(rule, 2)).thenReturn(new SequenceBatch()
            .setStart(11L).setEnd(20L).setCount(2).setPeriod("20260923"));

        SequenceBatchAO batch = sequenceRuleService.nextBatch("order_no", 2);

        assertEquals("PO20260923-0011", batch.getStartCode());
        assertEquals("PO20260923-0020", batch.getEndCode());
        assertEquals(10, batch.getStep());
    }

    @Test
    @DisplayName("批量预取：数量越界直接拒绝，不触碰规则查询")
    void should_throw_when_batch_size_invalid() {
        assertThrows(BusinessException.class, () -> sequenceRuleService.nextBatch("order_no", 0));
        assertThrows(BusinessException.class, () -> sequenceRuleService.nextBatch("order_no", -1));

        verifyNoInteractions(sequenceRuleManager);
    }

    @Test
    @DisplayName("生成编码：规则编码空白同样按不存在处理")
    void should_throw_when_rule_not_found_blank_code() {
        when(sequenceRuleManager.getByCode(anyString())).thenReturn(null);

        assertThrows(BusinessException.class, () -> sequenceRuleService.nextCode(" "));
        verify(sequenceRuleManager, never()).next(any(), eq(1));
    }

}
