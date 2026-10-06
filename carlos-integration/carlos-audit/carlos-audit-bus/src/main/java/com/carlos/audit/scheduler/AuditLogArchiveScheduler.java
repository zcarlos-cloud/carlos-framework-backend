package com.carlos.audit.scheduler;

import com.carlos.audit.service.AuditLogArchiveService;
import com.carlos.job.client.annotation.AutoRegisterJob;
import com.carlos.job.client.annotation.ScheduleJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 审计日志冷数据归档定时任务
 *
 * <p>默认每天凌晨 2 点执行，将超过冷数据天数的 ClickHouse 分区归档到对象存储。
 * 由 carlos-job 调度中心派发，调度周期以 job_info 为准（库是唯一真相源），
 * 默认 cron 对应原配置 carlos.audit.archive.cron 的缺省值。</p>
 *
 * @author Carlos
 * @date 2026-06-12
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "carlos.audit.archive", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AuditLogArchiveScheduler {

    private final AuditLogArchiveService archiveService;

    /**
     * 每日凌晨 2 点执行冷数据归档
     */
    @ScheduleJob("auditLogArchiveHandler")
    @AutoRegisterJob(cron = "0 0 2 * * ?", description = "审计日志冷数据归档（ClickHouse 分区转储对象存储）", author = "carlos")
    public void archiveDaily() {
        log.info("审计日志冷数据归档定时任务开始执行");
        try {
            archiveService.archiveColdData();
        } catch (Exception e) {
            log.error("审计日志冷数据归档定时任务执行失败", e);
        }
        log.info("审计日志冷数据归档定时任务执行结束");
    }
}
