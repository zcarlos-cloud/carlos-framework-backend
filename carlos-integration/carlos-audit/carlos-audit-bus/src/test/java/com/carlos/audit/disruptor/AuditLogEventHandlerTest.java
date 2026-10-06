package com.carlos.audit.disruptor;

import com.carlos.audit.clickhouse.ClickHouseBatchWriter;
import com.carlos.audit.manager.AuditLogConfigManager;
import com.carlos.audit.pojo.dto.AuditLogConfigDTO;
import com.carlos.audit.pojo.dto.AuditLogMainDTO;
import com.carlos.disruptor.core.DisruptorEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AuditLogEventHandler 单元测试
 */
@ExtendWith(MockitoExtension.class)
class AuditLogEventHandlerTest {

    @Mock
    private ClickHouseBatchWriter batchWriter;

    @Mock
    private AuditLogConfigManager logConfigManager;

    @InjectMocks
    private AuditLogEventHandler eventHandler;

    @BeforeEach
    void setUp() {
        AuditLogConfigDTO defaultConfig = new AuditLogConfigDTO();
        defaultConfig.setRetentionDays(90);
        defaultConfig.setStoreDataChange(true);
        defaultConfig.setStoreTechnical(true);
        lenient().when(logConfigManager.getEffectiveConfig(any(), any())).thenReturn(defaultConfig);
    }

    @Test
    void should_set_retention_deadline_from_config() throws Exception {
        AuditLogMainDTO auditLog = createAuditLog();
        auditLog.setEventDate(LocalDate.of(2026, 6, 1));

        AuditLogConfigDTO config = new AuditLogConfigDTO();
        config.setRetentionDays(30);
        config.setStoreDataChange(true);
        config.setStoreTechnical(true);
        when(logConfigManager.getEffectiveConfig("USER_LOGIN", "1")).thenReturn(config);

        eventHandler.onEvent(new DisruptorEvent<>(auditLog), 0, false);

        ArgumentCaptor<AuditLogMainDTO> captor = ArgumentCaptor.forClass(AuditLogMainDTO.class);
        verify(batchWriter, times(1)).add(captor.capture());
        assertEquals(LocalDate.of(2026, 7, 1), captor.getValue().getRetentionDeadline());
    }

    @Test
    void should_clear_data_change_fields_when_disabled() throws Exception {
        AuditLogMainDTO auditLog = createAuditLog();
        auditLog.setOldData("{\"name\":\"old\"}");
        auditLog.setNewData("{\"name\":\"new\"}");
        auditLog.setChangeSummary("name changed");
        auditLog.setChangedFieldCount(1);
        auditLog.setHasDataChange(true);
        auditLog.setEntityClass("com.example.User");
        auditLog.setTableName("t_user");

        AuditLogConfigDTO config = new AuditLogConfigDTO();
        config.setRetentionDays(90);
        config.setStoreDataChange(false);
        config.setStoreTechnical(true);
        when(logConfigManager.getEffectiveConfig("USER_LOGIN", "1")).thenReturn(config);

        eventHandler.onEvent(new DisruptorEvent<>(auditLog), 0, false);

        ArgumentCaptor<AuditLogMainDTO> captor = ArgumentCaptor.forClass(AuditLogMainDTO.class);
        verify(batchWriter, times(1)).add(captor.capture());
        AuditLogMainDTO result = captor.getValue();
        assertNull(result.getOldData());
        assertNull(result.getNewData());
        assertNull(result.getChangeSummary());
        assertNull(result.getChangedFieldCount());
        assertNull(result.getHasDataChange());
        assertNull(result.getEntityClass());
        assertNull(result.getTableName());
    }

    @Test
    void should_preserve_data_change_fields_when_enabled() throws Exception {
        AuditLogMainDTO auditLog = createAuditLog();
        auditLog.setOldData("{\"name\":\"old\"}");
        auditLog.setNewData("{\"name\":\"new\"}");
        auditLog.setChangeSummary("name changed");
        auditLog.setChangedFieldCount(1);
        auditLog.setHasDataChange(true);
        auditLog.setEntityClass("com.example.User");
        auditLog.setTableName("t_user");

        AuditLogConfigDTO config = new AuditLogConfigDTO();
        config.setRetentionDays(90);
        config.setStoreDataChange(true);
        config.setStoreTechnical(true);
        when(logConfigManager.getEffectiveConfig("USER_LOGIN", "1")).thenReturn(config);

        eventHandler.onEvent(new DisruptorEvent<>(auditLog), 0, false);

        ArgumentCaptor<AuditLogMainDTO> captor = ArgumentCaptor.forClass(AuditLogMainDTO.class);
        verify(batchWriter, times(1)).add(captor.capture());
        AuditLogMainDTO result = captor.getValue();
        assertNotNull(result.getOldData());
        assertNotNull(result.getNewData());
        assertNotNull(result.getChangeSummary());
        assertNotNull(result.getChangedFieldCount());
        assertNotNull(result.getHasDataChange());
        assertNotNull(result.getEntityClass());
        assertNotNull(result.getTableName());
    }

    @Test
    void should_clear_technical_fields_when_disabled() throws Exception {
        AuditLogMainDTO auditLog = createAuditLog();
        auditLog.setDbQueryCount(10);
        auditLog.setDbQueryTimeMs(100);
        auditLog.setExternalCallCount(2);
        auditLog.setExternalCallTimeMs(50);
        auditLog.setCustomMetrics("{\"cpu\":80}");

        AuditLogConfigDTO config = new AuditLogConfigDTO();
        config.setRetentionDays(90);
        config.setStoreDataChange(true);
        config.setStoreTechnical(false);
        when(logConfigManager.getEffectiveConfig("USER_LOGIN", "1")).thenReturn(config);

        eventHandler.onEvent(new DisruptorEvent<>(auditLog), 0, false);

        ArgumentCaptor<AuditLogMainDTO> captor = ArgumentCaptor.forClass(AuditLogMainDTO.class);
        verify(batchWriter, times(1)).add(captor.capture());
        AuditLogMainDTO result = captor.getValue();
        assertNull(result.getDbQueryCount());
        assertNull(result.getDbQueryTimeMs());
        assertNull(result.getExternalCallCount());
        assertNull(result.getExternalCallTimeMs());
        assertNull(result.getCustomMetrics());
    }

    @Test
    void should_preserve_technical_fields_when_enabled() throws Exception {
        AuditLogMainDTO auditLog = createAuditLog();
        auditLog.setDbQueryCount(10);
        auditLog.setDbQueryTimeMs(100);
        auditLog.setExternalCallCount(2);
        auditLog.setExternalCallTimeMs(50);
        auditLog.setCustomMetrics("{\"cpu\":80}");

        AuditLogConfigDTO config = new AuditLogConfigDTO();
        config.setRetentionDays(90);
        config.setStoreDataChange(true);
        config.setStoreTechnical(true);
        when(logConfigManager.getEffectiveConfig("USER_LOGIN", "1")).thenReturn(config);

        eventHandler.onEvent(new DisruptorEvent<>(auditLog), 0, false);

        ArgumentCaptor<AuditLogMainDTO> captor = ArgumentCaptor.forClass(AuditLogMainDTO.class);
        verify(batchWriter, times(1)).add(captor.capture());
        AuditLogMainDTO result = captor.getValue();
        assertNotNull(result.getDbQueryCount());
        assertNotNull(result.getDbQueryTimeMs());
        assertNotNull(result.getExternalCallCount());
        assertNotNull(result.getExternalCallTimeMs());
        assertNotNull(result.getCustomMetrics());
    }

    @Test
    void should_use_default_retention_when_event_date_is_null() throws Exception {
        AuditLogMainDTO auditLog = createAuditLog();
        auditLog.setEventDate(null);
        LocalDate expectedDeadline = LocalDate.now().plusDays(7);

        AuditLogConfigDTO config = new AuditLogConfigDTO();
        config.setRetentionDays(7);
        config.setStoreDataChange(true);
        config.setStoreTechnical(true);
        when(logConfigManager.getEffectiveConfig("USER_LOGIN", "1")).thenReturn(config);

        eventHandler.onEvent(new DisruptorEvent<>(auditLog), 0, false);

        ArgumentCaptor<AuditLogMainDTO> captor = ArgumentCaptor.forClass(AuditLogMainDTO.class);
        verify(batchWriter, times(1)).add(captor.capture());
        assertEquals(expectedDeadline, captor.getValue().getRetentionDeadline());
    }

    private AuditLogMainDTO createAuditLog() {
        AuditLogMainDTO dto = new AuditLogMainDTO();
        dto.setLogType("USER_LOGIN");
        dto.setTenantId("1");
        dto.setServerTime(java.time.LocalDateTime.now());
        return dto;
    }
}
