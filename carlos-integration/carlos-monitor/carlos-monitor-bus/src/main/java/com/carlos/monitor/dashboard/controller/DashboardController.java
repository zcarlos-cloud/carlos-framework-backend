package com.carlos.monitor.dashboard.controller;

import com.carlos.core.response.Result;
import com.carlos.monitor.dashboard.pojo.vo.AbnormalVO;
import com.carlos.monitor.dashboard.pojo.vo.AppDashboardVO;
import com.carlos.monitor.dashboard.pojo.vo.DashboardSummaryVO;
import com.carlos.monitor.dashboard.pojo.vo.DashboardTrendVO;
import com.carlos.monitor.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 监控大盘聚合 rest服务接口
 * </p>
 *
 * @author Carlos
 * @date 2026年10月5日
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/monitor/dashboard")
@Tag(name = "监控大盘")
public class DashboardController {

    private final DashboardService dashboardService;

    @PreAuthorize("hasPermission('monitor:dashboard', 'view')")
    @GetMapping("/summary")
    @Operation(summary = "大盘汇总", description = "实例总数/健康/异常（按类型）、当前告警数（按级别）、采集状态统计")
    public Result<DashboardSummaryVO> summary() {
        return Result.success(dashboardService.summary());
    }

    @PreAuthorize("hasPermission('monitor:dashboard', 'view')")
    @GetMapping("/trend")
    @Operation(summary = "跨实例指标趋势", description = "Top N 实例关键指标小时聚合趋势")
    public Result<DashboardTrendVO> trend(@RequestParam("metric") String metric,
                                          @RequestParam(value = "range", defaultValue = "24h") String range,
                                          @RequestParam(value = "topN", defaultValue = "5") int topN) {
        return Result.success(dashboardService.trend(metric, range, topN));
    }

    @PreAuthorize("hasPermission('monitor:dashboard', 'view')")
    @GetMapping("/app/{appName}")
    @Operation(summary = "应用视角大盘", description = "关联实例列表 + 健康 + 各实例关键当前值")
    public Result<AppDashboardVO> appDetail(@PathVariable("appName") String appName) {
        return Result.success(dashboardService.appDetail(appName));
    }

    @PreAuthorize("hasPermission('monitor:dashboard', 'view')")
    @GetMapping("/abnormal")
    @Operation(summary = "异常滚动", description = "异常实例 + 最新未恢复告警滚动列表")
    public Result<AbnormalVO> abnormal(@RequestParam(value = "alarmLimit", defaultValue = "20") int alarmLimit) {
        return Result.success(dashboardService.abnormal(alarmLimit));
    }
}
