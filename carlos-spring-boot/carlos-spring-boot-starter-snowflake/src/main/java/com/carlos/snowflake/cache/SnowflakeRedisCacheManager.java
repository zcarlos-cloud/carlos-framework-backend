package com.carlos.snowflake.cache;

import cn.hutool.core.util.StrUtil;
import com.carlos.redis.util.RedisUtil;
import com.carlos.snowflake.SnowflakeInfo;
import com.carlos.snowflake.SnowflakeProperties;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * <p>
 * 缓存管理
 * 缓存设置定时过期，防止应用异常宕机，从而缓存一直存在的问题，所以需要对缓存不断续期
 * </p>
 *
 * @author Carlos
 * @date 2025-03-17 13:54
 */
@Slf4j
public class SnowflakeRedisCacheManager implements SnowflakeCacheManager {

    private final SnowflakeProperties properties;

    private volatile String snowflakeRedisKey;

    private volatile ScheduledExecutorService executorService;

    private final AtomicBoolean scheduled = new AtomicBoolean(false);

    public SnowflakeRedisCacheManager(SnowflakeProperties properties) {
        this.properties = properties;
        log.info("SnowflakeRedisCacheManager 初始化完成");
    }

    @Override
    public boolean putCache(SnowflakeInfo bean) {
        // 幂等性保护：已初始化过则不再重复设置
        if (snowflakeRedisKey != null) {
            log.warn("SnowflakeRedisCacheManager 已经初始化过，key={}，跳过重复设置", snowflakeRedisKey);
            return true;
        }

        bean.setCreateTime(LocalDateTime.now());
        Duration redisExpire = properties.getRedisExpire();
        bean.setExpireTime(bean.getCreateTime().plusSeconds(redisExpire.getSeconds()));
        long refresh = redisExpire.getSeconds() - 30 * 60;

        String key = properties.getNamespace() + StrUtil.COLON + bean.getWorkerId() + StrUtil.UNDERLINE + bean.getDataCenterId();
        Boolean save = RedisUtil.setIfAbsent(key, bean, properties.getRedisExpire().getSeconds(), TimeUnit.SECONDS);
        if (Boolean.TRUE.equals(save)) {
            this.snowflakeRedisKey = key;
            // 执行定期更新（仅成功时启动，且只启动一次）
            if (scheduled.compareAndSet(false, true)) {
                ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1,
                    Thread.ofVirtual().factory());
                this.executorService = executor;
                executor.scheduleAtFixedRate(this::resetExpire, refresh, refresh, TimeUnit.SECONDS);
            }
            return true;
        }
        return false;
    }

    @Override
    public void delCache() {
        ScheduledExecutorService executor = this.executorService;
        if (executor != null) {
            executor.shutdownNow();
            this.executorService = null;
        }
        String key = this.snowflakeRedisKey;
        if (key != null) {
            RedisUtil.delete(key);
            log.debug("Delete snowflake key:{} ", key);
        }
        // 重置状态，允许重新初始化
        scheduled.set(false);
        this.snowflakeRedisKey = null;
    }

    /**
     * 从新设置过时时间，由定时任务调用
     *
     * @author carlos
     * @date 2021/12/13 11:16
     */
    @Override
    public void resetExpire() {
        String key = this.snowflakeRedisKey;
        if (key == null) {
            log.warn("snowflakeRedisKey 为空，跳过续期");
            return;
        }
        try {
            RedisUtil.setExpire(key, properties.getRedisExpire().getSeconds(), TimeUnit.SECONDS);
            log.info("Reset expire of snowflake key:[{}]", key);
        } catch (Exception e) {
            log.error("续期 snowflake key:[{}] 失败", key, e);
        }
    }
}
