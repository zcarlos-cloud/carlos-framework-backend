package com.carlos.monitor.collect.sink;

/**
 * <p>
 * Sink 事件类型
 * </p>
 *
 * @author Carlos
 * @date 2026年10月2日
 */
public enum SinkEventType {

    /**
     * 指标点（monitor_metric_point）
     */
    METRIC_POINT,

    /**
     * Redis 慢日志（monitor_redis_slowlog）
     */
    SLOWLOG,

    /**
     * 慢 SQL（monitor_db_slow_sql）
     */
    SLOW_SQL
}
