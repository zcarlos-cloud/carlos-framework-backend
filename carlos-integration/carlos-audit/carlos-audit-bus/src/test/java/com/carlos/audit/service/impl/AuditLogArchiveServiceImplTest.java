package com.carlos.audit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carlos.audit.clickhouse.ClickHouseBatchWriter;
import com.carlos.audit.clickhouse.ClickHouseQueryClient;
import com.carlos.audit.config.AuditProperties;
import com.carlos.audit.manager.AuditLogArchiveRecordManager;
import com.carlos.audit.mapper.AuditLogArchiveRecordMapper;
import com.carlos.audit.pojo.dto.AuditLogArchiveRecordDTO;
import com.carlos.audit.pojo.dto.AuditLogMainDTO;
import com.carlos.audit.pojo.entity.AuditLogArchiveRecord;
import com.carlos.audit.pojo.enums.AuditLogArchiveStateEnum;
import com.carlos.audit.service.AuditLogArchiveRecordService;
import com.carlos.audit.storage.AuditLogStorage;
import com.carlos.oss.core.OssTemplate;
import com.carlos.oss.model.OssFile;
import com.clickhouse.client.ClickHouseClient;
import com.clickhouse.client.ClickHouseNode;
import com.clickhouse.client.ClickHouseRequest;
import com.clickhouse.client.ClickHouseResponse;
import com.clickhouse.data.ClickHouseValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link AuditLogArchiveServiceImpl} 单元测试
 *
 * @author Carlos
 * @date 2026-06-12
 */
@ExtendWith(MockitoExtension.class)
class AuditLogArchiveServiceImplTest {

    @Mock
    private ClickHouseQueryClient queryClient;

    @Mock
    private ClickHouseClient clickHouseClient;

    @Mock
    private ClickHouseNode clickHouseNode;

    @Mock
    private ClickHouseBatchWriter batchWriter;

    @Mock
    private AuditLogArchiveRecordService archiveRecordService;

    @Mock
    private AuditLogArchiveRecordManager archiveRecordManager;

    @Mock
    private AuditLogArchiveRecordMapper archiveRecordMapper;

    @Mock
    private AuditLogStorage auditLogStorage;

    @Mock
    private ObjectProvider<OssTemplate> ossTemplateProvider;

    @Mock
    private OssTemplate ossTemplate;

    private AuditProperties auditProperties;

    private AuditLogArchiveServiceImpl archiveService;

    @BeforeEach
    void setUp() {
        auditProperties = new AuditProperties();
        AuditProperties.Archive archive = auditProperties.getArchive();
        archive.setEnabled(true);
        archive.setColdDays(400);
        archive.setStorageType("OSS");
        archive.getOss().setBucketName("test-bucket");
        archive.getOss().setPrefix("audit-logs/");

        archiveService = new AuditLogArchiveServiceImpl(
            queryClient, clickHouseClient, clickHouseNode, batchWriter,
            archiveRecordService, archiveRecordManager, auditLogStorage,
            ossTemplateProvider, auditProperties);
    }

    @Test
    @DisplayName("archiveColdData：应归档完全早于截止日期的 ClickHouse 分区")
    void should_archive_partitions_older_than_cold_days() throws Exception {
        // given
        AuditLogArchiveServiceImpl.PartitionRow oldPartition = new AuditLogArchiveServiceImpl.PartitionRow();
        oldPartition.setPartition("202501");
        AuditLogArchiveServiceImpl.PartitionRow currentPartition = new AuditLogArchiveServiceImpl.PartitionRow();
        currentPartition.setPartition("202506");
        when(queryClient.queryForList(anyString(), eq(AuditLogArchiveServiceImpl.PartitionRow.class), any(ClickHouseValue[].class)))
            .thenReturn(List.of(oldPartition, currentPartition));

        AuditLogArchiveServiceImpl.CountRow countRow = new AuditLogArchiveServiceImpl.CountRow();
        countRow.setTotalCount(100L);
        when(queryClient.queryForObject(anyString(), eq(AuditLogArchiveServiceImpl.CountRow.class), any(ClickHouseValue[].class)))
            .thenReturn(countRow);

        @SuppressWarnings({"unchecked", "rawtypes"})
        ClickHouseRequest request = mock(ClickHouseRequest.class);
        when(clickHouseClient.read(clickHouseNode)).thenReturn(request);
        when(request.query(anyString())).thenReturn(request);
        when(request.executeAndWait()).thenReturn(mock(ClickHouseResponse.class));

        when(ossTemplateProvider.getIfAvailable()).thenReturn(ossTemplate);
        when(ossTemplate.putObject(anyString(), anyString(), any(InputStream.class), anyString()))
            .thenReturn(OssFile.builder().filePath("audit-logs/audit-archive-202501-xxx.csv").build());

        doAnswer(invocation -> {
            Path path = invocation.getArgument(1);
            Files.writeString(path, "a,b\n1,2\n");
            return 10L;
        }).when(queryClient).exportToFile(anyString(), any(Path.class), any(ClickHouseValue[].class));

        AtomicReference<String> addStatusRef = new AtomicReference<>();
        doAnswer(invocation -> {
            AuditLogArchiveRecordDTO dto = invocation.getArgument(0);
            addStatusRef.set(dto.getArchiveStatus());
            return null;
        }).when(archiveRecordService).addAuditLogArchiveRecord(any());

        // when
        archiveService.archiveColdData();

        // then
        assertEquals(AuditLogArchiveStateEnum.ARCHIVING.name(), addStatusRef.get());
        verify(queryClient, times(1)).queryForList(anyString(), eq(AuditLogArchiveServiceImpl.PartitionRow.class), any(ClickHouseValue[].class));
        verify(queryClient, times(1)).queryForObject(anyString(), eq(AuditLogArchiveServiceImpl.CountRow.class), any(ClickHouseValue[].class));
        verify(queryClient, times(1)).exportToFile(anyString(), any(Path.class), any(ClickHouseValue[].class));
        verify(ossTemplate, times(1)).putObject(eq("test-bucket"), anyString(), any(InputStream.class), eq("text/csv"));
        verify(request, times(1)).query(contains("DROP PARTITION 202501"));
        verify(request, times(1)).executeAndWait();

        ArgumentCaptor<AuditLogArchiveRecordDTO> updateCaptor = ArgumentCaptor.forClass(AuditLogArchiveRecordDTO.class);
        verify(archiveRecordService, times(1)).updateAuditLogArchiveRecord(updateCaptor.capture());
        AuditLogArchiveRecordDTO updated = updateCaptor.getValue();
        assertEquals(AuditLogArchiveStateEnum.COMPLETED.name(), updated.getArchiveStatus());
        assertEquals(100L, updated.getRecordCount());
        assertNotNull(updated.getFileSizeBytes());
        assertTrue(updated.getFileSizeBytes() > 0);
        assertNotNull(updated.getVerifyChecksum());
        assertTrue(updated.getStoragePath().startsWith("audit-logs/"));
    }

    @Test
    @DisplayName("archiveColdData：归档禁用时不应执行任何操作")
    void should_skip_archive_when_disabled() {
        // given
        auditProperties.getArchive().setEnabled(false);

        // when
        archiveService.archiveColdData();

        // then
        verify(queryClient, never()).queryForList(anyString(), any(Class.class), any(ClickHouseValue[].class));
        verify(archiveRecordService, never()).addAuditLogArchiveRecord(any());
    }

    @Test
    @DisplayName("restoreArchive：应从 OSS 下载 CSV 并恢复至 ClickHouse")
    void should_restore_archive_from_oss() throws Exception {
        // given
        String archiveId = "abc123def456";
        AuditLogArchiveRecord entity = new AuditLogArchiveRecord();
        entity.setId(1L);
        entity.setArchiveId(archiveId);
        entity.setArchiveStatus(AuditLogArchiveStateEnum.COMPLETED);
        entity.setStoragePath("audit-logs/audit-archive-202501-xxx.csv");

        when(archiveRecordManager.getBaseMapper()).thenReturn(archiveRecordMapper);
        @SuppressWarnings("unchecked")
        LambdaQueryWrapper<AuditLogArchiveRecord> wrapper = mock(LambdaQueryWrapper.class);
        when(archiveRecordManager.queryWrapper()).thenReturn(wrapper);
        when(wrapper.eq(any(), any())).thenReturn(wrapper);
        when(archiveRecordMapper.selectOne(wrapper)).thenReturn(entity);

        when(ossTemplateProvider.getIfAvailable()).thenReturn(ossTemplate);
        String csvContent = "id,server_time,log_type,operation,tenant_id\n"
            + "1001,2025-01-15 10:30:00.000,USER_LOGIN,用户登录,0\n"
            + "1002,2025-01-15 11:00:00.000,ORDER_CREATE,创建订单,0\n";
        when(ossTemplate.getObject(eq("test-bucket"), eq("audit-logs/audit-archive-202501-xxx.csv")))
            .thenReturn(new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8)));

        // when
        archiveService.restoreArchive(archiveId);

        // then
        verify(batchWriter, times(2)).add(any(AuditLogMainDTO.class));
        ArgumentCaptor<AuditLogArchiveRecordDTO> updateCaptor = ArgumentCaptor.forClass(AuditLogArchiveRecordDTO.class);
        verify(archiveRecordService, times(1)).updateAuditLogArchiveRecord(updateCaptor.capture());
        AuditLogArchiveRecordDTO updated = updateCaptor.getValue();
        assertEquals(AuditLogArchiveStateEnum.RESTORED.name(), updated.getArchiveStatus());
        assertEquals(archiveId, updated.getArchiveId());
    }

    @Test
    @DisplayName("restoreArchive：归档记录不存在时应抛出异常")
    void should_throw_when_archive_not_found() {
        // given
        String archiveId = "nonexistent";
        when(archiveRecordManager.getBaseMapper()).thenReturn(archiveRecordMapper);
        @SuppressWarnings("unchecked")
        LambdaQueryWrapper<AuditLogArchiveRecord> wrapper = mock(LambdaQueryWrapper.class);
        when(archiveRecordManager.queryWrapper()).thenReturn(wrapper);
        when(wrapper.eq(any(), any())).thenReturn(wrapper);
        when(archiveRecordMapper.selectOne(wrapper)).thenReturn(null);

        // when & then
        try {
            archiveService.restoreArchive(archiveId);
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("归档记录不存在"));
        }
        verify(batchWriter, never()).add(any());
    }

    @Test
    @DisplayName("restoreArchive：归档状态不允许恢复时应抛出异常")
    void should_throw_when_archive_status_invalid() {
        // given
        String archiveId = "abc123";
        AuditLogArchiveRecord entity = new AuditLogArchiveRecord();
        entity.setId(1L);
        entity.setArchiveId(archiveId);
        entity.setArchiveStatus(AuditLogArchiveStateEnum.ARCHIVING);

        when(archiveRecordManager.getBaseMapper()).thenReturn(archiveRecordMapper);
        @SuppressWarnings("unchecked")
        LambdaQueryWrapper<AuditLogArchiveRecord> wrapper = mock(LambdaQueryWrapper.class);
        when(archiveRecordManager.queryWrapper()).thenReturn(wrapper);
        when(wrapper.eq(any(), any())).thenReturn(wrapper);
        when(archiveRecordMapper.selectOne(wrapper)).thenReturn(entity);

        // when & then
        try {
            archiveService.restoreArchive(archiveId);
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("归档状态不允许恢复"));
        }
        verify(batchWriter, never()).add(any());
    }

    @Test
    @DisplayName("parseCsvWithNames：应正确解析 CSVWithNames 格式")
    void should_parse_csv_with_names() throws Exception {
        // given
        Path csvFile = Files.createTempFile("test-csv-", ".csv");
        String csvContent = "id,server_time,log_type,operation,tenant_id\n"
            + "1001,2025-01-15 10:30:00.000,USER_LOGIN,用户登录,0\n"
            + "1002,2025-01-15 11:00:00.000,ORDER_CREATE,\"创建,订单\",1\n";
        Files.writeString(csvFile, csvContent);

        // when
        List<AuditLogMainDTO> result = archiveService.parseCsvWithNames(csvFile);

        // then
        assertEquals(2, result.size());
        assertEquals("USER_LOGIN", result.get(0).getLogType());
        assertEquals("用户登录", result.get(0).getOperation());
        assertEquals("0", result.get(0).getTenantId());
        assertEquals("ORDER_CREATE", result.get(1).getLogType());
        assertEquals("创建,订单", result.get(1).getOperation());
        assertEquals("1", result.get(1).getTenantId());

        Files.deleteIfExists(csvFile);
    }
}
