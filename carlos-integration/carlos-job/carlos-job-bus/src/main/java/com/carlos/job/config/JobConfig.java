package com.carlos.job.config;

import com.carlos.job.listener.JobAlarmListener;
import com.carlos.job.listener.JobRetryListener;
import com.carlos.job.manager.JobInfoManager;
import com.carlos.message.api.MessageSendApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <p>
 * 调度中心配置
 * </p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(JobProperties.class)
public class JobConfig {

    private final Scheduler scheduler;

    private final JobInfoManager jobInfoManager;

    private final JobProperties jobProperties;

    /**
     * 调度器就绪后注册全局监听器（失败重试、失败报警）
     *
     * @param messageSendApiProvider 消息中心 Feign（可选，未启用消息服务时降级）
     * @return 初始化回调
     */
    @Bean
    public SmartInitializingSingleton registerJobListeners(ObjectProvider<MessageSendApi> messageSendApiProvider) {
        return () -> {
            try {
                scheduler.getListenerManager().addJobListener(new JobRetryListener(scheduler, jobInfoManager));
                scheduler.getListenerManager()
                    .addJobListener(new JobAlarmListener(jobProperties, jobInfoManager, messageSendApiProvider));
                log.info("Job listeners registered: {}, {}", JobRetryListener.NAME, JobAlarmListener.NAME);
            } catch (SchedulerException e) {
                // 监听器注册失败不阻断启动，仅降级（无失败重试/报警）
                log.error("Register job listeners fail", e);
            }
        };
    }

}
