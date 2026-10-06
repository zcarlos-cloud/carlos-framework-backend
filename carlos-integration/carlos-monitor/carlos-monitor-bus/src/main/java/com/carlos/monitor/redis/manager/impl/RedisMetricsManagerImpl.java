package com.carlos.monitor.redis.manager.impl;

import cn.hutool.core.util.StrUtil;
import com.carlos.monitor.collect.sink.MonitorCkQueryClient;
import com.carlos.monitor.constant.CacheKeyConstant;
import com.carlos.monitor.instance.connection.InstanceConnectionRegistry;
import com.carlos.monitor.instance.connection.ManagedConnection;
import com.carlos.monitor.instance.connection.RedisManagedConnection;
import com.carlos.monitor.redis.manager.RedisMetricsManager;
import com.carlos.monitor.redis.pojo.vo.RedisNodeVO;
import com.carlos.redis.util.RedisUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * Redis 监控查询 查询封装实现类（实时值 Caffeine 60s + Redis Hash 二级缓存，时序走 CK）
 * </p>
 *
 * @author Carlos
 * @date 2026年10月3日
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class RedisMetricsManagerImpl implements RedisMetricsManager {

    /**
     * 当前值快照的本地缓存 TTL（秒），Redis 侧快照 TTL 5min 由 sink 侧维护（设计 2.2）
     */
    private static final long CURRENT_CAFFEINE_SECONDS = 60L;

    private final MonitorCkQueryClient ckQueryClient;

    private final InstanceConnectionRegistry connectionRegistry;

    private final Cache<Long, Map<String, String>> currentCache = Caffeine.newBuilder()
        .expireAfterWrite(CURRENT_CAFFEINE_SECONDS, TimeUnit.SECONDS)
        .maximumSize(1000)
        .build();

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, String> getCurrentMetrics(Long instanceId) {
        Map<String, String> cached = currentCache.getIfPresent(instanceId);
        if (cached != null) {
            return cached;
        }
        Map<Object, Object> hash = RedisUtil.getHash(CacheKeyConstant.metricCurrentKey(instanceId));
        Map<String, String> result = new LinkedHashMap<>();
        if (hash != null) {
            hash.forEach((field, value) -> result.put(String.valueOf(field), String.valueOf(value)));
        }
        currentCache.put(instanceId, result);
        return result;
    }

    @Override
    public List<?> getMetricHistory(Long instanceId, String metricName, String node,
                                    Instant start, Instant end, boolean aggregated, int limit) {
        if (aggregated) {
            return ckQueryClient.queryMetricHourly(instanceId, metricName, node, start, end);
        }
        return ckQueryClient.queryMetricPoints(instanceId, metricName, node, start, end, limit);
    }

    @Override
    public List<RedisNodeVO> listNodes(Long instanceId) {
        ManagedConnection connection = connectionRegistry.getConnection(instanceId);
        if (!(connection instanceof RedisManagedConnection redisConnection)) {
            return List.of();
        }
        if (redisConnection.isCluster()) {
            return parseClusterNodes(redisConnection.getClusterConnection().sync().clusterNodes());
        }
        return parseReplicationNodes(redisConnection.getConnection().sync().info("replication"));
    }

    @Override
    public List<MonitorCkQueryClient.SlowlogQueryRow> pageSlowlogs(Long instanceId, String node,
                                                                   Instant start, Instant end, Long minDurationUs,
                                                                   int offset, int limit) {
        return ckQueryClient.pageSlowlogs(instanceId, node, start, end, minDurationUs, offset, limit);
    }

    @Override
    public void evictCurrentCache(Long instanceId) {
        currentCache.invalidate(instanceId);
    }

    /**
     * 解析 CLUSTER NODES：id addr flags master ping pong epoch link-state [slots...]
     */
    private List<RedisNodeVO> parseClusterNodes(String clusterNodes) {
        List<RedisNodeVO> nodes = new ArrayList<>();
        if (StrUtil.isBlank(clusterNodes)) {
            return nodes;
        }
        for (String line : clusterNodes.split("\r?\n")) {
            String[] parts = line.trim().split("\\s+");
            if (parts.length < 8) {
                continue;
            }
            RedisNodeVO node = new RedisNodeVO();
            String address = parts[1];
            int atIdx = address.indexOf('@');
            node.setAddress(atIdx > 0 ? address.substring(0, atIdx) : address);
            String flags = parts[2];
            node.setRole(flags.contains("master") ? "master" : "slave");
            node.setStatus(parts[7]);
            if (parts.length > 8) {
                node.setSlots(String.join(" ", List.of(parts).subList(8, parts.length)));
            }
            if (!"-".equals(parts[3])) {
                node.setMasterAddress(parts[3]);
            }
            nodes.add(node);
        }
        return nodes;
    }

    /**
     * 解析 INFO replication（standalone/sentinel）：role + slaveN:ip=...,port=...,state=...
     */
    private List<RedisNodeVO> parseReplicationNodes(String info) {
        List<RedisNodeVO> nodes = new ArrayList<>();
        if (StrUtil.isBlank(info)) {
            return nodes;
        }
        RedisNodeVO self = new RedisNodeVO();
        self.setAddress("self");
        for (String line : info.split("\r?\n")) {
            if (line.startsWith("role:")) {
                self.setRole(line.substring(5).trim());
                self.setStatus("connected");
            } else if (line.startsWith("slave") && line.contains(":ip=")) {
                RedisNodeVO slave = new RedisNodeVO();
                String kv = line.substring(line.indexOf(':') + 1);
                Map<String, String> fields = new LinkedHashMap<>();
                for (String pair : kv.split(",")) {
                    int idx = pair.indexOf('=');
                    if (idx > 0) {
                        fields.put(pair.substring(0, idx), pair.substring(idx + 1));
                    }
                }
                slave.setAddress(fields.get("ip") + ":" + fields.get("port"));
                slave.setRole("slave");
                slave.setStatus(fields.get("state"));
                slave.setMasterAddress("self");
                nodes.add(slave);
            } else if (line.startsWith("master_host:")) {
                self.setMasterAddress(line.substring(12).trim() + ":" + valueOf(info, "master_port"));
            }
        }
        nodes.addFirst(self);
        return nodes;
    }

    private String valueOf(String info, String key) {
        for (String line : info.split("\r?\n")) {
            if (line.startsWith(key + ":")) {
                return line.substring(key.length() + 1).trim();
            }
        }
        return "";
    }
}
