package com.carlos.audit.recovery;

import com.carlos.audit.clickhouse.ClickHouseBatchWriter;
import com.carlos.audit.config.AuditProperties;
import com.carlos.audit.pojo.dto.AuditLogMainDTO;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 备份 CSV 恢复服务
 *
 * <p>应用启动时扫描备份目录，将失败的审计日志 CSV 回写至 ClickHouse，
 * 处理完成后移入 processed/ 子目录</p>
 *
 * @author Carlos
 * @date 2026-06-12
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "carlos.audit.batch-writer", name = "recovery-on-startup", havingValue = "true")
public class BackupRecoveryService {

    private static final DateTimeFormatter BACKUP_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ClickHouseBatchWriter batchWriter;
    private final AuditProperties auditProperties;

    @PostConstruct
    public void recoverOnStartup() {
        String backupPath = auditProperties.getBatchWriter().getLocalBackupPath();
        Path dir = Paths.get(backupPath);
        if (!Files.isDirectory(dir)) {
            log.info("备份目录不存在，跳过启动恢复: {}", backupPath);
            return;
        }

        List<Path> csvFiles = findCsvFiles(dir);
        if (csvFiles.isEmpty()) {
            log.debug("备份目录无 CSV 文件，跳过启动恢复");
            return;
        }

        log.info("开始启动恢复，备份目录: {}，待处理文件数: {}", backupPath, csvFiles.size());
        int totalRecovered = 0;
        int successFiles = 0;

        for (Path csvFile : csvFiles) {
            try {
                int recovered = recoverFile(csvFile);
                if (recovered > 0) {
                    totalRecovered += recovered;
                    successFiles++;
                    moveToProcessed(csvFile, dir);
                }
            } catch (Exception e) {
                log.error("恢复备份文件失败: {}", csvFile.getFileName(), e);
            }
        }

        log.info("启动恢复完成，成功恢复 {}/{} 个文件，共 {} 条记录",
            successFiles, csvFiles.size(), totalRecovered);
    }

    private List<Path> findCsvFiles(Path dir) {
        List<Path> csvFiles = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.csv")) {
            for (Path path : stream) {
                csvFiles.add(path);
            }
        } catch (IOException e) {
            log.error("扫描备份目录失败: {}", dir, e);
        }
        csvFiles.sort(Path::compareTo);
        return csvFiles;
    }

    int recoverFile(Path csvFile) throws IOException {
        int count = 0;
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(Files.newInputStream(csvFile), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                log.warn("备份文件为空: {}", csvFile.getFileName());
                return 0;
            }

            String dataLine;
            while ((dataLine = reader.readLine()) != null) {
                if (dataLine.isBlank()) {
                    continue;
                }
                AuditLogMainDTO dto = parseBackupLine(dataLine);
                if (dto != null) {
                    batchWriter.add(dto);
                    count++;
                }
            }
        }
        log.info("恢复备份文件 {}，共 {} 条记录", csvFile.getFileName(), count);
        return count;
    }

    private AuditLogMainDTO parseBackupLine(String line) {
        String[] fields = parseCsvLine(line);
        if (fields.length < 7) {
            log.warn("备份行字段不足，跳过: {}", line);
            return null;
        }
        // 格式: backup_time,id,server_time,log_type,principal_id,operation,reason
        AuditLogMainDTO dto = new AuditLogMainDTO();
        try {
            dto.setId(parseLong(fields[1]));
            dto.setServerTime(parseDateTime(fields[2]));
            dto.setLogType(fields[3]);
            dto.setPrincipalId(fields[4]);
            dto.setOperation(fields[5]);
        } catch (Exception e) {
            log.warn("解析备份行失败，跳过: {}", line, e);
            return null;
        }
        return dto;
    }

    private void moveToProcessed(Path csvFile, Path backupDir) {
        Path processedDir = backupDir.resolve("processed");
        try {
            Files.createDirectories(processedDir);
            Path target = processedDir.resolve(csvFile.getFileName());
            Files.move(csvFile, target, StandardCopyOption.REPLACE_EXISTING);
            log.debug("备份文件已移至 processed: {}", target);
        } catch (IOException e) {
            log.error("移动备份文件到 processed 目录失败: {}", csvFile, e);
        }
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

    private LocalDateTime parseDateTime(String value) {
        try {
            return LocalDateTime.parse(value, BACKUP_TIME_FORMATTER);
        } catch (Exception e) {
            return null;
        }
    }
}
