package com.carlos.redis.ratelimit;

import com.carlos.core.exception.BusinessException;

/**
 * 限流异常
 * <p>
 * 当请求触发限流规则时抛出，提示用户请求过于频繁
 *
 * @author carlos
 * @date 2026-04-08
 */
public class RateLimitException extends BusinessException {

    private static final long serialVersionUID = 1L;

    /**
     * 限流键
     */
    private final String rateLimitKey;

    /**
     * 等待时间（毫秒）
     */
    private final long waitTimeMillis;

    public RateLimitException(String rateLimitKey) {
        super("请求过于频繁，请稍后重试");
        this.rateLimitKey = rateLimitKey;
        this.waitTimeMillis = -1;
    }

    public RateLimitException(String rateLimitKey, String message) {
        super(message);
        this.rateLimitKey = rateLimitKey;
        this.waitTimeMillis = -1;
    }

    public RateLimitException(String rateLimitKey, long waitTimeMillis) {
        super(String.format("请求过于频繁，请在 %d 秒后重试", waitTimeMillis / 1000));
        this.rateLimitKey = rateLimitKey;
        this.waitTimeMillis = waitTimeMillis;
    }

    public RateLimitException(String rateLimitKey, String message, Throwable cause) {
        super(message, cause);
        this.rateLimitKey = rateLimitKey;
        this.waitTimeMillis = -1;
    }

    public String getRateLimitKey() {
        return rateLimitKey;
    }

    public long getWaitTimeMillis() {
        return waitTimeMillis;
    }
}
