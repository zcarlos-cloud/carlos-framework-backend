package com.carlos.job.controller;

import com.carlos.core.pagination.Paging;
import com.carlos.job.convert.JobExecutionLogConvert;
import com.carlos.job.pojo.param.JobLogPageParam;
import com.carlos.job.pojo.vo.JobExecutionLogVO;
import com.carlos.job.service.JobExecutionLogService;
import com.carlos.log.annotation.Log;
import com.carlos.log.enums.BusinessType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 任务执行日志 rest服务接口
 * </p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("job/log")
@Tag(name = "任务执行日志")
public class JobLogController {

    public static final String BASE_NAME = "任务执行日志";

    private final JobExecutionLogService executionLogService;

    @PreAuthorize("hasPermission('job:log', 'view')")
    @GetMapping("detail")
    @Operation(summary = BASE_NAME + "详情")
    public JobExecutionLogVO detail(@RequestParam Long id) {
        return JobExecutionLogConvert.INSTANCE.toVO(executionLogService.getLogDetail(id));
    }

    @PreAuthorize("hasPermission('job:log', 'view')")
    @GetMapping("page")
    @Operation(summary = BASE_NAME + "分页列表")
    public Paging<JobExecutionLogVO> page(JobLogPageParam param) {
        return executionLogService.getPage(param);
    }

    @Log(
        title = "任务执行日志",
        logType = "JOB_LOG_CLEAN",
        businessType = BusinessType.CLEAN,
        operation = "'清理任务执行日志, 保留天数: ' + #retainDays",
        targetType = "JOB_EXECUTION_LOG",
        riskLevel = 70
    )
    @PreAuthorize("hasPermission('job:log', 'clean')")
    @PostMapping("clean")
    @Operation(summary = "清理历史日志")
    public int clean(@RequestParam(defaultValue = "30") @Min(1) @Max(3650) Integer retainDays) {
        return executionLogService.cleanLogs(retainDays);
    }

}
