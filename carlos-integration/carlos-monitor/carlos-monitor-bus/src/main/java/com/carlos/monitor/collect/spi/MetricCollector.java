package com.carlos.monitor.collect.spi;

import com.carlos.monitor.enums.InstanceTypeEnum;

import java.util.List;

/**
 * <p>
 * 指标采集器 SPI
 * 新中间件类型 = 新增 SPI 实现 + 字典项，不动主链路
 * </p>
 * <p>
 * 实现约束：单个实例的采集异常由实现内部消化或上抛，
 * 路由层（CollectTaskRouter）会整体 try-catch 隔离，不影响其他实例
 * </p>
 *
 * @author Carlos
 * @date 2026年10月2日
 */
public interface MetricCollector {

    /**
     * 是否支持该实例类型（一个采集器可支持多种类型，如 Redis 三形态）
     *
     * @param instanceType 实例类型
     * @return true=支持
     */
    boolean support(InstanceTypeEnum instanceType);

    /**
     * 采集指标点
     *
     * @param ctx 采集上下文
     * @return 指标点列表（失败返回空列表，不得返回 null）
     * @throws Exception 连接级致命异常（路由层据此判定实例 DOWN）
     */
    List<MetricPoint> collect(CollectorContext ctx) throws Exception;
}
