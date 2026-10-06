package com.carlos.audit.service;

import com.carlos.audit.manager.AuditLogArchiveRecordManager;
import com.carlos.audit.pojo.dto.AuditLogArchiveRecordDTO;
import com.carlos.audit.storage.AuditLogStorage;
import com.carlos.log.entity.OperationLog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link AuditLogArchiveRecordService} 单元测试
 *
 * @author Carlos
 * @date 2026-06-12
 */
@ExtendWith(MockitoExtension.class)
class AuditLogArchiveRecordServiceTest {

    @Mock
    private AuditLogArchiveRecordManager logArchiveRecordManager;

    @Mock
    private AuditLogStorage auditLogStorage;

    @InjectMocks
    private AuditLogArchiveRecordService archiveRecordService;

    @Test
    @DisplayName("addAuditLogArchiveRecord：保存成功时应写入自审计日志")
    void should_write_self_audit_when_add_success() {
        AuditLogArchiveRecordDTO dto = createDto();
        when(logArchiveRecordManager.add(dto)).thenReturn(true);

        archiveRecordService.addAuditLogArchiveRecord(dto);

        ArgumentCaptor<OperationLog> captor = ArgumentCaptor.forClass(OperationLog.class);
        verify(auditLogStorage).storeWithoutSampling(captor.capture());
        OperationLog log = captor.getValue();
        assertEquals("AUDIT_ARCHIVE_RECORD_CHANGE", log.getLogType());
        assertEquals("AUDIT_ARCHIVE_RECORD", log.getTargetType());
        assertEquals("新增审计日志归档记录", log.getOperation());
        assertEquals("ARCH-2026-001", log.getTargetName());
        assertNotNull(log.getTargetSnapshot());
    }

    @Test
    @DisplayName("addAuditLogArchiveRecord：保存失败时不应写入自审计日志")
    void should_not_write_self_audit_when_add_fail() {
        AuditLogArchiveRecordDTO dto = createDto();
        when(logArchiveRecordManager.add(dto)).thenReturn(false);

        archiveRecordService.addAuditLogArchiveRecord(dto);

        verify(auditLogStorage, never()).storeWithoutSampling(any());
    }

    @Test
    @DisplayName("updateAuditLogArchiveRecord：修改成功时应写入自审计日志")
    void should_write_self_audit_when_update_success() {
        AuditLogArchiveRecordDTO dto = createDto();
        dto.setId(2L);
        when(logArchiveRecordManager.modify(dto)).thenReturn(true);

        archiveRecordService.updateAuditLogArchiveRecord(dto);

        ArgumentCaptor<OperationLog> captor = ArgumentCaptor.forClass(OperationLog.class);
        verify(auditLogStorage).storeWithoutSampling(captor.capture());
        OperationLog log = captor.getValue();
        assertEquals("AUDIT_ARCHIVE_RECORD_CHANGE", log.getLogType());
        assertEquals("修改审计日志归档记录", log.getOperation());
        assertEquals("2", log.getTargetId());
    }

    @Test
    @DisplayName("updateAuditLogArchiveRecord：修改失败时不应写入自审计日志")
    void should_not_write_self_audit_when_update_fail() {
        AuditLogArchiveRecordDTO dto = createDto();
        when(logArchiveRecordManager.modify(dto)).thenReturn(false);

        archiveRecordService.updateAuditLogArchiveRecord(dto);

        verify(auditLogStorage, never()).storeWithoutSampling(any());
    }

    @Test
    @DisplayName("deleteAuditLogArchiveRecord：删除成功时应逐条写入自审计日志")
    void should_write_self_audit_when_delete_success() {
        when(logArchiveRecordManager.delete(1L)).thenReturn(true);
        when(logArchiveRecordManager.delete(2L)).thenReturn(false);

        archiveRecordService.deleteAuditLogArchiveRecord(Set.of(1L, 2L));

        ArgumentCaptor<OperationLog> captor = ArgumentCaptor.forClass(OperationLog.class);
        verify(auditLogStorage, times(1)).storeWithoutSampling(captor.capture());
        OperationLog log = captor.getValue();
        assertEquals("删除审计日志归档记录", log.getOperation());
        assertEquals("1", log.getTargetId());
        assertNull(log.getTargetSnapshot());
    }

    @Test
    @DisplayName("deleteAuditLogArchiveRecord：全部删除失败时不应写入自审计日志")
    void should_not_write_self_audit_when_all_delete_fail() {
        when(logArchiveRecordManager.delete(any(Serializable.class))).thenReturn(false);

        archiveRecordService.deleteAuditLogArchiveRecord(Set.of(1L));

        verify(auditLogStorage, never()).storeWithoutSampling(any());
    }

    private AuditLogArchiveRecordDTO createDto() {
        AuditLogArchiveRecordDTO dto = new AuditLogArchiveRecordDTO();
        dto.setId(1L);
        dto.setArchiveId("ARCH-2026-001");
        dto.setArchiveDate(LocalDate.of(2026, 6, 12));
        dto.setStartTime(LocalDateTime.of(2026, 6, 1, 0, 0));
        dto.setEndTime(LocalDateTime.of(2026, 6, 11, 23, 59));
        dto.setRecordCount(10000L);
        dto.setFileSizeBytes(1024000L);
        dto.setStoragePath("oss://bucket/audit/archive/ARCH-2026-001.parquet");
        dto.setStorageType("OSS");
        dto.setArchiveStatus("SUCCESS");
        return dto;
    }
}
