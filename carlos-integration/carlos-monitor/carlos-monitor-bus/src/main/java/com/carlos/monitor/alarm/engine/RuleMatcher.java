package com.carlos.monitor.alarm.engine;

import cn.hutool.core.util.StrUtil;
import com.carlos.monitor.alarm.manager.MonitorAlarmRuleManager;
import com.carlos.monitor.alarm.pojo.entity.MonitorAlarmRule;
import com.carlos.monitor.collect.spi.MetricPoint;
import com.carlos.monitor.config.MonitorProperties;
import com.carlos.monitor.instance.manager.MonitorInstanceManager;
import com.carlos.monitor.instance.pojo.dto.MonitorInstanceDTO;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 告警规则匹配器
 * metric_source → 启用规则索引（Caffeine 缓存 carlos.monitor.alarm.rule-cache-sec，
 * 规则增删改时由 Service 调用 invalidate() 失效）；
 * targetType=INSTANCE/GROUP/ALL 三种展开为具体实例判定
 * </p>
 *
 * @author Carlos
 * @date 2026年10月5日
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RuleMatcher {

    private static final String CACHE_KEY = "enabledRules";

    private final MonitorAlarmRuleManager alarmRuleManager;

    private final MonitorInstanceManager instanceManager;

    private final MonitorProperties monitorProperties;

    private volatile Cache<String, List<MonitorAlarmRule>> ruleCache;

    /**
     * 匹配指标点命中的规则（同指标名 + 来源 + 目标范围覆盖该实例）
     *
     * @param point 指标点
     * @return 命中的规则列表
     */
    public List<MonitorAlarmRule> match(MetricPoint point) {
        List<MonitorAlarmRule> matched = new ArrayList<>();
        for (MonitorAlarmRule rule : enabledRules()) {
            if (!rule.getMetricName().equals(point.metricName())) {
                continue;
            }
            if (!rule.getMetricSource().equals(point.source().getCode())) {
                continue;
            }
            if (targetCovers(rule, point.instanceId())) {
                matched.add(rule);
            }
        }
        return matched;
    }

    /**
     * 失效规则缓存（规则增删改/启停时调用）
     */
    public void invalidate() {
        cache().invalidate(CACHE_KEY);
    }

    /**
     * 目标范围判定：ALL 全覆盖；INSTANCE 比对实例ID；GROUP 比对实例分组
     */
    private boolean targetCovers(MonitorAlarmRule rule, Long instanceId) {
        String targetType = StrUtil.nullToDefault(rule.getTargetType(), "INSTANCE");
        return switch (targetType) {
            case "ALL" -> true;
            case "INSTANCE" -> instanceId.equals(rule.getTargetId());
            case "GROUP" -> {
                MonitorInstanceDTO instance = instanceManager.getDtoById(instanceId);
                yield instance != null && instance.getGroupId() != null
                    && instance.getGroupId().equals(rule.getTargetId());
            }
            default -> false;
        };
    }

    private List<MonitorAlarmRule> enabledRules() {
        List<MonitorAlarmRule> rules = cache().getIfPresent(CACHE_KEY);
        if (rules == null) {
            rules = alarmRuleManager.listEnabled();
            cache().put(CACHE_KEY, rules);
        }
        return rules;
    }

    private Cache<String, List<MonitorAlarmRule>> cache() {
        if (ruleCache == null) {
            synchronized (this) {
                if (ruleCache == null) {
                    ruleCache = Caffeine.newBuilder()
                        .expireAfterWrite(monitorProperties.getAlarm().getRuleCacheSec(), TimeUnit.SECONDS)
                        .build();
                }
            }
        }
        return ruleCache;
    }
}
