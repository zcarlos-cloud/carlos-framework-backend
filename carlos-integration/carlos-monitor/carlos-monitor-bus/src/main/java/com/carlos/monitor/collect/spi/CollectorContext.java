package com.carlos.monitor.collect.spi;

import com.carlos.monitor.config.MonitorProperties;
import com.carlos.monitor.instance.connection.ManagedConnection;
import com.carlos.monitor.instance.pojo.dto.MonitorInstanceDTO;

/**
 * <p>
 * 采集上下文：携带实例 DTO、受管连接与模块配置
 * </p>
 *
 * @author Carlos
 * @date 2026年10月2日
 */
public record CollectorContext(MonitorInstanceDTO instance, ManagedConnection connection,
                               MonitorProperties properties) {

    /**
     * 实例ID
     */
    public Long instanceId() {
        return instance.getId();
    }
}
