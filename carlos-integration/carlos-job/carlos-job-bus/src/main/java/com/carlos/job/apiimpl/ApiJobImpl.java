package com.carlos.job.apiimpl;

import com.carlos.core.response.Result;
import com.carlos.job.api.ApiJob;
import com.carlos.job.pojo.ao.JobInfoAO;
import com.carlos.job.pojo.param.ApiJobCreateParam;
import com.carlos.job.pojo.param.JobCreateParam;
import com.carlos.job.pojo.vo.JobVO;
import com.carlos.job.service.JobService;
import com.carlos.log.annotation.Log;
import com.carlos.log.enums.BusinessType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 定时任务管理 API 实现（服务间编程式调用）
 * </p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
@Slf4j
@RestController
@RequestMapping("/api/job")
@RequiredArgsConstructor
@Tag(name = "定时任务管理（服务间调用）")
public class ApiJobImpl implements ApiJob {

    private final JobService jobService;

    @Override
    @Log(
        title = "定时任务",
        logType = "JOB_API_CREATE",
        businessType = BusinessType.INSERT,
        operation = "'服务间调用-新增定时任务: ' + #param.jobName",
        targetType = "JOB_INFO",
        targetId = "#param.jobName",
        riskLevel = 25
    )
    @PostMapping("/create")
    @Operation(summary = "编程式创建任务")
    public Result<Boolean> create(@RequestBody ApiJobCreateParam param) {
        JobCreateParam createParam = new JobCreateParam();
        BeanUtils.copyProperties(param, createParam);
        jobService.addJob(createParam);
        return Result.success(Boolean.TRUE);
    }

    @Override
    @Log(
        title = "定时任务",
        logType = "JOB_API_RUN_ONCE",
        businessType = BusinessType.OTHER,
        operation = "'服务间调用-手动触发任务: ' + #jobName",
        targetType = "JOB_INFO",
        targetId = "#jobName",
        riskLevel = 40
    )
    @PostMapping("/runOnce")
    @Operation(summary = "手动触发一次")
    public Result<Boolean> runOnce(@RequestParam("jobName") String jobName,
                                   @RequestParam(value = "jobGroup", required = false) String jobGroup) {
        jobService.runOnce(jobName, jobGroup);
        return Result.success(Boolean.TRUE);
    }

    @Override
    @Log(
        title = "定时任务",
        logType = "JOB_API_PAUSE",
        businessType = BusinessType.UPDATE_STATUS,
        operation = "'服务间调用-暂停定时任务: ' + #jobName",
        targetType = "JOB_INFO",
        targetId = "#jobName",
        riskLevel = 40
    )
    @PostMapping("/pause")
    @Operation(summary = "暂停任务")
    public Result<Boolean> pause(@RequestParam("jobName") String jobName,
                                 @RequestParam(value = "jobGroup", required = false) String jobGroup) {
        jobService.pauseJob(jobName, jobGroup);
        return Result.success(Boolean.TRUE);
    }

    @Override
    @Log(
        title = "定时任务",
        logType = "JOB_API_RESUME",
        businessType = BusinessType.UPDATE_STATUS,
        operation = "'服务间调用-恢复定时任务: ' + #jobName",
        targetType = "JOB_INFO",
        targetId = "#jobName",
        riskLevel = 40
    )
    @PostMapping("/resume")
    @Operation(summary = "恢复任务")
    public Result<Boolean> resume(@RequestParam("jobName") String jobName,
                                  @RequestParam(value = "jobGroup", required = false) String jobGroup) {
        jobService.resumeJob(jobName, jobGroup);
        return Result.success(Boolean.TRUE);
    }

    @Override
    @Log(
        title = "定时任务",
        logType = "JOB_API_DELETE",
        businessType = BusinessType.DELETE,
        operation = "'服务间调用-删除定时任务: ' + #jobName",
        targetType = "JOB_INFO",
        targetId = "#jobName",
        riskLevel = 70
    )
    @PostMapping("/delete")
    @Operation(summary = "删除任务")
    public Result<Boolean> delete(@RequestParam("jobName") String jobName,
                                  @RequestParam(value = "jobGroup", required = false) String jobGroup) {
        jobService.deleteJob(jobName, jobGroup);
        return Result.success(Boolean.TRUE);
    }

    @Override
    @GetMapping("/info")
    @Operation(summary = "查询任务信息")
    public Result<JobInfoAO> info(@RequestParam("jobName") String jobName,
                                  @RequestParam(value = "jobGroup", required = false) String jobGroup) {
        JobVO vo = jobService.getJobInfo(jobName, jobGroup);
        JobInfoAO ao = new JobInfoAO();
        BeanUtils.copyProperties(vo, ao);
        return Result.success(ao);
    }

}
