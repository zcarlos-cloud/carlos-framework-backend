package com.carlos.monitor.collect.sink;

import com.carlos.monitor.collect.spi.MetricPoint;
import com.carlos.monitor.collect.spi.SlowSqlRow;
import com.carlos.monitor.collect.spi.SlowlogRow;

/**
 * <p>
 * Sink 事件（Disruptor 环形缓冲载荷，三类事件共用一个环）
 * </p>
 *
 * @author Carlos
 * @date 2026年10月2日
 */
public record MonitorSinkEvent(SinkEventType type, MetricPoint metricPoint,
                               SlowlogRow slowlogRow, SlowSqlRow slowSqlRow) {

    public static MonitorSinkEvent ofMetric(MetricPoint point) {
        return new MonitorSinkEvent(SinkEventType.METRIC_POINT, point, null, null);
    }

    public static MonitorSinkEvent ofSlowlog(SlowlogRow row) {
        return new MonitorSinkEvent(SinkEventType.SLOWLOG, null, row, null);
    }

    public static MonitorSinkEvent ofSlowSql(SlowSqlRow row) {
        return new MonitorSinkEvent(SinkEventType.SLOW_SQL, null, null, row);
    }
}
