package com.carlos.monitor.collect.sink;

import com.carlos.disruptor.core.DisruptorEvent;
import com.carlos.disruptor.core.DisruptorEventHandler;
import com.carlos.json.JsonUtils;
import com.carlos.monitor.collect.spi.MetricPoint;
import com.carlos.monitor.constant.CacheKeyConstant;
import com.carlos.redis.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 监控指标 Disruptor 事件处理器
 * 职责：指标点当前值快照回写 Redis（供实时查询）、触发告警评估钩子（N6）、批量落盘 CK
 * 单线程事件处理器内调用，无并发问题
 * </p>
 *
 * @author Carlos
 * @date 2026年10月2日
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MonitorMetricEventHandler implements DisruptorEventHandler<MonitorSinkEvent> {

    /**
     * 当前值快照 TTL（秒）
     */
    private static final long CURRENT_SNAPSHOT_TTL_SECONDS = 300L;

    private final MonitorClickHouseWriter clickHouseWriter;

    private final ObjectProvider<AlarmEvaluateHook> alarmEvaluateHook;

    @Override
    public void onEvent(DisruptorEvent<MonitorSinkEvent> event, long sequence, boolean endOfBatch) {
        MonitorSinkEvent sinkEvent = event.getData();
        if (sinkEvent == null) {
            log.warn("收到空的监控 sink 事件，序列号: {}", sequence);
            return;
        }
        try {
            switch (sinkEvent.type()) {
                case METRIC_POINT -> handleMetricPoint(sinkEvent.metricPoint());
                case SLOWLOG -> clickHouseWriter.addSlowlog(sinkEvent.slowlogRow());
                case SLOW_SQL -> clickHouseWriter.addSlowSql(sinkEvent.slowSqlRow());
                default -> log.warn("未知 sink 事件类型: {}", sinkEvent.type());
            }
        } catch (Exception e) {
            // 单事件失败不阻断后续事件（指标允许丢失，日志留痕）
            log.error("处理监控 sink 事件失败，序列号: {}, 类型: {}", sequence, sinkEvent.type(), e);
        }
    }

    private void handleMetricPoint(MetricPoint point) {
        if (point == null) {
            return;
        }
        // 1. 当前值快照回写 Redis（Hash：field=metricName[@node]，value={v,ts}），供 N4/N5 实时查询
        snapshotCurrentValue(point);

        // 2. 告警评估钩子（N6 告警引擎实现后生效，落盘前评估）
        AlarmEvaluateHook hook = alarmEvaluateHook.getIfAvailable();
        if (hook != null) {
            hook.evaluate(List.of(point));
        }

        // 3. 加入 CK 批量写入缓冲
        clickHouseWriter.addMetricPoint(point);
    }

    private void snapshotCurrentValue(MetricPoint point) {
        try {
            String key = CacheKeyConstant.metricCurrentKey(point.instanceId());
            String field = point.node().isEmpty() ? point.metricName() : point.metricName() + "@" + point.node();
            Map<String, Object> value = new HashMap<>();
            value.put("v", point.value());
            value.put("ts", point.ts().toEpochMilli());
            RedisUtil.putHash(key, field, JsonUtils.toJson(value), CURRENT_SNAPSHOT_TTL_SECONDS);
        } catch (Exception e) {
            // 快照失败不影响落盘
            log.warn("回写指标当前值快照失败, instanceId:{}, metric:{}, error:{}",
                point.instanceId(), point.metricName(), e.getMessage());
        }
    }

    @Override
    public String getHandlerName() {
        return "MonitorMetricEventHandler";
    }
}
