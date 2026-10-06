package com.carlos.system.sequence.support;

import cn.hutool.core.util.StrUtil;
import com.carlos.core.exception.BusinessException;
import com.carlos.system.enums.SystemErrorCode;
import com.carlos.system.sequence.pojo.enums.SequenceDatePatternEnum;
import com.carlos.system.sequence.pojo.enums.SequenceResetCycleEnum;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 序列号编码格式化/键构建工具
 * </p>
 *
 * <p>纯函数集合（无 Spring 依赖、无 Redis 访问），便于单测覆盖；Redis 键名与 TTL 口径也收敛在此，
 * Manager 与 Service 共用同一套规则。</p>
 *
 * <p>编码结构：{@code prefix + period + separator + 左补零流水}。</p>
 *
 * @author carlos
 * @since 3.0.0
 */
public final class SequenceCodeFormatter {

    /**
     * Redis 计数键前缀：{@code system:sequence:{ruleCode}}（无日期段）或 {@code system:sequence:{ruleCode}:{period}}
     *
     * <p>命名遵循框架的 {@code {模块}:{业务}:{标识}} 约定，避免与其它模块在共享库中撞名。</p>
     */
    public static final String KEY_PREFIX = "system:sequence:";

    /**
     * 无过期（不含日期段的规则：全局只有一个计数键，本就不需要过期）
     */
    public static final long NO_EXPIRE = -1L;

    /**
     * 按日重置的键 TTL：2 天（留足跨日续用窗口）
     */
    private static final long TTL_DAILY_SECONDS = TimeUnit.DAYS.toSeconds(2);
    /**
     * 按月重置的键 TTL：45 天
     */
    private static final long TTL_MONTHLY_SECONDS = TimeUnit.DAYS.toSeconds(45);
    /**
     * 按年重置的键 TTL：400 天
     */
    private static final long TTL_YEARLY_SECONDS = TimeUnit.DAYS.toSeconds(400);

    /**
     * 各日期段格式的一整个周期时长 × 2（宽限窗口），用于兜住「TTL 短于日期段」导致的键中途过期
     */
    private static final long PERIOD_TTL_YEAR_SECONDS = TimeUnit.DAYS.toSeconds(730);
    private static final long PERIOD_TTL_MONTH_SECONDS = TimeUnit.DAYS.toSeconds(62);
    private static final long PERIOD_TTL_DAY_SECONDS = TimeUnit.DAYS.toSeconds(2);
    private static final long PERIOD_TTL_HOUR_SECONDS = TimeUnit.HOURS.toSeconds(2);
    private static final long PERIOD_TTL_SECOND_SECONDS = TimeUnit.MINUTES.toSeconds(2);

    /**
     * 流水位数上限（10^12 仍可用 long 精确表示）
     */
    public static final int MAX_SEQ_LENGTH = 12;

    private SequenceCodeFormatter() {
    }

    /**
     * 解析编码所属日期段
     *
     * @param datePattern 日期段格式（可为空，表示不含日期段）
     * @param now         当前时间
     * @return 日期段字符串，不含日期段时为空串
     * @throws BusinessException 日期段格式不在白名单内（库中存量脏数据防御）
     */
    public static String resolvePeriod(String datePattern, LocalDateTime now) {
        if (StrUtil.isBlank(datePattern)) {
            return "";
        }
        SequenceDatePatternEnum pattern = SequenceDatePatternEnum.of(datePattern);
        if (pattern == null) {
            throw SystemErrorCode.SYS_SEQUENCE_DATE_PATTERN_INVALID.exception();
        }
        return now.format(DateTimeFormatter.ofPattern(pattern.getCode()));
    }

    /**
     * 构建 Redis 计数键
     *
     * @param ruleCode 规则编码
     * @param period   日期段（空串表示无日期段）
     * @return Redis 键
     */
    public static String redisKey(String ruleCode, String period) {
        return StrUtil.isBlank(period) ? KEY_PREFIX + ruleCode : KEY_PREFIX + ruleCode + ":" + period;
    }

    /**
     * 带日期段的规则其计数键的扫描 pattern（清理用）
     *
     * <p>⚠️ 必须带上冒号界符：{@code system:sequence:order:*} 不会命中 {@code system:sequence:order_no:20260923}，
     * 而无界符的 {@code system:sequence:order*} 会 —— 那会把**其它规则的计数键**一并删掉，
     * 对方下次自增从 1 开始、与历史编码整段重号。无日期段的键是精确键，
     * 由 {@link #redisKey(String, String)} 单独删除。</p>
     *
     * @param ruleCode 规则编码
     * @return Redis glob pattern
     */
    public static String redisKeyPattern(String ruleCode) {
        return KEY_PREFIX + ruleCode + ":*";
    }

    /**
     * 解析计数键 TTL
     *
     * <p>取「重置周期对应的 TTL」与「日期段周期时长 × 2」的**较大者**：
     * 前者是用户意图，后者是硬下限 —— 键的存活时间必须覆盖它所属的整个日期段，
     * 否则键会在周期未结束时过期、流水归零重发（服务层另有一条显式校验把这种组合拒之门外，
     * 这里的 max 只是纵深防御）。</p>
     *
     * <p>不含日期段的规则只有一个全局键，返回 {@link #NO_EXPIRE}（此时"重置周期"只能是永不重置，
     * 由服务层校验保证）。</p>
     *
     * @param resetCycle  重置周期（不可为空，取不到就是数据有问题，宁可报错也不静默兜底）
     * @param datePattern 日期段格式（可空表示不含日期段）
     * @return 秒
     * @throws BusinessException 重置周期为空（库中存量脏数据防御）
     */
    public static long resolveTtlSeconds(SequenceResetCycleEnum resetCycle, String datePattern) {
        long periodTtl = resolvePeriodTtlSeconds(datePattern);
        if (periodTtl == NO_EXPIRE) {
            return NO_EXPIRE;
        }
        return Math.max(resolveCycleTtlSeconds(resetCycle), periodTtl);
    }

    /**
     * 重置周期是否长于（或等于）日期段粒度
     *
     * <p>比较的是**语义时长**（周期 vs 粒度），不是上面那两个带宽限的 TTL ——
     * 拿 2 倍粒度的 TTL 去比 1 倍周期的 TTL 会把「按月日期段 + 按月重置」这种最自然的组合误判为不兼容。</p>
     *
     * <p>不兼容的含义是「用户表达的意图该格式表达不出来」：编码里只有月份段，却要求按日重置，
     * 实际重置发生在月切换时。此时计数键虽然因 TTL 取 max 而不会中途过期，但配置本身就是错的，
     * 应当直接拒绝而不是留个「看着生效其实没生效」的规则。</p>
     *
     * <p>不含日期段时一律返回 true（该组合由「无日期段必须永不重置」那条规则校验）。</p>
     *
     * @param resetCycle  重置周期
     * @param datePattern 日期段格式
     * @return 兼容返回 true
     */
    public static boolean isCycleCompatible(SequenceResetCycleEnum resetCycle, String datePattern) {
        if (StrUtil.isBlank(datePattern) || resetCycle == SequenceResetCycleEnum.NEVER) {
            return true;
        }
        return cycleDurationSeconds(resetCycle) >= periodGranularitySeconds(datePattern);
    }

    /**
     * 号段中第一个**真正被分配**的流水值
     *
     * <p>一次预取 {@code count} 个、步长 {@code step} 时，本次自增区间是
     * {@code (end - count*step, end]}，按其递增顺序第一个可用值是 {@code end - count*step + step}
     * —— 而不是区间起点 {@code end - count*step + 1}（步长大于 1 时那个值永远不会被分配）。</p>
     *
     * @param end   段尾流水值
     * @param count 预取数量
     * @param step  步长
     * @return 段首流水值
     */
    public static long firstSeq(long end, int count, int step) {
        return end - (long) count * step + step;
    }

    /**
     * 格式化编码
     *
     * @param prefix    前缀（可空）
     * @param period    日期段（可空）
     * @param separator 分隔符（可空）
     * @param seq       流水值
     * @param seqLength 流水位数
     * @return 业务编码
     * @throws BusinessException 流水值超出位数上限
     */
    public static String format(String prefix, String period, String separator, long seq, int seqLength) {
        long modulus = modulus(seqLength);
        if (seq >= modulus || seq < 0) {
            throw SystemErrorCode.SYS_SEQUENCE_OVERFLOW.exception();
        }
        String padded = String.format("%0" + seqLength + "d", seq);
        return StrUtil.nullToEmpty(prefix) + StrUtil.nullToEmpty(period) + StrUtil.nullToEmpty(separator) + padded;
    }

    /**
     * 指定位数的最大可表示流水数（10^seqLength）
     *
     * @param seqLength 流水位数
     * @return 取模基数
     */
    public static long modulus(int seqLength) {
        long modulus = 1L;
        for (int i = 0; i < seqLength; i++) {
            modulus *= 10L;
        }
        return modulus;
    }

    /**
     * 重置周期对应的 TTL
     */
    private static long resolveCycleTtlSeconds(SequenceResetCycleEnum resetCycle) {
        if (resetCycle == null) {
            throw SystemErrorCode.SYS_PARAM_SEQUENCE_RESET_CYCLE_INVALID.exception();
        }
        return switch (resetCycle) {
            case DAILY -> TTL_DAILY_SECONDS;
            case MONTHLY -> TTL_MONTHLY_SECONDS;
            case YEARLY -> TTL_YEARLY_SECONDS;
            case NEVER -> NO_EXPIRE;
        };
    }

    /**
     * 日期段一整个周期时长 × 2（不含日期段返回 {@link #NO_EXPIRE}）
     */
    private static long resolvePeriodTtlSeconds(String datePattern) {
        if (StrUtil.isBlank(datePattern)) {
            return NO_EXPIRE;
        }
        SequenceDatePatternEnum pattern = SequenceDatePatternEnum.of(datePattern);
        if (pattern == null) {
            throw SystemErrorCode.SYS_SEQUENCE_DATE_PATTERN_INVALID.exception();
        }
        return switch (pattern) {
            case YEAR -> PERIOD_TTL_YEAR_SECONDS;
            case MONTH -> PERIOD_TTL_MONTH_SECONDS;
            case DAY -> PERIOD_TTL_DAY_SECONDS;
            case HOUR -> PERIOD_TTL_HOUR_SECONDS;
            case SECOND -> PERIOD_TTL_SECOND_SECONDS;
        };
    }

    /**
     * 重置周期的语义时长（按上界取整：按月按 31 天、按年按 366 天）
     */
    private static long cycleDurationSeconds(SequenceResetCycleEnum resetCycle) {
        if (resetCycle == null) {
            throw SystemErrorCode.SYS_PARAM_SEQUENCE_RESET_CYCLE_INVALID.exception();
        }
        return switch (resetCycle) {
            case NEVER -> Long.MAX_VALUE;
            case DAILY -> TimeUnit.DAYS.toSeconds(1);
            case MONTHLY -> TimeUnit.DAYS.toSeconds(31);
            case YEARLY -> TimeUnit.DAYS.toSeconds(366);
        };
    }

    /**
     * 日期段一个桶的粒度（按上界取整：月按 31 天、年按 366 天）
     */
    private static long periodGranularitySeconds(String datePattern) {
        SequenceDatePatternEnum pattern = SequenceDatePatternEnum.of(datePattern);
        if (pattern == null) {
            throw SystemErrorCode.SYS_SEQUENCE_DATE_PATTERN_INVALID.exception();
        }
        return switch (pattern) {
            case YEAR -> TimeUnit.DAYS.toSeconds(366);
            case MONTH -> TimeUnit.DAYS.toSeconds(31);
            case DAY -> TimeUnit.DAYS.toSeconds(1);
            case HOUR -> TimeUnit.HOURS.toSeconds(1);
            case SECOND -> 1L;
        };
    }
}
