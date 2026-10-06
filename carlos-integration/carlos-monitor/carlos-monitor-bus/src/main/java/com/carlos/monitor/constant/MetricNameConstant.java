package com.carlos.monitor.constant;

/**
 * <p>
 * 指标名常量（metric_name，CK monitor_metric_point.metric_name / 告警规则引用）
 * 命名规范：{来源}.{域}.{指标}，与字典/告警规则共用
 * </p>
 *
 * @author Carlos
 * @date 2026年10月2日
 */
public interface MetricNameConstant {

    // ==================== 实例通用 ====================
    /**
     * 实例不可达（采集失败时产出，值恒为 1）
     */
    String INSTANCE_UNREACHABLE = "instance.unreachable";

    // ==================== Redis（source=REDIS） ====================
    String REDIS_MEMORY_USED = "redis.memory.used";

    String REDIS_MEMORY_USED_RSS = "redis.memory.used_rss";

    String REDIS_MEMORY_FRAGMENTATION_RATIO = "redis.memory.fragmentation_ratio";

    String REDIS_MEMORY_MAXMEMORY = "redis.memory.maxmemory";

    String REDIS_MEMORY_USED_PERCENT = "redis.memory.used_percent";

    String REDIS_STATS_OPS_PER_SEC = "redis.stats.ops_per_sec";

    String REDIS_STATS_EXPIRED_KEYS = "redis.stats.expired_keys";

    String REDIS_STATS_EVICTED_KEYS = "redis.stats.evicted_keys";

    String REDIS_STATS_KEYSPACE_HITS = "redis.stats.keyspace_hits";

    String REDIS_STATS_KEYSPACE_MISSES = "redis.stats.keyspace_misses";

    String REDIS_STATS_HIT_RATE = "redis.stats.hit_rate";

    String REDIS_CLIENTS_CONNECTED = "redis.clients.connected";

    String REDIS_CLIENTS_BLOCKED = "redis.clients.blocked";

    String REDIS_PERSISTENCE_RDB_LAST_SAVE_TIME = "redis.persistence.rdb_last_save_time";

    String REDIS_PERSISTENCE_RDB_CHANGES_SINCE_LAST_SAVE = "redis.persistence.rdb_changes_since_last_save";

    String REDIS_PERSISTENCE_AOF_ENABLED = "redis.persistence.aof_enabled";

    String REDIS_REPLICATION_CONNECTED_SLAVES = "redis.replication.connected_slaves";

    String REDIS_REPLICATION_MASTER_LINK_STATUS = "redis.replication.master_link_status";

    String REDIS_REPLICATION_ROLE = "redis.replication.role";

    /**
     * keyspace 各 db 的 key 数量（tags={db:dbN}）
     */
    String REDIS_KEYSPACE_KEYS = "redis.keyspace.keys";

    /**
     * keyspace 各 db 的带过期 key 数量（tags={db:dbN}）
     */
    String REDIS_KEYSPACE_EXPIRES = "redis.keyspace.expires";

    /**
     * keyspace 各 db 的平均 TTL（tags={db:dbN}）
     */
    String REDIS_KEYSPACE_AVG_TTL = "redis.keyspace.avg_ttl";

    String REDIS_CLUSTER_STATE = "redis.cluster.state";

    String REDIS_CLUSTER_SLOTS_ASSIGNED = "redis.cluster.slots_assigned";

    // ==================== 数据库（source=DB） ====================
    String DB_POOL_ACTIVE_COUNT = "db.pool.active_count";

    String DB_POOL_POOLING_COUNT = "db.pool.pooling_count";

    String DB_POOL_WAIT_THREAD_COUNT = "db.pool.wait_thread_count";

    String DB_POOL_MAX_ACTIVE = "db.pool.max_active";

    String DB_CONNECTIONS_ACTIVE = "db.connections.active";

    String DB_XACT_COMMIT = "db.xact.commit";

    String DB_XACT_ROLLBACK = "db.xact.rollback";

    String DB_TUP_RETURNED = "db.tup.returned";

    String DB_TUP_FETCHED = "db.tup.fetched";

    String DB_TUP_INSERTED = "db.tup.inserted";

    String DB_TUP_UPDATED = "db.tup.updated";

    String DB_TUP_DELETED = "db.tup.deleted";

    String DB_DEADLOCKS = "db.deadlocks";

    String DB_QUESTIONS = "db.questions";

    String DB_SLOW_QUERIES = "db.slow_queries";

    String DB_THREADS_CONNECTED = "db.threads.connected";

    String DB_THREADS_RUNNING = "db.threads.running";

    /**
     * 会话数（tags={state:active/idle/...}）
     */
    String DB_SESSIONS = "db.sessions";

    // ==================== 微服务（source=SERVICE） ====================
    String SERVICE_HEALTH = "service.health";

    String SERVICE_INSTANCE_COUNT = "service.instance.count";

    String SERVICE_INSTANCE_HEALTHY_COUNT = "service.instance.healthy.count";

    String SERVICE_JVM_MEMORY_USED = "service.jvm.memory.used";

    String SERVICE_JVM_MEMORY_MAX = "service.jvm.memory.max";

    String SERVICE_JVM_THREADS_LIVE = "service.jvm.threads.live";

    String SERVICE_JVM_GC_PAUSE = "service.jvm.gc.pause";

    String SERVICE_PROCESS_CPU_USAGE = "service.process.cpu.usage";

    String SERVICE_HTTP_REQUESTS_COUNT = "service.http.requests.count";

    String SERVICE_HTTP_ERROR_RATE = "service.http.error_rate";

    String SERVICE_TOMCAT_THREADS_CURRENT = "service.tomcat.threads.current";

    /**
     * 服务实例 UP（告警模板引用，等价 SERVICE_HEALTH 服务级最小值）
     */
    String SERVICE_INSTANCE_UP = "service.instance.up";
}
