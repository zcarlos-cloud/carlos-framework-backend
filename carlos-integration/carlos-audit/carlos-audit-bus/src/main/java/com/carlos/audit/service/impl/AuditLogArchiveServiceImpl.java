package com.carlos.audit.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.carlos.audit.api.pojo.enums.AuditErrorCode;
import com.carlos.audit.clickhouse.ClickHouseBatchWriter;
import com.carlos.audit.clickhouse.ClickHouseQueryClient;
import com.carlos.audit.config.AuditProperties;
import com.carlos.audit.manager.AuditLogArchiveRecordManager;
import com.carlos.audit.pojo.dto.AuditLogArchiveRecordDTO;
import com.carlos.audit.pojo.dto.AuditLogMainDTO;
import com.carlos.audit.pojo.entity.AuditLogArchiveRecord;
import com.carlos.audit.pojo.enums.AuditLogArchiveStateEnum;
import com.carlos.audit.service.AuditLogArchiveRecordService;
import com.carlos.audit.service.AuditLogArchiveService;
import com.carlos.audit.service.AuditSelfLogHelper;
import com.carlos.audit.storage.AuditLogStorage;
import com.carlos.core.exception.BusinessException;
import com.carlos.log.entity.OperationLog;
import com.carlos.oss.core.OssTemplate;
import com.carlos.oss.model.OssFile;
import com.clickhouse.client.ClickHouseClient;
import com.clickhouse.client.ClickHouseNode;
import com.clickhouse.client.ClickHouseResponse;
import com.clickhouse.data.value.ClickHouseStringValue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 审计日志冷数据归档服务实现
 *
 * @author Carlos
 * @date 2026-06-12
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "carlos.audit.archive", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AuditLogArchiveServiceImpl implements AuditLogArchiveService {

    private static final String TABLE = "audit_log_main_local";
    private static final DateTimeFormatter PARTITION_FORMAT = DateTimeFormatter.ofPattern("yyyyMM");
    private static final String LOG_TYPE = "AUDIT_ARCHIVE_OPERATION";
    private static final String TARGET_TYPE = "AUDIT_ARCHIVE";

    private final ClickHouseQueryClient queryClient;
    private final ClickHouseClient clickHouseClient;
    private final ClickHouseNode clickHouseNode;
    private final ClickHouseBatchWriter batchWriter;
    private final AuditLogArchiveRecordService archiveRecordService;
    private final AuditLogArchiveRecordManager archiveRecordManager;
    private final AuditLogStorage auditLogStorage;
    private final ObjectProvider<OssTemplate> ossTemplateProvider;
    private final AuditProperties auditProperties;

    @Override
    public void archiveColdData() {
        AuditProperties.Archive archiveConfig = auditProperties.getArchive();
        if (!archiveConfig.isEnabled()) {
            log.info("审计日志冷数据归档已禁用");
            return;
        }

        LocalDate archiveBeforeDate = LocalDate.now().minusDays(archiveConfig.getColdDays());
        List<String> partitions = findArchivablePartitions(archiveBeforeDate);
        if (partitions.isEmpty()) {
            log.debug("没有需要归档的 ClickHouse 分区，归档截止日期: {}", archiveBeforeDate);
            return;
        }

        log.info("开始审计日志冷数据归档，截止日期: {}，待归档分区数: {}", archiveBeforeDate, partitions.size());
        int successCount = 0;
        for (String partition : partitions) {
            if (archivePartition(partition, archiveBeforeDate)) {
                successCount++;
            }
        }
        log.info("审计日志冷数据归档完成，成功 {}/{} 个分区", successCount, partitions.size());
        writeSelfAudit(archiveBeforeDate, partitions.size(), successCount);
    }

    @Override
    public void restoreArchive(String archiveId) {
        log.info("开始恢复归档数据，archiveId: {}", archiveId);

        AuditLogArchiveRecord entity = archiveRecordManager.getBaseMapper()
            .selectOne(archiveRecordManager.queryWrapper()
                .eq(AuditLogArchiveRecord::getArchiveId, archiveId));
        if (entity == null) {
            throw AuditErrorCode.AUDIT_ARCHIVE_FAILED.exception("归档记录不存在: " + archiveId);
        }

        AuditLogArchiveStateEnum currentStatus = entity.getArchiveStatus();
        if (currentStatus != AuditLogArchiveStateEnum.COMPLETED
            && currentStatus != AuditLogArchiveStateEnum.RESTORED) {
            throw AuditErrorCode.AUDIT_ARCHIVE_FAILED.exception(
                "归档状态不允许恢复，当前状态: " + currentStatus + "，archiveId: " + archiveId);
        }

        String storagePath = entity.getStoragePath();
        if (storagePath == null || storagePath.isBlank()) {
            throw AuditErrorCode.AUDIT_ARCHIVE_FAILED.exception("归档存储路径为空，archiveId: " + archiveId);
        }

        OssTemplate ossTemplate = ossTemplateProvider.getIfAvailable();
        if (ossTemplate == null) {
            throw AuditErrorCode.AUDIT_ARCHIVE_FAILED.exception("OssTemplate 未启用，无法恢复归档");
        }

        AuditProperties.Archive.Oss oss = auditProperties.getArchive().getOss();
        String bucketName = oss.getBucketName();
        if (bucketName == null || bucketName.isBlank()) {
            throw AuditErrorCode.AUDIT_ARCHIVE_FAILED.exception("OSS bucketName 未配置");
        }

        Path tempFile = null;
        try {
            tempFile = downloadFromOss(ossTemplate, bucketName, storagePath);
            List<AuditLogMainDTO> restoredDtos = parseCsvWithNames(tempFile);
            if (restoredDtos.isEmpty()) {
                log.info("归档文件无有效数据，跳过恢复，archiveId: {}", archiveId);
                return;
            }

            for (AuditLogMainDTO dto : restoredDtos) {
                batchWriter.add(dto);
            }

            AuditLogArchiveRecordDTO updateDto = new AuditLogArchiveRecordDTO();
            updateDto.setId(entity.getId());
            updateDto.setArchiveId(archiveId);
            updateDto.setArchiveStatus(AuditLogArchiveStateEnum.RESTORED.name());
            archiveRecordService.updateAuditLogArchiveRecord(updateDto);

            log.info("归档数据恢复完成，archiveId: {}，恢复记录数: {}", archiveId, restoredDtos.size());
        } catch (Exception e) {
            log.error("归档数据恢复失败，archiveId: {}", archiveId, e);
            throw AuditErrorCode.AUDIT_ARCHIVE_FAILED.exception("归档数据恢复失败: " + archiveId, e);
        } finally {
            deleteTempFile(tempFile);
        }
    }

    private List<String> findArchivablePartitions(LocalDate archiveBeforeDate) {
        String sql = "SELECT DISTINCT partition FROM system.parts "
            + "WHERE database = :database AND table = :table AND active = 1 ORDER BY partition";
        String database = auditProperties.getClickhouse().getDatabase();
        List<PartitionRow> rows = queryClient.queryForList(sql, PartitionRow.class,
            ClickHouseStringValue.of(database), ClickHouseStringValue.of(TABLE));
        return rows.stream()
            .map(PartitionRow::getPartition)
            .filter(partition -> isPartitionFullyBefore(partition, archiveBeforeDate))
            .toList();
    }

    private boolean isPartitionFullyBefore(String partition, LocalDate archiveBeforeDate) {
        try {
            YearMonth yearMonth = YearMonth.parse(partition, PARTITION_FORMAT);
            LocalDate partitionEnd = yearMonth.atEndOfMonth();
            return !partitionEnd.isAfter(archiveBeforeDate);
        } catch (Exception e) {
            log.warn("无法解析 ClickHouse 分区名: {}", partition);
            return false;
        }
    }

    private boolean archivePartition(String partition, LocalDate archiveBeforeDate) {
        YearMonth yearMonth = YearMonth.parse(partition, PARTITION_FORMAT);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        log.info("开始归档分区 {}，数据范围: {} ~ {}", partition, startDate, endDate);

        Long recordCount = countRecords(startDate, endDate);
        if (recordCount == null || recordCount == 0L) {
            log.info("分区 {} 无数据，跳过归档", partition);
            return true;
        }

        AuditLogArchiveRecordDTO record = buildRecord(partition, startDate, endDate, recordCount);
        archiveRecordService.addAuditLogArchiveRecord(record);

        Path tempFile = null;
        try {
            tempFile = exportPartition(partition, startDate, endDate);
            String checksum = DigestUtil.md5Hex(tempFile.toFile());
            long fileSize = Files.size(tempFile);
            OssFile ossFile = uploadArchive(partition, tempFile);
            updateRecordSuccess(record, ossFile, fileSize, checksum);
            dropPartition(partition);
            archiveRecordService.updateAuditLogArchiveRecord(record);
            log.info("分区 {} 归档成功，记录数: {}，文件大小: {}，存储路径: {}",
                partition, recordCount, fileSize, record.getStoragePath());
            return true;
        } catch (Exception e) {
            log.error("分区 {} 归档失败", partition, e);
            updateRecordFailed(record);
            return false;
        } finally {
            deleteTempFile(tempFile);
        }
    }

    private Long countRecords(LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT count() AS totalCount FROM " + TABLE
            + " WHERE event_date >= toDate(:startDate) AND event_date <= toDate(:endDate)";
        CountRow row = queryClient.queryForObject(sql, CountRow.class,
            ClickHouseStringValue.of(startDate.toString()),
            ClickHouseStringValue.of(endDate.toString()));
        return row != null ? row.getTotalCount() : 0L;
    }

    private AuditLogArchiveRecordDTO buildRecord(String partition, LocalDate startDate,
                                                  LocalDate endDate, long recordCount) {
        AuditLogArchiveRecordDTO dto = new AuditLogArchiveRecordDTO();
        dto.setArchiveId(UUID.randomUUID().toString().replace("-", ""));
        dto.setArchiveDate(LocalDate.now());
        dto.setStartTime(LocalDateTime.of(startDate, LocalTime.MIN));
        dto.setEndTime(LocalDateTime.of(endDate, LocalTime.MAX));
        dto.setRecordCount(recordCount);
        dto.setStorageType(auditProperties.getArchive().getStorageType());
        dto.setArchiveStatus(AuditLogArchiveStateEnum.ARCHIVING.name());
        return dto;
    }

    private Path exportPartition(String partition, LocalDate startDate, LocalDate endDate) throws IOException {
        String sql = "SELECT * FROM " + TABLE
            + " WHERE event_date >= toDate(:startDate) AND event_date <= toDate(:endDate)"
            + " FORMAT CSVWithNames";
        Path tempFile = Files.createTempFile("audit-archive-" + partition + "-", ".csv");
        queryClient.exportToFile(sql, tempFile,
            ClickHouseStringValue.of(startDate.toString()),
            ClickHouseStringValue.of(endDate.toString()));
        return tempFile;
    }

    private OssFile uploadArchive(String partition, Path tempFile) throws IOException {
        AuditProperties.Archive.Oss oss = auditProperties.getArchive().getOss();
        String bucketName = oss.getBucketName();
        if (bucketName == null || bucketName.isBlank()) {
            throw AuditErrorCode.AUDIT_ARCHIVE_FAILED.exception("OSS bucketName 未配置");
        }
        OssTemplate ossTemplate = ossTemplateProvider.getIfAvailable();
        if (ossTemplate == null) {
            throw AuditErrorCode.AUDIT_ARCHIVE_FAILED.exception("OssTemplate 未启用");
        }
        String objectName = buildObjectName(partition);
        try (InputStream in = Files.newInputStream(tempFile)) {
            return ossTemplate.putObject(bucketName, objectName, in, "text/csv");
        }
    }

    private String buildObjectName(String partition) {
        AuditProperties.Archive.Oss oss = auditProperties.getArchive().getOss();
        String prefix = oss.getPrefix();
        if (prefix == null) {
            prefix = "";
        } else if (!prefix.isEmpty() && !prefix.endsWith("/")) {
            prefix = prefix + "/";
        }
        String archiveId = UUID.randomUUID().toString().replace("-", "");
        return prefix + "audit-archive-" + partition + "-" + archiveId + ".csv";
    }

    private void dropPartition(String partition) {
        String sql = "ALTER TABLE " + TABLE + " DROP PARTITION " + partition;
        try (ClickHouseResponse response = clickHouseClient.read(clickHouseNode).query(sql).executeAndWait()) {
            log.debug("已删除 ClickHouse 分区: {}", partition);
        } catch (Exception e) {
            throw AuditErrorCode.AUDIT_ARCHIVE_FAILED.exception("删除 ClickHouse 分区失败: " + partition, e);
        }
    }

    private void updateRecordSuccess(AuditLogArchiveRecordDTO record, OssFile ossFile,
                                      long fileSize, String checksum) {
        record.setFileSizeBytes(fileSize);
        record.setStoragePath(ossFile != null ? ossFile.getFilePath() : null);
        record.setVerifyChecksum(checksum);
        record.setArchiveStatus(AuditLogArchiveStateEnum.COMPLETED.name());
    }

    private void updateRecordFailed(AuditLogArchiveRecordDTO record) {
        record.setArchiveStatus(AuditLogArchiveStateEnum.FAILED.name());
        try {
            archiveRecordService.updateAuditLogArchiveRecord(record);
        } catch (Exception e) {
            log.error("更新归档记录失败状态失败, archiveId:{}", record.getArchiveId(), e);
        }
    }

    private void deleteTempFile(Path tempFile) {
        if (tempFile == null) {
            return;
        }
        try {
            Files.deleteIfExists(tempFile);
        } catch (IOException e) {
            log.warn("删除临时归档文件失败: {}", tempFile, e);
        }
    }

    private void writeSelfAudit(LocalDate archiveBeforeDate, int totalPartitions, int successCount) {
        try {
            OperationLog operationLog = AuditSelfLogHelper.build(
                LOG_TYPE, "执行审计日志冷数据归档", TARGET_TYPE,
                String.valueOf(LocalDate.now()), archiveBeforeDate.toString(),
                new ArchiveSummary(totalPartitions, successCount), "0");
            auditLogStorage.storeWithoutSampling(operationLog);
        } catch (Exception e) {
            log.error("写入归档自审计日志失败", e);
        }
    }

    private Path downloadFromOss(OssTemplate ossTemplate, String bucketName, String storagePath) throws IOException {
        try (InputStream in = ossTemplate.getObject(bucketName, storagePath)) {
            Path tempFile = Files.createTempFile("audit-restore-", ".csv");
            Files.copy(in, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return tempFile;
        }
    }

    /**
     * 解析 CSVWithNames 格式文件为 AuditLogMainDTO 列表
     *
     * <p>CSVWithNames 第一行为列名，后续行为数据。仅解析关键字段，
     * 其余字段由 ClickHouseBatchWriter.withRequiredFields 自动填充</p>
     */
    List<AuditLogMainDTO> parseCsvWithNames(Path csvFile) throws IOException {
        List<AuditLogMainDTO> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(Files.newInputStream(csvFile), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                return result;
            }
            String[] headers = parseCsvLine(headerLine);

            String dataLine;
            while ((dataLine = reader.readLine()) != null) {
                if (dataLine.isBlank()) {
                    continue;
                }
                String[] values = parseCsvLine(dataLine);
                AuditLogMainDTO dto = mapRow(headers, values);
                if (dto != null) {
                    result.add(dto);
                }
            }
        }
        return result;
    }

    private AuditLogMainDTO mapRow(String[] headers, String[] values) {
        AuditLogMainDTO dto = new AuditLogMainDTO();
        for (int i = 0; i < Math.min(headers.length, values.length); i++) {
            String key = headers[i];
            String value = values[i];
            if (value == null || value.isEmpty()) {
                continue;
            }
            switch (key) {
                case "id": dto.setId(parseLong(value)); break;
                case "server_time": dto.setServerTime(parseDateTime(value)); break;
                case "event_date": dto.setEventDate(parseDate(value)); break;
                case "client_time": dto.setClientTime(parseDateTime(value)); break;
                case "event_time": dto.setEventTime(parseDateTime(value)); break;
                case "duration_ms": dto.setDurationMs(parseInt(value)); break;
                case "retention_deadline": dto.setRetentionDeadline(parseDate(value)); break;
                case "log_schema_version": dto.setLogSchemaVersion(parseInt(value)); break;
                case "log_type": dto.setLogType(value); break;
                case "risk_level": dto.setRiskLevel(parseInt(value)); break;
                case "principal_id": dto.setPrincipalId(value); break;
                case "principal_name": dto.setPrincipalName(value); break;
                case "tenant_id": dto.setTenantId(value); break;
                case "dept_id": dto.setDeptId(value); break;
                case "dept_name": dto.setDeptName(value); break;
                case "dept_path": dto.setDeptPath(value); break;
                case "target_type": dto.setTargetType(value); break;
                case "target_id": dto.setTargetId(value); break;
                case "target_name": dto.setTargetName(value); break;
                case "target_snapshot": dto.setTargetSnapshot(value); break;
                case "operation": dto.setOperation(value); break;
                case "approval_comment": dto.setApprovalComment(value); break;
                case "result_code": dto.setResultCode(value); break;
                case "result_message": dto.setResultMessage(value); break;
                case "client_ip": dto.setClientIp(value); break;
                case "client_port": dto.setClientPort(parseInt(value)); break;
                case "server_ip": dto.setServerIp(value); break;
                case "user_agent": dto.setUserAgent(value); break;
                case "device_fingerprint": dto.setDeviceFingerprint(value); break;
                case "location_country": dto.setLocationCountry(value); break;
                case "location_province": dto.setLocationProvince(value); break;
                case "location_city": dto.setLocationCity(value); break;
                case "roles": dto.setRoles(value); break;
                case "permissions": dto.setPermissions(value); break;
                case "biz_scene": dto.setBizScene(value); break;
                case "biz_order_no": dto.setBizOrderNo(value); break;
                case "related_biz_ids": dto.setRelatedBizIds(value); break;
                case "process_id": dto.setProcessId(value); break;
                case "batch_id": dto.setBatchId(value); break;
                case "batch_index": dto.setBatchIndex(parseInt(value)); break;
                case "batch_total": dto.setBatchTotal(parseInt(value)); break;
                case "task_id": dto.setTaskId(value); break;
                case "approver_id": dto.setApproverId(value); break;
                case "entity_class": dto.setEntityClass(value); break;
                case "table_name": dto.setTableName(value); break;
                case "change_summary": dto.setChangeSummary(value); break;
                case "changed_field_count": dto.setChangedFieldCount(parseInt(value)); break;
                case "old_data": dto.setOldData(value); break;
                case "new_data": dto.setNewData(value); break;
                case "trace_id": dto.setTraceId(value); break;
                case "span_id": dto.setSpanId(value); break;
                case "parent_span_id": dto.setParentSpanId(value); break;
                case "trace_path": dto.setTracePath(value); break;
                case "db_query_count": dto.setDbQueryCount(parseInt(value)); break;
                case "db_query_time_ms": dto.setDbQueryTimeMs(parseInt(value)); break;
                case "external_call_count": dto.setExternalCallCount(parseInt(value)); break;
                case "external_call_time_ms": dto.setExternalCallTimeMs(parseInt(value)); break;
                case "custom_metrics": dto.setCustomMetrics(value); break;
                case "request_payload_ref": dto.setRequestPayloadRef(value); break;
                case "response_payload_ref": dto.setResponsePayloadRef(value); break;
                case "app_name": dto.setAppName(value); break;
                case "app_version": dto.setAppVersion(value); break;
                case "cluster": dto.setCluster(value); break;
                case "host_name": dto.setHostName(value); break;
                case "tag_keys": dto.setTagKeys(value); break;
                case "tag_values": dto.setTagValues(value); break;
                case "attachment_count": dto.setAttachmentCount(parseInt(value)); break;
                case "attachment_types": dto.setAttachmentTypes(value); break;
                case "attachment_total_size": dto.setAttachmentTotalSize(parseLong(value)); break;
                case "first_attachment_ref": dto.setFirstAttachmentRef(value); break;
                case "attachment_refs": dto.setAttachmentRefs(value); break;
                case "dynamic_tags": dto.setDynamicTags(value); break;
                case "dynamic_extras": dto.setDynamicExtras(value); break;
                case "created_time": dto.setCreatedTime(parseDateTime(value)); break;
                case "updated_time": dto.setUpdatedTime(parseDateTime(value)); break;
                default: break;
            }
        }
        return dto;
    }

    private String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        sb.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    sb.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    fields.add(sb.toString());
                    sb.setLength(0);
                } else {
                    sb.append(c);
                }
            }
        }
        fields.add(sb.toString());
        return fields.toArray(new String[0]);
    }

    private Long parseLong(String value) {
        try { return Long.parseLong(value); } catch (NumberFormatException e) { return null; }
    }

    private Integer parseInt(String value) {
        try { return Integer.parseInt(value); } catch (NumberFormatException e) { return null; }
    }

    private LocalDate parseDate(String value) {
        try { return LocalDate.parse(value); } catch (Exception e) { return null; }
    }

    private LocalDateTime parseDateTime(String value) {
        try {
            if (value.contains("T")) {
                return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
            return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } catch (Exception e2) {
                return null;
            }
        }
    }

    @Data
    static class PartitionRow {
        private String partition;
    }

    @Data
    static class CountRow {
        private Long totalCount;
    }

    @Data
    @AllArgsConstructor
    private static class ArchiveSummary {
        private int totalPartitions;
        private int successCount;
    }
}
