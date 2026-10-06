package com.carlos.job.controller;

import com.carlos.job.pojo.vo.JobDashboardVO;
import com.carlos.job.pojo.vo.SchedulerMetaVO;
import com.carlos.job.pojo.vo.SchedulerNodeVO;
import com.carlos.job.pojo.vo.ServiceInstanceVO;
import com.carlos.job.service.JobDashboardService;
import com.carlos.job.service.JobMonitorService;
import com.carlos.job.service.JobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <p>
 * 定时任务 REST 接口测试（standalone MockMvc，不启动 Spring 上下文）
 * </p>
 * <p>
 * 覆盖 J4/J5 新增端点：触发时间预览、调度器运维（元数据/节点/服务实例/全局控制）、运行报表。
 * standalone 模式下无 ResponseWrapAdvice 包装，断言针对裸 VO 的 JSON 结构。
 * </p>
 *
 * @author Carlos
 * @date 2026年9月23日
 */
@ExtendWith(MockitoExtension.class)
class JobControllerApiTest {

    @Mock
    private JobService jobService;

    @Mock
    private JobMonitorService monitorService;

    @Mock
    private JobDashboardService dashboardService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
            new JobController(jobService),
            new JobMonitorController(monitorService),
            new JobDashboardController(dashboardService)
        ).build();
    }

    @Test
    @DisplayName("预览触发时间应返回 ISO 时间数组")
    void should_preview_fire_times() throws Exception {
        List<LocalDateTime> times = List.of(LocalDateTime.of(2026, 9, 24, 2, 0), LocalDateTime.of(2026, 9, 25, 2, 0));
        when(jobService.previewFireTimes(eq(1), eq("0 0 2 * * ?"), eq(5))).thenReturn(times);

        mockMvc.perform(get("/job/previewFireTimes")
                .param("scheduleType", "1")
                .param("scheduleConf", "0 0 2 * * ?")
                .param("count", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0]").value("2026-09-24T02:00:00"))
            .andExpect(jsonPath("$[1]").value("2026-09-25T02:00:00"));
    }

    @Test
    @DisplayName("调度器元数据应返回 Quartz SchedulerMetaData 字段")
    void should_return_scheduler_meta() throws Exception {
        when(monitorService.getSchedulerMeta()).thenReturn(new SchedulerMetaVO()
            .setSchedulerName("carlosJobScheduler")
            .setInstanceId("node1")
            .setClustered(true)
            .setStandby(false)
            .setThreadPoolSize(10));

        mockMvc.perform(get("/job/monitor/schedulerMeta"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.schedulerName").value("carlosJobScheduler"))
            .andExpect(jsonPath("$.clustered").value(true))
            .andExpect(jsonPath("$.standby").value(false))
            .andExpect(jsonPath("$.threadPoolSize").value(10));
    }

    @Test
    @DisplayName("节点列表应返回 QRTZ_SCHEDULER_STATE 视图")
    void should_return_nodes() throws Exception {
        when(monitorService.listNodes()).thenReturn(List.of(new SchedulerNodeVO()
            .setSchedulerName("carlosJobScheduler")
            .setInstanceId("node1")
            .setLastCheckinTime(1727000000000L)
            .setCheckinInterval(10000L)
            .setAlive(true)));

        mockMvc.perform(get("/job/monitor/nodes"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].instanceId").value("node1"))
            .andExpect(jsonPath("$[0].alive").value(true));
    }

    @Test
    @DisplayName("服务实例查询缺少 serviceName 应返回 400")
    void should_reject_service_instances_without_name() throws Exception {
        mockMvc.perform(get("/job/monitor/serviceInstances"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("服务实例查询应返回实例列表")
    void should_return_service_instances() throws Exception {
        when(monitorService.listServiceInstances("carlos-message")).thenReturn(List.of(new ServiceInstanceVO()
            .setServiceName("carlos-message")
            .setAddress("192.168.3.40:9511")
            .setHealthy(true)));

        mockMvc.perform(get("/job/monitor/serviceInstances").param("serviceName", "carlos-message"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].address").value("192.168.3.40:9511"));
    }

    @Test
    @DisplayName("调度器全局控制应调用对应服务方法")
    void should_invoke_scheduler_controls() throws Exception {
        mockMvc.perform(post("/job/monitor/standby")).andExpect(status().isOk());
        mockMvc.perform(post("/job/monitor/start")).andExpect(status().isOk());
        mockMvc.perform(post("/job/monitor/pauseAll")).andExpect(status().isOk());
        mockMvc.perform(post("/job/monitor/resumeAll")).andExpect(status().isOk());

        verify(monitorService).standby();
        verify(monitorService).start();
        verify(monitorService).pauseAll();
        verify(monitorService).resumeAll();
    }

    @Test
    @DisplayName("运行报表默认按 7 天聚合")
    void should_dashboard_default_7_days() throws Exception {
        when(dashboardService.overview(7)).thenReturn(new JobDashboardVO().setJobCount(3L).setTodaySuccessRate(100.0));

        mockMvc.perform(get("/job/dashboard/overview").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.jobCount").value(3))
            .andExpect(jsonPath("$.todaySuccessRate").value(100.0));

        verify(dashboardService).overview(7);
    }
}
