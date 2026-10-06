package com.carlos.audit.clickhouse;

import com.carlos.audit.api.pojo.enums.AuditLogCategoryEnum;
import com.carlos.audit.api.pojo.enums.AuditLogPrincipalTypeEnum;
import com.carlos.audit.api.pojo.enums.AuditLogStatusEnum;
import com.carlos.audit.config.AuditProperties;
import com.carlos.audit.config.BackPressureStrategy;
import com.carlos.audit.pojo.dto.AuditLogMainDTO;
import com.carlos.json.JsonUtils;
import com.carlos.job.client.annotation.AutoRegisterJob;
import com.carlos.job.client.annotation.ScheduleJob;
import com.carlos.snowflake.SnowflakeUtil;
import com.clickhouse.client.ClickHouseClient;
import com.clickhouse.client.ClickHouseException;
import com.clickhouse.client.ClickHouseNode;
import com.clickhouse.client.ClickHouseResponse;
import com.clickhouse.data.ClickHouseFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * <p>
 * ClickHouse 批量写入器（双缓冲 + ClickHouseClient 原生批量插入）
 * </p>
 *
 * <p>优化要点：</p>
 * <ul>
 *   <li>修复双缓冲 clear() 污染异步数据的致命 Bug，每次 flush 后创建新 ArrayList 实例</li>
 *   <li>使用 ClickHouse 官方 Java Client 批量插入 API（FORMAT JSONEachRow），彻底消除 SQL 字符串拼接与注入风险</li>
 *   <li>循环重试替代递归重试，避免栈溢出风险</li>
 *   <li>destroy() 优雅关闭，awaitTermination 等待异步任务完成</li>
 *   <li>本地磁盘备份兜底，配置 localBackupOnFailure 即可启用</li>
 *   <li>统一在 Java 端生成 id 和时间戳，避免客户端与服务端时间不一致</li>
 *   <li>防御性背压：可配置策略（DROP_OLDEST / DROP_NEWEST / BLOCK / SAMPLE），防止 OOM</li>
 * </ul>
 *
 * @author Carlos
 * @date 2026-03-06
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClickHouseBatchWriter {

    private final ClickHouseClient clickHouseClient;
    private final ClickHouseNode clickHouseNode;
    private final AuditProperties auditProperties;
    private final MeterRegistry meterRegistry;

    // 无锁缓冲区（ConcurrentLinkedQueue 的 size() 为 O(n)，使用 AtomicInteger 维护近似计数用于背压判断）
    private final ConcurrentLinkedQueue<AuditLogMainDTO> activeBuffer = new ConcurrentLinkedQueue<>();
    private final AtomicInteger bufferSize = new AtomicInteger(0);

    // 异步执行器
    private ExecutorService flushExecutor;

    // 并发刷新信号量
    private Semaphore flushSemaphore;

    // 统计指标
    private final AtomicLong totalWritten = new AtomicLong(0);
    private final AtomicLong totalFailed = new AtomicLong(0);
    private final AtomicLong bufferOverflow = new AtomicLong(0);
    private final AtomicLong totalDiscarded = new AtomicLong(0);

    // 自适应批量大小：当前有效的批次大小
    private final AtomicInteger effectiveBatchSize = new AtomicInteger(0);

    private Counter writtenCounter;
    private Counter failedCounter;
    private Counter discardedCounter;
    private Timer flushTimer;

    private static final DateTimeFormatter CK_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final DateTimeFormatter BACKUP_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @PostConstruct
    public void init() {
        int maxConcurrentFlushes = auditProperties.getBatchWriter().getMaxConcurrentFlushes();
        log.info("初始化 ClickHouse 批量写入器，batchSize={}, flushIntervalMs={}, maxConcurrentFlushes={}",
            auditProperties.getBatchWriter().getBatchSize(),
            auditProperties.getBatchWriter().getFlushInterval(),
            maxConcurrentFlushes);
        flushExecutor = Executors.newVirtualThreadPerTaskExecutor();
        flushSemaphore = new Semaphore(maxConcurrentFlushes);
        effectiveBatchSize.set(auditProperties.getBatchWriter().getBatchSize());

        writtenCounter = Counter.builder("carlos.audit.clickhouse.written")
            .description("成功写入 ClickHouse 的审计日志条数")
            .register(meterRegistry);
        failedCounter = Counter.builder("carlos.audit.clickhouse.failed")
            .description("写入 ClickHouse 失败的审计日志条数")
            .register(meterRegistry);
        discardedCounter = Counter.builder("carlos.audit.clickhouse.discarded")
            .description("因缓冲区溢出被丢弃的审计日志条数")
            .register(meterRegistry);
        flushTimer = Timer.builder("carlos.audit.clickhouse.flush.duration")
            .description("ClickHouse 批量刷新耗时")
            .register(meterRegistry);
        Gauge.builder("carlos.audit.clickhouse.buffer.size", this, w -> (double) w.getActiveBufferSize())
            .description("当前缓冲区中的审计日志条数")
            .register(meterRegistry);
        Gauge.builder("carlos.audit.clickhouse.active.flushes", this, w -> (double) w.getActiveFlushCount())
            .description("当前正在执行的刷新任务数")
            .register(meterRegistry);
    }

    @PreDestroy
    public void destroy() {
        log.info("关闭 ClickHouse 批量写入器，执行最终刷新");
        flush();
        if (flushExecutor != null) {
            flushExecutor.shutdown();
            try {
                if (!flushExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                    log.warn("刷新任务未在 30 秒内完成，强制关闭");
                    flushExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                flushExecutor.shutdownNow();
            }
        }
    }

    /**
     * 添加日志到缓冲区
     */
    public void add(AuditLogMainDTO dto) {
        if (dto == null) {
            return;
        }

        dto = withRequiredFields(dto);

        int maxBufferSize = auditProperties.getBatchWriter().getMaxBufferSize();
        if (bufferSize.get() >= maxBufferSize) {
            bufferOverflow.incrementAndGet();
            if (!acquireSpace(maxBufferSize)) {
                return;
            }
        }

        activeBuffer.offer(dto);
        bufferSize.incrementAndGet();

        int batchSize = effectiveBatchSize.get();
        // 达到批次大小，立即刷新
        if (bufferSize.get() >= batchSize) {
            flush();
        }
    }

    /**
     * 背压处理，返回 true 表示可以继续写入当前日志
     */
    private boolean acquireSpace(int maxBufferSize) {
        BackPressureStrategy strategy = auditProperties.getBatchWriter().getBackPressureStrategy();
        switch (strategy) {
            case DROP_OLDEST:
                dropOldestUntil(maxBufferSize / 2);
                return true;
            case DROP_NEWEST:
                totalDiscarded.incrementAndGet();
                discardedCounter.increment();
                return false;
            case BLOCK:
                blockUntilSpace(maxBufferSize);
                return true;
            case SAMPLE:
                return sampleAndMakeSpace(maxBufferSize);
            default:
                dropOldestUntil(maxBufferSize / 2);
                return true;
        }
    }

    private void dropOldestUntil(int targetSize) {
        int discardedCount = 0;
        // 限制单次最大丢弃数量，避免并发持续入队导致无限循环
        int maxDiscard = Math.max(targetSize, 100);
        while (bufferSize.get() > targetSize && discardedCount < maxDiscard) {
            AuditLogMainDTO discarded = activeBuffer.poll();
            if (discarded == null) {
                break;
            }
            bufferSize.decrementAndGet();
            discardedCount++;
            totalDiscarded.incrementAndGet();
        }
        if (discardedCount > 0) {
            discardedCounter.increment(discardedCount);
            log.warn("审计日志缓冲区异常溢出，DROP_OLDEST 丢弃 {} 条最旧数据", discardedCount);
        }
    }

    private void blockUntilSpace(int maxBufferSize) {
        int timeoutMs = auditProperties.getBatchWriter().getBackPressureBlockTimeoutMs();
        long deadline = System.currentTimeMillis() + 5000;
        while (bufferSize.get() >= maxBufferSize && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(timeoutMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private boolean sampleAndMakeSpace(int maxBufferSize) {
        double sampleRate = auditProperties.getBatchWriter().getBackPressureSampleRate();
        sampleRate = Math.max(0.0D, Math.min(1.0D, sampleRate));
        if (ThreadLocalRandom.current().nextDouble() >= sampleRate) {
            totalDiscarded.incrementAndGet();
            discardedCounter.increment();
            return false;
        }
        dropOldestUntil(maxBufferSize - 1);
        return true;
    }

    /**
     * 周期刷新（兜底机制）
     *
     * <p>由 carlos-job 调度中心派发，每 10 秒触发一次；缓冲区攒够 batchSize 时 {@link #add} 会立即刷新。
     * 调度周期以 job_info 为准（库是唯一真相源），如需调整请改任务管理后台。
     * 注意：调度中心强制最小调度间隔 10 秒（{@code JobScheduleManagerImpl.MIN_INTERVAL_MS}），
     * 注解里写更小的间隔会在注册时被 400 拒绝。</p>
     */
    @ScheduleJob("auditClickHouseFlushHandler")
    @AutoRegisterJob(cron = "0/10 * * * * ?", description = "审计日志批量写入 ClickHouse 周期刷新（每 10 秒兜底）", author = "carlos")
    public void scheduledFlush() {
        flush();
    }

    /**
     * 执行刷新（从无锁队列中按批次 drain 到本地列表）
     */
    private void flush() {
        int batchSize = effectiveBatchSize.get();
        while (true) {
            if (activeBuffer.peek() == null) {
                return;
            }
            if (!flushSemaphore.tryAcquire()) {
                log.debug("当前并发刷新任务数已达上限，跳过本次刷新");
                return;
            }
            List<AuditLogMainDTO> toFlush = new ArrayList<>(batchSize);
            for (int i = 0; i < batchSize; i++) {
                AuditLogMainDTO dto = activeBuffer.poll();
                if (dto == null) {
                    break;
                }
                toFlush.add(dto);
                bufferSize.decrementAndGet();
            }
            if (toFlush.isEmpty()) {
                flushSemaphore.release();
                return;
            }
            final int size = toFlush.size();
            flushExecutor.submit(() -> doFlush(toFlush, size));
        }
    }

    private static final long MAX_BACKOFF_MS = 60_000L;

    /**
     * 执行写入（带指数退避循环重试）
     */
    private void doFlush(List<AuditLogMainDTO> logs, int size) {
        int maxRetries = auditProperties.getBatchWriter().getRetryTimes();

        try {
            for (int retry = 0; retry <= maxRetries; retry++) {
                try {
                    if (retry > 0) {
                        long backoffMs = Math.min(1000L * (1L << (retry - 1)), MAX_BACKOFF_MS);
                        Thread.sleep(backoffMs);
                    }
                    long startNs = System.nanoTime();
                    executeBatchInsert(logs);
                    long elapsedNs = System.nanoTime() - startNs;
                    flushTimer.record(elapsedNs, TimeUnit.NANOSECONDS);
                    adjustBatchSize(elapsedNs);
                    totalWritten.addAndGet(size);
                    writtenCounter.increment(size);
                    log.debug("批量写入 ClickHouse 成功，条数: {}", size);
                    return;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("批量写入 ClickHouse 被中断，数据条数: {}", size);
                    break;
                } catch (Exception e) {
                    boolean retryable = isRetryable(e);
                    if (retryable && retry < maxRetries) {
                        long nextBackoff = Math.min(1000L * (1L << retry), MAX_BACKOFF_MS);
                        log.warn("批量写入 ClickHouse 失败，将在 {}ms 后重试，重试次数: {}/{}，数据条数: {}",
                            nextBackoff, retry, maxRetries, size, e);
                    } else if (retryable) {
                        log.warn("批量写入 ClickHouse 失败，已是最后一次重试，重试次数: {}/{}，数据条数: {}",
                            retry, maxRetries, size, e);
                    } else {
                        log.error("批量写入 ClickHouse 失败，遇到不可重试异常，数据条数: {}", size, e);
                        break;
                    }
                }
            }

            totalFailed.addAndGet(size);
            failedCounter.increment(size);
            log.error("批量写入 ClickHouse 最终失败，数据条数: {}", size);

            if (auditProperties.getBatchWriter().isLocalBackupOnFailure()) {
                backupToLocal(logs);
            }
        } finally {
            flushSemaphore.release();
            // 释放信号量后尝试继续处理缓冲区中可能堆积的下一批数据
            flush();
        }
    }

    /**
     * 判断异常是否可重试（递归解包装）
     */
    private boolean isRetryable(Throwable e) {
        Throwable current = e;
        while (current != null) {
            if (current instanceof IOException) {
                return true;
            }
            if (current instanceof ClickHouseException) {
                int code = ((ClickHouseException) current).getErrorCode();
                return code == ClickHouseException.ERROR_NETWORK
                    || code == ClickHouseException.ERROR_TIMEOUT
                    || code == ClickHouseException.ERROR_ABORTED
                    || code == ClickHouseException.ERROR_SESSION_NOT_FOUND
                    || code == ClickHouseException.ERROR_SESSION_IS_LOCKED;
            }
            current = current.getCause();
        }
        return false;
    }

    /**
     * 使用 ClickHouse 官方 Java Client 批量插入 API（FORMAT JSONEachRow）
     */
    private void executeBatchInsert(List<AuditLogMainDTO> logs) throws Exception {
        byte[] data = buildJsonEachRow(logs);
        try (ClickHouseResponse response = clickHouseClient.read(clickHouseNode)
            .write()
            .table("audit_log_main_local")
            .format(ClickHouseFormat.JSONEachRow)
            .data(new ByteArrayInputStream(data))
            .executeAndWait()
        ) {
            // 写入操作，无需处理响应体

        }
    }

    /**
     * 将日志列表构建为 JSONEachRow 字节流
     */
    private byte[] buildJsonEachRow(List<AuditLogMainDTO> logs) {
        StringBuilder sb = new StringBuilder();
        for (AuditLogMainDTO log : logs) {
            sb.append(toJsonLine(log)).append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 将单条审计日志转换为 JSONEachRow 格式的一行 JSON
     */
    private String toJsonLine(AuditLogMainDTO log) {
        return JsonUtils.toJson(ClickHouseRow.from(log));
    }

    private static String formatDateTime(LocalDateTime dt) {
        return dt != null ? dt.format(CK_DATETIME_FORMATTER) : null;
    }

    private static String formatDate(LocalDate d) {
        return d != null ? d.toString() : null;
    }

    /**
     * 确保 DTO 包含所有非空必填字段，返回新的 DTO 对象（遵循不可变性原则）
     */
    private AuditLogMainDTO withRequiredFields(AuditLogMainDTO source) {
        if (isComplete(source)) {
            return source;
        }
        AuditLogMainDTO dto = new AuditLogMainDTO();
        BeanUtils.copyProperties(source, dto);

        if (dto.getServerTime() == null) {
            dto.setServerTime(LocalDateTime.now());
        }
        if (dto.getEventDate() == null) {
            dto.setEventDate(dto.getServerTime().toLocalDate());
        }
        if (dto.getId() == null) {
            dto.setId(SnowflakeUtil.longId());
        }
        if (dto.getRetentionDeadline() == null) {
            dto.setRetentionDeadline(dto.getEventDate());
        }
        if (dto.getLogSchemaVersion() == null) {
            dto.setLogSchemaVersion(3);
        }
        if (dto.getCategory() == null) {
            dto.setCategory(AuditLogCategoryEnum.SYSTEM);
        }
        if (dto.getLogType() == null) {
            dto.setLogType("UNKNOWN");
        }
        if (dto.getRiskLevel() == null) {
            dto.setRiskLevel(0);
        }
        if (dto.getPrincipalId() == null) {
            dto.setPrincipalId("");
        }
        if (dto.getPrincipalType() == null) {
            dto.setPrincipalType(AuditLogPrincipalTypeEnum.USER);
        }
        if (dto.getTenantId() == null) {
            dto.setTenantId("0");
        }
        if (dto.getLogStatus() == null) {
            dto.setLogStatus(AuditLogStatusEnum.SUCCESS);
        }
        if (dto.getOperation() == null) {
            dto.setOperation("");
        }
        if (dto.getCreatedTime() == null) {
            dto.setCreatedTime(LocalDateTime.now());
        }
        return dto;
    }

    private boolean isComplete(AuditLogMainDTO dto) {
        return dto.getServerTime() != null
            && dto.getEventDate() != null
            && dto.getId() != null
            && dto.getRetentionDeadline() != null
            && dto.getLogSchemaVersion() != null
            && dto.getCategory() != null
            && dto.getLogType() != null
            && dto.getRiskLevel() != null
            && dto.getPrincipalId() != null
            && dto.getPrincipalType() != null
            && dto.getTenantId() != null
            && dto.getLogStatus() != null
            && dto.getOperation() != null
            && dto.getCreatedTime() != null;
    }

    /**
     * 本地磁盘备份（失败兜底）
     */
    private void backupToLocal(List<AuditLogMainDTO> logs) {
        try {
            Path dir = Paths.get(auditProperties.getBatchWriter().getLocalBackupPath());
            Files.createDirectories(dir);
            String fileName = "audit-backup-" + LocalDate.now() + ".csv";
            Path file = dir.resolve(fileName);

            boolean isNewFile = !Files.exists(file);
            try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                if (isNewFile) {
                    writer.write("backup_time,id,server_time,log_type,principal_id,operation,reason");
                    writer.newLine();
                }
                String backupTime = LocalDateTime.now().format(BACKUP_TIME_FORMATTER);
                for (AuditLogMainDTO log : logs) {
                    writer.write(String.format("%s,%s,%s,%s,%s,%s,%s",
                        backupTime,
                        log.getId(),
                        log.getServerTime().format(BACKUP_TIME_FORMATTER),
                        escapeCsv(log.getLogType()),
                        escapeCsv(log.getPrincipalId()),
                        escapeCsv(log.getOperation()),
                        "CLICKHOUSE_WRITE_FAILED"
                    ));
                    writer.newLine();
                }
            }
            log.info("审计日志已本地备份，文件: {}，条数: {}", file, logs.size());
        } catch (IOException e) {
            log.error("本地备份审计日志失败", e);
        }
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n") || escaped.contains("\r")) {
            escaped = "\"" + escaped + "\"";
        }
        return escaped;
    }

    // ============ 监控指标 ============

    public int getEffectiveBatchSize() {
        return effectiveBatchSize.get();
    }

    /**
     * 根据最近一次刷新耗时自适应调整批量大小
     *
     * <p>flushElapsedNs 低于目标耗时 → 增大批次（提高吞吐）；
     * 高于目标耗时 → 减小批次（降低延迟）</p>
     */
    private void adjustBatchSize(long flushElapsedNs) {
        if (!auditProperties.getBatchWriter().isAdaptiveBatchSize()) {
            return;
        }
        long flushElapsedMs = TimeUnit.NANOSECONDS.toMillis(flushElapsedNs);
        long targetMs = auditProperties.getBatchWriter().getAdaptiveTargetFlushMs();
        int step = auditProperties.getBatchWriter().getAdaptiveBatchStep();
        int min = auditProperties.getBatchWriter().getAdaptiveBatchMin();
        int max = auditProperties.getBatchWriter().getAdaptiveBatchMax();

        int current = effectiveBatchSize.get();
        int next;
        if (flushElapsedMs < targetMs) {
            next = Math.min(current + step, max);
        } else {
            next = Math.max(current - step, min);
        }

        if (next != current && effectiveBatchSize.compareAndSet(current, next)) {
            log.debug("自适应批量大小调整: {} → {} (flush 耗时 {}ms)", current, next, flushElapsedMs);
        }
    }

    public int getActiveBufferSize() {
        return bufferSize.get();
    }

    public int getActiveFlushCount() {
        if (flushSemaphore == null) {
            return 0;
        }
        int max = auditProperties.getBatchWriter().getMaxConcurrentFlushes();
        return max - flushSemaphore.availablePermits();
    }

    public long getTotalWritten() {
        return totalWritten.get();
    }

    public long getTotalFailed() {
        return totalFailed.get();
    }

    public long getBufferOverflow() {
        return bufferOverflow.get();
    }

    public long getTotalDiscarded() {
        return totalDiscarded.get();
    }

    /**
     * 专门用于 ClickHouse JSONEachRow 序列化的内部类
     * 参考 AuditLogMain 实体类及 AuditLogMainDTO 的完整字段定义
     */
    @Data
    static class ClickHouseRow {
        // 1. 主键与时间体系
        @JsonProperty("server_time")
        private String serverTime;
        @JsonProperty("event_date")
        private String eventDate;
        @JsonProperty("id")
        private Long id;
        @JsonProperty("client_time")
        private String clientTime;
        @JsonProperty("event_time")
        private String eventTime;
        @JsonProperty("duration_ms")
        private Integer durationMs;
        @JsonProperty("retention_deadline")
        private String retentionDeadline;

        // 2. 分类与版本
        @JsonProperty("log_schema_version")
        private Integer logSchemaVersion;
        @JsonProperty("category")
        private String category;
        @JsonProperty("log_type")
        private String logType;
        @JsonProperty("risk_level")
        private Integer riskLevel;

        // 3. 操作者体系
        @JsonProperty("principal_id")
        private String principalId;
        @JsonProperty("principal_type")
        private String principalType;
        @JsonProperty("principal_name")
        private String principalName;
        @JsonProperty("tenant_id")
        private String tenantId;
        @JsonProperty("dept_id")
        private String deptId;
        @JsonProperty("dept_name")
        private String deptName;
        @JsonProperty("dept_path")
        private String deptPath;

        // 4. 目标对象体系
        @JsonProperty("target_type")
        private String targetType;
        @JsonProperty("target_id")
        private String targetId;
        @JsonProperty("target_name")
        private String targetName;
        @JsonProperty("target_snapshot")
        private String targetSnapshot;

        // 5. 操作结果
        @JsonProperty("approval_comment")
        private String approvalComment;
        @JsonProperty("log_status")
        private String logStatus;
        @JsonProperty("result_code")
        private String resultCode;
        @JsonProperty("result_message")
        private String resultMessage;
        @JsonProperty("operation")
        private String operation;

        // 6. 网络与设备
        @JsonProperty("client_ip")
        private String clientIp;
        @JsonProperty("client_port")
        private Integer clientPort;
        @JsonProperty("server_ip")
        private String serverIp;
        @JsonProperty("user_agent")
        private String userAgent;
        @JsonProperty("device_fingerprint")
        private String deviceFingerprint;
        @JsonProperty("location_country")
        private String locationCountry;
        @JsonProperty("location_province")
        private String locationProvince;
        @JsonProperty("location_city")
        private String locationCity;
        @JsonProperty("location_lat")
        private java.math.BigDecimal locationLat;
        @JsonProperty("location_lon")
        private java.math.BigDecimal locationLon;

        // 7. 认证与权限
        @JsonProperty("auth_type")
        private String authType;
        @JsonProperty("auth_provider")
        private String authProvider;
        @JsonProperty("roles")
        private String roles;
        @JsonProperty("permissions")
        private String permissions;

        // 8. 业务上下文
        @JsonProperty("biz_channel")
        private String bizChannel;
        @JsonProperty("biz_scene")
        private String bizScene;
        @JsonProperty("biz_order_no")
        private String bizOrderNo;
        @JsonProperty("related_biz_ids")
        private String relatedBizIds;
        @JsonProperty("monetary_amount")
        private java.math.BigDecimal monetaryAmount;
        @JsonProperty("process_id")
        private String processId;

        // 9. 批量操作
        @JsonProperty("batch_id")
        private String batchId;
        @JsonProperty("batch_index")
        private Integer batchIndex;
        @JsonProperty("batch_total")
        private Integer batchTotal;

        // 10. 审批工作流
        @JsonProperty("task_id")
        private String taskId;
        @JsonProperty("approver_id")
        private String approverId;

        // 11. 数据变更详情
        @JsonProperty("has_data_change")
        private Boolean hasDataChange;
        @JsonProperty("entity_class")
        private String entityClass;
        @JsonProperty("table_name")
        private String tableName;
        @JsonProperty("change_summary")
        private String changeSummary;
        @JsonProperty("changed_field_count")
        private Integer changedFieldCount;
        @JsonProperty("old_data")
        private String oldData;
        @JsonProperty("new_data")
        private String newData;
        @JsonProperty("old_data_compressed")
        private Boolean oldDataCompressed;
        @JsonProperty("new_data_compressed")
        private Boolean newDataCompressed;

        // 12. 技术上下文
        @JsonProperty("trace_id")
        private String traceId;
        @JsonProperty("span_id")
        private String spanId;
        @JsonProperty("parent_span_id")
        private String parentSpanId;
        @JsonProperty("trace_path")
        private String tracePath;
        @JsonProperty("db_query_count")
        private Integer dbQueryCount;
        @JsonProperty("db_query_time_ms")
        private Integer dbQueryTimeMs;
        @JsonProperty("external_call_count")
        private Integer externalCallCount;
        @JsonProperty("external_call_time_ms")
        private Integer externalCallTimeMs;
        @JsonProperty("custom_metrics")
        private String customMetrics;

        // Payload 存储引用
        @JsonProperty("request_payload_ref")
        private String requestPayloadRef;
        @JsonProperty("response_payload_ref")
        private String responsePayloadRef;
        @JsonProperty("payload_storage_type")
        private String payloadStorageType;

        // 环境信息
        @JsonProperty("app_name")
        private String appName;
        @JsonProperty("app_version")
        private String appVersion;
        @JsonProperty("cluster")
        private String cluster;
        @JsonProperty("host_name")
        private String hostName;

        // 13. 动态标签（ClickHouse Array 列）
        @JsonProperty("tag_keys")
        private List<String> tagKeys;
        @JsonProperty("tag_values")
        private List<String> tagValues;

        // 14. 附件元数据
        @JsonProperty("attachment_count")
        private Integer attachmentCount;
        @JsonProperty("attachment_types")
        private List<String> attachmentTypes;
        @JsonProperty("attachment_total_size")
        private Long attachmentTotalSize;
        @JsonProperty("first_attachment_ref")
        private String firstAttachmentRef;
        @JsonProperty("attachment_refs")
        private String attachmentRefs;

        // 15. 动态扩展
        @JsonProperty("dynamic_tags")
        private String dynamicTags;
        @JsonProperty("dynamic_extras")
        private String dynamicExtras;

        // 16. 系统字段
        @JsonProperty("created_time")
        private String createdTime;
        @JsonProperty("updated_time")
        private String updatedTime;

        /**
         * 从 AuditLogMainDTO 构造 ClickHouseRow（避免反射复制）
         */
        public static ClickHouseRow from(AuditLogMainDTO log) {
            ClickHouseRow row = new ClickHouseRow();
            row.setId(log.getId());
            row.setServerTime(ClickHouseBatchWriter.formatDateTime(log.getServerTime()));
            row.setEventDate(ClickHouseBatchWriter.formatDate(log.getEventDate()));
            row.setClientTime(ClickHouseBatchWriter.formatDateTime(log.getClientTime()));
            row.setEventTime(ClickHouseBatchWriter.formatDateTime(log.getEventTime()));
            row.setDurationMs(log.getDurationMs());
            row.setRetentionDeadline(ClickHouseBatchWriter.formatDate(log.getRetentionDeadline()));
            row.setLogSchemaVersion(log.getLogSchemaVersion());
            row.setCategory(enumName(log.getCategory()));
            row.setLogType(log.getLogType());
            row.setRiskLevel(log.getRiskLevel());
            row.setPrincipalId(log.getPrincipalId());
            row.setPrincipalType(enumName(log.getPrincipalType()));
            row.setPrincipalName(log.getPrincipalName());
            row.setTenantId(log.getTenantId());
            row.setDeptId(log.getDeptId());
            row.setDeptName(log.getDeptName());
            row.setDeptPath(log.getDeptPath());
            row.setTargetType(log.getTargetType());
            row.setTargetId(log.getTargetId());
            row.setTargetName(log.getTargetName());
            row.setTargetSnapshot(log.getTargetSnapshot());
            row.setApprovalComment(log.getApprovalComment());
            row.setLogStatus(enumName(log.getLogStatus()));
            row.setResultCode(log.getResultCode());
            row.setResultMessage(log.getResultMessage());
            row.setOperation(log.getOperation());
            row.setClientIp(log.getClientIp());
            row.setClientPort(log.getClientPort());
            row.setServerIp(log.getServerIp());
            row.setUserAgent(log.getUserAgent());
            row.setDeviceFingerprint(log.getDeviceFingerprint());
            row.setLocationCountry(log.getLocationCountry());
            row.setLocationProvince(log.getLocationProvince());
            row.setLocationCity(log.getLocationCity());
            row.setLocationLat(log.getLocationLat());
            row.setLocationLon(log.getLocationLon());
            row.setAuthType(enumName(log.getAuthType()));
            row.setAuthProvider(enumName(log.getAuthProvider()));
            row.setRoles(log.getRoles());
            row.setPermissions(log.getPermissions());
            row.setBizChannel(enumName(log.getBizChannel()));
            row.setBizScene(log.getBizScene());
            row.setBizOrderNo(log.getBizOrderNo());
            row.setRelatedBizIds(log.getRelatedBizIds());
            row.setMonetaryAmount(log.getMonetaryAmount());
            row.setProcessId(log.getProcessId());
            row.setBatchId(log.getBatchId());
            row.setBatchIndex(log.getBatchIndex());
            row.setBatchTotal(log.getBatchTotal());
            row.setTaskId(log.getTaskId());
            row.setApproverId(log.getApproverId());
            row.setHasDataChange(log.getHasDataChange());
            row.setEntityClass(log.getEntityClass());
            row.setTableName(log.getTableName());
            row.setChangeSummary(log.getChangeSummary());
            row.setChangedFieldCount(log.getChangedFieldCount());
            row.setOldData(log.getOldData());
            row.setNewData(log.getNewData());
            row.setOldDataCompressed(log.getOldDataCompressed());
            row.setNewDataCompressed(log.getNewDataCompressed());
            row.setTraceId(log.getTraceId());
            row.setSpanId(log.getSpanId());
            row.setParentSpanId(log.getParentSpanId());
            row.setTracePath(log.getTracePath());
            row.setDbQueryCount(log.getDbQueryCount());
            row.setDbQueryTimeMs(log.getDbQueryTimeMs());
            row.setExternalCallCount(log.getExternalCallCount());
            row.setExternalCallTimeMs(log.getExternalCallTimeMs());
            row.setCustomMetrics(log.getCustomMetrics());
            row.setRequestPayloadRef(log.getRequestPayloadRef());
            row.setResponsePayloadRef(log.getResponsePayloadRef());
            row.setPayloadStorageType(enumName(log.getPayloadStorageType()));
            row.setAppName(log.getAppName());
            row.setAppVersion(log.getAppVersion());
            row.setCluster(log.getCluster());
            row.setHostName(log.getHostName());
            row.setTagKeys(splitComma(log.getTagKeys()));
            row.setTagValues(splitComma(log.getTagValues()));
            row.setAttachmentCount(log.getAttachmentCount());
            row.setAttachmentTypes(splitComma(log.getAttachmentTypes()));
            row.setAttachmentTotalSize(log.getAttachmentTotalSize());
            row.setFirstAttachmentRef(log.getFirstAttachmentRef());
            row.setAttachmentRefs(log.getAttachmentRefs());
            row.setDynamicTags(log.getDynamicTags());
            row.setDynamicExtras(log.getDynamicExtras());
            row.setCreatedTime(ClickHouseBatchWriter.formatDateTime(log.getCreatedTime()));
            row.setUpdatedTime(ClickHouseBatchWriter.formatDateTime(log.getUpdatedTime()));
            return row;
        }

        private static String enumName(Enum<?> value) {
            return value != null ? value.name() : null;
        }

        /**
         * 将逗号分隔字符串拆分为 ClickHouse Array 列对应的列表（null/空白保留为 null）
         */
        private static List<String> splitComma(String value) {
            if (value == null || value.isBlank()) {
                return null;
            }
            return Arrays.stream(value.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        }
    }
}
