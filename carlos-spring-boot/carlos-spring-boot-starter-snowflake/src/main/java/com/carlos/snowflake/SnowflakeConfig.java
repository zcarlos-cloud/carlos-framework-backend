package com.carlos.snowflake;

import cn.hutool.core.lang.Snowflake;
import com.carlos.snowflake.cache.SnowflakeCacheManager;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.lang.Nullable;

/**
 * <p>
 * 分布式雪花算法配置
 * </p>
 * <p>
 * 支持两种模式：
 * 1. Redis 模式：多实例分布式协调 workerId/dataCenterId（需要引入 carlos-spring-boot-starter-redis）
 * 2. 本地模式：基于 IP 哈希生成 workerId/dataCenterId（无 Redis 依赖，轻量级）
 * </p>
 *
 * @author carlos
 * @date 2020/10/25 23:44
 */
@Slf4j
@AutoConfiguration
@AllArgsConstructor
@EnableConfigurationProperties(SnowflakeProperties.class)
public class SnowflakeConfig {

    private final SnowflakeProperties properties;

    /**
     * 雪花算法初始化器
     * <p>
     * 注意：当存在 Redis 时，SnowflakeCacheManager 由 SnowflakeRedisCacheAutoConfiguration 提供；
     * 当不存在 Redis 时，由 SnowflakeLocalCacheAutoConfiguration 提供本地实现
     * </p>
     *
     * @param cacheManager 缓存管理器（可能为 null，但实际情况中由自动配置提供）
     * @return SnowflakeInitiator 实例
     */
    @Bean
    public SnowflakeInitiator snowflakeInitiator(@Nullable SnowflakeCacheManager cacheManager) {
        return new SnowflakeInitiator(properties, cacheManager);
    }

    @Bean
    public Snowflake snowflake(SnowflakeInitiator initiator) {
        SnowflakeInfo dto = initiator.init();
        Long dataCenterId = dto.getDataCenterId();
        Long workerId = dto.getWorkerId();
        Snowflake snowflake = new Snowflake(workerId, dataCenterId, properties.isUseSystemClock());

        // 初始化 SnowflakeUtil 静态工具类
        SnowflakeUtil.init(snowflake);

        if (log.isInfoEnabled()) {
            log.info("Snowflake ID生成器已注册 - workerId:{}, dataCenterId:{}, 模式:{}",
                workerId, dataCenterId,
                properties.getWorkerId() != null || properties.getDataCenterId() != null ? "配置模式" : "自动模式");
        }
        return snowflake;
    }
}
