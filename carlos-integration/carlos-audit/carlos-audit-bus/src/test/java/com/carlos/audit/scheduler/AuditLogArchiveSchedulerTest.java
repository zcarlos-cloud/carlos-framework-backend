package com.carlos.audit.scheduler;

import com.carlos.audit.service.AuditLogArchiveService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * {@link AuditLogArchiveScheduler} 单元测试
 *
 * @author Carlos
 * @date 2026-06-12
 */
@ExtendWith(MockitoExtension.class)
class AuditLogArchiveSchedulerTest {

    @Mock
    private AuditLogArchiveService archiveService;

    @InjectMocks
    private AuditLogArchiveScheduler scheduler;

    @Test
    @DisplayName("archiveDaily：应调用归档服务执行冷数据归档")
    void should_call_archive_service_when_archive_daily() {
        scheduler.archiveDaily();

        verify(archiveService, times(1)).archiveColdData();
    }
}
