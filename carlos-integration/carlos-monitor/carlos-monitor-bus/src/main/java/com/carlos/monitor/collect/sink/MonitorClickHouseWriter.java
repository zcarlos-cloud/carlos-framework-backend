package com.carlos.monitor.collect.sink;

import cn.hutool.core.util.StrUtil;
import com.carlos.json.JsonUtils;
import com.carlos.job.client.annotation.AutoRegisterJob;
import com.carlos.job.client.annotation.ScheduleJob;
import com.carlos.monitor.collect.spi.MetricPoint;
import com.carlos.monitor.collect.spi.SlowSqlRow;
import com.carlos.monitor.collect.spi.SlowlogRow;
import com.carlos.monitor.config.MonitorProperties;
import com.clickhouse.client.ClickHouseClient;
import com.clickhouse.client.ClickHouseException;
import com.clickhouse.client.ClickHouseNode;
import com.clickhouse.client.ClickHouseResponse;
import com.clickhouse.data.ClickHouseFormat;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * <p>
 * 监控指标 ClickHouse 批量写入器（双缓冲 + JSONEachRow 原生 client 插入）
 * 复刻 carlos-audit ClickHouseBatchWriter 模式：每次 flush 新建批次列表、
 * 循环重试替代递归、失败落盘备份、Micrometer 指标；支持三张表
 * （monitor_metric_point / monitor_redis_slowlog / monitor_db_slow_sql）
 * </p>
 *
 * @author Carlos
 * @date 2026年10月2日
 */
@Slf4j
@Component
public class MonitorClickHouseWriter {

    /**
     * CK 表名
     */
    public static final String TABLE_METRIC_POINT = "monitor_metric_point";

    public static final String TABLE_REDIS_SLOWLOG = "monitor_redis_slowlog";

    public static final String TABLE_DB_SLOW_SQL = "monitor_db_slow_sql";

    private static final DateTimeFormatter CK_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /**
     * 单缓冲上限（溢出丢弃最旧数据，防御性背压）
     */
    private static final int MAX_BUFFER_SIZE = 100_000;

    /**
     * 重试次数
     */
    private static final int MAX_RETRIES = 3;

    private static final long MAX_BACKOFF_MS = 60_000L;

    /**
     * 并发刷新上限
     */
    private static final int MAX_CONCURRENT_FLUSHES = 4;

    private final ClickHouseClient clickHouseClient;

    private final ClickHouseNode clickHouseNode;

    private final MonitorProperties monitorProperties;

    private final MeterRegistry meterRegistry;

    // 三类数据的无锁缓冲（size() 为 O(n)，用 AtomicInteger 维护近似计数用于背压判断）
    private final ConcurrentLinkedQueue<MetricPoint> metricBuffer = new ConcurrentLinkedQueue<>();
    private final AtomicInteger metricSize = new AtomicInteger(0);
    private final ConcurrentLinkedQueue<SlowlogRow> slowlogBuffer = new ConcurrentLinkedQueue<>();
    private final AtomicInteger slowlogSize = new AtomicInteger(0);
    private final ConcurrentLinkedQueue<SlowSqlRow> slowSqlBuffer = new ConcurrentLinkedQueue<>();
    private final AtomicInteger slowSqlSize = new AtomicInteger(0);

    private ExecutorService flushExecutor;

    private Semaphore flushSemaphore;

    private final AtomicLong totalWritten = new AtomicLong(0);
    private final AtomicLong totalFailed = new AtomicLong(0);
    private final AtomicLong totalDiscarded = new AtomicLong(0);

    private Counter writtenCounter;
    private Counter failedCounter;
    private Counter discardedCounter;
    private Timer flushTimer;

    public MonitorClickHouseWriter(@Qualifier("monitorClickHouseClient") ClickHouseClient clickHouseClient,
                                   @Qualifier("monitorClickHouseNode") ClickHouseNode clickHouseNode,
                                   MonitorProperties monitorProperties,
                                   MeterRegistry meterRegistry) {
        this.clickHouseClient = clickHouseClient;
        this.clickHouseNode = clickHouseNode;
        this.monitorProperties = monitorProperties;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void init() {
        log.info("初始化监控 ClickHouse 批量写入器，batchSize={}, flushIntervalMs={}",
            monitorProperties.getSink().getClickhouse().getBatchSize(),
            monitorProperties.getSink().getClickhouse().getFlushIntervalMs());
        flushExecutor = Executors.newVirtualThreadPerTaskExecutor();
        flushSemaphore = new Semaphore(MAX_CONCURRENT_FLUSHES);

        writtenCounter = Counter.builder("carlos.monitor.sink.written")
            .description("成功写入 ClickHouse 的监控数据条数")
            .register(meterRegistry);
        failedCounter = Counter.builder("carlos.monitor.sink.failed")
            .description("写入 ClickHouse 失败的监控数据条数")
            .register(meterRegistry);
        discardedCounter = Counter.builder("carlos.monitor.sink.discarded")
            .description("因缓冲区溢出被丢弃的监控数据条数")
            .register(meterRegistry);
        flushTimer = Timer.builder("carlos.monitor.sink.flush.duration")
            .description("ClickHouse 批量刷新耗时")
            .register(meterRegistry);
        Gauge.builder("carlos.monitor.sink.buffer.size", this, w -> (double) w.getActiveBufferSize())
            .description("当前缓冲区中的监控数据条数")
            .register(meterRegistry);
    }

    @PreDestroy
    public void destroy() {
        log.info("关闭监控 ClickHouse 批量写入器，执行最终刷新");
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
     * 添加指标点到缓冲区
     */
    public void addMetricPoint(MetricPoint point) {
        if (point == null) {
            return;
        }
        offerWithBackpressure(metricBuffer, metricSize, point);
        if (metricSize.get() >= batchSize()) {
            flush();
        }
    }

    /**
     * 添加 Redis 慢日志行到缓冲区
     */
    public void addSlowlog(SlowlogRow row) {
        if (row == null) {
            return;
        }
        offerWithBackpressure(slowlogBuffer, slowlogSize, row);
        if (slowlogSize.get() >= batchSize()) {
            flush();
        }
    }

    /**
     * 添加慢 SQL 行到缓冲区
     */
    public void addSlowSql(SlowSqlRow row) {
        if (row == null) {
            return;
        }
        offerWithBackpressure(slowSqlBuffer, slowSqlSize, row);
        if (slowSqlSize.get() >= batchSize()) {
            flush();
        }
    }

    /**
     * 周期刷新（兜底机制，由 carlos-job 调度中心派发；攒够 batchSize 时 add 会立即刷新）
     */
    @ScheduleJob("monitorClickHouseFlushHandler")
    @AutoRegisterJob(cron = "0/10 * * * * ?", description = "监控指标批量写入 ClickHouse 周期刷新（每 10 秒兜底）", author = "carlos")
    public void scheduledFlush() {
        flush();
    }

    /**
     * 刷新全部缓冲
     */
    public void flush() {
        flushBuffer(metricBuffer, metricSize, TABLE_METRIC_POINT, MonitorClickHouseWriter::metricPointToJson);
        flushBuffer(slowlogBuffer, slowlogSize, TABLE_REDIS_SLOWLOG, MonitorClickHouseWriter::slowlogToJson);
        flushBuffer(slowSqlBuffer, slowSqlSize, TABLE_DB_SLOW_SQL, MonitorClickHouseWriter::slowSqlToJson);
    }

    private int batchSize() {
        return monitorProperties.getSink().getClickhouse().getBatchSize();
    }

    /**
     * 背压：缓冲区溢出时丢弃最旧数据（指标允许丢失，下一采集周期会再来）
     */
    private <T> void offerWithBackpressure(ConcurrentLinkedQueue<T> buffer, AtomicInteger size, T item) {
        if (size.get() >= MAX_BUFFER_SIZE) {
            int target = MAX_BUFFER_SIZE / 2;
            int discarded = 0;
            while (size.get() > target) {
                T dropped = buffer.poll();
                if (dropped == null) {
                    break;
                }
                size.decrementAndGet();
                discarded++;
            }
            totalDiscarded.addAndGet(discarded);
            discardedCounter.increment(discarded);
            log.warn("监控数据缓冲区溢出，DROP_OLDEST 丢弃 {} 条最旧数据", discarded);
        }
        buffer.offer(item);
        size.incrementAndGet();
    }

    private <T> void flushBuffer(ConcurrentLinkedQueue<T> buffer, AtomicInteger size,
                                 String table, Function<T, String> jsonMapper) {
        int batch = batchSize();
        while (true) {
            if (buffer.peek() == null) {
                return;
            }
            if (!flushSemaphore.tryAcquire()) {
                log.debug("当前并发刷新任务数已达上限，跳过本次刷新, table:{}", table);
                return;
            }
            List<String> jsonLines = new ArrayList<>(batch);
            for (int i = 0; i < batch; i++) {
                T item = buffer.poll();
                if (item == null) {
                    break;
                }
                jsonLines.add(jsonMapper.apply(item));
                size.decrementAndGet();
            }
            if (jsonLines.isEmpty()) {
                flushSemaphore.release();
                return;
            }
            flushExecutor.submit(() -> doFlush(table, jsonLines));
        }
    }

    /**
     * 执行写入（带指数退避循环重试，失败落盘备份）
     */
    private void doFlush(String table, List<String> jsonLines) {
        int size = jsonLines.size();
        try {
            for (int retry = 0; retry <= MAX_RETRIES; retry++) {
                try {
                    if (retry > 0) {
                        Thread.sleep(Math.min(1000L * (1L << (retry - 1)), MAX_BACKOFF_MS));
                    }
                    long startNs = System.nanoTime();
                    insertBatch(table, jsonLines);
                    flushTimer.record(System.nanoTime() - startNs, TimeUnit.NANOSECONDS);
                    totalWritten.addAndGet(size);
                    writtenCounter.increment(size);
                    log.debug("批量写入 ClickHouse 成功，table:{}, 条数:{}", table, size);
                    return;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("批量写入 ClickHouse 被中断，table:{}, 条数:{}", table, size);
                    break;
                } catch (Exception e) {
                    if (!isRetryable(e) || retry >= MAX_RETRIES) {
                        log.error("批量写入 ClickHouse 失败，table:{}, 重试次数:{}/{}, 条数:{}",
                            table, retry, MAX_RETRIES, size, e);
                    } else {
                        log.warn("批量写入 ClickHouse 失败将重试，table:{}, 重试:{}/{}, 条数:{}, error:{}",
                            table, retry, MAX_RETRIES, size, e.getMessage());
                    }
                    if (!isRetryable(e)) {
                        break;
                    }
                }
            }

            totalFailed.addAndGet(size);
            failedCounter.increment(size);
            log.error("批量写入 ClickHouse 最终失败，table:{}, 条数:{}", table, size);
            backupToLocal(table, jsonLines);
        } finally {
            flushSemaphore.release();
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
                    || code == ClickHouseException.ERROR_ABORTED;
            }
            current = current.getCause();
        }
        return false;
    }

    /**
     * 使用 ClickHouse 官方 Java Client 批量插入 API（FORMAT JSONEachRow，备份恢复重放亦走此入口）
     *
     * @param table     目标表
     * @param jsonLines JSONEachRow 行
     * @throws Exception 写入异常
     */
    public void insertBatch(String table, List<String> jsonLines) throws Exception {
        String data = StrUtil.join("\n", jsonLines);
        try (ClickHouseResponse ignored = clickHouseClient.read(clickHouseNode)
            .write()
            .table(table)
            .format(ClickHouseFormat.JSONEachRow)
            .data(new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)))
            .executeAndWait()
        ) {
            // 写入操作，无需处理响应体
        }
    }

    /**
     * 本地磁盘备份（失败兜底，JSONLines 原样落盘，恢复时直接重放）
     */
    private void backupToLocal(String table, List<String> jsonLines) {
        try {
            Path dir = Paths.get(monitorProperties.getSink().getClickhouse().getBackupDir());
            Files.createDirectories(dir);
            Path file = dir.resolve(table + "-backup-" + LocalDate.now() + ".jsonl");
            Files.write(file, jsonLines, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            log.info("监控数据已本地备份，文件: {}, 条数: {}", file, jsonLines.size());
        } catch (IOException e) {
            log.error("本地备份监控数据失败, table:{}", table, e);
        }
    }

    // ============ 行 JSON 构建（JSONEachRow，字段名对齐 CK 列） ============

    private static String metricPointToJson(MetricPoint point) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("instance_id", point.instanceId());
        row.put("metric_source", point.source().getCode());
        row.put("metric_name", point.metricName());
        row.put("node", point.node());
        row.put("tags", point.tags());
        row.put("value", point.value());
        row.put("ts", formatDateTime(point.ts()));
        row.put("event_date", formatDate(point.ts()));
        return JsonUtils.toJson(row);
    }

    private static String slowlogToJson(SlowlogRow slowlog) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("instance_id", slowlog.instanceId());
        row.put("node", slowlog.node() == null ? "" : slowlog.node());
        row.put("slowlog_id", slowlog.slowlogId());
        row.put("ts", formatDateTime(slowlog.ts()));
        row.put("duration_us", slowlog.durationUs());
        row.put("command", slowlog.command());
        row.put("client_addr", slowlog.clientAddr() == null ? "" : slowlog.clientAddr());
        row.put("event_date", formatDate(slowlog.ts()));
        return JsonUtils.toJson(row);
    }

    private static String slowSqlToJson(SlowSqlRow slowSql) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("instance_id", slowSql.instanceId());
        row.put("fingerprint", slowSql.fingerprint());
        row.put("sample_sql", slowSql.sampleSql());
        row.put("db_name", slowSql.dbName());
        row.put("exec_count", slowSql.execCount());
        row.put("avg_ms", slowSql.avgMs());
        row.put("max_ms", slowSql.maxMs());
        row.put("last_exec_time", formatDateTime(slowSql.lastExecTime()));
        row.put("collect_time", formatDateTime(slowSql.collectTime()));
        row.put("event_date", formatDate(slowSql.collectTime()));
        return JsonUtils.toJson(row);
    }

    private static String formatDateTime(Instant instant) {
        if (instant == null) {
            instant = Instant.now();
        }
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).format(CK_DATETIME_FORMATTER);
    }

    private static String formatDate(Instant instant) {
        if (instant == null) {
            instant = Instant.now();
        }
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).toLocalDate().toString();
    }

    // ============ 监控指标 ============

    public int getActiveBufferSize() {
        return metricSize.get() + slowlogSize.get() + slowSqlSize.get();
    }

    public long getTotalWritten() {
        return totalWritten.get();
    }

    public long getTotalFailed() {
        return totalFailed.get();
    }

    public long getTotalDiscarded() {
        return totalDiscarded.get();
    }
}
