package com.carlos.monitor.instance.connection;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * Redis 受管连接
 * standalone/sentinel 持有 {@link StatefulRedisConnection}，
 * cluster 持有 {@link StatefulRedisClusterConnection}
 * </p>
 *
 * @author Carlos
 * @date 2026年10月1日
 */
@Slf4j
@Getter
public final class RedisManagedConnection implements ManagedConnection {

    private final Long instanceId;

    private final RedisClient redisClient;

    private final RedisClusterClient redisClusterClient;

    private final StatefulRedisConnection<String, String> connection;

    private final StatefulRedisClusterConnection<String, String> clusterConnection;

    public RedisManagedConnection(Long instanceId, RedisClient redisClient,
                                  StatefulRedisConnection<String, String> connection) {
        this.instanceId = instanceId;
        this.redisClient = redisClient;
        this.connection = connection;
        this.redisClusterClient = null;
        this.clusterConnection = null;
    }

    public RedisManagedConnection(Long instanceId, RedisClusterClient redisClusterClient,
                                  StatefulRedisClusterConnection<String, String> clusterConnection) {
        this.instanceId = instanceId;
        this.redisClusterClient = redisClusterClient;
        this.clusterConnection = clusterConnection;
        this.redisClient = null;
        this.connection = null;
    }

    /**
     * 是否 cluster 模式连接
     *
     * @return true=cluster
     */
    public boolean isCluster() {
        return clusterConnection != null;
    }

    @Override
    public void close() {
        try {
            if (connection != null) {
                connection.close();
            }
            if (clusterConnection != null) {
                clusterConnection.close();
            }
            if (redisClient != null) {
                redisClient.shutdown();
            }
            if (redisClusterClient != null) {
                redisClusterClient.shutdown();
            }
        } catch (Exception e) {
            log.warn("Close redis managed connection fail, instanceId:{}, error:{}", instanceId, e.getMessage());
        }
    }
}
