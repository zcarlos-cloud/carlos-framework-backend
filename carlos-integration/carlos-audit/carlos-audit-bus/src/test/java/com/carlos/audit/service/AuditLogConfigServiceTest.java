package com.carlos.audit.service;

import com.carlos.audit.manager.AuditLogConfigManager;
import com.carlos.audit.pojo.dto.AuditLogConfigDTO;
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
import java.math.BigDecimal;
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
 * {@link AuditLogConfigService} 单元测试
 *
 * @author Carlos
 * @date 2026-06-12
 */
@ExtendWith(MockitoExtension.class)
class AuditLogConfigServiceTest {

    @Mock
    private AuditLogConfigManager logConfigManager;

    @Mock
    private AuditLogStorage auditLogStorage;

    @InjectMocks
    private AuditLogConfigService configService;

    @Test
    @DisplayName("addAuditLogConfig：保存成功时应写入自审计日志")
    void should_write_self_audit_when_add_success() {
        AuditLogConfigDTO dto = createDto();
        when(logConfigManager.add(dto)).thenReturn(true);

        configService.addAuditLogConfig(dto);

        ArgumentCaptor<OperationLog> captor = ArgumentCaptor.forClass(OperationLog.class);
        verify(auditLogStorage).storeWithoutSampling(captor.capture());
        OperationLog log = captor.getValue();
        assertEquals("AUDIT_CONFIG_CHANGE", log.getLogType());
        assertEquals("AUDIT_CONFIG", log.getTargetType());
        assertEquals("新增审计日志配置", log.getOperation());
        assertEquals("USER_LOGIN", log.getTargetName());
        assertNotNull(log.getTargetSnapshot());
        assertEquals("1", log.getTenantId());
    }

    @Test
    @DisplayName("addAuditLogConfig：保存失败时不应写入自审计日志")
    void should_not_write_self_audit_when_add_fail() {
        AuditLogConfigDTO dto = createDto();
        when(logConfigManager.add(dto)).thenReturn(false);

        configService.addAuditLogConfig(dto);

        verify(auditLogStorage, never()).storeWithoutSampling(any());
    }

    @Test
    @DisplayName("updateAuditLogConfig：修改成功时应写入自审计日志")
    void should_write_self_audit_when_update_success() {
        AuditLogConfigDTO dto = createDto();
        dto.setId(2L);
        when(logConfigManager.modify(dto)).thenReturn(true);

        configService.updateAuditLogConfig(dto);

        ArgumentCaptor<OperationLog> captor = ArgumentCaptor.forClass(OperationLog.class);
        verify(auditLogStorage).storeWithoutSampling(captor.capture());
        OperationLog log = captor.getValue();
        assertEquals("AUDIT_CONFIG_CHANGE", log.getLogType());
        assertEquals("修改审计日志配置", log.getOperation());
        assertEquals("2", log.getTargetId());
    }

    @Test
    @DisplayName("updateAuditLogConfig：修改失败时不应写入自审计日志")
    void should_not_write_self_audit_when_update_fail() {
        AuditLogConfigDTO dto = createDto();
        when(logConfigManager.modify(dto)).thenReturn(false);

        configService.updateAuditLogConfig(dto);

        verify(auditLogStorage, never()).storeWithoutSampling(any());
    }

    @Test
    @DisplayName("deleteAuditLogConfig：删除成功时应逐条写入自审计日志")
    void should_write_self_audit_when_delete_success() {
        when(logConfigManager.delete(1L)).thenReturn(true);
        when(logConfigManager.delete(2L)).thenReturn(false);

        configService.deleteAuditLogConfig(Set.of(1L, 2L));

        ArgumentCaptor<OperationLog> captor = ArgumentCaptor.forClass(OperationLog.class);
        verify(auditLogStorage, times(1)).storeWithoutSampling(captor.capture());
        OperationLog log = captor.getValue();
        assertEquals("删除审计日志配置", log.getOperation());
        assertEquals("1", log.getTargetId());
        assertNull(log.getTargetSnapshot());
    }

    @Test
    @DisplayName("deleteAuditLogConfig：全部删除失败时不应写入自审计日志")
    void should_not_write_self_audit_when_all_delete_fail() {
        when(logConfigManager.delete(any(Serializable.class))).thenReturn(false);

        configService.deleteAuditLogConfig(Set.of(1L));

        verify(auditLogStorage, never()).storeWithoutSampling(any());
    }

    private AuditLogConfigDTO createDto() {
        AuditLogConfigDTO dto = new AuditLogConfigDTO();
        dto.setId(1L);
        dto.setLogType("USER_LOGIN");
        dto.setRetentionDays(30);
        dto.setSamplingRate(BigDecimal.ONE);
        dto.setAsyncWrite(true);
        dto.setStoreDataChange(true);
        dto.setStoreTechnical(true);
        dto.setTenantId("1");
        return dto;
    }
}
