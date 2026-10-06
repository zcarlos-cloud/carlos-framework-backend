package com.carlos.job.pojo.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * <p>
 * 调度策略枚举单元测试
 * </p>
 *
 * @author Carlos
 * @date 2026年9月23日
 */
class JobStrategyEnumTest {

    @Test
    @DisplayName("应该按编码解析调度类型")
    void should_resolve_schedule_type_by_code() {
        assertEquals(JobScheduleTypeEnum.CRON, JobScheduleTypeEnum.of(1));
        assertEquals(JobScheduleTypeEnum.FIXED_INTERVAL, JobScheduleTypeEnum.of(2));
        assertEquals(JobScheduleTypeEnum.ONCE, JobScheduleTypeEnum.of(3));
        assertNull(JobScheduleTypeEnum.of(99));
    }

    @Test
    @DisplayName("应该按编码解析过期策略并兜底智能策略")
    void should_resolve_misfire_strategy_with_default() {
        assertEquals(JobMisfireStrategyEnum.SMART_POLICY, JobMisfireStrategyEnum.of(0));
        assertEquals(JobMisfireStrategyEnum.IGNORE_MISFIRES, JobMisfireStrategyEnum.of(1));
        assertEquals(JobMisfireStrategyEnum.FIRE_AND_PROCEED, JobMisfireStrategyEnum.of(2));
        assertEquals(JobMisfireStrategyEnum.DO_NOTHING, JobMisfireStrategyEnum.of(3));
        assertNull(JobMisfireStrategyEnum.of(99));
        // 空值/未知兜底 SMART_POLICY
        assertEquals(JobMisfireStrategyEnum.SMART_POLICY, JobMisfireStrategyEnum.ofDefault(null));
        assertEquals(JobMisfireStrategyEnum.SMART_POLICY, JobMisfireStrategyEnum.ofDefault(99));
        assertEquals(JobMisfireStrategyEnum.DO_NOTHING, JobMisfireStrategyEnum.ofDefault(3));
    }

    @Test
    @DisplayName("应该按编码解析并发模式并兜底串行")
    void should_resolve_concurrent_mode_with_default() {
        assertEquals(JobConcurrentModeEnum.SERIAL, JobConcurrentModeEnum.of(1));
        assertEquals(JobConcurrentModeEnum.PARALLEL, JobConcurrentModeEnum.of(2));
        assertNull(JobConcurrentModeEnum.of(99));
        assertEquals(JobConcurrentModeEnum.SERIAL, JobConcurrentModeEnum.ofDefault(null));
        assertEquals(JobConcurrentModeEnum.PARALLEL, JobConcurrentModeEnum.ofDefault(2));
    }

}
