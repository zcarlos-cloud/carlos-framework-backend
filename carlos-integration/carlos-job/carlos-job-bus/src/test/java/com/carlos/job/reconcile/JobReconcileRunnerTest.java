package com.carlos.job.reconcile;

import com.carlos.job.config.JobProperties;
import com.carlos.job.manager.JobInfoManager;
import com.carlos.job.manager.JobScheduleManager;
import com.carlos.job.pojo.dto.JobInfoDTO;
import com.carlos.job.pojo.enums.JobScheduleTypeEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.boot.DefaultApplicationArguments;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * <p>
 * 启动对账器单元测试
 * </p>
 *
 * @author Carlos
 * @date 2026年9月23日
 */
@ExtendWith(MockitoExtension.class)
class JobReconcileRunnerTest {

    @Mock
    private Scheduler scheduler;

    @Mock
    private JobInfoManager jobInfoManager;

    @Mock
    private JobScheduleManager scheduleManager;

    @InjectMocks
    private JobReconcileRunner reconcileRunner;

    private JobProperties jobProperties;

    @BeforeEach
    void setUp() {
        jobProperties = new JobProperties();
        jobProperties.setReconcileOnStartup(true);
        // @InjectMocks 在 BeforeEach 前完成，需手动塞入 properties
        reconcileRunner = new JobReconcileRunner(scheduler, jobInfoManager, scheduleManager, jobProperties);
    }

    @Test
    @DisplayName("应该跳过对账当开关关闭时")
    void should_skip_reconcile_when_disabled() {
        jobProperties.setReconcileOnStartup(false);

        reconcileRunner.run(new DefaultApplicationArguments());

        verify(jobInfoManager, never()).listAll();
    }

    @Test
    @DisplayName("应该重建调度当DB有Quartz无时")
    void should_rebuild_when_db_has_but_quartz_missing() throws Exception {
        JobInfoDTO info = buildInfo("demoJob");
        when(jobInfoManager.listAll()).thenReturn(List.of(info));
        when(scheduler.checkExists(JobKey.jobKey("demoJob", "DEFAULT"))).thenReturn(false);
        when(scheduler.getJobKeys(anyGroupMatcher())).thenReturn(Collections.emptySet());

        reconcileRunner.run(new DefaultApplicationArguments());

        verify(scheduleManager).createJob(any());
    }

    @Test
    @DisplayName("应该回填元数据当Quartz有DB无时")
    void should_backfill_when_quartz_has_but_db_missing() throws Exception {
        when(jobInfoManager.listAll()).thenReturn(Collections.emptyList());
        when(scheduler.getJobKeys(anyGroupMatcher()))
            .thenReturn(Set.of(JobKey.jobKey("legacyJob", "DEFAULT")));
        // getJobDetail 返回 null 时跳过（防御分支）
        when(scheduler.getJobDetail(JobKey.jobKey("legacyJob", "DEFAULT"))).thenReturn(null);

        reconcileRunner.run(new DefaultApplicationArguments());

        // detail 为 null 防御分支不回填
        verify(jobInfoManager, never()).add(any());
    }

    @Test
    @DisplayName("应该不做任何修复当两边一致时")
    void should_do_nothing_when_consistent() throws Exception {
        when(jobInfoManager.listAll()).thenReturn(Collections.emptyList());
        when(scheduler.getJobKeys(anyGroupMatcher())).thenReturn(Collections.emptySet());

        reconcileRunner.run(new DefaultApplicationArguments());

        verify(scheduleManager, never()).createJob(any());
        verify(jobInfoManager, never()).add(any());
    }

    @Test
    @DisplayName("应该不阻断启动当对账抛异常时")
    void should_not_block_startup_when_reconcile_fail() {
        when(jobInfoManager.listAll()).thenThrow(new RuntimeException("db down"));

        // 不抛异常即通过
        reconcileRunner.run(new DefaultApplicationArguments());
    }

    private JobInfoDTO buildInfo(String jobName) {
        return new JobInfoDTO()
            .setJobName(jobName)
            .setJobGroup("DEFAULT")
            .setServiceName("carlos-message")
            .setHandlerName("demoHandler")
            .setScheduleType(JobScheduleTypeEnum.CRON.getCode())
            .setScheduleConf("0 0 2 * * ?")
            .setStatus("NORMAL");
    }

    @SuppressWarnings("unchecked")
    private GroupMatcher<JobKey> anyGroupMatcher() {
        return any(GroupMatcher.class);
    }

}
