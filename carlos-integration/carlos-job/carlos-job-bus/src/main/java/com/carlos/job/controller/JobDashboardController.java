package com.carlos.job.controller;

import com.carlos.job.pojo.vo.JobDashboardVO;
import com.carlos.job.service.JobDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 调度运行报表 rest服务接口
 * </p>
 *
 * @author Carlos
 * @date 2026年9月23日
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("job/dashboard")
@Tag(name = "调度运行报表")
public class JobDashboardController {

    private final JobDashboardService dashboardService;

    @PreAuthorize("hasPermission('job:dashboard', 'view')")
    @GetMapping("overview")
    @Operation(summary = "运行报表总览")
    @Parameter(name = "days", description = "日期分布天数，默认 7，上限 90")
    public JobDashboardVO overview(@RequestParam(required = false, defaultValue = "7") int days) {
        return dashboardService.overview(days);
    }

}
