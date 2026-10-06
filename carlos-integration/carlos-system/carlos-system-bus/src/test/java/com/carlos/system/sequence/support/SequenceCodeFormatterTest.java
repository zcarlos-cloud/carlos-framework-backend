package com.carlos.system.sequence.support;

import com.carlos.core.exception.BusinessException;
import com.carlos.system.sequence.pojo.enums.SequenceResetCycleEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SequenceCodeFormatter 单元测试
 *
 * <p>S5 序列号：日期段解析、Redis 键构建、TTL 口径、重置周期与日期段粒度的兼容性、
 * 号段段首计算、编码格式化与溢出保护。</p>
 *
 * @author carlos
 */
class SequenceCodeFormatterTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 23, 10, 30, 45);

    @Test
    @DisplayName("日期段：空白格式返回空串，白名单格式按当前时间渲染")
    void should_resolve_period() {
        assertEquals("", SequenceCodeFormatter.resolvePeriod(null, NOW));
        assertEquals("", SequenceCodeFormatter.resolvePeriod("  ", NOW));
        assertEquals("2026", SequenceCodeFormatter.resolvePeriod("yyyy", NOW));
        assertEquals("202609", SequenceCodeFormatter.resolvePeriod("yyyyMM", NOW));
        assertEquals("20260923", SequenceCodeFormatter.resolvePeriod("yyyyMMdd", NOW));
        assertEquals("2026092310", SequenceCodeFormatter.resolvePeriod("yyyyMMddHH", NOW));
        assertEquals("20260923103045", SequenceCodeFormatter.resolvePeriod("yyyyMMddHHmmss", NOW));
    }

    @Test
    @DisplayName("日期段：非白名单格式抛异常（防库中脏数据注入格式串）")
    void should_throw_when_period_pattern_unsupported() {
        assertThrows(BusinessException.class, () -> SequenceCodeFormatter.resolvePeriod("yyyy-MM-dd", NOW));
    }

    @Test
    @DisplayName("Redis 键：无日期段是精确键，有日期段是「前缀 + 冒号 + period」")
    void should_build_redis_key() {
        assertEquals("system:sequence:order_no", SequenceCodeFormatter.redisKey("order_no", ""));
        assertEquals("system:sequence:order_no", SequenceCodeFormatter.redisKey("order_no", null));
        assertEquals("system:sequence:order_no:20260923", SequenceCodeFormatter.redisKey("order_no", "20260923"));
    }

    @Test
    @DisplayName("Redis 清理 pattern：必须带冒号界符，不能把同前缀的其它规则一起命中")
    void should_build_collision_free_redis_key_pattern() {
        assertEquals("system:sequence:order_no:*", SequenceCodeFormatter.redisKeyPattern("order_no"));
        // 反例：无界符的 system:sequence:order_no* 会连 order_no_extra 的计数键一起删掉，
        // 对方下次自增从 1 开始、与历史编码整段重号
        assertNotEquals(SequenceCodeFormatter.KEY_PREFIX + "order_no*",
            SequenceCodeFormatter.redisKeyPattern("order_no"));
    }

    @Test
    @DisplayName("TTL：无日期段不过期；有日期段时取「重置周期 TTL」与「日期段周期 ×2」的较大者")
    void should_resolve_ttl() {
        // 不含日期段：只有一个全局键，永不过期
        assertEquals(-1L, SequenceCodeFormatter.resolveTtlSeconds(SequenceResetCycleEnum.NEVER, null));
        assertEquals(-1L, SequenceCodeFormatter.resolveTtlSeconds(SequenceResetCycleEnum.NEVER, "  "));
        // 有日期段但永不重置：键只需活过一个日期段，取日期段周期 ×2
        assertEquals(TimeUnit.DAYS.toSeconds(2),
            SequenceCodeFormatter.resolveTtlSeconds(SequenceResetCycleEnum.NEVER, "yyyyMMdd"));
        // 按日重置 + 按日日期段：周期 TTL 与日期段 TTL 相同
        assertEquals(TimeUnit.DAYS.toSeconds(2),
            SequenceCodeFormatter.resolveTtlSeconds(SequenceResetCycleEnum.DAILY, "yyyyMMdd"));
        // 按月日期段：日期段周期（62 天）比重置周期 TTL（45 天）长，取 62 天，否则键会在月内过期
        assertEquals(TimeUnit.DAYS.toSeconds(62),
            SequenceCodeFormatter.resolveTtlSeconds(SequenceResetCycleEnum.MONTHLY, "yyyyMM"));
        // 按年日期段同理取 730 天
        assertEquals(TimeUnit.DAYS.toSeconds(730),
            SequenceCodeFormatter.resolveTtlSeconds(SequenceResetCycleEnum.YEARLY, "yyyy"));
    }

    @Test
    @DisplayName("TTL：重置周期为空时抛异常（库中脏数据不静默兜底）")
    void should_throw_when_reset_cycle_null() {
        assertThrows(BusinessException.class, () -> SequenceCodeFormatter.resolveTtlSeconds(null, "yyyyMMdd"));
    }

    @Test
    @DisplayName("兼容性：重置周期不得短于日期段粒度")
    void should_check_cycle_compatible() {
        // 不兼容：编码里只有月份段却要求按日重置（真实重置发生在月切换时，配置无效）
        assertFalse(SequenceCodeFormatter.isCycleCompatible(SequenceResetCycleEnum.DAILY, "yyyyMM"));
        // 不兼容：只有年份段却要求按月/按日重置
        assertFalse(SequenceCodeFormatter.isCycleCompatible(SequenceResetCycleEnum.MONTHLY, "yyyy"));
        assertFalse(SequenceCodeFormatter.isCycleCompatible(SequenceResetCycleEnum.DAILY, "yyyy"));
        // 兼容：周期与粒度相同或更长
        assertTrue(SequenceCodeFormatter.isCycleCompatible(SequenceResetCycleEnum.MONTHLY, "yyyyMM"));
        assertTrue(SequenceCodeFormatter.isCycleCompatible(SequenceResetCycleEnum.YEARLY, "yyyy"));
        assertTrue(SequenceCodeFormatter.isCycleCompatible(SequenceResetCycleEnum.DAILY, "yyyyMMdd"));
        assertTrue(SequenceCodeFormatter.isCycleCompatible(SequenceResetCycleEnum.DAILY, "yyyyMMddHH"));
        // 永不重置对任何日期段都兼容；不含日期段由另一条规则（必须永不重置）管，这里一律放行
        assertTrue(SequenceCodeFormatter.isCycleCompatible(SequenceResetCycleEnum.NEVER, "yyyyMM"));
        assertTrue(SequenceCodeFormatter.isCycleCompatible(SequenceResetCycleEnum.DAILY, ""));
        assertTrue(SequenceCodeFormatter.isCycleCompatible(SequenceResetCycleEnum.DAILY, null));
    }

    @Test
    @DisplayName("号段段首：段内第一个真正被分配的流水值，而非自增区间的起点")
    void should_compute_first_seq() {
        assertEquals(1L, SequenceCodeFormatter.firstSeq(1L, 1, 1));
        assertEquals(5L, SequenceCodeFormatter.firstSeq(7L, 3, 1));
        // 步长 10、取 2 个：本次自增区间是 (0, 20]，实际分配的是 10 与 20
        assertEquals(10L, SequenceCodeFormatter.firstSeq(20L, 2, 10));
        // 步长大于 1 时，区间起点 +1 那个值永远不会被分配
        assertEquals(11L, SequenceCodeFormatter.firstSeq(21L, 2, 10));
    }

    @Test
    @DisplayName("格式化：prefix + period + separator + 左补零流水")
    void should_format_code() {
        assertEquals("PO20260923-0001", SequenceCodeFormatter.format("PO", "20260923", "-", 1L, 4));
        assertEquals("000123", SequenceCodeFormatter.format(null, null, null, 123L, 6));
        assertEquals("ORDER0001", SequenceCodeFormatter.format("ORDER", "", "", 1L, 4));
        // 流水位数内允许的最大值
        assertEquals("9999", SequenceCodeFormatter.format(null, null, null, 9999L, 4));
    }

    @Test
    @DisplayName("格式化：流水超出位数上限抛异常")
    void should_throw_when_seq_overflow() {
        assertThrows(BusinessException.class, () -> SequenceCodeFormatter.format("PO", "", "-", 10000L, 4));
        assertThrows(BusinessException.class, () -> SequenceCodeFormatter.format(null, null, null, -1L, 4));
    }

    @Test
    @DisplayName("取模基数：10 的 seqLength 次方")
    void should_compute_modulus() {
        assertEquals(10L, SequenceCodeFormatter.modulus(1));
        assertEquals(10000L, SequenceCodeFormatter.modulus(4));
        assertEquals(1000000000000L, SequenceCodeFormatter.modulus(SequenceCodeFormatter.MAX_SEQ_LENGTH));
    }

}
