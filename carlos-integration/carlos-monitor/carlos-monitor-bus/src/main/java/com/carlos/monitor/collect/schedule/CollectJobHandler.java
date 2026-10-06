package com.carlos.monitor.collect.schedule;

import com.carlos.job.client.annotation.AutoRegisterJob;
import com.carlos.job.client.annotation.ScheduleJob;
import com.carlos.monitor.config.MonitorProperties;
import com.carlos.monitor.instance.manager.MonitorInstanceManager;
import com.carlos.monitor.instance.pojo.dto.MonitorInstanceDTO;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 采集调度处理器（carlos-job 调度中心派发）
 * 每 10s 扫描到期实例（collect_enabled=true 且 last_collect_time + collect_interval &lt;= now），
 * 虚拟线程池并发采集（并发上限 carlos.monitor.collect.parallelism）。
 * </p>
 * <p>
 * 说明：设计的 N 分片（shard-count）由单机扫描内部并发承载 —— 注解式任务注册无法按分片数
 * 动态展开 N 条 job 记录，且实例量级（百级）下单任务并发采集已足够；实例规模上来后可在
 * 调度中心按 handler 参数拆分多条任务（handler 支持 String params 预留）。
 * </p>
 *
 * @author Carlos
 * @date 2026年10月2日
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "carlos.monitor.collect", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CollectJobHandler {

    /**
     * 单次调度执行的整体超时（秒）：防止个别实例僵死拖垮下一轮调度
     */
    private static final long SCAN_TIMEOUT_SECONDS = 60L;

    private final MonitorInstanceManager instanceManager;

    private final CollectTaskRouter collectTaskRouter;

    private final MonitorProperties monitorProperties;

    private ExecutorService collectExecutor;

    @PostConstruct
    public void init() {
        collectExecutor = Executors.newVirtualThreadPerTaskExecutor();
    }

    @PreDestroy
    public void destroy() {
        if (collectExecutor != null) {
            collectExecutor.shutdownNow();
        }
    }

    /**
     * 到期实例采集扫描（每 10 秒，job 中心最小调度间隔）
     */
    @ScheduleJob("monitorCollectScanHandler")
    @AutoRegisterJob(cron = "0/10 * * * * ?", description = "监控实例到期采集扫描（虚拟线程并发采集）", author = "carlos")
    public void scanDueInstances() {
        List<MonitorInstanceDTO> instances = instanceManager.listCollectEnabled();
        if (instances.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        int parallelism = Math.max(1, monitorProperties.getCollect().getParallelism());
        Semaphore semaphore = new Semaphore(parallelism);

        int dueCount = 0;
        List<Future<?>> futures = new ArrayList<>();
        for (MonitorInstanceDTO instance : instances) {
            if (!isDue(instance, now)) {
                continue;
            }
            dueCount++;
            futures.add(collectExecutor.submit(() -> {
                try {
                    semaphore.acquire();
                    try {
                        collectTaskRouter.collect(instance);
                    } finally {
                        semaphore.release();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    // 单实例异常已被路由层隔离，此处仅兜底
                    log.warn("实例采集任务异常, instanceId:{}, error:{}", instance.getId(), e.getMessage());
                }
            }));
        }
        if (dueCount > 0) {
            log.debug("采集扫描完成，到期实例数: {}", dueCount);
        }
        // 等待本轮任务收尾（超时直接返回，虚拟线程不占用平台线程）
        awaitCurrentRound(futures);
    }

    /**
     * 实例是否到期：从未采集 或 last_collect_time + collect_interval <= now
     */
    private boolean isDue(MonitorInstanceDTO instance, LocalDateTime now) {
        if (instance.getLastCollectTime() == null) {
            return true;
        }
        int interval = instance.getCollectInterval() == null ? 30 : instance.getCollectInterval();
        return !instance.getLastCollectTime().plusSeconds(interval).isAfter(now);
    }

    private void awaitCurrentRound(List<Future<?>> futures) {
        long deadline = System.currentTimeMillis() + SCAN_TIMEOUT_SECONDS * 1000;
        for (Future<?> future : futures) {
            long remaining = deadline - System.currentTimeMillis();
            if (remaining <= 0) {
                log.warn("采集扫描超时 {}s，剩余任务转入后台执行", SCAN_TIMEOUT_SECONDS);
                return;
            }
            try {
                future.get(remaining, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                // 单任务异常已在任务内捕获，get 仅为等待收尾
                log.debug("采集任务结束: {}", e.getMessage());
            }
        }
    }
}
