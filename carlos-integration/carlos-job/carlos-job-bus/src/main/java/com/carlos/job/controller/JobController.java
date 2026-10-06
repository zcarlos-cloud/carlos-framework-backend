package com.carlos.job.controller;

import com.carlos.core.pagination.Paging;
import com.carlos.job.pojo.param.JobCreateParam;
import com.carlos.job.pojo.param.JobPageParam;
import com.carlos.job.pojo.param.JobUpdateParam;
import com.carlos.job.pojo.vo.JobVO;
import com.carlos.job.service.JobService;
import com.carlos.log.annotation.Log;
import com.carlos.log.enums.BusinessType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 定时任务 rest服务接口
 * </p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("job")
@Tag(name = "定时任务")
public class JobController {

    public static final String BASE_NAME = "定时任务";

    private final JobService jobService;

    @Log(
        title = "定时任务",
        logType = "JOB_CREATE",
        businessType = BusinessType.INSERT,
        operation = "'新增定时任务: ' + #param.jobName",
        targetType = "JOB_INFO",
        targetId = "#param.jobName",
        riskLevel = 25
    )
    @PreAuthorize("hasPermission('job:list', 'create')")
    @PostMapping("create")
    @Operation(summary = "新增" + BASE_NAME)
    public void create(@RequestBody @Validated JobCreateParam param) {
        jobService.addJob(param);
    }

    @Log(
        title = "定时任务",
        logType = "JOB_UPDATE",
        businessType = BusinessType.UPDATE,
        operation = "'修改定时任务: ' + #param.jobName",
        targetType = "JOB_INFO",
        targetId = "#param.jobName",
        riskLevel = 30
    )
    @PreAuthorize("hasPermission('job:list', 'update')")
    @PostMapping("update")
    @Operation(summary = "更新" + BASE_NAME)
    public void update(@RequestBody @Validated JobUpdateParam param) {
        jobService.updateJob(param);
    }

    @Log(
        title = "定时任务",
        logType = "JOB_PAUSE",
        businessType = BusinessType.UPDATE_STATUS,
        operation = "'暂停定时任务: ' + #jobName",
        targetType = "JOB_INFO",
        targetId = "#jobName",
        riskLevel = 40
    )
    @PreAuthorize("hasPermission('job:list', 'changeState')")
    @PostMapping("pause")
    @Operation(summary = "暂停" + BASE_NAME)
    @Parameter(name = "jobName", description = "任务名称", required = true)
    public void pause(@RequestParam String jobName,
                      @RequestParam(required = false) String jobGroup) {
        jobService.pauseJob(jobName, jobGroup);
    }

    @Log(
        title = "定时任务",
        logType = "JOB_RESUME",
        businessType = BusinessType.UPDATE_STATUS,
        operation = "'恢复定时任务: ' + #jobName",
        targetType = "JOB_INFO",
        targetId = "#jobName",
        riskLevel = 40
    )
    @PreAuthorize("hasPermission('job:list', 'changeState')")
    @PostMapping("resume")
    @Operation(summary = "恢复" + BASE_NAME)
    @Parameter(name = "jobName", description = "任务名称", required = true)
    public void resume(@RequestParam String jobName,
                       @RequestParam(required = false) String jobGroup) {
        jobService.resumeJob(jobName, jobGroup);
    }

    @Log(
        title = "定时任务",
        logType = "JOB_RUN_ONCE",
        businessType = BusinessType.OTHER,
        operation = "'手动触发任务: ' + #jobName",
        targetType = "JOB_INFO",
        targetId = "#jobName",
        riskLevel = 40
    )
    @PreAuthorize("hasPermission('job:list', 'trigger')")
    @PostMapping("runOnce")
    @Operation(summary = "手动触发一次" + BASE_NAME)
    @Parameter(name = "jobName", description = "任务名称", required = true)
    public void runOnce(@RequestParam String jobName,
                        @RequestParam(required = false) String jobGroup) {
        jobService.runOnce(jobName, jobGroup);
    }

    @Log(
        title = "定时任务",
        logType = "JOB_DELETE",
        businessType = BusinessType.DELETE,
        operation = "'删除定时任务: ' + #jobName",
        targetType = "JOB_INFO",
        targetId = "#jobName",
        riskLevel = 70
    )
    @PreAuthorize("hasPermission('job:list', 'delete')")
    @PostMapping("delete")
    @Operation(summary = "删除" + BASE_NAME)
    @Parameter(name = "jobName", description = "任务名称", required = true)
    public void delete(@RequestParam String jobName,
                       @RequestParam(required = false) String jobGroup) {
        jobService.deleteJob(jobName, jobGroup);
    }

    @PreAuthorize("hasPermission('job:list', 'view')")
    @GetMapping("detail")
    @Operation(summary = BASE_NAME + "详情")
    @Parameter(name = "jobName", description = "任务名称", required = true)
    public JobVO detail(@RequestParam String jobName,
                        @RequestParam(required = false) String jobGroup) {
        return jobService.getJobInfo(jobName, jobGroup);
    }

    @PreAuthorize("hasPermission('job:list', 'view')")
    @GetMapping("page")
    @Operation(summary = BASE_NAME + "分页列表")
    public Paging<JobVO> page(JobPageParam param) {
        return jobService.getPage(param);
    }

    @PreAuthorize("hasPermission('job:list', 'view')")
    @GetMapping("previewFireTimes")
    @Operation(summary = "预览未来触发时间")
    @Parameter(name = "scheduleType", description = "调度类型: 1-CRON 2-固定间隔 3-一次性")
    @Parameter(name = "scheduleConf", description = "调度配置: cron 表达式/间隔毫秒/触发时间(ISO)")
    @Parameter(name = "count", description = "预览条数，默认 5，上限 20")
    public List<LocalDateTime> previewFireTimes(@RequestParam(required = false) Integer scheduleType,
                                                @RequestParam String scheduleConf,
                                                @RequestParam(required = false, defaultValue = "5") int count) {
        return jobService.previewFireTimes(scheduleType, scheduleConf, count);
    }

}
