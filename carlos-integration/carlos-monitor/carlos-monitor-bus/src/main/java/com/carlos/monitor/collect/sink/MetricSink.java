package com.carlos.monitor.collect.sink;

import com.carlos.monitor.collect.spi.MetricPoint;
import com.carlos.monitor.collect.spi.SlowSqlRow;
import com.carlos.monitor.collect.spi.SlowlogRow;
import com.carlos.disruptor.core.DisruptorTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * <p>
 * 指标写入入口（采集侧唯一入口）
 * 事件投入 Disruptor 环形缓冲，由 MonitorMetricEventHandler 批量聚合落盘
 * </p>
 *
 * @author Carlos
 * @date 2026年10月2日
 */
@Slf4j
@Component
public class MetricSink {

    private final DisruptorTemplate<MonitorSinkEvent> disruptorTemplate;

    public MetricSink(@Qualifier("monitorDisruptorTemplate") DisruptorTemplate<MonitorSinkEvent> disruptorTemplate) {
        this.disruptorTemplate = disruptorTemplate;
    }

    /**
     * 投递指标点
     */
    public void offer(MetricPoint point) {
        if (point != null) {
            publish(MonitorSinkEvent.ofMetric(point));
        }
    }

    /**
     * 批量投递指标点
     */
    public void offerMetrics(List<MetricPoint> points) {
        if (points != null) {
            points.forEach(this::offer);
        }
    }

    /**
     * 投递 Redis 慢日志行
     */
    public void offerSlowlog(SlowlogRow row) {
        if (row != null) {
            publish(MonitorSinkEvent.ofSlowlog(row));
        }
    }

    /**
     * 批量投递 Redis 慢日志行
     */
    public void offerSlowlogs(List<SlowlogRow> rows) {
        if (rows != null) {
            rows.forEach(this::offerSlowlog);
        }
    }

    /**
     * 投递慢 SQL 行
     */
    public void offerSlowSql(SlowSqlRow row) {
        if (row != null) {
            publish(MonitorSinkEvent.ofSlowSql(row));
        }
    }

    /**
     * 批量投递慢 SQL 行
     */
    public void offerSlowSqls(List<SlowSqlRow> rows) {
        if (rows != null) {
            rows.forEach(this::offerSlowSql);
        }
    }

    private void publish(MonitorSinkEvent event) {
        boolean published = disruptorTemplate.tryPublishEvent(event);
        if (!published) {
            // 环形缓冲满：指标可丢弃（下一采集周期会再来），只告警不阻塞采集线程
            log.warn("Monitor disruptor ring buffer full, drop event type:{}", event.type());
        }
    }
}
