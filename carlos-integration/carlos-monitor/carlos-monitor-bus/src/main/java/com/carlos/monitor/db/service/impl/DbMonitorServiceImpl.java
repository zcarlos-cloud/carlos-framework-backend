package com.carlos.monitor.db.service.impl;

import cn.hutool.core.util.StrUtil;
import com.carlos.monitor.collect.sink.MonitorCkQueryClient;
import com.carlos.monitor.constant.CacheKeyConstant;
import com.carlos.monitor.db.manager.MonitorDbCapacitySnapshotManager;
import com.carlos.monitor.db.pojo.dto.DbLockRow;
import com.carlos.monitor.db.pojo.dto.DbSessionRow;
import com.carlos.monitor.db.pojo.vo.CapacitySnapshotVO;
import com.carlos.monitor.db.proxy.DbDataProxy;
import com.carlos.monitor.db.service.DbMonitorService;
import com.carlos.monitor.enums.MonitorErrorCode;
import com.carlos.monitor.instance.manager.MonitorInstanceManager;
import com.carlos.monitor.redis.proxy.RedisWriteGuard;
import com.carlos.redis.util.RedisUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 数据库监控查询 业务接口实现类
 * </p>
 *
 * @author Carlos
 * @date 2026年10月4日
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DbMonitorServiceImpl implements DbMonitorService {

    /**
     * 原始点查询上限
     */
    private static final int RAW_POINT_LIMIT = 10_000;

    private static final long CURRENT_CAFFEINE_SECONDS = 60L;

    private final MonitorInstanceManager instanceManager;

    private final MonitorDbCapacitySnapshotManager capacitySnapshotManager;

    private final MonitorCkQueryClient ckQueryClient;

    private final DbDataProxy dbDataProxy;

    private final RedisWriteGuard writeGuard;

    private final Cache<Long, Map<String, String>> currentCache = Caffeine.newBuilder()
        .expireAfterWrite(CURRENT_CAFFEINE_SECONDS, TimeUnit.SECONDS)
        .maximumSize(1000)
        .build();

    @Override
    public Map<String, String> getCurrentMetrics(Long instanceId) {
        checkInstance(instanceId);
        Map<String, String> cached = currentCache.getIfPresent(instanceId);
        if (cached != null) {
            return cached;
        }
        Map<Object, Object> hash = RedisUtil.getHash(CacheKeyConstant.metricCurrentKey(instanceId));
        Map<String, String> result = new LinkedHashMap<>();
        if (hash != null) {
            hash.forEach((field, value) -> result.put(String.valueOf(field), String.valueOf(value)));
        }
        currentCache.put(instanceId, result);
        return result;
    }

    @Override
    public List<?> getMetricHistory(Long instanceId, String metricName, String range, String node) {
        checkInstance(instanceId);
        long hours = parseRangeHours(range);
        Instant end = Instant.now();
        Instant start = end.minus(hours, ChronoUnit.HOURS);
        boolean aggregated = hours > 6;
        if (aggregated) {
            return ckQueryClient.queryMetricHourly(instanceId, metricName, node, start, end);
        }
        return ckQueryClient.queryMetricPoints(instanceId, metricName, node, start, end, RAW_POINT_LIMIT);
    }

    @Override
    public List<MonitorCkQueryClient.SlowSqlQueryRow> pageSlowSql(Long instanceId, String fingerprint,
                                                                 Integer hours, int offset, int limit) {
        checkInstance(instanceId);
        int effectiveHours = hours == null || hours <= 0 ? 24 : hours;
        Instant end = Instant.now();
        Instant start = end.minus(effectiveHours, ChronoUnit.HOURS);
        return ckQueryClient.pageSlowSqls(instanceId, fingerprint, start, end,
            Math.max(0, offset), Math.min(Math.max(1, limit), 200));
    }

    @Override
    public List<DbSessionRow> listSessions(Long instanceId) {
        checkInstance(instanceId);
        return dbDataProxy.listSessions(instanceId);
    }

    @Override
    public boolean killSession(Long instanceId, String sessionId, String confirmToken) {
        checkInstance(instanceId);
        writeGuard.consumeConfirmToken(instanceId, "db:session:kill:" + sessionId, confirmToken);
        return dbDataProxy.killSession(instanceId, sessionId);
    }

    @Override
    public List<DbLockRow> listLocks(Long instanceId) {
        checkInstance(instanceId);
        return dbDataProxy.listLocks(instanceId);
    }

    @Override
    public List<CapacitySnapshotVO> getCapacityTop(Long instanceId, LocalDate date, int topN) {
        checkInstance(instanceId);
        return capacitySnapshotManager.listTopCapacity(instanceId, date, topN);
    }

    private void checkInstance(Long instanceId) {
        if (instanceManager.getDtoById(instanceId) == null) {
            throw MonitorErrorCode.MONITOR_INSTANCE_NOT_FOUND.exception();
        }
    }

    private long parseRangeHours(String range) {
        if (StrUtil.isBlank(range)) {
            return 1L;
        }
        return switch (range.toLowerCase()) {
            case "1h" -> 1L;
            case "6h" -> 6L;
            case "24h" -> 24L;
            case "7d" -> 24L * 7;
            case "30d" -> 24L * 30;
            default -> 1L;
        };
    }
}
