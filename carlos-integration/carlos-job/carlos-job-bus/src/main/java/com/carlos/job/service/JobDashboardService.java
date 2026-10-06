package com.carlos.job.service;

import com.carlos.job.manager.JobExecutionLogManager;
import com.carlos.job.manager.JobInfoManager;
import com.carlos.job.pojo.enums.JobExecutionStatusEnum;
import com.carlos.job.pojo.vo.JobDashboardVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 调度运行报表 业务（Dashboard 聚合）
 * </p>
 * <p>
 * 数据源：job_execution_log（趋势/成功率/今日统计）+ Quartz Scheduler（在跑任务）+ QRTZ 节点。
 * </p>
 *
 * @author Carlos
 * @date 2026年9月23日
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobDashboardService {

    /**
     * 日期分布默认天数
     */
    private static final int DEFAULT_DAYS = 7;

    private final JobExecutionLogManager executionLogManager;

    private final JobInfoManager jobInfoManager;

    private final JobMonitorService monitorService;

    private final Scheduler scheduler;

    /**
     * 聚合运行报表
     *
     * @param days 日期分布天数（<=0 默认 7）
     * @return 报表视图
     */
    public JobDashboardVO overview(int days) {
        int rangeDays = days <= 0 ? DEFAULT_DAYS : Math.min(days, 90);
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime now = LocalDateTime.now();

        JobDashboardVO vo = new JobDashboardVO()
            .setJobCount(jobInfoManager.count())
            .setOnlineNodeCount(countAliveNodes())
            .setRunningJobCount(runningJobCount());

        // 今日统计
        Map<Integer, Long> todayStatus = executionLogManager.countByStatus(todayStart, now);
        long todaySuccess = todayStatus.getOrDefault(JobExecutionStatusEnum.SUCCESS.getCode(), 0L);
        long todayFail = todayStatus.getOrDefault(JobExecutionStatusEnum.FAILED.getCode(), 0L);
        long todayRunning = todayStatus.getOrDefault(JobExecutionStatusEnum.RUNNING.getCode(), 0L);
        long todayTotal = todaySuccess + todayFail + todayRunning;
        vo.setTodayTriggerCount(todayTotal)
            .setTodaySuccessCount(todaySuccess)
            .setTodayFailCount(todayFail)
            .setTodaySuccessRate(successRate(todaySuccess, todaySuccess + todayFail));

        // 日期分布（近 N 天）
        vo.setDateDistribution(buildDateDistribution(rangeDays, now));

        // 状态分布（近 N 天）
        vo.setStatusDistribution(buildStatusDistribution(rangeDays, now));
        return vo;
    }

    private List<JobDashboardVO.DateDistribution> buildDateDistribution(int days, LocalDateTime now) {
        LocalDateTime start = LocalDateTime.of(LocalDate.now().minusDays(days - 1L), LocalTime.MIN);
        List<Map<String, Object>> rows = executionLogManager.countDailyByStatus(start, now);
        Map<String, Map<String, Object>> byDate = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object date = row.get("date");
            if (date != null) {
                byDate.put(date.toString(), row);
            }
        }
        List<JobDashboardVO.DateDistribution> result = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            String date = LocalDate.now().minusDays(i).toString();
            Map<String, Object> row = byDate.get(date);
            result.add(new JobDashboardVO.DateDistribution()
                .setDate(date)
                .setSuccessCount(row == null ? 0L : toLong(row.get("success_count")))
                .setFailCount(row == null ? 0L : toLong(row.get("fail_count"))));
        }
        return result;
    }

    private List<JobDashboardVO.StatusDistribution> buildStatusDistribution(int days, LocalDateTime now) {
        LocalDateTime start = LocalDateTime.of(LocalDate.now().minusDays(days - 1L), LocalTime.MIN);
        Map<Integer, Long> byStatus = executionLogManager.countByStatus(start, now);
        List<JobDashboardVO.StatusDistribution> result = new ArrayList<>();
        for (JobExecutionStatusEnum status : JobExecutionStatusEnum.values()) {
            result.add(new JobDashboardVO.StatusDistribution()
                .setStatus(status.getCode())
                .setStatusName(status.getDesc())
                .setCount(byStatus.getOrDefault(status.getCode(), 0L)));
        }
        return result;
    }

    private long countAliveNodes() {
        return monitorService.listNodes().stream()
            .filter(node -> Boolean.TRUE.equals(node.getAlive()))
            .count();
    }

    private int runningJobCount() {
        try {
            return scheduler.getCurrentlyExecutingJobs().size();
        } catch (SchedulerException e) {
            log.warn("查询在跑任务数失败: {}", e.getMessage());
            return 0;
        }
    }

    private double successRate(long success, long total) {
        if (total <= 0) {
            return 0.0;
        }
        return Math.round(success * 10000.0 / total) / 100.0;
    }

    private Long toLong(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }

}
