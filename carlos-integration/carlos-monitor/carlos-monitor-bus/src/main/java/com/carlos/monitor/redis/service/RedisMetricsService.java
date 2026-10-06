package com.carlos.monitor.redis.service;

import com.carlos.monitor.collect.sink.MonitorCkQueryClient;
import com.carlos.monitor.redis.pojo.vo.RedisNodeVO;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * Redis 监控查询 业务接口
 * </p>
 *
 * @author Carlos
 * @date 2026年10月3日
 */
public interface RedisMetricsService {

    /**
     * 当前指标快照
     *
     * @param instanceId 实例ID
     * @return 指标名 → {v, ts} JSON 映射
     */
    Map<String, String> getCurrentMetrics(Long instanceId);

    /**
     * 指标时序（range<=6h 原始点，>6h 小时聚合）
     *
     * @param metricName 指标名
     * @param range      范围：1h/6h/24h/7d/30d
     * @param node       节点地址（可空）
     * @return 原始点或聚合点列表
     */
    List<?> getMetricHistory(Long instanceId, String metricName, String range, String node);

    /**
     * cluster/sentinel 节点与拓扑
     */
    List<RedisNodeVO> listNodes(Long instanceId);

    /**
     * 慢日志分页（时间/耗时/节点过滤）
     *
     * @param hours         最近 N 小时（默认 24）
     * @param minDurationUs 最小耗时（微秒，可空）
     * @param node          节点地址（可空）
     * @param offset        偏移
     * @param limit         每页大小
     */
    List<MonitorCkQueryClient.SlowlogQueryRow> pageSlowlogs(Long instanceId, String node,
                                                            Integer hours, Long minDurationUs,
                                                            int offset, int limit);
}
