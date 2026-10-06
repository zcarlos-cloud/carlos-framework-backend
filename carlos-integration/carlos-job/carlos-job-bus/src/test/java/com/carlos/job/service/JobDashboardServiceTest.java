package com.carlos.job.service;

import com.carlos.job.manager.JobExecutionLogManager;
import com.carlos.job.manager.JobInfoManager;
import com.carlos.job.pojo.enums.JobExecutionStatusEnum;
import com.carlos.job.pojo.vo.JobDashboardVO;
import com.carlos.job.pojo.vo.SchedulerNodeVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.Scheduler;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * <p>
 * 调度运行报表业务单元测试
 * </p>
 *
 * @author Carlos
 * @date 2026年9月23日
 */
@ExtendWith(MockitoExtension.class)
class JobDashboardServiceTest {

    @Mock
    private JobExecutionLogManager executionLogManager;

    @Mock
    private JobInfoManager jobInfoManager;

    @Mock
    private JobMonitorService monitorService;

    @Mock
    private Scheduler scheduler;

    private JobDashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new JobDashboardService(executionLogManager, jobInfoManager, monitorService, scheduler);
    }

    @Test
    @DisplayName("趋势分布应该按下划线小写 key 取值")
    void should_map_daily_distribution_by_snake_case_keys() {
        // given: 管理器返回的 key 是 PostgreSQL 折叠后的 SQL 别名形态（success_count / fail_count）
        when(executionLogManager.countDailyByStatus(any(), any()))
            .thenReturn(List.of(dailyRow(LocalDate.now().toString(), 4L, 3L)));
        when(executionLogManager.countByStatus(any(), any())).thenReturn(Map.of(1, 4L, 2, 3L));

        // when
        JobDashboardVO vo = dashboardService.overview(7);

        // then: 若 service 侧改回读驼峰 key（successCount），这两个断言会退回 0
        JobDashboardVO.DateDistribution today = vo.getDateDistribution().get(vo.getDateDistribution().size() - 1);
        assertEquals(LocalDate.now().toString(), today.getDate(), "最后一天应为今天");
        assertEquals(4L, today.getSuccessCount());
        assertEquals(3L, today.getFailCount());
    }

    @Test
    @DisplayName("趋势分布应该按近 N 天升序补齐缺失日期为 0")
    void should_fill_missing_days_with_zero() {
        // given: 只有 2 天前有数据
        String twoDaysAgo = LocalDate.now().minusDays(2).toString();
        when(executionLogManager.countDailyByStatus(any(), any()))
            .thenReturn(List.of(dailyRow(twoDaysAgo, 5L, 1L)));
        when(executionLogManager.countByStatus(any(), any())).thenReturn(Map.of());

        // when
        JobDashboardVO vo = dashboardService.overview(3);

        // then
        List<JobDashboardVO.DateDistribution> distribution = vo.getDateDistribution();
        assertEquals(3, distribution.size());
        assertEquals(
            List.of(LocalDate.now().minusDays(2).toString(), LocalDate.now().minusDays(1).toString(),
                LocalDate.now().toString()),
            distribution.stream().map(JobDashboardVO.DateDistribution::getDate).toList()
        );
        assertEquals(5L, distribution.get(0).getSuccessCount());
        assertEquals(1L, distribution.get(0).getFailCount());
        assertEquals(0L, distribution.get(1).getSuccessCount());
        assertEquals(0L, distribution.get(2).getSuccessCount());
    }

    @Test
    @DisplayName("应该按状态码聚合并计算今日成功率")
    void should_calculate_today_stats_and_success_rate() {
        // given: 3 成功 / 1 失败 / 1 执行中
        when(executionLogManager.countByStatus(any(), any())).thenReturn(Map.of(1, 3L, 2, 1L, 3, 1L));
        when(executionLogManager.countDailyByStatus(any(), any())).thenReturn(List.of());

        // when
        JobDashboardVO vo = dashboardService.overview(7);

        // then
        assertEquals(5L, vo.getTodayTriggerCount());
        assertEquals(3L, vo.getTodaySuccessCount());
        assertEquals(1L, vo.getTodayFailCount());
        assertEquals(75.0, vo.getTodaySuccessRate());
    }

    @Test
    @DisplayName("没有任何执行记录时成功率应该为 0 且状态分布覆盖全部枚举")
    void should_return_zero_success_rate_and_full_status_enum_when_no_records() {
        // given
        when(executionLogManager.countByStatus(any(), any())).thenReturn(Map.of());
        when(executionLogManager.countDailyByStatus(any(), any())).thenReturn(List.of());

        // when
        JobDashboardVO vo = dashboardService.overview(7);

        // then
        assertEquals(0.0, vo.getTodaySuccessRate());
        Set<Integer> statusCodes = vo.getStatusDistribution().stream()
            .map(JobDashboardVO.StatusDistribution::getStatus).collect(Collectors.toSet());
        assertEquals(
            Set.of(JobExecutionStatusEnum.SUCCESS.getCode(), JobExecutionStatusEnum.FAILED.getCode(),
                JobExecutionStatusEnum.RUNNING.getCode()),
            statusCodes
        );
        assertEquals(List.of(0L, 0L, 0L),
            vo.getStatusDistribution().stream().map(JobDashboardVO.StatusDistribution::getCount).toList());
    }

    @Test
    @DisplayName("应该只统计存活节点")
    void should_count_alive_nodes_only() {
        // given
        SchedulerNodeVO alive = new SchedulerNodeVO().setInstanceId("node-1").setAlive(true);
        SchedulerNodeVO dead = new SchedulerNodeVO().setInstanceId("node-2").setAlive(false);
        SchedulerNodeVO unknown = new SchedulerNodeVO().setInstanceId("node-3");
        when(monitorService.listNodes()).thenReturn(List.of(alive, dead, unknown));
        when(executionLogManager.countByStatus(any(), any())).thenReturn(Map.of());
        when(executionLogManager.countDailyByStatus(any(), any())).thenReturn(List.of());

        // when
        JobDashboardVO vo = dashboardService.overview(7);

        // then
        assertEquals(1L, vo.getOnlineNodeCount());
    }

    private Map<String, Object> dailyRow(String date, Long successCount, Long failCount) {
        Map<String, Object> row = new HashMap<>();
        row.put("date", date);
        row.put("success_count", successCount);
        row.put("fail_count", failCount);
        return row;
    }

}
