package com.carlos.monitor.alarm.engine;

import cn.hutool.core.util.StrUtil;
import com.carlos.monitor.alarm.pojo.entity.MonitorAlarmRule;
import com.carlos.monitor.collect.sink.AlarmEvaluateHook;
import com.carlos.monitor.collect.spi.MetricPoint;
import com.carlos.monitor.instance.manager.MonitorInstanceManager;
import com.carlos.monitor.instance.pojo.dto.MonitorInstanceDTO;
import com.carlos.monitor.util.MonitorCacheUtil;
import com.carlos.redis.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 告警引擎（AlarmEvaluateHook 实现，sink 单线程事件处理器内调用，无并发问题）
 * 批量指标点 → 规则匹配 → 条件评估 → 连续周期防抖 → 状态机触发/恢复
 * </p>
 *
 * @author Carlos
 * @date 2026年10月5日
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlarmEngine implements AlarmEvaluateHook {

    /**
     * 环比上一值缓存键前缀：monitor:alarm:prev:{ruleId}:{instanceId}:{metricName}
     */
    private static final String PREV_VALUE_PREFIX = "monitor:alarm:prev:";

    /**
     * 违规计数键前缀：monitor:alarm:violation:{ruleId}:{instanceId}
     */
    private static final String VIOLATION_PREFIX = "monitor:alarm:violation:";

    /**
     * 违规计数 TTL 基准倍数（3×collect_interval）
     */
    private static final int VIOLATION_TTL_MULTIPLIER = 3;

    private final RuleMatcher ruleMatcher;

    private final AlarmStateMachine alarmStateMachine;

    private final MonitorInstanceManager instanceManager;

    @Override
    public void evaluate(List<MetricPoint> points) {
        if (points == null || points.isEmpty()) {
            return;
        }
        for (MetricPoint point : points) {
            try {
                evaluatePoint(point);
            } catch (Exception e) {
                // 单点评估失败不影响批次内其他指标点
                log.warn("告警评估失败, instanceId:{}, metric:{}, error:{}",
                    point.instanceId(), point.metricName(), e.getMessage());
            }
        }
    }

    private void evaluatePoint(MetricPoint point) {
        List<MonitorAlarmRule> rules = ruleMatcher.match(point);
        if (rules.isEmpty()) {
            return;
        }
        for (MonitorAlarmRule rule : rules) {
            boolean violated = evaluateCondition(rule, point);
            String violationKey = VIOLATION_PREFIX + rule.getId() + ":" + point.instanceId();
            MonitorInstanceDTO instance = null;
            if (violated) {
                // 连续周期防抖：INCR + TTL=3×collect_interval，达到 durationCount 才触发
                int durationCount = rule.getDurationCount() == null ? 1 : rule.getDurationCount();
                if (durationCount <= 1) {
                    instance = instanceManager.getDtoById(point.instanceId());
                    alarmStateMachine.fire(rule, instance, point);
                    continue;
                }
                Long count = RedisUtil.incrementValue(violationKey, 1L,
                    violationTtl(point.instanceId()), TimeUnit.SECONDS);
                if (count != null && count >= durationCount) {
                    instance = instanceManager.getDtoById(point.instanceId());
                    alarmStateMachine.fire(rule, instance, point);
                }
            } else {
                // 指标恢复正常：清违规计数 + 状态机恢复
                RedisUtil.delete(violationKey);
                instance = instanceManager.getDtoById(point.instanceId());
                alarmStateMachine.recover(rule, instance, point);
            }
        }
    }

    /**
     * 条件评估：GT/GTE/LT/LTE/EQ/NE 直比；CHANGE_RATE_GT/LT 环比（上一值存 Redis）
     */
    private boolean evaluateCondition(MonitorAlarmRule rule, MetricPoint point) {
        double current = point.value();
        double threshold = rule.getThresholdValue().doubleValue();
        String op = StrUtil.nullToDefault(rule.getConditionOp(), "GT").toUpperCase();
        return switch (op) {
            case "GT" -> current > threshold;
            case "GTE" -> current >= threshold;
            case "LT" -> current < threshold;
            case "LTE" -> current <= threshold;
            case "EQ" -> Double.compare(current, threshold) == 0;
            case "NE" -> Double.compare(current, threshold) != 0;
            case "CHANGE_RATE_GT" -> changeRate(rule, point) > threshold;
            case "CHANGE_RATE_LT" -> changeRate(rule, point) < threshold;
            default -> {
                log.warn("未知的比较符: {}, ruleId={}", op, rule.getId());
                yield false;
            }
        };
    }

    /**
     * 环比变化率（%）：与上一采集值比较（上一值存 Redis monitor:alarm:prev:*），
     * 无上一值或上一值为 0 时记 0（首周期不触发）
     */
    private double changeRate(MonitorAlarmRule rule, MetricPoint point) {
        String prevKey = PREV_VALUE_PREFIX + rule.getId() + ":" + point.instanceId() + ":" + point.metricName();
        Double prev = MonitorCacheUtil.getDouble(prevKey);
        RedisUtil.setValue(prevKey, point.value(), 3600L);
        if (prev == null || prev == 0D) {
            return 0D;
        }
        return (point.value() - prev) / Math.abs(prev) * 100D;
    }

    /**
     * 违规计数 TTL：3×实例采集间隔（实例不存在时按 90s）
     */
    private long violationTtl(Long instanceId) {
        MonitorInstanceDTO instance = instanceManager.getDtoById(instanceId);
        int interval = instance != null && instance.getCollectInterval() != null
            ? instance.getCollectInterval() : 30;
        return (long) interval * VIOLATION_TTL_MULTIPLIER;
    }
}
