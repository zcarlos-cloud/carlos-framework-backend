package com.carlos.sms.ratelimit;

/**
 * 短信发送限流器接口
 */
public interface SmsRateLimiter {

    /**
     * 检查指定手机号当前是否可以发送短信
     *
     * @param phone 手机号
     * @return true 允许发送，false 被限流
     */
    boolean allowSend(String phone);

    /**
     * 记录一次发送成功
     *
     * @param phone 手机号
     */
    void recordSend(String phone);
}
