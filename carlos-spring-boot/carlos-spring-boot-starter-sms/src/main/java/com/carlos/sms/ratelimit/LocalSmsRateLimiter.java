package com.carlos.sms.ratelimit;

import com.carlos.sms.config.SmsProperties;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 本地内存短信限流器
 *
 * <p>按手机号进行日/分钟维度限流。注意：本地实现仅适用于单实例部署，
 * 多实例生产环境建议接入 Redis 等分布式限流。</p>
 */
@Slf4j
public class LocalSmsRateLimiter implements SmsRateLimiter {

    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter MINUTE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    private final SmsProperties.RateLimit rateLimit;
    private final ConcurrentHashMap<String, AtomicInteger> dayCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> minuteCounters = new ConcurrentHashMap<>();

    public LocalSmsRateLimiter(SmsProperties.RateLimit rateLimit) {
        this.rateLimit = rateLimit;
    }

    @Override
    public boolean allowSend(String phone) {
        if (!rateLimit.isEnabled()) {
            return true;
        }
        String dayKey = dayKey(phone);
        String minuteKey = minuteKey(phone);
        int dayCount = dayCounters.getOrDefault(dayKey, new AtomicInteger(0)).get();
        int minuteCount = minuteCounters.getOrDefault(minuteKey, new AtomicInteger(0)).get();
        if (dayCount >= rateLimit.getAccountMax() || minuteCount >= rateLimit.getMinuteMax()) {
            log.warn("SMS rate limit exceeded, phone={}, dayCount={}, minuteCount={}", maskPhone(phone), dayCount, minuteCount);
            return false;
        }
        return true;
    }

    @Override
    public void recordSend(String phone) {
        if (!rateLimit.isEnabled()) {
            return;
        }
        dayCounters.computeIfAbsent(dayKey(phone), k -> new AtomicInteger(0)).incrementAndGet();
        minuteCounters.computeIfAbsent(minuteKey(phone), k -> new AtomicInteger(0)).incrementAndGet();
    }

    private String dayKey(String phone) {
        return phone + ":" + LocalDateTime.now().format(DAY_FORMAT);
    }

    private String minuteKey(String phone) {
        return phone + ":" + LocalDateTime.now().format(MINUTE_FORMAT);
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
