package com.carlos.monitor.collect.sink;

import com.carlos.monitor.collect.spi.MetricPoint;

import java.util.List;

/**
 * <p>
 * 告警评估钩子（N6 告警引擎实现并注册为 Bean；未实现时评估自动跳过）
 * 在指标批次落盘前由 Disruptor 事件处理器单线程调用，无并发问题
 * </p>
 *
 * @author Carlos
 * @date 2026年10月2日
 */
public interface AlarmEvaluateHook {

    /**
     * 评估指标点是否触发告警
     *
     * @param points 指标点批次
     */
    void evaluate(List<MetricPoint> points);
}
