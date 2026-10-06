package com.carlos.monitor.instance.connection;

import com.carlos.monitor.config.MonitorProperties;
import com.carlos.core.exception.BusinessException;
import com.carlos.monitor.enums.MonitorErrorCode;
import com.carlos.monitor.instance.manager.MonitorInstanceManager;
import com.carlos.monitor.instance.pojo.dto.MonitorInstanceDTO;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 实例连接池注册中心
 * Guava LoadingCache&lt;String, ManagedConnection&gt;（key = instanceId 或 instanceId@dbIndex，
 * 数据管理按需选库时独立连接），容量与过期时间取 carlos.monitor.connection.*；
 * 实例编辑（连接配置变更）/删除时监听 {@link InstanceConnectionInvalidEvent} 显式失效并关闭连接
 * </p>
 *
 * @author Carlos
 * @date 2026年10月1日
 */
@Slf4j
@Component
public class InstanceConnectionRegistry {

    private final MonitorInstanceManager instanceManager;

    private final CredentialCipher credentialCipher;

    private final ManagedConnectionFactory connectionFactory;

    private final LoadingCache<String, ManagedConnection> cache;

    public InstanceConnectionRegistry(MonitorInstanceManager instanceManager,
                                      CredentialCipher credentialCipher,
                                      ManagedConnectionFactory connectionFactory,
                                      MonitorProperties properties) {
        this.instanceManager = instanceManager;
        this.credentialCipher = credentialCipher;
        this.connectionFactory = connectionFactory;
        MonitorProperties.Connection connectionProps = properties.getConnection();
        RemovalListener<String, ManagedConnection> removalListener = notification -> {
            ManagedConnection connection = notification.getValue();
            if (connection != null) {
                log.info("Close managed connection, instanceId:{}, cause:{}", notification.getKey(), notification.getCause());
                connection.close();
            }
        };
        this.cache = CacheBuilder.newBuilder()
            .maximumSize(connectionProps.getRegistryMaxSize())
            .expireAfterAccess(connectionProps.getExpireAfterAccessMin(), TimeUnit.MINUTES)
            .removalListener(removalListener)
            .build(CacheLoader.from(this::loadConnection));
    }

    /**
     * 获取实例连接（实例注册默认库，惰性创建，30min 未访问驱逐）
     *
     * @param instanceId 实例ID
     * @return 受管连接
     */
    public ManagedConnection getConnection(Long instanceId) {
        return getConnection(instanceId, null);
    }

    /**
     * 获取实例连接（可指定 Redis 逻辑库，惰性创建，30min 未访问驱逐）
     *
     * @param instanceId 实例ID
     * @param dbIndex    Redis 逻辑库（null 用实例注册默认；cluster 忽略）
     * @return 受管连接
     */
    public ManagedConnection getConnection(Long instanceId, Integer dbIndex) {
        try {
            return cache.get(cacheKey(instanceId, dbIndex));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof BusinessException businessException) {
                throw businessException;
            }
            log.warn("Get managed connection fail, instanceId:{}, db:{}, error:{}", instanceId, dbIndex,
                cause == null ? e.getMessage() : cause.getMessage());
            throw MonitorErrorCode.MONITOR_CONNECTION_UNAVAILABLE
                .exception("实例连接获取失败：" + (cause == null ? e.getMessage() : cause.getMessage()));
        }
    }

    /**
     * 显式失效实例连接（并关闭，含该实例所有选库连接）
     *
     * @param instanceId 实例ID
     */
    public void invalidate(Long instanceId) {
        if (instanceId != null) {
            String prefix = instanceId + "@";
            cache.asMap().keySet().removeIf(key -> key.equals(String.valueOf(instanceId)) || key.startsWith(prefix));
            cache.cleanUp();
        }
    }

    /**
     * 监听实例变更/删除事件：失效并关闭连接
     *
     * @param event 连接失效事件
     */
    @EventListener
    public void onInstanceConnectionInvalid(InstanceConnectionInvalidEvent event) {
        log.info("Invalidate managed connection, instanceId:{}, reason:{}", event.getInstanceId(), event.getReason());
        invalidate(event.getInstanceId());
    }

    @PreDestroy
    public void destroy() {
        cache.invalidateAll();
    }

    private ManagedConnection loadConnection(String key) {
        Long instanceId = parseInstanceId(key);
        Integer dbIndex = parseDbIndex(key);
        MonitorInstanceDTO dto = instanceManager.getDtoById(instanceId);
        if (dto == null) {
            throw MonitorErrorCode.MONITOR_INSTANCE_NOT_FOUND.exception();
        }
        String plainPassword = credentialCipher.decrypt(dto.getPasswordCipher());
        return connectionFactory.create(dto, plainPassword, dbIndex);
    }

    private String cacheKey(Long instanceId, Integer dbIndex) {
        return dbIndex == null ? String.valueOf(instanceId) : instanceId + "@" + dbIndex;
    }

    private Long parseInstanceId(String key) {
        int idx = key.indexOf('@');
        return Long.valueOf(idx < 0 ? key : key.substring(0, idx));
    }

    private Integer parseDbIndex(String key) {
        int idx = key.indexOf('@');
        return idx < 0 ? null : Integer.valueOf(key.substring(idx + 1));
    }
}
