package com.carlos.monitor.redis.proxy;

import cn.hutool.core.util.StrUtil;
import com.carlos.monitor.instance.connection.RedisManagedConnection;
import io.lettuce.core.KeyScanCursor;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.ScanCursor;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.models.role.RedisInstance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 游标式 SCAN 封装
 * 单次接口调用最多推进 K 个 SCAN 批次（K 可配防长阻塞），返回 nextCursor；
 * type 过滤在 SCAN 后批量 TYPE 管线化；
 * cluster 模式游标格式 nodeIndex:cursor，逐 master 节点推进
 * </p>
 *
 * @author Carlos
 * @date 2026年10月3日
 */
@Slf4j
@Component
public class KeyScanner {

    /**
     * SCAN 结果
     *
     * @param keys         过滤后的 key 列表（展示层编码前）
     * @param nextCursor   下一游标（finished=true 时为空串；cluster 格式 nodeIndex:cursor）
     * @param scannedCount 本次实际扫描的 key 数量（过滤前）
     * @param finished     是否扫描结束
     */
    public record KeyScanResult(List<String> keys, String nextCursor, long scannedCount, boolean finished) {
    }

    /**
     * 游标 SCAN
     *
     * @param connection Redis 受管连接
     * @param cursor     游标（空/INITIAL 从头开始；cluster 格式 nodeIndex:cursor）
     * @param pattern    匹配模式（可为空）
     * @param type       类型过滤（string/hash/list/set/zset/stream，可为空）
     * @param count      每批 SCAN COUNT
     * @param maxBatches 单次调用最多推进的批次数
     * @return 扫描结果
     */
    public KeyScanResult scan(RedisManagedConnection connection, String cursor, String pattern,
                              String type, int count, int maxBatches) {
        ScanArgs args = ScanArgs.Builder.matches(StrUtil.isBlank(pattern) ? "*" : pattern).limit(count);
        if (connection.isCluster()) {
            return scanCluster(connection, cursor, args, type, maxBatches);
        }
        List<String> candidates = new ArrayList<>();
        long scanned = 0;
        int batches = 0;
        boolean finished = false;
        ScanCursor scanCursor = StrUtil.isBlank(cursor) ? ScanCursor.INITIAL : ScanCursor.of(cursor);
        StatefulRedisConnection<String, String> standalone = connection.getConnection();
        while (batches < maxBatches) {
            KeyScanCursor<String> result = standalone.sync().scan(scanCursor, args);
            candidates.addAll(result.getKeys());
            scanned += result.getKeys().size();
            batches++;
            scanCursor = result;
            if (result.isFinished()) {
                finished = true;
                break;
            }
        }
        List<String> keys = filterByType(connection, candidates, type);
        String nextCursor = finished ? "" : scanCursor.getCursor();
        return new KeyScanResult(keys, nextCursor, scanned, finished);
    }

    /**
     * cluster：游标格式 nodeIndex:cursor，当前节点扫完推进下一 master 节点
     */
    private KeyScanResult scanCluster(RedisManagedConnection connection, String cursor,
                                      ScanArgs args, String type, int maxBatches) {
        List<RedisClusterNode> masters = new ArrayList<>();
        for (RedisClusterNode node : connection.getClusterConnection().getPartitions()) {
            if (node.getRole() == RedisInstance.Role.MASTER) {
                masters.add(node);
            }
        }
        if (masters.isEmpty()) {
            return new KeyScanResult(List.of(), "", 0, true);
        }
        int nodeIndex = 0;
        String nodeCursor = "";
        if (StrUtil.isNotBlank(cursor) && cursor.contains(":")) {
            String[] parts = cursor.split(":", 2);
            nodeIndex = Integer.parseInt(parts[0]);
            nodeCursor = parts[1];
        }
        List<String> candidates = new ArrayList<>();
        long scanned = 0;
        int batches = 0;
        boolean allFinished = false;
        while (batches < maxBatches && nodeIndex < masters.size()) {
            RedisClusterNode node = masters.get(nodeIndex);
            try (StatefulRedisConnection<String, String> nodeConnection =
                     connection.getClusterConnection().getConnection(node.getNodeId())) {
                ScanCursor scanCursor = StrUtil.isBlank(nodeCursor) ? ScanCursor.INITIAL : ScanCursor.of(nodeCursor);
                while (batches < maxBatches) {
                    KeyScanCursor<String> result = nodeConnection.sync().scan(scanCursor, args);
                    candidates.addAll(result.getKeys());
                    scanned += result.getKeys().size();
                    batches++;
                    scanCursor = result;
                    if (result.isFinished()) {
                        break;
                    }
                }
                if (scanCursor.isFinished()) {
                    // 当前节点扫完，推进下一节点
                    nodeIndex++;
                    nodeCursor = "";
                    if (nodeIndex >= masters.size()) {
                        allFinished = true;
                    }
                } else {
                    nodeCursor = scanCursor.getCursor();
                }
            } catch (Exception e) {
                log.warn("cluster 节点 SCAN 失败, node:{}, error:{}",
                    node.getUri().getHost() + ":" + node.getUri().getPort(), e.getMessage());
                nodeIndex++;
                nodeCursor = "";
                if (nodeIndex >= masters.size()) {
                    allFinished = true;
                }
            }
        }
        List<String> keys = filterByType(connection, candidates, type);
        String nextCursor = allFinished ? "" : nodeIndex + ":" + nodeCursor;
        return new KeyScanResult(keys, nextCursor, scanned, allFinished);
    }

    /**
     * 批量 TYPE（autoFlush=false 攒批后一次 flush），返回 key→类型 映射
     * cluster 连接按 key 自动路由到分片，共用同一段逻辑
     */
    public Map<String, String> batchTypes(RedisManagedConnection connection, List<String> keys) {
        if (keys.isEmpty()) {
            return Map.of();
        }
        if (connection.isCluster()) {
            RedisAdvancedClusterAsyncCommands<String, String> async =
                connection.getClusterConnection().async();
            connection.getClusterConnection().setAutoFlushCommands(false);
            try {
                List<RedisFuture<String>> futures = new ArrayList<>(keys.size());
                for (String key : keys) {
                    futures.add(async.type(key));
                }
                connection.getClusterConnection().flushCommands();
                return collectTypes(keys, futures);
            } finally {
                connection.getClusterConnection().setAutoFlushCommands(true);
            }
        }
        StatefulRedisConnection<String, String> standalone = connection.getConnection();
        RedisAsyncCommands<String, String> async = standalone.async();
        standalone.setAutoFlushCommands(false);
        try {
            List<RedisFuture<String>> futures = new ArrayList<>(keys.size());
            for (String key : keys) {
                futures.add(async.type(key));
            }
            standalone.flushCommands();
            return collectTypes(keys, futures);
        } finally {
            standalone.setAutoFlushCommands(true);
        }
    }

    private Map<String, String> collectTypes(List<String> keys, List<RedisFuture<String>> futures) {
        Map<String, String> types = new LinkedHashMap<>(keys.size());
        for (int i = 0; i < keys.size(); i++) {
            try {
                String type = futures.get(i).get(3, TimeUnit.SECONDS);
                types.put(keys.get(i), type == null ? "unknown" : type);
            } catch (Exception e) {
                log.debug("TYPE 查询失败, key:{}, error:{}", keys.get(i), e.getMessage());
                types.put(keys.get(i), "unknown");
            }
        }
        return types;
    }

    /**
     * type 过滤：SCAN 后批量 TYPE 管线化
     */
    private List<String> filterByType(RedisManagedConnection connection,
                                      List<String> candidates, String type) {
        if (StrUtil.isBlank(type) || candidates.isEmpty()) {
            return candidates;
        }
        String expectedType = type.toLowerCase();
        Map<String, String> types = batchTypes(connection, candidates);
        return candidates.stream()
            .filter(key -> expectedType.equals(types.get(key)))
            .toList();
    }
}
