package com.carlos.monitor.redis.manager;

import com.carlos.monitor.collect.sink.MonitorCkQueryClient;
import com.carlos.monitor.redis.pojo.vo.RedisNodeVO;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * Redis 监控查询 查询封装接口
 * </p>
 *
 * @author Carlos
 * @date 2026年10月3日
 */
public interface RedisMetricsManager {

    /**
     * 当前指标快照（Caffeine 60s + Redis Hash monitor:metric:current:{id}，TTL 5min 由 sink 侧维护）
     *
     * @param instanceId 实例ID
     * @return 指标名（含节点后缀）→ {v, ts} 映射
     */
    Map<String, String> getCurrentMetrics(Long instanceId);

    /**
     * 指标时序（range<=6h 原始点，>6h 小时聚合）
     *
     * @param aggregated 是否聚合结果（小时表）
     */
    List<?> getMetricHistory(Long instanceId, String metricName, String node,
                             Instant start, Instant end, boolean aggregated, int limit);

    /**
     * cluster/sentinel 节点与拓扑（CLUSTER NODES / INFO replication 解析）
     *
     * @param instanceId 实例ID
     * @return 节点列表
     */
    List<RedisNodeVO> listNodes(Long instanceId);

    /**
     * 慢日志分页（CK）
     */
    List<MonitorCkQueryClient.SlowlogQueryRow> pageSlowlogs(Long instanceId, String node,
                                                            Instant start, Instant end, Long minDurationUs,
                                                            int offset, int limit);

    /**
     * 失效当前指标快照的 Caffeine 缓存
     */
    void evictCurrentCache(Long instanceId);
}
