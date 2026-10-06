package com.carlos.message.retry;

import com.carlos.message.config.MessageProperties;
import com.carlos.message.core.retry.RetryPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * <p>
 * 指数退避重试策略
 * </p>
 *
 * <p>
 * 重试间隔公式：interval = initialInterval * multiplier^(retryCount - 1)，
 * 不超过 maxInterval。
 * </p>
 *
 * @author Carlos
 * @date 2026/5/30
 */
@Component
@RequiredArgsConstructor
public class ExponentialBackoffRetryPolicy implements RetryPolicy {

    private final MessageProperties messageProperties;

    @Override
    public boolean shouldRetry(int retryCount, String errorCode) {
        return retryCount < getMaxRetries();
    }

    @Override
    public long getRetryInterval(int retryCount) {
        if (retryCount <= 0) {
            return 0;
        }
        long interval = (long) (messageProperties.getRetry().getInitialInterval()
            * Math.pow(messageProperties.getRetry().getMultiplier(), retryCount - 1));
        return Math.min(interval, messageProperties.getRetry().getMaxInterval());
    }

    @Override
    public int getMaxRetries() {
        return messageProperties.getRetry().getMaxAttempts();
    }
}
