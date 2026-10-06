package com.carlos.audit.clickhouse;

import cn.hutool.core.lang.Snowflake;
import com.carlos.audit.api.pojo.enums.AuditLogAuthProviderEnum;
import com.carlos.audit.api.pojo.enums.AuditLogAuthTypeEnum;
import com.carlos.audit.api.pojo.enums.AuditLogBizChannelEnum;
import com.carlos.audit.api.pojo.enums.AuditLogCategoryEnum;
import com.carlos.audit.api.pojo.enums.AuditLogPrincipalTypeEnum;
import com.carlos.audit.api.pojo.enums.AuditLogStatusEnum;
import com.carlos.audit.config.AuditProperties;
import com.carlos.audit.config.BackPressureStrategy;
import com.carlos.audit.pojo.dto.AuditLogMainDTO;
import com.carlos.audit.pojo.enums.AuditLogPayloadStorageTypeEnum;
import com.carlos.snowflake.SnowflakeUtil;
import com.clickhouse.client.ClickHouseClient;
import com.clickhouse.client.ClickHouseException;
import com.clickhouse.client.ClickHouseNode;
import com.clickhouse.client.ClickHouseRequest;
import com.clickhouse.client.ClickHouseResponse;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ClickHouseBatchWriter 单元测试
 *
 * @author Carlos
 * @date 2026-06-12
 */
@ExtendWith(MockitoExtension.class)
class ClickHouseBatchWriterTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock
    private ClickHouseClient clickHouseClient;

    @Mock
    private ClickHouseNode clickHouseNode;

    @BeforeAll
    static void initSnowflake() {
        SnowflakeUtil.init(new Snowflake(0L, 0L, true));
    }

    @Test
    void should_generate_unique_ids_for_ten_thousand_logs() throws Exception {
        AuditProperties auditProperties = new AuditProperties();
        ClickHouseBatchWriter writer = new ClickHouseBatchWriter(clickHouseClient, clickHouseNode, auditProperties, new SimpleMeterRegistry());

        Method ensureMethod = ClickHouseBatchWriter.class.getDeclaredMethod("withRequiredFields", AuditLogMainDTO.class);
        ensureMethod.setAccessible(true);

        int count = 10000;
        Set<Long> ids = new HashSet<>(count);
        for (int i = 0; i < count; i++) {
            AuditLogMainDTO dto = new AuditLogMainDTO();
            AuditLogMainDTO enriched = (AuditLogMainDTO) ensureMethod.invoke(writer, dto);

            assertNotNull(enriched.getId(), "第 " + i + " 条日志 ID 不应为空");
            assertTrue(enriched.getId() > 0, "第 " + i + " 条日志 ID 应大于 0");
            ids.add(enriched.getId());
        }

        assertEquals(count, ids.size(), "1 万次生成应无重复 ID");
    }

    @Test
    void should_serialize_all_fields_to_json_each_row() throws Exception {
        AuditProperties auditProperties = new AuditProperties();
        ClickHouseBatchWriter writer = new ClickHouseBatchWriter(clickHouseClient, clickHouseNode, auditProperties, new SimpleMeterRegistry());

        Method toJsonLineMethod = ClickHouseBatchWriter.class.getDeclaredMethod("toJsonLine", AuditLogMainDTO.class);
        toJsonLineMethod.setAccessible(true);

        AuditLogMainDTO dto = buildFullDto();
        String json = (String) toJsonLineMethod.invoke(writer, dto);

        JsonNode node = OBJECT_MAPPER.readTree(json);

        assertEquals(dto.getId(), node.get("id").asLong());
        assertEquals("2026-06-12 10:30:00.000", node.get("server_time").asText());
        assertEquals("2026-06-12", node.get("event_date").asText());
        assertEquals("BUSINESS", node.get("category").asText());
        assertEquals("USER", node.get("principal_type").asText());
        assertEquals("SUCCESS", node.get("log_status").asText());
        assertEquals("PASSWORD", node.get("auth_type").asText());
        assertEquals("LOCAL", node.get("auth_provider").asText());
        assertEquals("WEB", node.get("biz_channel").asText());
        assertEquals("OSS", node.get("payload_storage_type").asText());
        assertEquals("order_123", node.get("biz_order_no").asText());
        assertEquals("login", node.get("operation").asText());
        assertTrue(node.get("monetary_amount").asText().startsWith("99.5"));
        assertEquals(3, node.get("log_schema_version").asInt());
    }

    @Test
    void should_expose_micrometer_metrics_after_init() throws Exception {
        AuditProperties auditProperties = new AuditProperties();
        auditProperties.getBatchWriter().setRetryTimes(0);
        auditProperties.getBatchWriter().setLocalBackupOnFailure(false);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        ClickHouseBatchWriter writer = new ClickHouseBatchWriter(clickHouseClient, clickHouseNode, auditProperties, meterRegistry);

        writer.init();
        try {
            assertEquals(0.0D, meterRegistry.get("carlos.audit.clickhouse.buffer.size").gauge().value(), 0.001D);

            AuditLogMainDTO dto = new AuditLogMainDTO();
            dto.setLogType("USER_LOGIN");
            writer.add(dto);
            assertEquals(1.0D, meterRegistry.get("carlos.audit.clickhouse.buffer.size").gauge().value(), 0.001D);

            Method doFlushMethod = ClickHouseBatchWriter.class.getDeclaredMethod("doFlush", java.util.List.class, int.class);
            doFlushMethod.setAccessible(true);
            doFlushMethod.invoke(writer, Collections.singletonList(dto), 1);

            assertEquals(1.0D, meterRegistry.get("carlos.audit.clickhouse.failed").counter().count(), 0.001D);
            assertEquals(0.0D, meterRegistry.get("carlos.audit.clickhouse.written").counter().count(), 0.001D);
            assertEquals(0.0D, meterRegistry.get("carlos.audit.clickhouse.flush.duration").timer().count(), 0.001D);
        } finally {
            writer.destroy();
        }
    }

    @Test
    void should_not_lose_logs_under_concurrent_add_and_flush() throws Exception {
        AuditProperties auditProperties = new AuditProperties();
        auditProperties.getBatchWriter().setBatchSize(10);
        auditProperties.getBatchWriter().setRetryTimes(0);
        auditProperties.getBatchWriter().setLocalBackupOnFailure(false);
        ClickHouseBatchWriter writer = new ClickHouseBatchWriter(clickHouseClient, clickHouseNode, auditProperties, new SimpleMeterRegistry());
        writer.init();

        try {
            int producers = 4;
            int perProducer = 100;
            int total = producers * perProducer;

            ExecutorService producerExecutor = Executors.newFixedThreadPool(producers);
            for (int i = 0; i < producers; i++) {
                final int index = i;
                producerExecutor.submit(() -> {
                    for (int j = 0; j < perProducer; j++) {
                        AuditLogMainDTO dto = new AuditLogMainDTO();
                        dto.setLogType("TYPE_" + index);
                        writer.add(dto);
                    }
                });
            }
            producerExecutor.shutdown();
            assertTrue(producerExecutor.awaitTermination(30, TimeUnit.SECONDS), "生产者线程应在 30 秒内完成");

            long deadline = System.currentTimeMillis() + 5000;
            while (System.currentTimeMillis() < deadline
                && writer.getTotalFailed() + writer.getActiveBufferSize() < total) {
                writer.scheduledFlush();
                Thread.sleep(20);
            }

            assertEquals(total, writer.getTotalFailed() + writer.getActiveBufferSize(),
                "并发添加与刷新不应丢失日志");
        } finally {
            writer.destroy();
        }
    }

    @Test
    void should_drop_oldest_when_back_pressure_overflow() throws Exception {
        AuditProperties auditProperties = new AuditProperties();
        auditProperties.getBatchWriter().setBatchSize(100);
        auditProperties.getBatchWriter().setMaxBufferSize(10);
        auditProperties.getBatchWriter().setBackPressureStrategy(BackPressureStrategy.DROP_OLDEST);
        auditProperties.getBatchWriter().setLocalBackupOnFailure(false);
        ClickHouseBatchWriter writer = new ClickHouseBatchWriter(clickHouseClient, clickHouseNode, auditProperties, new SimpleMeterRegistry());
        writer.init();

        try {
            for (int i = 0; i < 20; i++) {
                AuditLogMainDTO dto = new AuditLogMainDTO();
                dto.setLogType("LOG_" + i);
                writer.add(dto);
            }
            assertTrue(writer.getActiveBufferSize() <= 10, "DROP_OLDEST 后缓冲区不应超过最大值");
            assertTrue(writer.getTotalDiscarded() > 0, "DROP_OLDEST 应丢弃数据");
        } finally {
            writer.destroy();
        }
    }

    @Test
    void should_drop_newest_when_back_pressure_overflow() throws Exception {
        AuditProperties auditProperties = new AuditProperties();
        auditProperties.getBatchWriter().setBatchSize(100);
        auditProperties.getBatchWriter().setMaxBufferSize(10);
        auditProperties.getBatchWriter().setBackPressureStrategy(BackPressureStrategy.DROP_NEWEST);
        auditProperties.getBatchWriter().setLocalBackupOnFailure(false);
        ClickHouseBatchWriter writer = new ClickHouseBatchWriter(clickHouseClient, clickHouseNode, auditProperties, new SimpleMeterRegistry());
        writer.init();

        try {
            for (int i = 0; i < 20; i++) {
                AuditLogMainDTO dto = new AuditLogMainDTO();
                dto.setLogType("LOG_" + i);
                writer.add(dto);
            }
            assertEquals(10, writer.getActiveBufferSize(), "DROP_NEWEST 应保留最早 10 条");
            assertEquals(10, writer.getTotalDiscarded(), "DROP_NEWEST 应丢弃 10 条新数据");
        } finally {
            writer.destroy();
        }
    }

    @Test
    void should_block_until_space_when_back_pressure_overflow() throws Exception {
        AuditProperties auditProperties = new AuditProperties();
        auditProperties.getBatchWriter().setBatchSize(5);
        auditProperties.getBatchWriter().setMaxBufferSize(10);
        auditProperties.getBatchWriter().setBackPressureStrategy(BackPressureStrategy.BLOCK);
        auditProperties.getBatchWriter().setBackPressureBlockTimeoutMs(5);
        auditProperties.getBatchWriter().setRetryTimes(0);
        auditProperties.getBatchWriter().setLocalBackupOnFailure(false);
        ClickHouseBatchWriter writer = new ClickHouseBatchWriter(clickHouseClient, clickHouseNode, auditProperties, new SimpleMeterRegistry());
        writer.init();

        try {
            for (int i = 0; i < 20; i++) {
                AuditLogMainDTO dto = new AuditLogMainDTO();
                dto.setLogType("LOG_" + i);
                writer.add(dto);
            }
            writer.scheduledFlush();

            long deadline = System.currentTimeMillis() + 2000;
            while (System.currentTimeMillis() < deadline && writer.getTotalFailed() < 20) {
                writer.scheduledFlush();
                Thread.sleep(20);
            }

            assertEquals(0, writer.getTotalDiscarded(), "BLOCK 策略不应丢弃数据");
            assertEquals(20, writer.getActiveBufferSize() + writer.getTotalFailed(),
                "BLOCK 策略下所有日志应在缓冲区或已尝试写入");
        } finally {
            writer.destroy();
        }
    }

    @Test
    void should_sample_back_pressure_with_zero_rate() throws Exception {
        AuditProperties auditProperties = new AuditProperties();
        auditProperties.getBatchWriter().setBatchSize(100);
        auditProperties.getBatchWriter().setMaxBufferSize(10);
        auditProperties.getBatchWriter().setBackPressureStrategy(BackPressureStrategy.SAMPLE);
        auditProperties.getBatchWriter().setBackPressureSampleRate(0.0D);
        auditProperties.getBatchWriter().setLocalBackupOnFailure(false);
        ClickHouseBatchWriter writer = new ClickHouseBatchWriter(clickHouseClient, clickHouseNode, auditProperties, new SimpleMeterRegistry());
        writer.init();

        try {
            for (int i = 0; i < 20; i++) {
                AuditLogMainDTO dto = new AuditLogMainDTO();
                dto.setLogType("LOG_" + i);
                writer.add(dto);
            }
            assertEquals(10, writer.getActiveBufferSize(), "采样率 0 时应保留最早 10 条");
            assertEquals(10, writer.getTotalDiscarded(), "采样率 0 时应丢弃 10 条新数据");
        } finally {
            writer.destroy();
        }
    }

    @Test
    void should_retain_approximately_half_when_sample_rate_is_fifty_percent() throws Exception {
        AuditProperties auditProperties = new AuditProperties();
        auditProperties.getBatchWriter().setBatchSize(1000);
        auditProperties.getBatchWriter().setMaxBufferSize(100);
        auditProperties.getBatchWriter().setBackPressureStrategy(BackPressureStrategy.SAMPLE);
        auditProperties.getBatchWriter().setBackPressureSampleRate(0.5D);
        auditProperties.getBatchWriter().setLocalBackupOnFailure(false);
        ClickHouseBatchWriter writer = new ClickHouseBatchWriter(clickHouseClient, clickHouseNode, auditProperties, new SimpleMeterRegistry());
        writer.init();

        try {
            int count = 1000;
            for (int i = 0; i < count; i++) {
                AuditLogMainDTO dto = new AuditLogMainDTO();
                dto.setLogType("LOG_" + i);
                writer.add(dto);
            }
            long retained = writer.getActiveBufferSize();
            long discarded = writer.getTotalDiscarded();
            assertEquals(count, retained + discarded, "采样后保留与丢弃之和应等于总数");
            assertTrue(retained >= 80 && retained <= 120,
                "采样率 50% 时保留数量应在期望值 100 附近（容忍 ±20），实际保留: " + retained);
        } finally {
            writer.destroy();
        }
    }

    @Test
    void should_retry_on_clickhouse_timeout_exception() throws Exception {
        AuditProperties auditProperties = new AuditProperties();
        auditProperties.getBatchWriter().setBatchSize(1);
        auditProperties.getBatchWriter().setRetryTimes(2);
        auditProperties.getBatchWriter().setLocalBackupOnFailure(false);
        ClickHouseRequest.Mutation mutation = mockClickHouseRequest(
            new ClickHouseException(ClickHouseException.ERROR_TIMEOUT, "timeout", clickHouseNode));

        ClickHouseBatchWriter writer = new ClickHouseBatchWriter(clickHouseClient, clickHouseNode, auditProperties, new SimpleMeterRegistry());
        writer.init();

        try {
            writer.add(new AuditLogMainDTO());
            writer.scheduledFlush();

            waitUntil(() -> writer.getTotalFailed() >= 1, 10000);
            assertEquals(1, writer.getTotalFailed(), "超时异常最终应失败 1 条");
            verify(mutation, times(3)).executeAndWait();
        } finally {
            writer.destroy();
        }
    }

    @Test
    void should_not_retry_on_non_retryable_exception() throws Exception {
        AuditProperties auditProperties = new AuditProperties();
        auditProperties.getBatchWriter().setBatchSize(1);
        auditProperties.getBatchWriter().setRetryTimes(2);
        auditProperties.getBatchWriter().setLocalBackupOnFailure(false);
        ClickHouseRequest.Mutation mutation = mockClickHouseRequest(
            new ClickHouseException(60, "authentication failed", clickHouseNode));

        ClickHouseBatchWriter writer = new ClickHouseBatchWriter(clickHouseClient, clickHouseNode, auditProperties, new SimpleMeterRegistry());
        writer.init();

        try {
            writer.add(new AuditLogMainDTO());
            writer.scheduledFlush();

            waitUntil(() -> writer.getTotalFailed() >= 1, 5000);
            assertEquals(1, writer.getTotalFailed(), "不可重试异常最终应失败 1 条");
            verify(mutation, times(1)).executeAndWait();
        } finally {
            writer.destroy();
        }
    }

    @Test
    void should_retry_on_io_exception() throws Exception {
        AuditProperties auditProperties = new AuditProperties();
        auditProperties.getBatchWriter().setBatchSize(1);
        auditProperties.getBatchWriter().setRetryTimes(2);
        auditProperties.getBatchWriter().setLocalBackupOnFailure(false);
        ClickHouseRequest.Mutation mutation = mockClickHouseRequest(
            new RuntimeException("wrapped", new java.io.IOException("connection reset")));

        ClickHouseBatchWriter writer = new ClickHouseBatchWriter(clickHouseClient, clickHouseNode, auditProperties, new SimpleMeterRegistry());
        writer.init();

        try {
            writer.add(new AuditLogMainDTO());
            writer.scheduledFlush();

            waitUntil(() -> writer.getTotalFailed() >= 1, 10000);
            assertEquals(1, writer.getTotalFailed(), "IO 异常最终应失败 1 条");
            verify(mutation, times(3)).executeAndWait();
        } finally {
            writer.destroy();
        }
    }

    @Test
    void should_limit_concurrent_flush_tasks() throws Exception {
        AuditProperties auditProperties = new AuditProperties();
        auditProperties.getBatchWriter().setBatchSize(1);
        auditProperties.getBatchWriter().setMaxConcurrentFlushes(2);
        auditProperties.getBatchWriter().setRetryTimes(0);
        auditProperties.getBatchWriter().setLocalBackupOnFailure(false);

        CountDownLatch blockLatch = new CountDownLatch(2);
        CountDownLatch releaseLatch = new CountDownLatch(1);
        ClickHouseRequest.Mutation mutation = mockSlowClickHouseRequest(blockLatch, releaseLatch);

        ClickHouseBatchWriter writer = new ClickHouseBatchWriter(clickHouseClient, clickHouseNode, auditProperties, new SimpleMeterRegistry());
        writer.init();

        try {
            for (int i = 0; i < 5; i++) {
                writer.add(new AuditLogMainDTO());
            }
            writer.scheduledFlush();

            waitUntil(() -> writer.getActiveFlushCount() >= 2, 2000);
            assertEquals(2, writer.getActiveFlushCount(), "并发刷新任务数应达到上限 2");

            releaseLatch.countDown();
            waitUntil(() -> writer.getTotalWritten() >= 5, 10000);
            assertEquals(5, writer.getTotalWritten(), "全部 5 条日志最终应成功写入");
        } finally {
            writer.destroy();
        }
    }

    private ClickHouseRequest.Mutation mockSlowClickHouseRequest(CountDownLatch blockLatch, CountDownLatch releaseLatch) throws ClickHouseException {
        ClickHouseRequest<?> request = mock(ClickHouseRequest.class);
        ClickHouseRequest.Mutation mutation = mock(ClickHouseRequest.Mutation.class);
        when(request.write()).thenReturn(mutation);
        when(mutation.table(anyString())).thenReturn(mutation);
        when(mutation.format(any())).thenReturn(mutation);
        when(mutation.data(any(ByteArrayInputStream.class))).thenReturn(mutation);
        when(mutation.executeAndWait()).thenAnswer(invocation -> {
            blockLatch.countDown();
            releaseLatch.await();
            return mock(ClickHouseResponse.class);
        });
        doReturn(request).when(clickHouseClient).read(clickHouseNode);
        return mutation;
    }

    private void waitUntil(java.util.function.BooleanSupplier condition, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline && !condition.getAsBoolean()) {
            Thread.sleep(50);
        }
    }

    private ClickHouseRequest.Mutation mockClickHouseRequest(Throwable throwable) throws ClickHouseException {
        ClickHouseRequest<?> request = mock(ClickHouseRequest.class);
        ClickHouseRequest.Mutation mutation = mock(ClickHouseRequest.Mutation.class);
        when(request.write()).thenReturn(mutation);
        when(mutation.table(anyString())).thenReturn(mutation);
        when(mutation.format(any())).thenReturn(mutation);
        when(mutation.data(any(ByteArrayInputStream.class))).thenReturn(mutation);
        when(mutation.executeAndWait()).thenThrow(throwable);
        doReturn(request).when(clickHouseClient).read(clickHouseNode);
        return mutation;
    }

    private AuditLogMainDTO buildFullDto() {
        AuditLogMainDTO dto = new AuditLogMainDTO();
        dto.setId(12345L);
        dto.setServerTime(LocalDateTime.of(2026, 6, 12, 10, 30, 0, 0));
        dto.setEventDate(LocalDate.of(2026, 6, 12));
        dto.setClientTime(LocalDateTime.of(2026, 6, 12, 10, 29, 55, 0));
        dto.setEventTime(LocalDateTime.of(2026, 6, 12, 10, 30, 0, 0));
        dto.setDurationMs(120);
        dto.setRetentionDeadline(LocalDate.of(2026, 9, 12));
        dto.setLogSchemaVersion(3);
        dto.setCategory(AuditLogCategoryEnum.BUSINESS);
        dto.setLogType("USER_LOGIN");
        dto.setRiskLevel(10);
        dto.setPrincipalId("user_1");
        dto.setPrincipalType(AuditLogPrincipalTypeEnum.USER);
        dto.setPrincipalName("Carlos");
        dto.setTenantId("tenant_1");
        dto.setDeptId("dept_1");
        dto.setDeptName("Tech");
        dto.setDeptPath("1/2");
        dto.setTargetType("ORDER");
        dto.setTargetId("order_123");
        dto.setTargetName("测试订单");
        dto.setTargetSnapshot("{\"status\":\"PAID\"}");
        dto.setLogStatus(AuditLogStatusEnum.SUCCESS);
        dto.setResultCode("0");
        dto.setResultMessage("成功");
        dto.setOperation("login");
        dto.setApprovalComment("通过");
        dto.setClientIp("127.0.0.1");
        dto.setClientPort(8080);
        dto.setServerIp("10.0.0.1");
        dto.setUserAgent("Mozilla/5.0");
        dto.setDeviceFingerprint("fp_1");
        dto.setLocationCountry("CN");
        dto.setLocationProvince("GD");
        dto.setLocationCity("SZ");
        dto.setLocationLat(new BigDecimal("22.54"));
        dto.setLocationLon(new BigDecimal("114.06"));
        dto.setAuthType(AuditLogAuthTypeEnum.PASSWORD);
        dto.setAuthProvider(AuditLogAuthProviderEnum.LOCAL);
        dto.setRoles("[\"ADMIN\"]");
        dto.setPermissions("[\"order:read\"]");
        dto.setBizChannel(AuditLogBizChannelEnum.WEB);
        dto.setBizScene("LOGIN");
        dto.setBizOrderNo("order_123");
        dto.setRelatedBizIds("[\"inv_1\"]");
        dto.setMonetaryAmount(new BigDecimal("99.50"));
        dto.setProcessId("proc_1");
        dto.setBatchId("batch_1");
        dto.setBatchIndex(0);
        dto.setBatchTotal(1);
        dto.setTaskId("task_1");
        dto.setApproverId("approver_1");
        dto.setHasDataChange(true);
        dto.setEntityClass("com.carlos.Order");
        dto.setTableName("t_order");
        dto.setChangeSummary("修改价格");
        dto.setChangedFieldCount(1);
        dto.setOldData("{\"price\":100}");
        dto.setNewData("{\"price\":99.5}");
        dto.setOldDataCompressed(false);
        dto.setNewDataCompressed(false);
        dto.setTraceId("trace_1");
        dto.setSpanId("span_1");
        dto.setParentSpanId("parent_1");
        dto.setTracePath("a/b");
        dto.setDbQueryCount(3);
        dto.setDbQueryTimeMs(45);
        dto.setExternalCallCount(1);
        dto.setExternalCallTimeMs(20);
        dto.setCustomMetrics("{\"cache\":1}");
        dto.setRequestPayloadRef("req_ref");
        dto.setResponsePayloadRef("resp_ref");
        dto.setPayloadStorageType(AuditLogPayloadStorageTypeEnum.OSS);
        dto.setAppName("order-service");
        dto.setAppVersion("1.0.0");
        dto.setCluster("prod");
        dto.setHostName("host-1");
        dto.setTagKeys("[\"env\"]");
        dto.setTagValues("[\"prod\"]");
        dto.setAttachmentCount(0);
        dto.setAttachmentTypes("[]");
        dto.setAttachmentTotalSize(0L);
        dto.setFirstAttachmentRef("");
        dto.setAttachmentRefs("[]");
        dto.setDynamicTags("{\"source\":\"web\"}");
        dto.setDynamicExtras("{\"extra\":\"info\"}");
        dto.setCreatedTime(LocalDateTime.of(2026, 6, 12, 10, 30, 0, 0));
        dto.setUpdatedTime(LocalDateTime.of(2026, 6, 12, 10, 30, 30, 0));
        return dto;
    }

    @Test
    void should_increase_batch_size_when_flush_is_fast() throws Exception {
        AuditProperties auditProperties = new AuditProperties();
        auditProperties.getBatchWriter().setBatchSize(500);
        auditProperties.getBatchWriter().setAdaptiveBatchSize(true);
        auditProperties.getBatchWriter().setAdaptiveTargetFlushMs(1000);
        auditProperties.getBatchWriter().setAdaptiveBatchStep(100);
        auditProperties.getBatchWriter().setAdaptiveBatchMin(100);
        auditProperties.getBatchWriter().setAdaptiveBatchMax(5000);

        ClickHouseBatchWriter writer = new ClickHouseBatchWriter(clickHouseClient, clickHouseNode, auditProperties, new SimpleMeterRegistry());
        writer.init();
        assertEquals(500, writer.getEffectiveBatchSize(), "初始批次大小应为配置值");

        // 模拟快速刷新 (1ms, 远低于 1000ms 目标)
        Method adjustMethod = ClickHouseBatchWriter.class.getDeclaredMethod("adjustBatchSize", long.class);
        adjustMethod.setAccessible(true);
        adjustMethod.invoke(writer, TimeUnit.MILLISECONDS.toNanos(1));

        assertEquals(600, writer.getEffectiveBatchSize(), "快速刷新应增大批次");
    }

    @Test
    void should_decrease_batch_size_when_flush_is_slow() throws Exception {
        AuditProperties auditProperties = new AuditProperties();
        auditProperties.getBatchWriter().setBatchSize(500);
        auditProperties.getBatchWriter().setAdaptiveBatchSize(true);
        auditProperties.getBatchWriter().setAdaptiveTargetFlushMs(100);
        auditProperties.getBatchWriter().setAdaptiveBatchStep(100);
        auditProperties.getBatchWriter().setAdaptiveBatchMin(100);
        auditProperties.getBatchWriter().setAdaptiveBatchMax(5000);

        ClickHouseBatchWriter writer = new ClickHouseBatchWriter(clickHouseClient, clickHouseNode, auditProperties, new SimpleMeterRegistry());
        writer.init();
        assertEquals(500, writer.getEffectiveBatchSize());

        // 模拟慢刷新 (5000ms, 远高于 100ms 目标)
        Method adjustMethod = ClickHouseBatchWriter.class.getDeclaredMethod("adjustBatchSize", long.class);
        adjustMethod.setAccessible(true);
        adjustMethod.invoke(writer, TimeUnit.MILLISECONDS.toNanos(5000));

        assertEquals(400, writer.getEffectiveBatchSize(), "慢刷新应减小批次");
    }

    @Test
    void should_not_adjust_batch_size_when_disabled() throws Exception {
        AuditProperties auditProperties = new AuditProperties();
        auditProperties.getBatchWriter().setBatchSize(500);
        auditProperties.getBatchWriter().setAdaptiveBatchSize(false);

        ClickHouseBatchWriter writer = new ClickHouseBatchWriter(clickHouseClient, clickHouseNode, auditProperties, new SimpleMeterRegistry());
        writer.init();
        assertEquals(500, writer.getEffectiveBatchSize());

        Method adjustMethod = ClickHouseBatchWriter.class.getDeclaredMethod("adjustBatchSize", long.class);
        adjustMethod.setAccessible(true);
        adjustMethod.invoke(writer, TimeUnit.MILLISECONDS.toNanos(1));

        assertEquals(500, writer.getEffectiveBatchSize(), "禁用时应保持原批次大小");
    }

    @Test
    void should_respect_batch_size_boundaries() throws Exception {
        AuditProperties auditProperties = new AuditProperties();
        auditProperties.getBatchWriter().setBatchSize(500);
        auditProperties.getBatchWriter().setAdaptiveBatchSize(true);
        auditProperties.getBatchWriter().setAdaptiveTargetFlushMs(1000);
        auditProperties.getBatchWriter().setAdaptiveBatchStep(100);
        auditProperties.getBatchWriter().setAdaptiveBatchMin(100);
        auditProperties.getBatchWriter().setAdaptiveBatchMax(5000);

        ClickHouseBatchWriter writer = new ClickHouseBatchWriter(clickHouseClient, clickHouseNode, auditProperties, new SimpleMeterRegistry());
        writer.init();

        Method adjustMethod = ClickHouseBatchWriter.class.getDeclaredMethod("adjustBatchSize", long.class);
        adjustMethod.setAccessible(true);

        // 极端快：应停在 max
        for (int i = 0; i < 60; i++) {
            adjustMethod.invoke(writer, TimeUnit.MILLISECONDS.toNanos(1));
        }
        assertEquals(5000, writer.getEffectiveBatchSize(), "批次不应超过最大值 5000");

        // 极端慢：应停在 min
        for (int i = 0; i < 60; i++) {
            adjustMethod.invoke(writer, TimeUnit.MILLISECONDS.toNanos(10000));
        }
        assertEquals(100, writer.getEffectiveBatchSize(), "批次不应低于最小值 100");
    }
}
