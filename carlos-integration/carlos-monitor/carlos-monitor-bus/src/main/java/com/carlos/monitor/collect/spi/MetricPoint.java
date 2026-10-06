package com.carlos.monitor.collect.spi;

import com.carlos.monitor.enums.MetricSourceEnum;

import java.time.Instant;
import java.util.Map;

/**
 * <p>
 * 指标点（对齐 CK 表 monitor_metric_point）
 * </p>
 *
 * @author Carlos
 * @date 2026年10月2日
 */
public record MetricPoint(Long instanceId, MetricSourceEnum source, String metricName,
                          String node, Map<String, String> tags, double value, Instant ts) {

    public MetricPoint {
        node = node == null ? "" : node;
        tags = tags == null ? Map.of() : tags;
        ts = ts == null ? Instant.now() : ts;
    }

    /**
     * 构造无 tags 的指标点
     */
    public static MetricPoint of(Long instanceId, MetricSourceEnum source, String metricName,
                                 String node, double value) {
        return new MetricPoint(instanceId, source, metricName, node, Map.of(), value, Instant.now());
    }

    /**
     * 构造带 tags 的指标点
     */
    public static MetricPoint of(Long instanceId, MetricSourceEnum source, String metricName,
                                 String node, Map<String, String> tags, double value) {
        return new MetricPoint(instanceId, source, metricName, node, tags, value, Instant.now());
    }
}
