package com.carlos.message.core.retry;

/**
 * <p>
 * 重试策略接口
 * </p>
 *
 * @author Carlos
 * @date 2026/3/11
 */
public interface RetryPolicy {

    /**
     * 是否需要重试
     *
     * @param retryCount 当前重试次数
     * @param errorCode  错误码
     * @return 是否需要重试
     */
    boolean shouldRetry(int retryCount, String errorCode);

    /**
     * 获取下次重试间隔（毫秒）
     *
     * @param retryCount 当前重试次数
     * @return 重试间隔
     */
    long getRetryInterval(int retryCount);

    /**
     * 获取最大重试次数
     *
     * @return 最大重试次数
     */
    int getMaxRetries();
}
