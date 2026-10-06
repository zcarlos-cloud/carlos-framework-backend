package com.carlos.snowflake.cache;

import cn.hutool.core.net.NetUtil;
import com.carlos.snowflake.SnowflakeInfo;
import com.carlos.snowflake.SnowflakeProperties;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

/**
 * <p>
 * 本地缓存管理器（轻量级实现，无 Redis 依赖）
 * </p>
 * <p>
 * 适用于单机或开发环境，基于 IP 地址哈希生成 workerId 和 dataCenterId
 * </p>
 *
 * @author Carlos
 * @date 2025-03-17 13:54
 */
@Slf4j
public class SnowflakeLocalCacheManager implements SnowflakeCacheManager {

    private final SnowflakeProperties properties;

    public SnowflakeLocalCacheManager(SnowflakeProperties properties) {
        this.properties = properties;
        log.info("SnowflakeLocalCacheManager 初始化完成（本地模式，无 Redis）");
    }

    @Override
    public boolean putCache(SnowflakeInfo bean) {
        bean.setCreateTime(LocalDateTime.now());
        // 本地模式不过期
        bean.setExpireTime(LocalDateTime.MAX);

        // 如果配置了固定的 workerId 和 dataCenterId，直接使用
        if (properties.getWorkerId() != null && properties.getDataCenterId() != null) {
            bean.setWorkerId(properties.getWorkerId());
            bean.setDataCenterId(properties.getDataCenterId());
            log.debug("使用配置的 workerId:{} dataCenterId:{}", properties.getWorkerId(), properties.getDataCenterId());
            return true;
        }

        // 基于 IP 地址生成 workerId 和 dataCenterId
        String ip = NetUtil.getLocalhostStr();
        long ipHash = ip.hashCode() & 0xFFFFFFFFL;

        // 生成 workerId (0-31)
        long workerId = ipHash & 0x1F;
        // 生成 dataCenterId (0-31)
        long dataCenterId = (ipHash >> 5) & 0x1F;

        // 如果用户配置了其中一个，使用配置值
        if (properties.getWorkerId() != null) {
            workerId = properties.getWorkerId();
        }
        if (properties.getDataCenterId() != null) {
            dataCenterId = properties.getDataCenterId();
        }

        bean.setWorkerId(workerId);
        bean.setDataCenterId(dataCenterId);

        if (log.isInfoEnabled()) {
            log.info("本地模式生成雪花ID配置 - IP:{}, workerId:{}, dataCenterId:{}", ip, workerId, dataCenterId);
        }
        return true;
    }

    @Override
    public void delCache() {
        // 本地模式无需清理缓存
        log.debug("本地模式无需清理缓存");
    }

    @Override
    public void resetExpire() {
        // 本地模式无需续期
        log.debug("本地模式无需续期");
    }
}
