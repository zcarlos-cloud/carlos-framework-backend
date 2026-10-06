package com.carlos.message.core.limiter;

/**
 * <p>
 * 限流器接口
 * </p>
 *
 * @author Carlos
 * @date 2026/3/11
 */
public interface RateLimiter {

    /**
     * 尝试获取许可
     *
     * @param channelCode 渠道编码
     * @param permits     许可数量
     * @return 是否成功
     */
    boolean tryAcquire(String channelCode, int permits);

    /**
     * 尝试获取单个许可
     *
     * @param channelCode 渠道编码
     * @return 是否成功
     */
    default boolean tryAcquire(String channelCode) {
        return tryAcquire(channelCode, 1);
    }

    /**
     * 更新限流配置
     *
     * @param channelCode 渠道编码
     * @param qps         每秒请求数
     * @param burst       突发流量
     */
    void updateConfig(String channelCode, int qps, int burst);
}
