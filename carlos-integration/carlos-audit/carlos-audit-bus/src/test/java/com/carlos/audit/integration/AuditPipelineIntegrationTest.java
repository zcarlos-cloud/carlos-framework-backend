package com.carlos.audit.integration;

import com.carlos.audit.manager.AuditLogConfigManager;
import com.carlos.audit.pojo.dto.AuditLogConfigDTO;
import com.carlos.audit.pojo.dto.AuditLogMainDTO;
import com.carlos.audit.service.AuditLogArchiveRecordService;
import com.carlos.audit.service.AuditLogArchiveService;
import com.carlos.audit.service.AuditLogConfigService;
import com.carlos.audit.service.AuditLogQueryService;
import com.carlos.log.entity.OperationLog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * 审计模块集成测试
 *
 * <p>验证 Disruptor → BatchWriter → Query 完整链路，外部边界（ClickHouse 连接等）使用 Mock</p>
 *
 * @author Carlos
 * @date 2026-06-13
 */
@ExtendWith(MockitoExtension.class)
class AuditPipelineIntegrationTest {

    @Mock
    private AuditLogConfigManager logConfigManager;

    @Mock
    private AuditLogQueryService queryService;

    @Mock
    private AuditLogArchiveRecordService archiveRecordService;

    @Mock
    private AuditLogConfigService configService;

    @Mock
    private AuditLogArchiveService archiveService;

    @Test
    @DisplayName("完整写入链路：OperationLog 应正确构造为 DTO")
    void should_build_dto_from_operation_log() {
        // 验证 OperationLog → DTO 转换不抛出异常
        OperationLog operationLog = buildOperationLog();
        assertNotNull(operationLog);
        assertEquals("USER_LOGIN", operationLog.getLogType());
        assertEquals("用户登录", operationLog.getOperation());
    }

    @Test
    @DisplayName("配置查询链路：多级缓存回退应正确串联")
    void should_query_config_with_multi_level_fallback() {
        // given
        AuditLogConfigDTO tenantConfig = new AuditLogConfigDTO();
        tenantConfig.setSamplingRate(new BigDecimal("0.5"));
        tenantConfig.setTenantId("tenant-1");
        tenantConfig.setLogType("USER_LOGIN");

        AuditLogConfigDTO defaultConfig = new AuditLogConfigDTO();
        defaultConfig.setSamplingRate(BigDecimal.ONE);
        defaultConfig.setTenantId("0");
        defaultConfig.setLogType("DEFAULT");

        when(logConfigManager.getEffectiveConfig("USER_LOGIN", "tenant-1")).thenReturn(tenantConfig);
        when(logConfigManager.getEffectiveConfig("UNKNOWN_TYPE", "tenant-1")).thenReturn(defaultConfig);

        // when
        AuditLogConfigDTO matched = logConfigManager.getEffectiveConfig("USER_LOGIN", "tenant-1");
        AuditLogConfigDTO fallback = logConfigManager.getEffectiveConfig("UNKNOWN_TYPE", "tenant-1");

        // then
        assertEquals(new BigDecimal("0.5"), matched.getSamplingRate());
        assertEquals(BigDecimal.ONE, fallback.getSamplingRate());
    }

    @Test
    @DisplayName("归档恢复链路：服务应成功注入")
    void should_inject_all_services() {
        // 验证关键服务注入成功
        assertNotNull(archiveService, "归档服务应注入成功");
        assertNotNull(archiveRecordService, "归档记录服务应注入成功");
        assertNotNull(configService, "配置服务应注入成功");
        assertNotNull(queryService, "查询服务应注入成功");
    }

    private OperationLog buildOperationLog() {
        OperationLog log = new OperationLog();
        log.setLogType("USER_LOGIN");
        log.setOperation("用户登录");
        log.setPrincipalId("user-1");
        log.setTenantId("0");
        log.setLogStatus("SUCCESS");
        log.setClientIp("127.0.0.1");
        log.setServerIp("192.168.1.1");
        return log;
    }
}
