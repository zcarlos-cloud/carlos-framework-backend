package com.carlos.monitor.dashboard.service.impl;

import cn.hutool.core.util.StrUtil;
import com.carlos.json.JsonUtils;
import com.carlos.monitor.alarm.manager.MonitorAlarmRecordManager;
import com.carlos.monitor.alarm.pojo.entity.MonitorAlarmRecord;
import com.carlos.monitor.alarm.pojo.vo.AlarmRecordVO;
import com.carlos.monitor.collect.sink.MonitorCkQueryClient;
import com.carlos.monitor.constant.MetricNameConstant;
import com.carlos.monitor.dashboard.pojo.vo.AbnormalVO;
import com.carlos.monitor.dashboard.pojo.vo.AppDashboardVO;
import com.carlos.monitor.dashboard.pojo.vo.DashboardSummaryVO;
import com.carlos.monitor.dashboard.pojo.vo.DashboardTrendVO;
import com.carlos.monitor.dashboard.service.DashboardService;
import com.carlos.monitor.instance.manager.MonitorInstanceManager;
import com.carlos.monitor.instance.pojo.dto.MonitorInstanceDTO;
import com.carlos.monitor.instance.service.MonitorInstanceService;
import com.carlos.monitor.redis.manager.RedisMetricsManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 监控大盘聚合 业务接口实现类
 * 聚合逻辑全部走各域 Manager（实例/告警/指标快照/CK 查询）
 * </p>
 *
 * @author Carlos
 * @date 2026年10月5日
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    /**
     * 采集停滞阈值（分钟）
     */
    private static final int STALE_MINUTES = 10;

    private final MonitorInstanceManager instanceManager;

    private final MonitorAlarmRecordManager alarmRecordManager;

    private final RedisMetricsManager redisMetricsManager;

    private final MonitorCkQueryClient ckQueryClient;

    private final MonitorInstanceService instanceService;

    @Override
    public DashboardSummaryVO summary() {
        List<MonitorInstanceDTO> instances = instanceManager.listAllInstances();
        DashboardSummaryVO vo = new DashboardSummaryVO();
        vo.setTotalInstances(instances.size());
        int up = 0;
        int down = 0;
        int unknown = 0;
        int collectedLastHour = 0;
        int stale = 0;
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        LocalDateTime staleBefore = LocalDateTime.now().minusMinutes(STALE_MINUTES);
        Map<String, DashboardSummaryVO.TypeStat> typeStats = new LinkedHashMap<>();
        for (MonitorInstanceDTO instance : instances) {
            String health = StrUtil.nullToDefault(instance.getHealthStatus(), "UNKNOWN");
            String type = instance.getInstanceType() == null ? "UNKNOWN" : instance.getInstanceType().getCode();
            DashboardSummaryVO.TypeStat stat = typeStats.computeIfAbsent(type, key -> {
                DashboardSummaryVO.TypeStat item = new DashboardSummaryVO.TypeStat();
                item.setInstanceType(key);
                item.setTotal(0);
                item.setUp(0);
                item.setDown(0);
                return item;
            });
            stat.setTotal(stat.getTotal() + 1);
            switch (health) {
                case "UP" -> {
                    up++;
                    stat.setUp(stat.getUp() + 1);
                }
                case "DOWN" -> {
                    down++;
                    stat.setDown(stat.getDown() + 1);
                }
                default -> unknown++;
            }
            if (instance.getLastCollectTime() != null && instance.getLastCollectTime().isAfter(oneHourAgo)) {
                collectedLastHour++;
            }
            if (instance.getLastCollectTime() == null || instance.getLastCollectTime().isBefore(staleBefore)) {
                stale++;
            }
        }
        vo.setUpCount(up);
        vo.setDownCount(down);
        vo.setUnknownCount(unknown);
        vo.setTypeStats(new ArrayList<>(typeStats.values()));
        vo.setCollectedLastHour(collectedLastHour);
        vo.setStaleCount(stale);

        // 未恢复告警按级别统计
        Map<String, Long> levelCounts = new LinkedHashMap<>();
        for (MonitorAlarmRecord record : alarmRecordManager.listRecentUnresolved(50)) {
            levelCounts.merge(record.getAlarmLevel(), 1L, Long::sum);
        }
        List<DashboardSummaryVO.LevelStat> levelStats = new ArrayList<>();
        levelCounts.forEach((level, count) -> {
            DashboardSummaryVO.LevelStat stat = new DashboardSummaryVO.LevelStat();
            stat.setAlarmLevel(level);
            stat.setCount(count);
            levelStats.add(stat);
        });
        vo.setAlarmStats(levelStats);
        return vo;
    }

    @Override
    public DashboardTrendVO trend(String metric, String range, int topN) {
        long hours = switch (StrUtil.nullToDefault(range, "24h").toLowerCase()) {
            case "6h" -> 6L;
            case "7d" -> 24L * 7;
            default -> 24L;
        };
        // 按当前快照值排序取 Top N 实例
        List<MetricValue> candidates = new ArrayList<>();
        for (MonitorInstanceDTO instance : instanceManager.listAllInstances()) {
            Double current = currentMetricValue(instance.getId(), metric);
            if (current != null) {
                candidates.add(new MetricValue(instance, current));
            }
        }
        candidates.sort(Comparator.comparingDouble(MetricValue::value).reversed());
        int limit = Math.min(Math.max(topN, 1), 10);

        DashboardTrendVO vo = new DashboardTrendVO();
        vo.setMetricName(metric);
        vo.setRange(range);
        List<DashboardTrendVO.InstanceSeries> seriesList = new ArrayList<>();
        Instant end = Instant.now();
        Instant start = end.minus(hours, ChronoUnit.HOURS);
        for (MetricValue candidate : candidates.stream().limit(limit).toList()) {
            MonitorInstanceDTO instance = candidate.instance();
            DashboardTrendVO.InstanceSeries series = new DashboardTrendVO.InstanceSeries();
            series.setInstanceId(instance.getId());
            series.setInstanceName(instance.getInstanceName());
            List<DashboardTrendVO.Point> points = new ArrayList<>();
            for (MonitorCkQueryClient.MetricHourlyRow row
                : ckQueryClient.queryMetricHourly(instance.getId(), metric, null, start, end)) {
                DashboardTrendVO.Point point = new DashboardTrendVO.Point();
                point.setTs(row.hour());
                point.setValue(row.avgValue());
                points.add(point);
            }
            series.setPoints(points);
            seriesList.add(series);
        }
        vo.setSeries(seriesList);
        return vo;
    }

    /**
     * 实例当前值（趋势 Top N 排序用）
     */
    private record MetricValue(MonitorInstanceDTO instance, double value) {
    }

    @Override
    public AppDashboardVO appDetail(String appName) {
        List<MonitorInstanceDTO> instances = instanceService.listByAppName(appName);
        AppDashboardVO vo = new AppDashboardVO();
        vo.setAppName(appName);
        List<AppDashboardVO.AppInstanceVO> instanceVOs = new ArrayList<>();
        for (MonitorInstanceDTO instance : instances) {
            AppDashboardVO.AppInstanceVO item = new AppDashboardVO.AppInstanceVO();
            item.setInstanceId(instance.getId());
            item.setInstanceName(instance.getInstanceName());
            item.setInstanceType(instance.getInstanceType() == null ? null : instance.getInstanceType().getCode());
            item.setHealthStatus(instance.getHealthStatus());
            item.setLastCollectTime(instance.getLastCollectTime());
            item.setMetrics(keyMetrics(instance));
            instanceVOs.add(item);
        }
        vo.setInstances(instanceVOs);
        return vo;
    }

    @Override
    public AbnormalVO abnormal(int alarmLimit) {
        AbnormalVO vo = new AbnormalVO();
        List<AbnormalVO.AbnormalInstanceVO> abnormalInstances = new ArrayList<>();
        for (MonitorInstanceDTO instance : instanceManager.listAllInstances()) {
            if (!"DOWN".equals(instance.getHealthStatus())) {
                continue;
            }
            AbnormalVO.AbnormalInstanceVO item = new AbnormalVO.AbnormalInstanceVO();
            item.setInstanceId(instance.getId());
            item.setInstanceName(instance.getInstanceName());
            item.setInstanceType(instance.getInstanceType() == null ? null : instance.getInstanceType().getCode());
            item.setEnv(instance.getEnv() == null ? null : instance.getEnv().getCode());
            item.setHealthStatus(instance.getHealthStatus());
            item.setLastCollectTime(instance.getLastCollectTime());
            abnormalInstances.add(item);
        }
        vo.setAbnormalInstances(abnormalInstances);
        List<AlarmRecordVO> alarms = alarmRecordManager.listRecentUnresolved(alarmLimit).stream()
            .map(this::toAlarmVO)
            .toList();
        vo.setAlarms(alarms);
        return vo;
    }

    /**
     * 关键当前指标（应用视角：内存使用率/OPS/连接数/池水位，存在才带）
     */
    private Map<String, String> keyMetrics(MonitorInstanceDTO instance) {
        Map<String, String> snapshot = redisMetricsManager.getCurrentMetrics(instance.getId());
        Map<String, String> result = new LinkedHashMap<>();
        List<String> preferredKeys = List.of(
            MetricNameConstant.REDIS_MEMORY_USED_PERCENT, MetricNameConstant.REDIS_STATS_OPS_PER_SEC,
            MetricNameConstant.REDIS_CLIENTS_CONNECTED, MetricNameConstant.DB_POOL_ACTIVE_COUNT,
            MetricNameConstant.DB_CONNECTIONS_ACTIVE, MetricNameConstant.DB_QUESTIONS);
        for (String key : preferredKeys) {
            if (snapshot.containsKey(key)) {
                result.put(key, snapshot.get(key));
            }
        }
        return result;
    }

    /**
     * 当前快照中某指标的数值（{v,ts} JSON 中的 v）
     */
    private Double currentMetricValue(Long instanceId, String metric) {
        Map<String, String> snapshot = redisMetricsManager.getCurrentMetrics(instanceId);
        String json = snapshot.get(metric);
        if (StrUtil.isBlank(json) || !JsonUtils.isValidJson(json)) {
            return null;
        }
        Object v = JsonUtils.toMap(json).get("v");
        if (v instanceof Number number) {
            return number.doubleValue();
        }
        return null;
    }

    private AlarmRecordVO toAlarmVO(MonitorAlarmRecord entity) {
        AlarmRecordVO vo = new AlarmRecordVO();
        vo.setId(entity.getId());
        vo.setRuleId(entity.getRuleId());
        vo.setRuleName(entity.getRuleName());
        vo.setInstanceId(entity.getInstanceId());
        vo.setInstanceName(entity.getInstanceName());
        vo.setMetricName(entity.getMetricName());
        vo.setMetricValue(entity.getMetricValue());
        vo.setThresholdValue(entity.getThresholdValue());
        vo.setAlarmLevel(entity.getAlarmLevel());
        vo.setAlarmStatus(entity.getAlarmStatus());
        vo.setAlarmContent(entity.getAlarmContent());
        vo.setFirstTriggerTime(entity.getFirstTriggerTime());
        vo.setLastTriggerTime(entity.getLastTriggerTime());
        vo.setNotifyStatus(entity.getNotifyStatus());
        return vo;
    }
}
