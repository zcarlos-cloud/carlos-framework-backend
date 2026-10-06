package com.carlos.monitor.collect.sink;

import com.carlos.monitor.config.MonitorProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 监控数据备份恢复服务
 * 应用启动时扫描备份目录（carlos.monitor.sink.clickhouse.backup-dir），
 * 将失败落盘的 JSONLines 按表重放至 ClickHouse，完成后移入 processed/ 子目录
 * </p>
 *
 * @author Carlos
 * @date 2026年10月2日
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MonitorBackupRecovery {

    /**
     * 重放批次大小
     */
    private static final int REPLAY_BATCH_SIZE = 2000;

    private final MonitorClickHouseWriter clickHouseWriter;

    private final MonitorProperties monitorProperties;

    @PostConstruct
    public void recoverOnStartup() {
        String backupDir = monitorProperties.getSink().getClickhouse().getBackupDir();
        Path dir = Paths.get(backupDir);
        if (!Files.isDirectory(dir)) {
            log.info("监控备份目录不存在，跳过启动恢复: {}", backupDir);
            return;
        }
        List<Path> backupFiles = findBackupFiles(dir);
        if (backupFiles.isEmpty()) {
            return;
        }
        log.info("开始监控数据启动恢复，备份目录: {}，待处理文件数: {}", backupDir, backupFiles.size());
        int successFiles = 0;
        for (Path file : backupFiles) {
            try {
                if (recoverFile(file)) {
                    successFiles++;
                    moveToProcessed(file, dir);
                }
            } catch (Exception e) {
                log.error("恢复备份文件失败: {}", file.getFileName(), e);
            }
        }
        log.info("监控数据启动恢复完成，成功恢复 {}/{} 个文件", successFiles, backupFiles.size());
    }

    private List<Path> findBackupFiles(Path dir) {
        List<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.jsonl")) {
            for (Path path : stream) {
                files.add(path);
            }
        } catch (IOException e) {
            log.error("扫描监控备份目录失败: {}", dir, e);
        }
        files.sort(Path::compareTo);
        return files;
    }

    /**
     * 重放单个备份文件（文件名前缀即表名：{table}-backup-yyyy-MM-dd.jsonl）
     */
    private boolean recoverFile(Path file) throws Exception {
        String fileName = file.getFileName().toString();
        String table = resolveTable(fileName);
        if (table == null) {
            log.warn("备份文件名无法识别目标表，跳过: {}", fileName);
            return false;
        }
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        int recovered = 0;
        List<String> batch = new ArrayList<>(REPLAY_BATCH_SIZE);
        for (String line : lines) {
            if (line.isBlank()) {
                continue;
            }
            batch.add(line);
            if (batch.size() >= REPLAY_BATCH_SIZE) {
                clickHouseWriter.insertBatch(table, batch);
                recovered += batch.size();
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            clickHouseWriter.insertBatch(table, batch);
            recovered += batch.size();
        }
        log.info("恢复备份文件 {} -> 表 {}，共 {} 条记录", fileName, table, recovered);
        return true;
    }

    private String resolveTable(String fileName) {
        if (fileName.startsWith(MonitorClickHouseWriter.TABLE_METRIC_POINT)) {
            return MonitorClickHouseWriter.TABLE_METRIC_POINT;
        }
        if (fileName.startsWith(MonitorClickHouseWriter.TABLE_REDIS_SLOWLOG)) {
            return MonitorClickHouseWriter.TABLE_REDIS_SLOWLOG;
        }
        if (fileName.startsWith(MonitorClickHouseWriter.TABLE_DB_SLOW_SQL)) {
            return MonitorClickHouseWriter.TABLE_DB_SLOW_SQL;
        }
        return null;
    }

    private void moveToProcessed(Path file, Path backupDir) {
        Path processedDir = backupDir.resolve("processed");
        try {
            Files.createDirectories(processedDir);
            Files.move(file, processedDir.resolve(file.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("移动备份文件到 processed 目录失败: {}", file, e);
        }
    }
}
