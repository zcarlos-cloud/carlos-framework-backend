package com.carlos.monitor.collect.redis;

import cn.hutool.core.util.StrUtil;
import com.carlos.monitor.collect.spi.CollectorContext;
import com.carlos.monitor.collect.spi.SlowlogRow;
import com.carlos.monitor.constant.CacheKeyConstant;
import com.carlos.monitor.enums.InstanceTypeEnum;
import com.carlos.monitor.instance.connection.RedisManagedConnection;
import com.carlos.monitor.util.MonitorCacheUtil;
import com.carlos.redis.util.RedisUtil;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.models.role.RedisInstance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * Redis 慢日志采集器
 * SLOWLOG GET 128，命令参数脱敏（保留命令名与 key 位置参数，value→***），
 * 按 slowlog_id 增量采集（上次 id 存 Redis 键 monitor:collect:slowlog-cursor:{instanceId}:{node}）
 * </p>
 *
 * @author Carlos
 * @date 2026年10月2日
 */
@Slf4j
@Component
public class RedisSlowlogCollector {

    /**
     * 单次拉取的慢日志条数
     */
    private static final int SLOWLOG_FETCH_COUNT = 128;

    /**
     * 采集游标 TTL（秒）：30 天
     */
    private static final long CURSOR_TTL_SECONDS = 30L * 24 * 3600;

    /**
     * 命令参数全保留的白名单（无 value 泄露风险）
     */
    private static final Set<String> KEEP_ALL_ARGS_COMMANDS = Set.of(
        "GET", "DEL", "UNLINK", "TYPE", "TTL", "PTTL", "EXISTS", "EXPIRE", "PEXPIRE",
        "PERSIST", "HLEN", "LLEN", "SCARD", "ZCARD", "STRLEN", "HGETALL", "KEYS", "SCAN");

    /**
     * 支持 Redis 三形态
     */
    public boolean support(InstanceTypeEnum instanceType) {
        return instanceType == InstanceTypeEnum.REDIS_STANDALONE
            || instanceType == InstanceTypeEnum.REDIS_CLUSTER
            || instanceType == InstanceTypeEnum.REDIS_SENTINEL;
    }

    /**
     * 采集慢日志（增量）
     *
     * @param ctx 采集上下文
     * @return 慢日志行（失败返回空列表）
     */
    public List<SlowlogRow> collectSlowlogs(CollectorContext ctx) {
        if (!(ctx.connection() instanceof RedisManagedConnection redisConnection)) {
            return List.of();
        }
        Long instanceId = ctx.instanceId();
        List<SlowlogRow> rows = new ArrayList<>();
        if (redisConnection.isCluster()) {
            // cluster：逐 master 节点采集
            for (RedisClusterNode node : redisConnection.getClusterConnection().getPartitions()) {
                if (node.getRole() != RedisInstance.Role.MASTER) {
                    continue;
                }
                String nodeAddress = node.getUri().getHost() + ":" + node.getUri().getPort();
                try (StatefulRedisConnection<String, String> nodeConnection =
                         redisConnection.getClusterConnection().getConnection(node.getNodeId())) {
                    List<Object> slowlogs = nodeConnection.sync().slowlogGet(SLOWLOG_FETCH_COUNT);
                    rows.addAll(parseSlowlogs(instanceId, nodeAddress, slowlogs));
                } catch (Exception e) {
                    log.warn("采集 cluster 节点慢日志失败, instanceId:{}, node:{}, error:{}",
                        instanceId, nodeAddress, e.getMessage());
                }
            }
        } else {
            List<Object> slowlogs = redisConnection.getConnection().sync().slowlogGet(SLOWLOG_FETCH_COUNT);
            rows.addAll(parseSlowlogs(instanceId, "", slowlogs));
        }
        return rows;
    }

    /**
     * 解析慢日志原始结构并按游标增量过滤
     * 原始结构：[slowlogId, timestamp(秒), durationUs, [args...], clientAddr, clientName]
     */
    @SuppressWarnings("unchecked")
    private List<SlowlogRow> parseSlowlogs(Long instanceId, String node, List<Object> slowlogs) {
        List<SlowlogRow> rows = new ArrayList<>();
        if (slowlogs == null || slowlogs.isEmpty()) {
            return rows;
        }
        String cursorKey = CacheKeyConstant.slowlogCursorKey(instanceId, node);
        Long lastId = MonitorCacheUtil.getLong(cursorKey);
        long lastSlowlogId = lastId == null ? -1L : lastId;
        long maxId = lastSlowlogId;
        for (Object entry : slowlogs) {
            if (!(entry instanceof List)) {
                continue;
            }
            List<Object> fields = (List<Object>) entry;
            if (fields.size() < 4) {
                continue;
            }
            long slowlogId = toLong(fields.get(0));
            if (slowlogId <= lastSlowlogId) {
                continue;
            }
            maxId = Math.max(maxId, slowlogId);
            long epochSecond = toLong(fields.get(1));
            long durationUs = toLong(fields.get(2));
            String command = maskCommand(fields.get(3));
            String clientAddr = fields.size() > 4 ? toText(fields.get(4)) : "";
            rows.add(new SlowlogRow(instanceId, node, slowlogId,
                Instant.ofEpochSecond(epochSecond), durationUs, command, clientAddr));
        }
        if (maxId > lastSlowlogId) {
            RedisUtil.setValue(cursorKey, maxId, CURSOR_TTL_SECONDS);
        }
        return rows;
    }

    /**
     * 命令脱敏：保留命令名与 key 位置参数，value 替换为 ***
     * 规则：白名单命令全保留；其余命令保留 command + 第一个 key 参数，之后全部脱敏
     */
    private String maskCommand(Object argsObj) {
        if (!(argsObj instanceof List)) {
            return toText(argsObj);
        }
        List<?> args = (List<?>) argsObj;
        if (args.isEmpty()) {
            return "";
        }
        String commandName = toText(args.getFirst()).toUpperCase();
        if (KEEP_ALL_ARGS_COMMANDS.contains(commandName)) {
            return joinArgs(args);
        }
        List<String> masked = new ArrayList<>(args.size());
        for (int i = 0; i < args.size(); i++) {
            // 0=命令名，1=key 位置参数，其余脱敏
            masked.add(i <= 1 ? toText(args.get(i)) : "***");
        }
        return StrUtil.join(" ", masked);
    }

    private String joinArgs(List<?> args) {
        List<String> texts = new ArrayList<>(args.size());
        for (Object arg : args) {
            texts.add(toText(arg));
        }
        return StrUtil.join(" ", texts);
    }

    private long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(toText(value));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private String toText(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof byte[] bytes) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        return String.valueOf(value);
    }
}
