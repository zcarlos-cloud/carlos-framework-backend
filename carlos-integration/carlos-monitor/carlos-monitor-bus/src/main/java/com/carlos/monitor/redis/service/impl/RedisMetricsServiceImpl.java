package com.carlos.monitor.redis.service.impl;

import cn.hutool.core.util.StrUtil;
import com.carlos.core.response.CommonErrorCode;
import com.carlos.monitor.collect.sink.MonitorCkQueryClient;
import com.carlos.monitor.enums.MonitorErrorCode;
import com.carlos.monitor.instance.manager.MonitorInstanceManager;
import com.carlos.monitor.redis.manager.RedisMetricsManager;
import com.carlos.monitor.redis.pojo.vo.RedisNodeVO;
import com.carlos.monitor.redis.service.RedisMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * Redis 监控查询 业务接口实现类
 * </p>
 *
 * @author Carlos
 * @date 2026年10月3日
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisMetricsServiceImpl implements RedisMetricsService {

    /**
     * 原始点查询上限（range<=6h）
     */
    private static final int RAW_POINT_LIMIT = 10_000;

    private final RedisMetricsManager redisMetricsManager;

    private final MonitorInstanceManager instanceManager;

    @Override
    public Map<String, String> getCurrentMetrics(Long instanceId) {
        checkInstance(instanceId);
        return redisMetricsManager.getCurrentMetrics(instanceId);
    }

    @Override
    public List<?> getMetricHistory(Long instanceId, String metricName, String range, String node) {
        checkInstance(instanceId);
        if (StrUtil.isBlank(metricName)) {
            throw CommonErrorCode.PARAM_VALIDATION_ERROR.exception("指标名不能为空");
        }
        long hours = parseRangeHours(range);
        Instant end = Instant.now();
        Instant start = end.minus(hours, ChronoUnit.HOURS);
        // 读路径设计 2.2：1h/6h 原始点，24h/7d/30d 小时聚合
        boolean aggregated = hours > 6;
        return redisMetricsManager.getMetricHistory(instanceId, metricName, node, start, end,
            aggregated, RAW_POINT_LIMIT);
    }

    @Override
    public List<RedisNodeVO> listNodes(Long instanceId) {
        checkInstance(instanceId);
        return redisMetricsManager.listNodes(instanceId);
    }

    @Override
    public List<MonitorCkQueryClient.SlowlogQueryRow> pageSlowlogs(Long instanceId, String node,
                                                                   Integer hours, Long minDurationUs,
                                                                   int offset, int limit) {
        checkInstance(instanceId);
        int effectiveHours = hours == null || hours <= 0 ? 24 : hours;
        Instant end = Instant.now();
        Instant start = end.minus(effectiveHours, ChronoUnit.HOURS);
        return redisMetricsManager.pageSlowlogs(instanceId, node, start, end, minDurationUs,
            Math.max(0, offset), Math.min(Math.max(1, limit), 200));
    }

    private void checkInstance(Long instanceId) {
        if (instanceManager.getDtoById(instanceId) == null) {
            throw MonitorErrorCode.MONITOR_INSTANCE_NOT_FOUND.exception();
        }
    }

    /**
     * range 解析：1h/6h/24h/7d/30d，默认 1h
     */
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
