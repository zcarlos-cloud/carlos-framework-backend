package com.carlos.redis.lock;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Redis分布式锁配置属性
 *
 * @author carlos
 * @date 2020/10/25 23:45
 */
@Data
@ConfigurationProperties(prefix = "carlos.redis.lock")
public class RedisLockProperties {

    private static final String DEFAULT_LOCK_PREFIX = "framework:lock:";

    /**
     * 是否启用分布式锁，默认启用
     */
    private Boolean enabled = false;

    /**
     * 分布式锁缓存前缀
     */
    private String prefix = DEFAULT_LOCK_PREFIX;
}
