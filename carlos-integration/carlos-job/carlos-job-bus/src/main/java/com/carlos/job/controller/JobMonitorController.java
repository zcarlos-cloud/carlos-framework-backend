package com.carlos.job.controller;

import com.carlos.job.pojo.vo.SchedulerMetaVO;
import com.carlos.job.pojo.vo.SchedulerNodeVO;
import com.carlos.job.pojo.vo.ServiceInstanceVO;
import com.carlos.job.service.JobMonitorService;
import com.carlos.log.annotation.Log;
import com.carlos.log.enums.BusinessType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 调度器运维 rest服务接口（集群节点、调度器元数据、待机/启动、目标服务实例）
 * </p>
 *
 * @author Carlos
 * @date 2026年9月23日
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("job/monitor")
@Tag(name = "调度器运维")
public class JobMonitorController {

    private final JobMonitorService monitorService;

    @PreAuthorize("hasPermission('job:monitor', 'view')")
    @GetMapping("schedulerMeta")
    @Operation(summary = "调度器元数据")
    public SchedulerMetaVO schedulerMeta() {
        return monitorService.getSchedulerMeta();
    }

    @PreAuthorize("hasPermission('job:monitor', 'view')")
    @GetMapping("nodes")
    @Operation(summary = "调度器集群节点列表")
    public List<SchedulerNodeVO> nodes() {
        return monitorService.listNodes();
    }

    @PreAuthorize("hasPermission('job:monitor', 'view')")
    @GetMapping("services")
    @Operation(summary = "可调度目标服务清单")
    public List<String> services() {
        return monitorService.listServices();
    }

    @PreAuthorize("hasPermission('job:monitor', 'view')")
    @GetMapping("serviceInstances")
    @Operation(summary = "目标服务在线实例")
    @Parameter(name = "serviceName", description = "服务名", required = true)
    public List<ServiceInstanceVO> serviceInstances(@RequestParam String serviceName) {
        return monitorService.listServiceInstances(serviceName);
    }

    @Log(
        title = "调度器运维",
        logType = "JOB_SCHEDULER_STANDBY",
        businessType = BusinessType.UPDATE_STATUS,
        operation = "'调度器待机(暂停所有触发)'",
        targetType = "JOB_SCHEDULER",
        riskLevel = 80
    )
    @PreAuthorize("hasPermission('job:monitor', 'standby')")
    @PostMapping("standby")
    @Operation(summary = "调度器待机(暂停所有触发)")
    public void standby() {
        monitorService.standby();
    }

    @Log(
        title = "调度器运维",
        logType = "JOB_SCHEDULER_START",
        businessType = BusinessType.UPDATE_STATUS,
        operation = "'调度器从待机恢复'",
        targetType = "JOB_SCHEDULER",
        riskLevel = 60
    )
    @PreAuthorize("hasPermission('job:monitor', 'start')")
    @PostMapping("start")
    @Operation(summary = "调度器从待机恢复")
    public void start() {
        monitorService.start();
    }

    @Log(
        title = "调度器运维",
        logType = "JOB_SCHEDULER_PAUSE_ALL",
        businessType = BusinessType.UPDATE_STATUS,
        operation = "'暂停全部任务触发'",
        targetType = "JOB_SCHEDULER",
        riskLevel = 70
    )
    @PreAuthorize("hasPermission('job:monitor', 'pauseAll')")
    @PostMapping("pauseAll")
    @Operation(summary = "暂停全部任务触发")
    public void pauseAll() {
        monitorService.pauseAll();
    }

    @Log(
        title = "调度器运维",
        logType = "JOB_SCHEDULER_RESUME_ALL",
        businessType = BusinessType.UPDATE_STATUS,
        operation = "'恢复全部任务触发'",
        targetType = "JOB_SCHEDULER",
        riskLevel = 60
    )
    @PreAuthorize("hasPermission('job:monitor', 'resumeAll')")
    @PostMapping("resumeAll")
    @Operation(summary = "恢复全部任务触发")
    public void resumeAll() {
        monitorService.resumeAll();
    }

}
