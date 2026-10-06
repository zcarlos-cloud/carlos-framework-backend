package com.carlos.monitor.instance.connection;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * <p>
 * 实例连接失效事件
 * 实例连接配置变更（addresses/username/passwordCipher/dbName/dbIndex/extConfig 任一变化）
 * 或实例删除时发布，由 InstanceConnectionRegistry 监听并关闭、剔除缓存连接
 * </p>
 *
 * @author Carlos
 * @date 2026年10月1日
 */
@Getter
public class InstanceConnectionInvalidEvent extends ApplicationEvent {

    private final Long instanceId;

    private final String reason;

    public InstanceConnectionInvalidEvent(Object source, Long instanceId, String reason) {
        super(source);
        this.instanceId = instanceId;
        this.reason = reason;
    }
}
