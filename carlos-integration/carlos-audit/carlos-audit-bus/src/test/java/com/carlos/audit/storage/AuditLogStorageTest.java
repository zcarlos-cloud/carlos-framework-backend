package com.carlos.audit.storage;

import com.carlos.audit.clickhouse.ClickHouseBatchWriter;
import com.carlos.audit.manager.AuditLogConfigManager;
import com.carlos.audit.pojo.dto.AuditLogConfigDTO;
import com.carlos.audit.pojo.dto.AuditLogMainDTO;
import com.carlos.disruptor.core.DisruptorTemplate;
import com.carlos.log.entity.OperationLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AuditLogStorage 单元测试
 *
 * @author Carlos
 * @date 2026-06-12
 */
@ExtendWith(MockitoExtension.class)
class AuditLogStorageTest {

    @Mock
    private DisruptorTemplate<AuditLogMainDTO> auditDisruptorTemplate;

    @Mock
    private ClickHouseBatchWriter batchWriter;

    @Mock
    private AuditLogConfigManager logConfigManager;

    @InjectMocks
    private AuditLogStorage auditLogStorage;

    @BeforeEach
    void setUp() {
        AuditLogConfigDTO defaultConfig = new AuditLogConfigDTO();
        defaultConfig.setSamplingRate(BigDecimal.ONE);
        defaultConfig.setAsyncWrite(true);
        defaultConfig.setStoreDataChange(true);
        defaultConfig.setStoreTechnical(true);
        defaultConfig.setRetentionDays(90);
        lenient().when(logConfigManager.getEffectiveConfig(any(), any())).thenReturn(defaultConfig);
    }

    @Test
    void should_return_true_when_storeBatch_null_list() {
        assertTrue(auditLogStorage.storeBatch(null));
        verify(auditDisruptorTemplate, never()).publishEventList(ArgumentMatchers.any());
    }

    @Test
    void should_return_true_when_storeBatch_empty_list() {
        assertTrue(auditLogStorage.storeBatch(Collections.emptyList()));
        verify(auditDisruptorTemplate, never()).publishEventList(ArgumentMatchers.any());
    }

    @Test
    void should_publish_converted_logs_when_storeBatch_valid_list() {
        OperationLog log1 = new OperationLog();
        log1.setLogType("USER_LOGIN");
        OperationLog log2 = new OperationLog();
        log2.setLogType("ORDER_PAY");

        boolean result = auditLogStorage.storeBatch(Arrays.asList(log1, log2));

        assertTrue(result);
        verify(auditDisruptorTemplate, times(1)).publishEventList(ArgumentMatchers.any());
        verify(batchWriter, never()).add(ArgumentMatchers.any());
    }

    @Test
    void should_skip_null_converted_logs_when_storeBatch_contains_null_element() {
        OperationLog log1 = new OperationLog();
        log1.setLogType("USER_LOGIN");
        OperationLog log2 = null;

        boolean result = auditLogStorage.storeBatch(Arrays.asList(log1, log2));

        assertTrue(result);
        verify(auditDisruptorTemplate, times(1)).publishEventList(ArgumentMatchers.any());
    }

    @Test
    void should_return_false_when_publishEventList_throws_exception() {
        OperationLog log = new OperationLog();
        log.setLogType("USER_LOGIN");
        doThrow(new RuntimeException("disruptor error"))
            .when(auditDisruptorTemplate).publishEventList(ArgumentMatchers.any());

        boolean result = auditLogStorage.storeBatch(Collections.singletonList(log));

        assertFalse(result);
    }

    @Test
    void should_drop_log_when_sampling_rate_is_zero() {
        OperationLog log = new OperationLog();
        log.setLogType("USER_LOGIN");
        AuditLogConfigDTO config = new AuditLogConfigDTO();
        config.setSamplingRate(BigDecimal.ZERO);
        config.setAsyncWrite(true);
        when(logConfigManager.getEffectiveConfig("USER_LOGIN", null)).thenReturn(config);

        boolean result = auditLogStorage.store(log);

        assertTrue(result);
        verify(auditDisruptorTemplate, never()).publishEvent(ArgumentMatchers.any());
        verify(batchWriter, never()).add(ArgumentMatchers.any());
    }

    @Test
    void should_write_directly_when_async_write_is_false() {
        OperationLog log = new OperationLog();
        log.setLogType("ORDER_PAY");
        AuditLogConfigDTO config = new AuditLogConfigDTO();
        config.setSamplingRate(BigDecimal.ONE);
        config.setAsyncWrite(false);
        when(logConfigManager.getEffectiveConfig("ORDER_PAY", null)).thenReturn(config);

        boolean result = auditLogStorage.store(log);

        assertTrue(result);
        verify(batchWriter, times(1)).add(ArgumentMatchers.any(AuditLogMainDTO.class));
        verify(auditDisruptorTemplate, never()).publishEvent(ArgumentMatchers.any());
    }

    @Test
    void should_handle_mixed_sync_and_async_logs_in_batch() {
        OperationLog asyncLog = new OperationLog();
        asyncLog.setLogType("USER_LOGIN");
        AuditLogConfigDTO asyncConfig = new AuditLogConfigDTO();
        asyncConfig.setSamplingRate(BigDecimal.ONE);
        asyncConfig.setAsyncWrite(true);

        OperationLog syncLog = new OperationLog();
        syncLog.setLogType("ORDER_PAY");
        AuditLogConfigDTO syncConfig = new AuditLogConfigDTO();
        syncConfig.setSamplingRate(BigDecimal.ONE);
        syncConfig.setAsyncWrite(false);

        when(logConfigManager.getEffectiveConfig("USER_LOGIN", null)).thenReturn(asyncConfig);
        when(logConfigManager.getEffectiveConfig("ORDER_PAY", null)).thenReturn(syncConfig);

        boolean result = auditLogStorage.storeBatch(Arrays.asList(asyncLog, syncLog));

        assertTrue(result);
        verify(auditDisruptorTemplate, times(1)).publishEventList(ArgumentMatchers.any());
        verify(batchWriter, times(1)).add(ArgumentMatchers.any(AuditLogMainDTO.class));
    }

    @Test
    @DisplayName("storeWithoutSampling：采样率为 0 时仍应写入")
    void should_write_without_sampling_when_store_without_sampling() {
        OperationLog log = new OperationLog();
        log.setLogType("AUDIT_CONFIG_CHANGE");

        boolean result = auditLogStorage.storeWithoutSampling(log);

        assertTrue(result);
        verify(auditDisruptorTemplate, times(1)).publishEvent(ArgumentMatchers.any());
        verify(batchWriter, never()).add(ArgumentMatchers.any());
    }

    @Test
    @DisplayName("storeWithoutSampling：不应查询配置管理器")
    void should_not_query_config_manager_when_store_without_sampling() {
        OperationLog log = new OperationLog();
        log.setLogType("AUDIT_ARCHIVE_RECORD_CHANGE");

        boolean result = auditLogStorage.storeWithoutSampling(log);

        assertTrue(result);
        verify(logConfigManager, never()).getEffectiveConfig(any(), any());
        verify(auditDisruptorTemplate, times(1)).publishEvent(ArgumentMatchers.any());
    }

    @Test
    @DisplayName("storeWithoutSampling：传入 null 应返回 false")
    void should_return_false_when_store_without_sampling_null() {
        assertFalse(auditLogStorage.storeWithoutSampling(null));
    }
}
