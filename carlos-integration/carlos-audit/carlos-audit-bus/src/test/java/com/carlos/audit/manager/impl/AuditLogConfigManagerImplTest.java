package com.carlos.audit.manager.impl;

import com.carlos.audit.mapper.AuditLogConfigMapper;
import com.carlos.audit.pojo.dto.AuditLogConfigDTO;
import com.carlos.audit.pojo.entity.AuditLogConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AuditLogConfigManagerImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
class AuditLogConfigManagerImplTest {

    @Mock
    private AuditLogConfigMapper mapper;

    @InjectMocks
    private AuditLogConfigManagerImpl manager;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(manager, "baseMapper", mapper);
        lenient().when(mapper.selectOne(any())).thenReturn(null);
    }

    @Test
    void should_return_tenant_config_when_exists() {
        AuditLogConfig tenantConfig = createEntity("USER_LOGIN", "1", 30,
            BigDecimal.ONE, true, true, true);
        when(mapper.selectOne(any())).thenReturn(tenantConfig);

        AuditLogConfigDTO result = manager.getEffectiveConfig("USER_LOGIN", "1");

        assertEquals("USER_LOGIN", result.getLogType());
        assertEquals("1", result.getTenantId());
        assertEquals(30, result.getRetentionDays());
    }

    @Test
    void should_fall_back_to_system_config_when_tenant_missing() {
        AuditLogConfig systemConfig = createEntity("USER_LOGIN", "0", 60,
            BigDecimal.valueOf(0.5), false, true, true);
        when(mapper.selectOne(any()))
            .thenReturn(null)
            .thenReturn(systemConfig);

        AuditLogConfigDTO result = manager.getEffectiveConfig("USER_LOGIN", "1");

        assertEquals("USER_LOGIN", result.getLogType());
        assertEquals("0", result.getTenantId());
        assertEquals(60, result.getRetentionDays());
        assertFalse(result.getAsyncWrite());
        assertEquals(BigDecimal.valueOf(0.5), result.getSamplingRate());
    }

    @Test
    void should_fall_back_to_default_rule_when_system_and_tenant_missing() {
        AuditLogConfig defaultConfig = createEntity("DEFAULT", "0", 180,
            BigDecimal.valueOf(0.1), true, false, false);
        when(mapper.selectOne(any()))
            .thenReturn(null)
            .thenReturn(null)
            .thenReturn(defaultConfig);

        AuditLogConfigDTO result = manager.getEffectiveConfig("USER_LOGIN", "1");

        assertEquals("DEFAULT", result.getLogType());
        assertEquals("0", result.getTenantId());
        assertEquals(180, result.getRetentionDays());
        assertEquals(BigDecimal.valueOf(0.1), result.getSamplingRate());
    }

    @Test
    void should_return_hardcoded_default_when_no_db_record() {
        AuditLogConfigDTO result = manager.getEffectiveConfig("USER_LOGIN", "1");

        assertNotNull(result);
        assertEquals("USER_LOGIN", result.getLogType());
        assertEquals("1", result.getTenantId());
        assertEquals(90, result.getRetentionDays());
        assertEquals(BigDecimal.ONE, result.getSamplingRate());
        assertTrue(result.getAsyncWrite());
        assertTrue(result.getStoreDataChange());
        assertTrue(result.getStoreTechnical());
    }

    @Test
    void should_use_default_log_type_and_tenant_when_input_blank() {
        AuditLogConfigDTO result = manager.getEffectiveConfig(null, null);

        assertNotNull(result);
        assertEquals("DEFAULT", result.getLogType());
        assertEquals("0", result.getTenantId());
    }

    @Test
    void should_cache_effective_config_and_avoid_repeated_db_queries() {
        AuditLogConfig tenantConfig = createEntity("USER_LOGIN", "1", 30,
            BigDecimal.ONE, true, true, true);
        when(mapper.selectOne(any()))
            .thenReturn(tenantConfig)
            .thenReturn(null)
            .thenReturn(null);

        AuditLogConfigDTO first = manager.getEffectiveConfig("USER_LOGIN", "1");
        AuditLogConfigDTO second = manager.getEffectiveConfig("USER_LOGIN", "1");

        assertEquals(30, first.getRetentionDays());
        assertEquals(30, second.getRetentionDays());
        verify(mapper, times(1)).selectOne(any());
    }

    @Test
    void should_invalidate_cache_after_modify() {
        AuditLogConfig tenantConfig = createEntity("USER_LOGIN", "1", 30,
            BigDecimal.ONE, true, true, true);
        AuditLogConfig systemConfig = createEntity("USER_LOGIN", "0", 60,
            BigDecimal.valueOf(0.5), false, true, true);
        when(mapper.selectOne(any()))
            .thenReturn(tenantConfig)
            .thenReturn(null)
            .thenReturn(null);

        AuditLogConfigDTO before = manager.getEffectiveConfig("USER_LOGIN", "1");
        assertEquals(30, before.getRetentionDays());

        AuditLogConfigDTO updateDto = new AuditLogConfigDTO();
        updateDto.setId(1L);
        updateDto.setLogType("USER_LOGIN");
        updateDto.setTenantId("1");
        when(mapper.updateById(any(AuditLogConfig.class))).thenReturn(1);
        manager.modify(updateDto);

        when(mapper.selectOne(any()))
            .thenReturn(null)
            .thenReturn(systemConfig)
            .thenReturn(null);
        AuditLogConfigDTO after = manager.getEffectiveConfig("USER_LOGIN", "1");
        assertEquals(60, after.getRetentionDays());
    }

    private AuditLogConfig createEntity(String logType, String tenantId, int retentionDays,
                                        BigDecimal samplingRate, boolean asyncWrite,
                                        boolean storeDataChange, boolean storeTechnical) {
        AuditLogConfig entity = new AuditLogConfig();
        entity.setId(1L);
        entity.setLogType(logType);
        entity.setTenantId(tenantId);
        entity.setRetentionDays(retentionDays);
        entity.setSamplingRate(samplingRate);
        entity.setAsyncWrite(asyncWrite);
        entity.setStoreDataChange(storeDataChange);
        entity.setStoreTechnical(storeTechnical);
        entity.setDeleted(false);
        return entity;
    }
}
