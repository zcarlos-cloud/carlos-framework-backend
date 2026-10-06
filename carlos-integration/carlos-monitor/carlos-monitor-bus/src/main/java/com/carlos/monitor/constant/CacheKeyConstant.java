package com.carlos.monitor.constant;

/**
 * <p>
 * 监控模块缓存键常量
 * 键规范：{模块}:{业务}:{标识}，模块前缀 monitor
 * </p>
 *
 * @author Carlos
 * @date 2026年10月1日
 */
public interface CacheKeyConstant {

    /**
     * 实例详情缓存键前缀：monitor:instance:{id}
     */
    String INSTANCE_PREFIX = "monitor:instance:";

    /**
     * 实例分页缓存键前缀：monitor:instance:page:{版本号}:{参数哈希}
     */
    String INSTANCE_PAGE_PREFIX = "monitor:instance:page:";

    /**
     * 实例分页缓存版本键：写操作递增，使全部历史分页键一次性失效
     */
    String INSTANCE_PAGE_VERSION = "monitor:instance:page:ver";

    /**
     * 实例详情缓存键
     *
     * @param id 实例ID
     * @return 缓存键
     */
    static String instanceKey(Object id) {
        return INSTANCE_PREFIX + id;
    }

    /**
     * 实例分页缓存键
     *
     * @param version   缓存版本号
     * @param paramHash 分页参数哈希
     * @return 缓存键
     */
    static String instancePageKey(long version, int paramHash) {
        return INSTANCE_PAGE_PREFIX + version + ":" + paramHash;
    }

    /**
     * 实例分页缓存失效模式（deletePattern 用）
     *
     * @return 匹配模式
     */
    static String instancePagePattern() {
        return INSTANCE_PAGE_PREFIX + "*";
    }

    /**
     * 实例当前指标快照键前缀：monitor:metric:current:{instanceId}（Hash，采集成功时由 sink 侧回写，TTL 5min）
     */
    String METRIC_CURRENT_PREFIX = "monitor:metric:current:";

    /**
     * Redis 慢日志采集游标键前缀：monitor:collect:slowlog-cursor:{instanceId}:{node}
     */
    String SLOWLOG_CURSOR_PREFIX = "monitor:collect:slowlog-cursor:";

    /**
     * 实例当前指标快照键
     *
     * @param instanceId 实例ID
     * @return 缓存键
     */
    static String metricCurrentKey(Object instanceId) {
        return METRIC_CURRENT_PREFIX + instanceId;
    }

    /**
     * Redis 慢日志采集游标键
     *
     * @param instanceId 实例ID
     * @param node       节点地址（单机为空串）
     * @return 缓存键
     */
    static String slowlogCursorKey(Object instanceId, String node) {
        return SLOWLOG_CURSOR_PREFIX + instanceId + ":" + (node == null ? "" : node);
    }
}
