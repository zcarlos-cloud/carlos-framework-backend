package com.carlos.system.sequence.manager.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.carlos.core.exception.BusinessException;
import com.carlos.redis.util.RedisUtil;
import com.carlos.system.sequence.mapper.SysSequenceRuleMapper;
import com.carlos.system.sequence.pojo.dto.SequenceBatch;
import com.carlos.system.sequence.pojo.dto.SysSequenceRuleDTO;
import com.carlos.system.sequence.pojo.entity.SysSequenceRule;
import com.carlos.system.sequence.pojo.enums.SequenceResetCycleEnum;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * SysSequenceRuleManagerImpl 单元测试
 *
 * <p>核心用例是 {@link #should_pass_four_scalar_args_to_lua()}：它守着 S5 上线前修掉的那个
 * 致命缺陷 —— {@code RedisTemplate.execute(script, keys, Object... args)} 的 ARGV 是**可变参数**，
 * Java 不会把集合展开成可变参数，传 {@code List} 只会得到**一个** ARGV 条目，Lua 侧
 * {@code tonumber(ARGV[1])} 拿到 nil，脚本必然报错并静默退化成数据库兜底路径
 * （表面上还能运行，实际每次生成都走 DB，Redis 白接）。
 * 因此这里断言的是「ARGV 个数为 4 且逐个为标量」，而不是「调用过 execute」。</p>
 *
 * <p>RedisTemplate 用自定义 Answer 而非 matcher 打桩：{@code execute} 是可变参数方法，
 * matcher 个数与「原样 / 展开」两种实参形态在 Mockito 里对不上，容易打成永不命中的空桩。</p>
 *
 * @author carlos
 */
@ExtendWith(MockitoExtension.class)
class SysSequenceRuleManagerImplTest {

    private static final String RULE_CODE = "order_no";

    @Mock
    private SysSequenceRuleMapper sequenceRuleMapper;

    @InjectMocks
    private SysSequenceRuleManagerImpl sequenceRuleManager;

    /**
     * 脚本返回值
     */
    private long scriptResult = 0L;

    /**
     * Redis 是否可用（false 时 execute 抛异常，走数据库兜底）
     */
    private boolean redisAvailable = true;

    /**
     * 最近一次 Lua 调用的 KEYS
     */
    private List<String> luaKeys;

    /**
     * 最近一次 Lua 调用的 ARGV
     */
    private Object[] luaArgs;

    /**
     * 最近一次 Lua 调用的 ARGV 序列化器（必须是 String 序列化器：框架默认的 JSON 值序列化器
     * 会把标量写成带引号的 {@code "1"}，Lua 侧 {@code tonumber(ARGV[1])} 直接拿到 nil）
     */
    private RedisSerializer<?> luaArgsSerializer;

    /**
     * 最近一次 Lua 调用的返回值序列化器
     */
    private RedisSerializer<?> luaResultSerializer;

    @BeforeAll
    static void initTableInfo() {
        // LambdaUpdateWrapper 解析 SysSequenceRule::getXxx 列名依赖 TableInfo 缓存，单测里需手工注册
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                SysSequenceRule.class);
    }

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        ReflectionTestUtils.setField(sequenceRuleManager, "baseMapper", sequenceRuleMapper);
        // BaseService.updateWrapper()/queryWrapper() 会调 MP 的 getEntityClass()，而它默认要从
        // **真实 mapper 代理**上解析实体类型（Mockito mock 上没有 MybatisMapperProxy 会直接抛异常）。
        // getEntityClass() 本身带缓存，预热这个字段即可绕开代理解析。
        ReflectionTestUtils.setField(sequenceRuleManager, "entityClass", SysSequenceRule.class);

        RedisTemplate<String, Object> redisTemplate = Mockito.mock(RedisTemplate.class, invocation -> {
            if ("execute".equals(invocation.getMethod().getName())) {
                Object[] args = invocation.getArguments();
                // Lua 调用形如 (script, argsSerializer, resultSerializer, keys, ARGV...)：
                // ARGV 可能是原样数组，也可能被 Mockito 展开成逐元素，两种形态都收进来
                if (args.length >= 5 && args[3] instanceof List) {
                    if (!redisAvailable) {
                        throw new IllegalStateException("connection refused");
                    }
                    luaKeys = (List<String>) args[3];
                    luaArgsSerializer = (RedisSerializer<?>) args[1];
                    luaResultSerializer = (RedisSerializer<?>) args[2];
                    luaArgs = args.length == 5 && args[4] instanceof Object[]
                            ? (Object[]) args[4]
                            : Arrays.copyOfRange(args, 4, args.length);
                    return scriptResult;
                }
            }
            return Mockito.RETURNS_DEFAULTS.answer(invocation);
        });
        RedisUtil.init(redisTemplate);
    }

    /**
     * 无日期段 + 永不重置的规则
     */
    private SysSequenceRuleDTO plainRule() {
        return new SysSequenceRuleDTO()
                .setId(1L)
                .setRuleCode(RULE_CODE)
                .setRuleName("订单号")
                .setPrefix("PO")
                .setDatePattern(null)
                .setSeparator("-")
                .setSeqLength(6)
                .setStep(1)
                .setResetCycle(SequenceResetCycleEnum.NEVER);
    }

    @Test
    @DisplayName("Redis 计数：ARGV 是 4 个独立标量，KEYS 是拼好的计数键")
    void should_pass_four_scalar_args_to_lua() {
        scriptResult = 5L;

        SequenceBatch batch = sequenceRuleManager.next(plainRule(), 1);

        assertNotNull(luaArgs, "未捕获到 Lua 调用：ARGV 没有按标量下发？");
        assertEquals(List.of("system:sequence:order_no"), luaKeys);
        // 两个序列化器都必须是 String：JSON 值序列化器会把标量写成 "1"，tonumber 拿到 nil
        assertInstanceOf(StringRedisSerializer.class, luaArgsSerializer,
                "ARGV 序列化器不是 String：标量会被 JSON 引号包裹，Lua 侧 tonumber 失败");
        assertInstanceOf(StringRedisSerializer.class, luaResultSerializer,
                "返回值序列化器不是 String：整数应答会被反序列化成 JSON 而非 Long");
        // ARGV = [自增量, TTL, 水位下限, 位数上限]，顺序与 INCR_SCRIPT 一致
        assertEquals(4, luaArgs.length);
        assertEquals("1", luaArgs[0]);
        assertEquals("-1", luaArgs[1]);
        assertEquals("0", luaArgs[2]);
        assertEquals("999999", luaArgs[3]);
        assertEquals(5L, batch.getEnd());
        assertEquals(5L, batch.getStart());
        assertEquals(1, batch.getCount());
        assertEquals("", batch.getPeriod());
    }

    @Test
    @DisplayName("Redis 计数：同日期段时以数据库水位为播种下限，防计数归零重号")
    void should_seed_from_watermark_in_same_period() {
        scriptResult = 42L;
        SysSequenceRuleDTO rule = plainRule().setCurrentValue(41L).setCurrentPeriod("");

        SequenceBatch batch = sequenceRuleManager.next(rule, 1);

        assertEquals("41", luaArgs[2]);
        assertEquals(42L, batch.getEnd());
    }

    @Test
    @DisplayName("Redis 计数：水位属于上一日期段时不参与播种（新周期从 1 开始）")
    void should_ignore_watermark_of_stale_period() {
        scriptResult = 1L;
        SysSequenceRuleDTO rule = plainRule().setCurrentValue(41L).setCurrentPeriod("20200101");

        SequenceBatch batch = sequenceRuleManager.next(rule, 1);

        assertEquals("0", luaArgs[2]);
        assertEquals(1L, batch.getEnd());
    }

    @Test
    @DisplayName("Redis 计数：批量取号按 count × step 递增，段首为段内第一个真正分配的流水")
    void should_allocate_batch_with_step() {
        scriptResult = 20L;
        SysSequenceRuleDTO rule = plainRule().setStep(10);

        SequenceBatch batch = sequenceRuleManager.next(rule, 2);

        assertEquals("20", luaArgs[0]);
        assertEquals(20L, batch.getEnd());
        assertEquals(10L, batch.getStart());
        assertEquals(2, batch.getCount());
    }

    @Test
    @DisplayName("Redis 计数：脚本返回溢出哨兵时抛异常，且不回写水位")
    void should_throw_when_lua_reports_overflow() {
        scriptResult = -1L;

        assertThrows(BusinessException.class, () -> sequenceRuleManager.next(plainRule(), 1));
        verifyNoInteractions(sequenceRuleMapper);
    }

    @Test
    @DisplayName("批量取号：数量越界直接拒绝，不触碰 Redis")
    void should_throw_when_count_invalid() {
        assertThrows(BusinessException.class, () -> sequenceRuleManager.next(plainRule(), 0));
        assertThrows(BusinessException.class, () -> sequenceRuleManager.next(plainRule(), 1001));

        assertNull(luaArgs);
        verifyNoInteractions(sequenceRuleMapper);
    }

    @Test
    @DisplayName("Redis 不可用：降级为数据库 CAS 自增，号段同样唯一")
    @SuppressWarnings("unchecked")
    void should_fall_back_to_db_when_redis_unavailable() {
        redisAvailable = false;
        SysSequenceRule current = new SysSequenceRule().setId(1L).setCurrentValue(5L).setCurrentPeriod("");
        when(sequenceRuleMapper.selectById(1L)).thenReturn(current);
        when(sequenceRuleMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);
        ArgumentCaptor<LambdaUpdateWrapper<SysSequenceRule>> wrapperCaptor =
                ArgumentCaptor.forClass(LambdaUpdateWrapper.class);

        SequenceBatch batch = sequenceRuleManager.next(plainRule(), 2);

        assertEquals(5L + 2, batch.getEnd());
        assertEquals(6L, batch.getStart());
        verify(sequenceRuleMapper).selectById(1L);
        // CAS 是并发下不重号的唯一保障：WHERE 必须带上「读到的旧水位」（5L 只可能来自 CAS 条件，
        // 本次 SET 写入的是 7L、id 是 1L），否则就退化成无条件 UPDATE、并发下必然重号
        verify(sequenceRuleMapper).update(any(), wrapperCaptor.capture());
        LambdaUpdateWrapper<SysSequenceRule> wrapper = wrapperCaptor.getValue();
        // MP 的 formatParam 是惰性的：条件里的实参要等 SQL 段生成时才进 paramNameValuePairs
        wrapper.getSqlSegment();
        Collection<Object> params = wrapper.getParamNameValuePairs().values();
        assertTrue(params.contains(5L), "CAS 条件里没有旧的水位值：并发下会重号");
    }

    @Test
    @DisplayName("数据库兜底：溢出预检与 Lua 同口径，越界整批拒绝且不推进水位")
    @SuppressWarnings("unchecked")
    void should_reject_overflow_without_advancing_watermark_in_db_path() {
        redisAvailable = false;
        // seqLength=4 ⇒ 上限 9999；水位 9950 + 请求 100 ⇒ 10050 越界
        SysSequenceRule current = new SysSequenceRule().setId(1L).setCurrentValue(9950L).setCurrentPeriod("");
        when(sequenceRuleMapper.selectById(1L)).thenReturn(current);

        SysSequenceRuleDTO rule = plainRule().setSeqLength(4);
        assertThrows(BusinessException.class, () -> sequenceRuleManager.next(rule, 100));

        // 关键：不能先写库再报错 —— 水位一旦被推到 10050，Redis 恢复后会以它播种，
        // 此后每次生成都溢出，规则被无故打死（9951~9999 这段合法号也白白烧掉）
        verify(sequenceRuleMapper, never()).update(any(), any());
    }

    @Test
    @DisplayName("数据库兜底：CAS 连续冲突时抛生成失败，绝不返回可能重复的号段")
    void should_throw_when_db_cas_conflict() {
        redisAvailable = false;
        SysSequenceRule current = new SysSequenceRule().setId(1L).setCurrentValue(5L).setCurrentPeriod("");
        when(sequenceRuleMapper.selectById(1L)).thenReturn(current);
        when(sequenceRuleMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(0);

        assertThrows(BusinessException.class, () -> sequenceRuleManager.next(plainRule(), 1));
    }

    @Test
    @DisplayName("清理计数键：Redis 异常只记日志，不把异常抛给删除流程")
    void should_swallow_redis_error_when_clearing_counters() {
        assertDoesNotThrow(() -> sequenceRuleManager.clearCounters(RULE_CODE));
    }

}
