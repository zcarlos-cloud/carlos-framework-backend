package com.carlos.monitor.redis.controller;

import cn.hutool.core.util.StrUtil;
import com.carlos.core.response.Result;
import com.carlos.monitor.audit.MonitorAudit;
import com.carlos.monitor.config.MonitorProperties;
import com.carlos.monitor.enums.MonitorErrorCode;
import com.carlos.monitor.redis.pojo.param.BatchDeleteParam;
import com.carlos.monitor.redis.pojo.param.ConfirmTokenParam;
import com.carlos.monitor.redis.pojo.param.KeyCreateParam;
import com.carlos.monitor.redis.pojo.param.KeyUpdateParam;
import com.carlos.monitor.redis.pojo.vo.ConfirmTokenVO;
import com.carlos.monitor.redis.pojo.vo.KeyDetailVO;
import com.carlos.monitor.redis.pojo.vo.KeyScanVO;
import com.carlos.monitor.redis.pojo.vo.KeyTreeNodeVO;
import com.carlos.monitor.redis.pojo.vo.KeyTreeVO;
import com.carlos.monitor.redis.pojo.vo.RedisDatabaseVO;
import com.carlos.monitor.redis.proxy.KeyCodec;
import com.carlos.monitor.redis.proxy.KeyScanner;
import com.carlos.monitor.redis.proxy.RedisDataProxy;
import com.carlos.monitor.redis.proxy.RedisWriteGuard;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * Redis 数据管理 rest服务接口
 * 写接口全部带 @MonitorAudit 审计埋点；高危批量删除必须携带 X-Confirm-Token
 * </p>
 *
 * @author Carlos
 * @date 2026年10月3日
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/monitor/redis")
@Tag(name = "Redis数据管理")
public class RedisDataController {

    private final RedisDataProxy redisDataProxy;

    private final RedisWriteGuard writeGuard;

    private final MonitorProperties monitorProperties;

    @PreAuthorize("hasPermission('monitor:redis:data', 'view')")
    @GetMapping("/{id}/keys")
    @Operation(summary = "SCAN 分页浏览 key", description = "返回 nextCursor/scannedCount；单次最多推进 K 批（K 可配）")
    public Result<KeyScanVO> keys(@PathVariable("id") Long id,
                                  @RequestParam(value = "cursor", required = false) String cursor,
                                  @RequestParam(value = "pattern", required = false) String pattern,
                                  @RequestParam(value = "type", required = false) String type,
                                  @RequestParam(value = "count", required = false) Integer count,
                                  @RequestParam(value = "db", required = false) Integer db) {
        KeyScanner.KeyScanResult result = redisDataProxy.scan(id, cursor, pattern, type, count, db);
        KeyScanVO vo = new KeyScanVO();
        vo.setKeys(result.keys().stream().map(KeyCodec::encode).toList());
        vo.setNextCursor(result.nextCursor());
        vo.setScannedCount(result.scannedCount());
        vo.setFinished(result.finished());
        return Result.success(vo);
    }

    @PreAuthorize("hasPermission('monitor:redis:data', 'view')")
    @GetMapping("/{id}/key-tree")
    @Operation(summary = "key 树形分级浏览", description = "按分隔符聚合一层（参考 RedisInsight Browser）；"
        + "folder 计数在扫描未完整时为近似值（approximate=true）")
    public Result<KeyTreeVO> keyTree(@PathVariable("id") Long id,
                                     @RequestParam(value = "prefix", required = false) String prefix,
                                     @RequestParam(value = "separator", required = false) String separator,
                                     @RequestParam(value = "pattern", required = false) String pattern,
                                     @RequestParam(value = "type", required = false) String type,
                                     @RequestParam(value = "cursor", required = false) String cursor,
                                     @RequestParam(value = "count", required = false) Integer count,
                                     @RequestParam(value = "db", required = false) Integer db) {
        RedisDataProxy.KeyTreeResult result =
            redisDataProxy.keyTree(id, prefix, separator, pattern, type, cursor, count, db);
        KeyTreeVO vo = new KeyTreeVO();
        vo.setNodes(result.nodes().stream().map(node -> {
            KeyTreeNodeVO item = new KeyTreeNodeVO();
            item.setName(node.name());
            item.setFullKey(node.fullKey());
            item.setType(node.type());
            item.setLeaf(node.leaf());
            item.setKeyCount(node.keyCount());
            return item;
        }).toList());
        vo.setNextCursor(result.nextCursor());
        vo.setScannedCount(result.scannedCount());
        vo.setFinished(result.finished());
        vo.setApproximate(!result.finished());
        return Result.success(vo);
    }

    @PreAuthorize("hasPermission('monitor:redis:data', 'view')")
    @GetMapping("/{id}/key")
    @Operation(summary = "key 详情", description = "类型/TTL/内存/值，值按类型分页（subCursor/subCount），敏感字段脱敏")
    public Result<KeyDetailVO> keyDetail(@PathVariable("id") Long id,
                                         @RequestParam("key") String key,
                                         @RequestParam(value = "subCursor", required = false) String subCursor,
                                         @RequestParam(value = "subCount", required = false) Integer subCount,
                                         @RequestParam(value = "db", required = false) Integer db) {
        RedisDataProxy.KeyDetail detail = redisDataProxy.keyDetail(id, key, subCursor, subCount, db);
        KeyDetailVO vo = new KeyDetailVO();
        vo.setKey(detail.key());
        vo.setType(detail.type());
        vo.setTtlSeconds(detail.ttlSeconds());
        vo.setMemoryBytes(detail.memoryBytes());
        vo.setElementCount(detail.elementCount());
        vo.setValue(detail.value());
        vo.setSubCursor(detail.subCursor());
        vo.setSubFinished(detail.subFinished());
        return Result.success(vo);
    }

    @PreAuthorize("hasPermission('monitor:redis:data', 'view')")
    @GetMapping("/{id}/databases")
    @Operation(summary = "Redis 逻辑库列表", description = "库总数取 CONFIG GET databases（无权限回退 16），"
        + "各库键数取 INFO KEYSPACE；cluster 恒返回 db0")
    public Result<List<RedisDatabaseVO>> databases(@PathVariable("id") Long id) {
        List<RedisDatabaseVO> list = redisDataProxy.databases(id).stream()
            .map(item -> {
                RedisDatabaseVO vo = new RedisDatabaseVO();
                vo.setIndex((Integer) item.get("index"));
                vo.setKeyCount((Long) item.get("keyCount"));
                vo.setExpires((Long) item.get("expires"));
                vo.setAvgTtl((Long) item.get("avgTtl"));
                vo.setTotalDbs((Integer) item.get("totalDbs"));
                return vo;
            })
            .toList();
        return Result.success(list);
    }

    @MonitorAudit(action = "REDIS_DATA_CREATE", target = "#id + ':' + #param.key", snapshot = false)
    @PreAuthorize("hasPermission('monitor:redis:data', 'edit')")
    @PostMapping("/{id}/key")
    @Operation(summary = "新增 key", description = "按类型创建（string/hash/list/set/zset）")
    public Result<Boolean> createKey(@PathVariable("id") Long id,
                                     @RequestBody @Validated KeyCreateParam param,
                                     @RequestParam(value = "db", required = false) Integer db) {
        switch (param.getType().toLowerCase()) {
            case "string" ->
                redisDataProxy.stringSet(id, param.getKey(), param.getValue(), param.getTtlSeconds(), db);
            case "hash" -> {
                requireField(param.getField(), "hash 字段名");
                redisDataProxy.hashPut(id, param.getKey(), param.getField(), param.getValue(), db);
            }
            case "list" -> redisDataProxy.listPush(id, param.getKey(), param.getValue(), false, db);
            case "set" -> redisDataProxy.setAdd(id, param.getKey(), param.getValue(), db);
            case "zset" -> {
                if (param.getScore() == null) {
                    throw MonitorErrorCode.MONITOR_REDIS_COMMAND_FORBIDDEN.exception("zset 分数不能为空");
                }
                redisDataProxy.zsetAdd(id, param.getKey(), param.getScore(), param.getValue(), db);
            }
            default -> throw MonitorErrorCode.MONITOR_REDIS_COMMAND_FORBIDDEN
                .exception("不支持的 key 类型：" + param.getType());
        }
        return Result.success(true);
    }

    @MonitorAudit(action = "REDIS_DATA_UPDATE", target = "#id + ':' + #param.key", snapshot = true)
    @PreAuthorize("hasPermission('monitor:redis:data', 'edit')")
    @PutMapping("/{id}/key")
    @Operation(summary = "修改 key", description = "修改值/元素/TTL（operation 区分操作）")
    public Result<Boolean> updateKey(@PathVariable("id") Long id,
                                     @RequestBody @Validated KeyUpdateParam param,
                                     @RequestParam(value = "db", required = false) Integer db) {
        switch (param.getOperation().toLowerCase()) {
            case "set-value" ->
                redisDataProxy.stringSet(id, param.getKey(), param.getValue(), param.getTtlSeconds(), db);
            case "hash-put" -> {
                requireField(param.getField(), "hash 字段名");
                redisDataProxy.hashPut(id, param.getKey(), param.getField(), param.getValue(), db);
            }
            case "add-element" -> addElement(id, param, db);
            case "remove-element" -> {
                requireField(param.getElement(), "待删除元素");
                redisDataProxy.removeElement(id, param.getKey(), param.getType(), param.getElement(), db);
            }
            case "expire" -> {
                if (param.getTtlSeconds() == null || param.getTtlSeconds() <= 0) {
                    throw MonitorErrorCode.MONITOR_REDIS_COMMAND_FORBIDDEN.exception("过期时间必须大于 0 秒");
                }
                redisDataProxy.expire(id, param.getKey(), param.getTtlSeconds(), db);
            }
            case "persist" -> redisDataProxy.persist(id, param.getKey(), db);
            case "rename" -> {
                requireField(param.getNewKey(), "新 key 名");
                redisDataProxy.rename(id, param.getKey(), param.getNewKey(), db);
            }
            default -> throw MonitorErrorCode.MONITOR_REDIS_COMMAND_FORBIDDEN
                .exception("不支持的操作：" + param.getOperation());
        }
        return Result.success(true);
    }

    @MonitorAudit(action = "REDIS_DATA_DELETE", target = "#id + ':' + #key", snapshot = true)
    @PreAuthorize("hasPermission('monitor:redis:data', 'delete')")
    @DeleteMapping("/{id}/key")
    @Operation(summary = "删除 key", description = "UNLINK 异步删除")
    public Result<Long> deleteKey(@PathVariable("id") Long id, @RequestParam("key") String key,
                                  @RequestParam(value = "db", required = false) Integer db) {
        return Result.success(redisDataProxy.delete(id, List.of(key), db));
    }

    @MonitorAudit(action = "REDIS_DATA_BATCH_DELETE", target = "#id + ':' + #param.pattern", snapshot = false, riskLevel = 80)
    @PreAuthorize("hasPermission('monitor:redis:data', 'batch-delete')")
    @PostMapping("/{id}/keys/batch-delete")
    @Operation(summary = "批量删除 key", description = "高危：必须携带 X-Confirm-Token；分批 500/批，批间 sleep 可配")
    public Result<Long> batchDelete(@PathVariable("id") Long id,
                                    @RequestBody @Validated BatchDeleteParam param,
                                    @RequestHeader(value = "X-Confirm-Token", required = false) String confirmToken,
                                    @RequestParam(value = "db", required = false) Integer db) {
        return Result.success(redisDataProxy.batchDelete(id, param.getPattern(), param.getKeys(), confirmToken, db));
    }

    @PreAuthorize("hasPermission('monitor:redis:data', 'edit')")
    @PostMapping("/{id}/confirm-token")
    @Operation(summary = "签发高危操作确认令牌", description = "入参操作摘要，60s 一次性，绑定实例与操作")
    public Result<ConfirmTokenVO> confirmToken(@PathVariable("id") Long id,
                                               @RequestBody @Validated ConfirmTokenParam param) {
        String token = writeGuard.issueConfirmToken(id, param.getOperation());
        ConfirmTokenVO vo = new ConfirmTokenVO();
        vo.setToken(token);
        vo.setTtlSeconds(monitorProperties.getRedis().getConfirmTokenTtlSec());
        return Result.success(vo);
    }

    private void addElement(Long id, KeyUpdateParam param, Integer db) {
        String type = StrUtil.nullToDefault(param.getType(), "").toLowerCase();
        switch (type) {
            case "list" -> redisDataProxy.listPush(id, param.getKey(), param.getValue(), false, db);
            case "set" -> redisDataProxy.setAdd(id, param.getKey(), param.getValue(), db);
            case "zset" -> {
                if (param.getScore() == null) {
                    throw MonitorErrorCode.MONITOR_REDIS_COMMAND_FORBIDDEN.exception("zset 分数不能为空");
                }
                redisDataProxy.zsetAdd(id, param.getKey(), param.getScore(), param.getValue(), db);
            }
            case "hash" -> {
                requireField(param.getField(), "hash 字段名");
                redisDataProxy.hashPut(id, param.getKey(), param.getField(), param.getValue(), db);
            }
            default -> throw MonitorErrorCode.MONITOR_REDIS_COMMAND_FORBIDDEN
                .exception("不支持添加元素的类型：" + param.getType());
        }
    }

    private void requireField(String value, String fieldName) {
        if (StrUtil.isBlank(value)) {
            throw MonitorErrorCode.MONITOR_REDIS_COMMAND_FORBIDDEN.exception(fieldName + "不能为空");
        }
    }
}
