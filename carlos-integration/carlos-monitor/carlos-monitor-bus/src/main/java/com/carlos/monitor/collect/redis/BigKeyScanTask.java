package com.carlos.monitor.collect.redis;

import cn.hutool.core.util.StrUtil;
import com.carlos.job.client.annotation.AutoRegisterJob;
import com.carlos.job.client.annotation.ScheduleJob;
import com.carlos.monitor.config.MonitorProperties;
import com.carlos.monitor.enums.InstanceTypeEnum;
import com.carlos.monitor.enums.MonitorErrorCode;
import com.carlos.monitor.instance.connection.InstanceConnectionRegistry;
import com.carlos.monitor.instance.connection.ManagedConnection;
import com.carlos.monitor.instance.connection.ManagedConnectionFactory;
import com.carlos.monitor.instance.connection.RedisManagedConnection;
import com.carlos.monitor.instance.manager.MonitorInstanceManager;
import com.carlos.monitor.instance.pojo.dto.MonitorInstanceDTO;
import com.carlos.monitor.redis.manager.MonitorBigkeyReportItemManager;
import com.carlos.monitor.redis.manager.MonitorBigkeyReportManager;
import com.carlos.monitor.redis.pojo.entity.MonitorBigkeyReport;
import com.carlos.monitor.redis.pojo.entity.MonitorBigkeyReportItem;
import io.lettuce.core.KeyScanCursor;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.ScanCursor;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.api.sync.RedisClusterCommands;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.models.role.RedisInstance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * <p>
 * 大 key / 热 key 扫描任务（独立低频任务）
 * SCAN + MEMORY USAGE（Redis &lt; 4.0 降级长度估算并 estimated=true 标记）；
 * Top-N 堆（top-n 可配）；每批 COUNT=collect.scan-count、批间 sleep=collect.bigkey.batch-sleep-ms 限速；
 * cluster 逐 master 节点；产出 report + items。
 * 热 key 基于 OBJECT FREQ（maxmemory-policy 非 LFU 时报告标记 FAILED 不支持）；
 * MONITOR 采样方式本期不做（TODO）
 * </p>
 *
 * @author Carlos
 * @date 2026年10月3日
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BigKeyScanTask {

    /**
     * 报告类型/触发方式/状态
     */
    public static final String TYPE_BIGKEY = "BIGKEY";

    public static final String TYPE_HOTKEY = "HOTKEY";

    public static final String TRIGGER_MANUAL = "MANUAL";

    public static final String TRIGGER_SCHEDULED = "SCHEDULED";

    public static final String STATUS_RUNNING = "RUNNING";

    /**
     * 估算模式下每个元素的估算字节数
     */
    private static final long ESTIMATED_BYTES_PER_ELEMENT = 64L;

    private final MonitorInstanceManager instanceManager;

    private final InstanceConnectionRegistry connectionRegistry;

    private final MonitorBigkeyReportManager reportManager;

    private final MonitorBigkeyReportItemManager reportItemManager;

    private final MonitorProperties monitorProperties;

    private ExecutorService scanExecutor;

    @PostConstruct
    public void init() {
        scanExecutor = Executors.newVirtualThreadPerTaskExecutor();
    }

    @PreDestroy
    public void destroy() {
        if (scanExecutor != null) {
            scanExecutor.shutdownNow();
        }
    }

    /**
     * 定时扫描全部 Redis 实例（cron 默认每日 02:30；
     * 注意：cron 以 job_info 为唯一真相源，carlos.monitor.collect.bigkey.cron 仅为初始播种值，
     * 调整调度请到任务管理后台改任务记录）
     */
    @ScheduleJob("monitorBigkeyScanHandler")
    @AutoRegisterJob(cron = "0 30 2 * * ?", description = "Redis 大 key 定时扫描（全部 Redis 实例）", author = "carlos")
    public void scheduledScan() {
        for (MonitorInstanceDTO instance : instanceManager.listCollectEnabled()) {
            if (ManagedConnectionFactory.isRedisType(instance.getInstanceType())) {
                triggerScan(instance.getId(), TYPE_BIGKEY, TRIGGER_SCHEDULED);
            }
        }
    }

    /**
     * 手动触发扫描（异步执行，立即返回报告ID）
     *
     * @param instanceId 实例ID
     * @param reportType 报告类型：BIGKEY/HOTKEY
     * @param triggerType 触发方式
     * @return 报告ID
     */
    public Long triggerScan(Long instanceId, String reportType, String triggerType) {
        MonitorInstanceDTO instance = instanceManager.getDtoById(instanceId);
        if (instance == null) {
            throw MonitorErrorCode.MONITOR_INSTANCE_NOT_FOUND.exception();
        }
        if (!ManagedConnectionFactory.isRedisType(instance.getInstanceType())) {
            throw MonitorErrorCode.MONITOR_INSTANCE_TYPE_UNSUPPORTED
                .exception("大key/热key 扫描仅支持 Redis 实例");
        }
        MonitorBigkeyReport report = new MonitorBigkeyReport();
        report.setInstanceId(instanceId);
        report.setReportType(reportType);
        report.setTriggerType(triggerType);
        report.setScanStatus(STATUS_RUNNING);
        report.setScannedKeys(0L);
        report.setSampleRate(100);
        report.setStartTime(LocalDateTime.now());
        reportManager.createReport(report);
        Long reportId = report.getId();
        scanExecutor.submit(() -> doScan(instance, reportId, reportType));
        return reportId;
    }

    /**
     * 执行扫描（单实例隔离 try-catch，失败标记报告 FAILED）
     */
    private void doScan(MonitorInstanceDTO instance, Long reportId, String reportType) {
        long scannedKeys = 0L;
        try {
            ManagedConnection connection = connectionRegistry.getConnection(instance.getId());
            if (!(connection instanceof RedisManagedConnection redisConnection)) {
                throw MonitorErrorCode.MONITOR_CONNECTION_UNAVAILABLE.exception();
            }
            // 热 key 前置校验：OBJECT FREQ 需要 LFU 策略
            if (TYPE_HOTKEY.equals(reportType) && !lfuEnabled(redisConnection)) {
                reportManager.markFailed(reportId, "maxmemory-policy 非 LFU，OBJECT FREQ 不可用，热key报告不支持");
                return;
            }
            int topN = monitorProperties.getCollect().getBigkey().getTopN();
            // Top-N 最小堆（按报告类型排序键）
            PriorityQueue<MonitorBigkeyReportItem> topHeap = new PriorityQueue<>(topN,
                (a, b) -> compareMetric(a, b, reportType));

            if (redisConnection.isCluster()) {
                for (RedisClusterNode node : redisConnection.getClusterConnection().getPartitions()) {
                    if (node.getRole() != RedisInstance.Role.MASTER) {
                        continue;
                    }
                    String nodeAddress = node.getUri().getHost() + ":" + node.getUri().getPort();
                    try (StatefulRedisConnection<String, String> nodeConnection =
                             redisConnection.getClusterConnection().getConnection(node.getNodeId())) {
                        scannedKeys += scanNode(reportId, nodeConnection.sync(), nodeAddress, reportType, topN, topHeap);
                    } catch (Exception e) {
                        log.warn("大key扫描节点失败, instanceId:{}, node:{}, error:{}",
                            instance.getId(), nodeAddress, e.getMessage());
                    }
                }
            } else {
                scannedKeys = scanNode(reportId, redisConnection.getConnection().sync(), "", reportType, topN, topHeap);
            }

            List<MonitorBigkeyReportItem> items = new ArrayList<>(topHeap);
            items.sort((a, b) -> compareMetric(b, a, reportType));
            reportItemManager.saveItems(items);
            reportManager.markSuccess(reportId, scannedKeys);
            log.info("大key/热key 扫描完成, instanceId:{}, reportId:{}, type:{}, scanned:{}, items:{}",
                instance.getId(), reportId, reportType, scannedKeys, items.size());
        } catch (Exception e) {
            log.warn("大key/热key 扫描失败, instanceId:{}, reportId:{}, error:{}",
                instance.getId(), reportId, e.getMessage());
            reportManager.markFailed(reportId, e.getMessage());
        }
    }

    /**
     * 扫描单节点（SCAN 全量 + 逐 key 度量，限速）
     */
    private long scanNode(Long reportId, RedisClusterCommands<String, String> commands, String nodeAddress,
                          String reportType, int topN, PriorityQueue<MonitorBigkeyReportItem> topHeap) {
        int scanCount = monitorProperties.getCollect().getScanCount();
        long batchSleepMs = monitorProperties.getCollect().getBigkey().getBatchSleepMs();
        ScanCursor cursor = ScanCursor.INITIAL;
        long scanned = 0L;
        while (true) {
            KeyScanCursor<String> result = commands.scan(cursor, ScanArgs.Builder.limit(scanCount));
            for (String key : result.getKeys()) {
                MonitorBigkeyReportItem item = measureKey(commands, reportId, nodeAddress, key, reportType);
                scanned++;
                if (item == null) {
                    continue;
                }
                topHeap.offer(item);
                if (topHeap.size() > topN) {
                    topHeap.poll();
                }
            }
            if (result.isFinished()) {
                break;
            }
            cursor = result;
            if (batchSleepMs > 0) {
                try {
                    Thread.sleep(batchSleepMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        return scanned;
    }

    /**
     * 度量单 key：MEMORY USAGE（&lt;4.0 降级长度估算 estimated=true）；HOTKEY 取 OBJECT FREQ
     */
    private MonitorBigkeyReportItem measureKey(RedisClusterCommands<String, String> commands, Long reportId,
                                               String nodeAddress, String key, String reportType) {
        try {
            String type = commands.type(key);
            MonitorBigkeyReportItem item = new MonitorBigkeyReportItem();
            item.setReportId(reportId);
            item.setNodeAddress(nodeAddress);
            item.setRedisKey(key);
            item.setKeyType(type);
            item.setTtlSeconds(commands.ttl(key));
            item.setEstimated(false);
            if (TYPE_HOTKEY.equals(reportType)) {
                Long freq = safeObjectFreq(commands, key);
                if (freq == null) {
                    return null;
                }
                item.setAccessFreq(freq);
            } else {
                fillMemory(commands, item, key, type);
            }
            return item;
        } catch (Exception e) {
            log.debug("度量 key 失败: {}, error:{}", key, e.getMessage());
            return null;
        }
    }

    /**
     * 内存度量：MEMORY USAGE 优先；不可用（Redis &lt; 4.0）降级元素长度估算并标记 estimated
     */
    private void fillMemory(RedisClusterCommands<String, String> commands, MonitorBigkeyReportItem item,
                            String key, String type) {
        Long memory = safeMemoryUsage(commands, key);
        if (memory != null) {
            item.setMemoryBytes(memory);
            item.setElementCount(elementCount(commands, key, type));
            return;
        }
        long elements = elementCount(commands, key, type);
        item.setElementCount(elements);
        item.setMemoryBytes(elements * ESTIMATED_BYTES_PER_ELEMENT);
        item.setEstimated(true);
    }

    private long elementCount(RedisClusterCommands<String, String> commands, String key, String type) {
        try {
            return switch (type) {
                case "string" -> commands.strlen(key);
                case "hash" -> commands.hlen(key);
                case "list" -> commands.llen(key);
                case "set" -> commands.scard(key);
                case "zset" -> commands.zcard(key);
                case "stream" -> commands.xlen(key);
                default -> 0L;
            };
        } catch (Exception e) {
            return 0L;
        }
    }

    private Long safeMemoryUsage(RedisClusterCommands<String, String> commands, String key) {
        try {
            return commands.memoryUsage(key);
        } catch (Exception e) {
            return null;
        }
    }

    private Long safeObjectFreq(RedisClusterCommands<String, String> commands, String key) {
        try {
            return commands.objectFreq(key);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * LFU 策略检测（maxmemory-policy 含 lfu）
     */
    private boolean lfuEnabled(RedisManagedConnection connection) {
        try {
            RedisClusterCommands<String, String> commands = connection.isCluster()
                ? connection.getClusterConnection().sync()
                : connection.getConnection().sync();
            String info = commands.info("memory");
            for (String line : info.split("\r?\n")) {
                if (line.startsWith("maxmemory_policy:")) {
                    return line.toLowerCase().contains("lfu");
                }
            }
        } catch (Exception e) {
            log.warn("LFU 策略检测失败, error:{}", e.getMessage());
        }
        return false;
    }

    /**
     * 报告类型排序键：BIGKEY 按 memoryBytes，HOTKEY 按 accessFreq
     */
    private int compareMetric(MonitorBigkeyReportItem a, MonitorBigkeyReportItem b, String reportType) {
        if (TYPE_HOTKEY.equals(reportType)) {
            return Long.compare(
                a.getAccessFreq() == null ? 0L : a.getAccessFreq(),
                b.getAccessFreq() == null ? 0L : b.getAccessFreq());
        }
        return Long.compare(
            a.getMemoryBytes() == null ? 0L : a.getMemoryBytes(),
            b.getMemoryBytes() == null ? 0L : b.getMemoryBytes());
    }
}
