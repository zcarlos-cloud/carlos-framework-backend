package com.carlos.redis.metrics;

import com.carlos.redis.caffeine.CaffeineUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 缓存指标收集器
 * <p>
 * 定期收集和上报缓存相关指标
 * </p>
 *
 * @author carlos
 * @date 2026-03-14
 */
@Slf4j
public class CacheMetricsCollector {

    private final CacheMetrics cacheMetrics;

    public CacheMetricsCollector(CacheMetrics cacheMetrics) {
        this.cacheMetrics = cacheMetrics;
        log.info("CacheMetricsCollector initialized");
    }

    /**
     * 定期打印缓存统计（每 5 分钟）
     */
    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void reportCacheStats() {
        try {
            // Caffeine 统计
            String caffeineStats = CaffeineUtil.stats();
            log.info("Caffeine cache stats: {}", caffeineStats);
            log.info("Caffeine cache size: {}", CaffeineUtil.size());

            // 本地统计
            log.info("Local cache metrics: {}", cacheMetrics.getStats());

        } catch (Exception e) {
            log.error("Failed to report cache stats", e);
        }
    }
}
