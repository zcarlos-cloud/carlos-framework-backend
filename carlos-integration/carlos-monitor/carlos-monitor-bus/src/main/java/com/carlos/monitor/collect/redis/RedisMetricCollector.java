package com.carlos.monitor.collect.redis;

import cn.hutool.core.util.StrUtil;
import com.carlos.monitor.collect.spi.CollectorContext;
import com.carlos.monitor.collect.spi.MetricCollector;
import com.carlos.monitor.collect.spi.MetricPoint;
import com.carlos.monitor.constant.MetricNameConstant;
import com.carlos.monitor.enums.InstanceTypeEnum;
import com.carlos.monitor.enums.MetricSourceEnum;
import com.carlos.monitor.instance.connection.RedisManagedConnection;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.models.role.RedisInstance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * Redis 指标采集器
 * INFO 分段解析（memory/stats/clients/persistence/replication/keyspace）；
 * cluster 模式逐节点采集（node 填节点地址）+ CLUSTER INFO；sentinel 模式识别主从角色
 * </p>
 *
 * @author Carlos
 * @date 2026年10月2日
 */
@Slf4j
@Component
public class RedisMetricCollector implements MetricCollector {

    @Override
    public boolean support(InstanceTypeEnum instanceType) {
        return instanceType == InstanceTypeEnum.REDIS_STANDALONE
            || instanceType == InstanceTypeEnum.REDIS_CLUSTER
            || instanceType == InstanceTypeEnum.REDIS_SENTINEL;
    }

    @Override
    public List<MetricPoint> collect(CollectorContext ctx) throws Exception {
        if (!(ctx.connection() instanceof RedisManagedConnection redisConnection)) {
            return List.of();
        }
        Long instanceId = ctx.instanceId();
        List<MetricPoint> points = new ArrayList<>();
        if (redisConnection.isCluster()) {
            collectCluster(instanceId, redisConnection, points);
        } else {
            // standalone / sentinel：单连接采集，node 为空；sentinel 主从角色由 INFO replication 体现
            String info = redisConnection.getConnection().sync().info();
            points.addAll(parseInfo(instanceId, "", info));
        }
        return points;
    }

    /**
     * cluster 模式：逐 master 节点 INFO + CLUSTER INFO
     */
    private void collectCluster(Long instanceId, RedisManagedConnection redisConnection,
                                List<MetricPoint> points) {
        Partitions partitions = redisConnection.getClusterConnection().getPartitions();
        for (RedisClusterNode partition : partitions) {
            // 只采 master 节点（slave 指标随主节点汇报）
            if (partition.getRole() != RedisInstance.Role.MASTER) {
                continue;
            }
            String nodeAddress = partition.getUri().getHost() + ":" + partition.getUri().getPort();
            try (StatefulRedisConnection<String, String> nodeConnection =
                     redisConnection.getClusterConnection().getConnection(partition.getNodeId())) {
                String info = nodeConnection.sync().info();
                points.addAll(parseInfo(instanceId, nodeAddress, info));
            } catch (Exception e) {
                // 单节点失败不影响其他节点
                log.warn("采集 cluster 节点失败, instanceId:{}, node:{}, error:{}",
                    instanceId, nodeAddress, e.getMessage());
            }
        }
        try {
            String clusterInfo = redisConnection.getClusterConnection().sync().clusterInfo();
            Map<String, String> values = parseInfoToMap(clusterInfo);
            points.add(metric(instanceId, MetricNameConstant.REDIS_CLUSTER_STATE, "",
                "ok".equals(values.get("cluster_state")) ? 1D : 0D));
            points.add(metric(instanceId, MetricNameConstant.REDIS_CLUSTER_SLOTS_ASSIGNED, "",
                parseDouble(values.get("cluster_slots_assigned"))));
        } catch (Exception e) {
            log.warn("采集 CLUSTER INFO 失败, instanceId:{}, error:{}", instanceId, e.getMessage());
        }
    }

    /**
     * 解析 INFO 全量文本为指标点
     */
    private List<MetricPoint> parseInfo(Long instanceId, String node, String info) {
        Map<String, String> values = parseInfoToMap(info);
        List<MetricPoint> points = new ArrayList<>();

        // memory
        double usedMemory = parseDouble(values.get("used_memory"));
        double maxMemory = parseDouble(values.get("maxmemory"));
        points.add(metric(instanceId, MetricNameConstant.REDIS_MEMORY_USED, node, usedMemory));
        points.add(metric(instanceId, MetricNameConstant.REDIS_MEMORY_USED_RSS, node,
            parseDouble(values.get("used_memory_rss"))));
        points.add(metric(instanceId, MetricNameConstant.REDIS_MEMORY_FRAGMENTATION_RATIO, node,
            parseDouble(values.get("mem_fragmentation_ratio"))));
        points.add(metric(instanceId, MetricNameConstant.REDIS_MEMORY_MAXMEMORY, node, maxMemory));
        // 命中率/使用率由采集器算好（maxmemory=0 表示未限制，使用率记 0）
        points.add(metric(instanceId, MetricNameConstant.REDIS_MEMORY_USED_PERCENT, node,
            maxMemory > 0 ? usedMemory * 100D / maxMemory : 0D));

        // stats
        double hits = parseDouble(values.get("keyspace_hits"));
        double misses = parseDouble(values.get("keyspace_misses"));
        points.add(metric(instanceId, MetricNameConstant.REDIS_STATS_OPS_PER_SEC, node,
            parseDouble(values.get("instantaneous_ops_per_sec"))));
        points.add(metric(instanceId, MetricNameConstant.REDIS_STATS_EXPIRED_KEYS, node,
            parseDouble(values.get("expired_keys"))));
        points.add(metric(instanceId, MetricNameConstant.REDIS_STATS_EVICTED_KEYS, node,
            parseDouble(values.get("evicted_keys"))));
        points.add(metric(instanceId, MetricNameConstant.REDIS_STATS_KEYSPACE_HITS, node, hits));
        points.add(metric(instanceId, MetricNameConstant.REDIS_STATS_KEYSPACE_MISSES, node, misses));
        points.add(metric(instanceId, MetricNameConstant.REDIS_STATS_HIT_RATE, node,
            hits + misses > 0 ? hits * 100D / (hits + misses) : 100D));

        // clients
        points.add(metric(instanceId, MetricNameConstant.REDIS_CLIENTS_CONNECTED, node,
            parseDouble(values.get("connected_clients"))));
        points.add(metric(instanceId, MetricNameConstant.REDIS_CLIENTS_BLOCKED, node,
            parseDouble(values.get("blocked_clients"))));

        // persistence
        points.add(metric(instanceId, MetricNameConstant.REDIS_PERSISTENCE_RDB_LAST_SAVE_TIME, node,
            parseDouble(values.get("rdb_last_save_time"))));
        points.add(metric(instanceId, MetricNameConstant.REDIS_PERSISTENCE_RDB_CHANGES_SINCE_LAST_SAVE, node,
            parseDouble(values.get("rdb_changes_since_last_save"))));
        points.add(metric(instanceId, MetricNameConstant.REDIS_PERSISTENCE_AOF_ENABLED, node,
            parseDouble(values.get("aof_enabled"))));

        // replication：role 主=1 从=0；master_link_status up=1 down=0
        String role = values.get("role");
        points.add(metric(instanceId, MetricNameConstant.REDIS_REPLICATION_ROLE, node,
            "master".equals(role) ? 1D : 0D));
        points.add(metric(instanceId, MetricNameConstant.REDIS_REPLICATION_CONNECTED_SLAVES, node,
            parseDouble(values.get("connected_slaves"))));
        points.add(metric(instanceId, MetricNameConstant.REDIS_REPLICATION_MASTER_LINK_STATUS, node,
            "up".equals(values.get("master_link_status")) ? 1D : 0D));

        // keyspace：db0:keys=123,expires=45,avg_ttl=678 → tags={db:dbN}
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (!entry.getKey().startsWith("db")) {
                continue;
            }
            Map<String, String> keyspaceKv = parseKeyspaceLine(entry.getValue());
            Map<String, String> tags = new HashMap<>();
            tags.put("db", entry.getKey());
            points.add(MetricPoint.of(instanceId, MetricSourceEnum.REDIS,
                MetricNameConstant.REDIS_KEYSPACE_KEYS, node, tags, parseDouble(keyspaceKv.get("keys"))));
            points.add(MetricPoint.of(instanceId, MetricSourceEnum.REDIS,
                MetricNameConstant.REDIS_KEYSPACE_EXPIRES, node, tags, parseDouble(keyspaceKv.get("expires"))));
            points.add(MetricPoint.of(instanceId, MetricSourceEnum.REDIS,
                MetricNameConstant.REDIS_KEYSPACE_AVG_TTL, node, tags, parseDouble(keyspaceKv.get("avg_ttl"))));
        }
        return points;
    }

    /**
     * INFO 文本解析为 kv（忽略 # section 行）
     */
    private Map<String, String> parseInfoToMap(String info) {
        Map<String, String> values = new HashMap<>();
        if (StrUtil.isBlank(info)) {
            return values;
        }
        for (String line : info.split("\r?\n")) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            int idx = line.indexOf(':');
            if (idx > 0) {
                values.put(line.substring(0, idx).trim(), line.substring(idx + 1).trim());
            }
        }
        return values;
    }

    /**
     * keyspace 行解析：keys=123,expires=45,avg_ttl=678
     */
    private Map<String, String> parseKeyspaceLine(String line) {
        Map<String, String> kv = new HashMap<>();
        for (String pair : line.split(",")) {
            int idx = pair.indexOf('=');
            if (idx > 0) {
                kv.put(pair.substring(0, idx).trim(), pair.substring(idx + 1).trim());
            }
        }
        return kv;
    }

    private MetricPoint metric(Long instanceId, String name, String node, double value) {
        return MetricPoint.of(instanceId, MetricSourceEnum.REDIS, name, node, value);
    }

    private double parseDouble(String value) {
        if (StrUtil.isBlank(value)) {
            return 0D;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0D;
        }
    }
}
