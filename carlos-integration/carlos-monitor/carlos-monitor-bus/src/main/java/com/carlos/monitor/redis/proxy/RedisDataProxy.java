package com.carlos.monitor.redis.proxy;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.carlos.json.JsonUtils;
import com.carlos.monitor.config.MonitorProperties;
import com.carlos.monitor.enums.InstanceTypeEnum;
import com.carlos.monitor.enums.MonitorErrorCode;
import com.carlos.monitor.instance.connection.InstanceConnectionRegistry;
import com.carlos.monitor.instance.connection.ManagedConnection;
import com.carlos.monitor.instance.connection.RedisManagedConnection;
import com.carlos.monitor.instance.manager.MonitorInstanceManager;
import com.carlos.monitor.instance.pojo.dto.MonitorInstanceDTO;
import com.carlos.monitor.mask.DataMasker;
import io.lettuce.core.Limit;
import io.lettuce.core.MapScanCursor;
import io.lettuce.core.Range;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.ScanCursor;
import io.lettuce.core.ScoredValueScanCursor;
import io.lettuce.core.StreamMessage;
import io.lettuce.core.ValueScanCursor;
import io.lettuce.core.cluster.api.sync.RedisClusterCommands;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * Redis 数据管理代理（类型化 API，无任何任意命令执行入口）
 * 所有 key 二进制安全编码由 {@link KeyCodec} 统一；cluster 模式由 lettuce cluster 连接
 * 按 CRC16 自动路由分片（key 浏览走 {@link KeyScanner} 逐节点聚合视图）
 * </p>
 * <p>
 * <b>禁用命令清单（设计上杜绝命令注入，本类不提供等价入口）：</b>
 * {@code KEYS、FLUSHALL、FLUSHDB、CONFIG、DEBUG、SHUTDOWN、MONITOR、SLAVEOF/REPLICAOF、CLUSTER 写操作}
 * </p>
 *
 * @author Carlos
 * @date 2026年10月3日
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisDataProxy {

    private final InstanceConnectionRegistry connectionRegistry;

    private final MonitorInstanceManager instanceManager;

    private final KeyScanner keyScanner;

    private final RedisWriteGuard writeGuard;

    private final DataMasker dataMasker;

    private final MonitorProperties monitorProperties;

    /**
     * key 详情
     */
    public record KeyDetail(String key, String type, Long ttlSeconds, Long memoryBytes,
                            Long elementCount, Object value, String subCursor, boolean subFinished) {
    }

    /**
     * key 浏览（SCAN 游标分页，单次最多推进 K 批）
     */
    public KeyScanner.KeyScanResult scan(Long instanceId, String cursor, String pattern,
                                         String type, Integer count, Integer dbIndex) {
        RedisManagedConnection connection = redisConnection(instanceId, dbIndex);
        int scanCount = count == null || count <= 0
            ? monitorProperties.getCollect().getScanCount() : count;
        return keyScanner.scan(connection, cursor, pattern, type,
            scanCount, monitorProperties.getRedis().getScanMaxBatches());
    }

    /**
     * key 树形节点（分隔符聚合的一层视图）
     *
     * @param name     节点名（folder 为分隔段，leaf 为 key 末段；展示编码）
     * @param fullKey  完整 key（仅 leaf；展示编码）
     * @param type     Redis 类型（仅 leaf）
     * @param leaf     是否叶子
     * @param keyCount folder 下匹配 key 数
     */
    public record KeyTreeNode(String name, String fullKey, String type, boolean leaf, long keyCount) {
    }

    public record KeyTreeResult(List<KeyTreeNode> nodes, String nextCursor,
                                long scannedCount, boolean finished) {
    }

    /**
     * key 树形分级浏览（参考 RedisInsight Browser：按分隔符聚合、逐层懒加载）
     * <ul>
     * <li>prefix 为空：扫描 pattern（为空则全量 *）作为树根一层</li>
     * <li>prefix 非空：扫描 prefix*，按分隔符截取下一层（子目录内忽略全局 pattern）</li>
     * <li>folder 计数为已扫描匹配数；扫描未完整时调用方应以近似值展示（finished=false）</li>
     * </ul>
     */
    public KeyTreeResult keyTree(Long instanceId, String prefix, String separator,
                                 String pattern, String type, String cursor, Integer count, Integer dbIndex) {
        RedisManagedConnection connection = redisConnection(instanceId, dbIndex);
        String sep = StrUtil.isBlank(separator) ? ":" : separator;
        String rawPrefix = StrUtil.isBlank(prefix) ? "" : KeyCodec.decode(prefix);
        String scanPattern = StrUtil.isBlank(rawPrefix)
            ? (StrUtil.isBlank(pattern) ? "*" : pattern) : rawPrefix + "*";
        int scanCount = count == null || count <= 0
            ? monitorProperties.getCollect().getScanCount() : count;
        KeyScanner.KeyScanResult result = keyScanner.scan(connection, cursor, scanPattern, type,
            scanCount, monitorProperties.getRedis().getScanMaxBatches());

        Map<String, Long> folderCounts = new LinkedHashMap<>();
        List<String> leafKeys = new ArrayList<>();
        for (String key : result.keys()) {
            if (!key.startsWith(rawPrefix)) {
                continue;
            }
            String remainder = key.substring(rawPrefix.length());
            int idx = remainder.indexOf(sep);
            if (idx > 0) {
                folderCounts.merge(remainder.substring(0, idx), 1L, Long::sum);
            } else {
                // 末段无分隔符（或以分隔符开头/结尾）均按本层叶子处理
                leafKeys.add(key);
            }
        }

        Map<String, String> leafTypes = keyScanner.batchTypes(connection, leafKeys);
        List<KeyTreeNode> nodes = new ArrayList<>(folderCounts.size() + leafKeys.size());
        folderCounts.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> nodes.add(new KeyTreeNode(KeyCodec.encode(entry.getKey()),
                null, null, false, entry.getValue())));
        leafKeys.stream()
            .map(key -> Map.entry(key.substring(rawPrefix.length()), key))
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> nodes.add(new KeyTreeNode(KeyCodec.encode(entry.getKey()),
                KeyCodec.encode(entry.getValue()),
                leafTypes.getOrDefault(entry.getValue(), "unknown"), true, 1)));
        return new KeyTreeResult(nodes, result.nextCursor(), result.scannedCount(), result.finished());
    }

    /**
     * key 详情（类型/TTL/内存/值，值按类型分页：subCursor/subCount）
     */
    public KeyDetail keyDetail(Long instanceId, String key, String subCursor, Integer subCount, Integer dbIndex) {
        RedisManagedConnection connection = redisConnection(instanceId, dbIndex);
        RedisClusterCommands<String, String> commands = commands(connection);
        String rawKey = KeyCodec.decode(key);
        String type = commands.type(rawKey);
        if (StrUtil.isBlank(type) || "none".equals(type)) {
            throw MonitorErrorCode.MONITOR_INSTANCE_NOT_FOUND.exception("key 不存在：" + key);
        }
        Long ttl = commands.ttl(rawKey);
        Long memoryBytes = safeMemoryUsage(commands, rawKey);
        int pageSize = subCount == null || subCount <= 0 ? 100 : subCount;
        Object value;
        Long elementCount = null;
        String nextSubCursor = "";
        boolean subFinished = true;
        switch (type) {
            case "string" -> {
                String raw = commands.get(rawKey);
                value = dataMasker.maskRedisValue(instanceId, rawKey, null, raw);
                elementCount = 1L;
            }
            case "hash" -> {
                MapScanCursor<String, String> cursor = commands.hscan(rawKey,
                    subCursor(subCursor), ScanArgs.Builder.limit(pageSize));
                Map<String, String> entries = new LinkedHashMap<>();
                cursor.getMap().forEach((field, val) ->
                    entries.put(field, dataMasker.maskRedisValue(instanceId, rawKey, field, val)));
                value = entries;
                elementCount = commands.hlen(rawKey);
                nextSubCursor = cursor.isFinished() ? "" : cursor.getCursor();
                subFinished = cursor.isFinished();
            }
            case "list" -> {
                long offset = parseOffset(subCursor);
                long total = commands.llen(rawKey);
                value = commands.lrange(rawKey, offset, offset + pageSize - 1).stream()
                    .map(item -> dataMasker.maskRedisValue(instanceId, rawKey, null, item))
                    .toList();
                elementCount = total;
                long next = offset + pageSize;
                subFinished = next >= total;
                nextSubCursor = subFinished ? "" : String.valueOf(next);
            }
            case "set" -> {
                ValueScanCursor<String> cursor = commands.sscan(rawKey,
                    subCursor(subCursor), ScanArgs.Builder.limit(pageSize));
                value = cursor.getValues();
                elementCount = commands.scard(rawKey);
                nextSubCursor = cursor.isFinished() ? "" : cursor.getCursor();
                subFinished = cursor.isFinished();
            }
            case "zset" -> {
                ScoredValueScanCursor<String> cursor = commands.zscan(rawKey,
                    subCursor(subCursor), ScanArgs.Builder.limit(pageSize));
                value = cursor.getValues();
                elementCount = commands.zcard(rawKey);
                nextSubCursor = cursor.isFinished() ? "" : cursor.getCursor();
                subFinished = cursor.isFinished();
            }
            case "stream" -> {
                // stream：XRANGE 按 entry id 续读（subCursor=上页最后一条 entry id）
                elementCount = commands.xlen(rawKey);
                Range<String> range = StrUtil.isBlank(subCursor)
                    ? Range.unbounded()
                    : Range.from(Range.Boundary.excluding(subCursor), Range.Boundary.unbounded());
                List<StreamMessage<String, String>> messages = commands.xrange(rawKey, range, Limit.from(pageSize));
                value = messages;
                subFinished = messages.size() < pageSize;
                nextSubCursor = subFinished || messages.isEmpty()
                    ? "" : messages.getLast().getId();
            }
            default -> throw MonitorErrorCode.MONITOR_REDIS_COMMAND_FORBIDDEN
                .exception("不支持的 key 类型：" + type);
        }
        return new KeyDetail(key, type, ttl, memoryBytes, elementCount, value, nextSubCursor, subFinished);
    }

    /**
     * Redis 逻辑库列表（参考 RedisInsight 库选择器）
     * 库总数取 CONFIG GET databases（无权限时回退 16）；各库键数/过期数取 INFO KEYSPACE
     *
     * @param instanceId 实例ID
     * @return 库列表（index/keyCount/expires/avgTtl/totalDbs；cluster 恒返回 db0）
     */
    public List<Map<String, Object>> databases(Long instanceId) {
        RedisManagedConnection connection = redisConnection(instanceId);
        RedisClusterCommands<String, String> commands = commands(connection);
        int totalDbs = resolveDatabaseCount(commands);
        Map<Integer, long[]> keyspace = parseInfoKeyspace(commands.info("keyspace"));
        List<Map<String, Object>> result = new ArrayList<>(totalDbs);
        for (int i = 0; i < totalDbs; i++) {
            long[] stat = keyspace.get(i);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("index", i);
            item.put("keyCount", stat == null ? 0L : stat[0]);
            item.put("expires", stat == null ? 0L : stat[1]);
            item.put("avgTtl", stat == null ? 0L : stat[2]);
            item.put("totalDbs", totalDbs);
            result.add(item);
        }
        return result;
    }

    private int resolveDatabaseCount(RedisClusterCommands<String, String> commands) {
        try {
            Map<String, String> config = commands.configGet("databases");
            if (config != null && config.containsKey("databases")) {
                return Math.max(1, Integer.parseInt(config.get("databases").trim()));
            }
        } catch (Exception e) {
            log.debug("CONFIG GET databases 不可用，回退 16, error:{}", e.getMessage());
        }
        return 16;
    }

    /**
     * 解析 INFO KEYSPACE 输出：db0:keys=193,expires=45,avg_ttl=0
     */
    /**
     * 解析 INFO KEYSPACE 输出：db0:keys=193,expires=45,avg_ttl=0
     */
    private Map<Integer, long[]> parseInfoKeyspace(String info) {
        Map<Integer, long[]> result = new LinkedHashMap<>();
        if (StrUtil.isBlank(info)) {
            return result;
        }
        for (String line : info.split("\r\n")) {
            if (!line.startsWith("db")) {
                continue;
            }
            int colon = line.indexOf(':');
            if (colon <= 2) {
                continue;
            }
            try {
                int index = Integer.parseInt(line.substring(2, colon).trim());
                long[] stat = new long[3];
                for (String pair : line.substring(colon + 1).split(",")) {
                    String[] kv = pair.split("=");
                    if (kv.length != 2) {
                        continue;
                    }
                    switch (kv[0].trim()) {
                        case "keys" -> stat[0] = Long.parseLong(kv[1].trim());
                        case "expires" -> stat[1] = Long.parseLong(kv[1].trim());
                        case "avg_ttl" -> stat[2] = Long.parseLong(kv[1].trim());
                        default -> {
                        }
                    }
                }
                result.put(index, stat);
            } catch (NumberFormatException e) {
                log.debug("INFO KEYSPACE 行解析失败, line:{}, error:{}", line, e.getMessage());
            }
        }
        return result;
    }

    // ==================== 写操作（全部经 writeGuard 前置校验） ====================

    /**
     * 新增/覆盖 string（ttlSeconds 为空或 &lt;=0 表示不过期）
     */
    public void stringSet(Long instanceId, String key, String value, Long ttlSeconds, Integer dbIndex) {
        checkWritable(instanceId);
        RedisClusterCommands<String, String> commands = commands(redisConnection(instanceId, dbIndex));
        String rawKey = KeyCodec.decode(key);
        if (ttlSeconds != null && ttlSeconds > 0) {
            commands.setex(rawKey, ttlSeconds, value);
        } else {
            commands.set(rawKey, value);
        }
    }

    /**
     * hash 字段写入
     */
    public void hashPut(Long instanceId, String key, String field, String value, Integer dbIndex) {
        checkWritable(instanceId);
        commands(redisConnection(instanceId, dbIndex)).hset(KeyCodec.decode(key), field, value);
    }

    /**
     * list 追加（left=true 头插，否则尾插）
     */
    public void listPush(Long instanceId, String key, String value, boolean left, Integer dbIndex) {
        checkWritable(instanceId);
        RedisClusterCommands<String, String> commands = commands(redisConnection(instanceId, dbIndex));
        String rawKey = KeyCodec.decode(key);
        if (left) {
            commands.lpush(rawKey, value);
        } else {
            commands.rpush(rawKey, value);
        }
    }

    /**
     * set 成员添加
     */
    public void setAdd(Long instanceId, String key, String member, Integer dbIndex) {
        checkWritable(instanceId);
        commands(redisConnection(instanceId, dbIndex)).sadd(KeyCodec.decode(key), member);
    }

    /**
     * zset 成员添加
     */
    public void zsetAdd(Long instanceId, String key, double score, String member, Integer dbIndex) {
        checkWritable(instanceId);
        commands(redisConnection(instanceId, dbIndex)).zadd(KeyCodec.decode(key), score, member);
    }

    /**
     * 元素删除（按类型：hash→HDEL field，list→LREM value，set→SREM，zset→ZREM）
     */
    public void removeElement(Long instanceId, String key, String type, String element, Integer dbIndex) {
        checkWritable(instanceId);
        RedisClusterCommands<String, String> commands = commands(redisConnection(instanceId, dbIndex));
        String rawKey = KeyCodec.decode(key);
        switch (StrUtil.nullToDefault(type, "").toLowerCase()) {
            case "hash" -> commands.hdel(rawKey, element);
            case "list" -> commands.lrem(rawKey, 0, element);
            case "set" -> commands.srem(rawKey, element);
            case "zset" -> commands.zrem(rawKey, element);
            default -> throw MonitorErrorCode.MONITOR_REDIS_COMMAND_FORBIDDEN
                .exception("不支持删除元素的类型：" + type);
        }
    }

    /**
     * 设置过期时间（秒）
     */
    public void expire(Long instanceId, String key, long seconds, Integer dbIndex) {
        checkWritable(instanceId);
        commands(redisConnection(instanceId, dbIndex)).expire(KeyCodec.decode(key), seconds);
    }

    /**
     * 移除过期时间
     */
    public void persist(Long instanceId, String key, Integer dbIndex) {
        checkWritable(instanceId);
        commands(redisConnection(instanceId, dbIndex)).persist(KeyCodec.decode(key));
    }

    /**
     * 重命名 key
     */
    public void rename(Long instanceId, String oldKey, String newKey, Integer dbIndex) {
        checkWritable(instanceId);
        commands(redisConnection(instanceId, dbIndex)).rename(KeyCodec.decode(oldKey), KeyCodec.decode(newKey));
    }

    /**
     * 删除 key（UNLINK 异步删除，不阻塞）
     */
    public long delete(Long instanceId, List<String> keys, Integer dbIndex) {
        checkWritable(instanceId);
        if (CollUtil.isEmpty(keys)) {
            return 0L;
        }
        String[] rawKeys = keys.stream().map(KeyCodec::decode).toArray(String[]::new);
        Long deleted = commands(redisConnection(instanceId, dbIndex)).unlink(rawKeys);
        return deleted == null ? 0L : deleted;
    }

    /**
     * 批量删除（高危：必须携带有效 confirm-token；分批 batch-delete-size，批间 sleep 可配）
     *
     * @param pattern      匹配模式（与 keys 二选一；pattern 走 SCAN 收集后分批 UNLINK）
     * @param keys         显式 key 列表
     * @param confirmToken 高危操作确认令牌
     * @return 删除数量
     */
    public long batchDelete(Long instanceId, String pattern, List<String> keys, String confirmToken, Integer dbIndex) {
        MonitorInstanceDTO instance = checkWritable(instanceId);
        String operation = StrUtil.isNotBlank(pattern) ? "batch-delete:pattern=" + pattern
            : "batch-delete:keys=" + (keys == null ? 0 : keys.size());
        writeGuard.consumeConfirmToken(instanceId, operation, confirmToken);

        List<String> toDelete = new ArrayList<>();
        if (CollUtil.isNotEmpty(keys)) {
            toDelete.addAll(keys);
        } else if (StrUtil.isNotBlank(pattern)) {
            // pattern：SCAN 全量收集（分页推进，上限 10 万防失控）
            String cursor = "";
            boolean finished = false;
            while (!finished && toDelete.size() < 100_000) {
                KeyScanner.KeyScanResult result = scan(instanceId, cursor, pattern, null,
                    monitorProperties.getCollect().getScanCount(), dbIndex);
                toDelete.addAll(result.keys());
                cursor = result.nextCursor();
                finished = result.finished();
            }
        }
        if (toDelete.isEmpty()) {
            return 0L;
        }
        int batchSize = monitorProperties.getRedis().getBatchDeleteSize();
        long sleepMs = monitorProperties.getRedis().getBatchDeleteSleepMs();
        long deleted = 0L;
        for (int from = 0; from < toDelete.size(); from += batchSize) {
            List<String> batch = toDelete.subList(from, Math.min(from + batchSize, toDelete.size()));
            deleted += delete(instanceId, batch, dbIndex);
            if (from + batchSize < toDelete.size() && sleepMs > 0) {
                try {
                    Thread.sleep(sleepMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        log.info("批量删除完成, instanceId:{}, pattern:{}, deleted:{}", instanceId, pattern, deleted);
        return deleted;
    }

    /**
     * 审计前值快照（类型/TTL/值摘要，value 截断 2000 并脱敏）
     */
    public String snapshotForAudit(Long instanceId, String key, Integer dbIndex) {
        try {
            RedisManagedConnection connection = redisConnection(instanceId, dbIndex);
            RedisClusterCommands<String, String> commands = commands(connection);
            String rawKey = KeyCodec.decode(key);
            String type = commands.type(rawKey);
            if (StrUtil.isBlank(type) || "none".equals(type)) {
                return "{\"exists\":false}";
            }
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("exists", true);
            snapshot.put("type", type);
            snapshot.put("ttlSeconds", commands.ttl(rawKey));
            Object value = switch (type) {
                case "string" -> commands.get(rawKey);
                case "hash" -> commands.hgetall(rawKey);
                case "list" -> commands.lrange(rawKey, 0, 99);
                case "set" -> commands.smembers(rawKey);
                case "zset" -> commands.zrangeWithScores(rawKey, 0, 99);
                default -> null;
            };
            snapshot.put("value", StrUtil.maxLength(JsonUtils.toJson(value), 2000));
            return JsonUtils.toJson(snapshot);
        } catch (Exception e) {
            log.warn("审计前值快照失败, instanceId:{}, error:{}", instanceId, e.getMessage());
            return "{\"snapshotError\":\"" + StrUtil.nullToEmpty(e.getMessage()) + "\"}";
        }
    }

    // ==================== 内部工具 ====================

    private MonitorInstanceDTO checkWritable(Long instanceId) {
        MonitorInstanceDTO instance = instanceManager.getDtoById(instanceId);
        if (instance != null && instance.getInstanceType() == InstanceTypeEnum.MICROSERVICE) {
            throw MonitorErrorCode.MONITOR_INSTANCE_TYPE_UNSUPPORTED
                .exception("微服务实例不支持 Redis 数据管理");
        }
        writeGuard.checkManageEnabled(instance);
        return instance;
    }

    private RedisManagedConnection redisConnection(Long instanceId) {
        return redisConnection(instanceId, null);
    }

    /**
     * Redis 连接（可指定逻辑库；cluster 恒为 db0，忽略 dbIndex）
     */
    private RedisManagedConnection redisConnection(Long instanceId, Integer dbIndex) {
        ManagedConnection connection = connectionRegistry.getConnection(instanceId, dbIndex);
        if (!(connection instanceof RedisManagedConnection redisConnection)) {
            throw MonitorErrorCode.MONITOR_CONNECTION_UNAVAILABLE.exception("目标实例不是 Redis 类型或连接不可用");
        }
        return redisConnection;
    }

    /**
     * 取命令执行入口：standalone/sentinel 用单连接；cluster 用 cluster 连接（lettuce 按 CRC16 自动路由）
     * 返回公共父接口 RedisClusterCommands（RedisCommands 与 RedisAdvancedClusterCommands 均实现之）
     */
    private RedisClusterCommands<String, String> commands(RedisManagedConnection connection) {
        return connection.isCluster()
            ? connection.getClusterConnection().sync()
            : connection.getConnection().sync();
    }

    private ScanCursor subCursor(String subCursor) {
        return StrUtil.isBlank(subCursor) ? ScanCursor.INITIAL : ScanCursor.of(subCursor);
    }

    private long parseOffset(String subCursor) {
        if (StrUtil.isBlank(subCursor)) {
            return 0L;
        }
        try {
            return Long.parseLong(subCursor);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private Long safeMemoryUsage(RedisClusterCommands<String, String> commands, String key) {
        try {
            return commands.memoryUsage(key);
        } catch (Exception e) {
            // Redis < 4.0 无 MEMORY USAGE，返回 null（前端显示 '-'）
            return null;
        }
    }
}
