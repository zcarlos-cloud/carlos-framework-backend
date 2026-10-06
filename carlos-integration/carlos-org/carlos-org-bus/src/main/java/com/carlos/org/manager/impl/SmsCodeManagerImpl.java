package com.carlos.org.manager.impl;

import com.carlos.org.manager.SmsCodeManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * <p>
 * 短信验证码 Manager 实现
 * </p>
 *
 * <p>在 Manager 层统一实现 Redis 操作。</p>
 *
 * @author Carlos
 * @date 2024/5/22 16:30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SmsCodeManagerImpl implements SmsCodeManager {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 短信验证码 Key 前缀
     * <p>与 carlos-auth 的 {@code AuthRedisKey.CAPTCHA}（{@code auth:captcha:%s:%s}，type=sms）
     * 同口径。org 只依赖 carlos-auth-api、够不到 auth-service 的常量类，故此处保留字面量，
     * 改动时两端必须同步。</p>
     */
    private static final String SMS_CODE_KEY_PREFIX = "auth:captcha:sms:";

    @Override
    public String getSmsCode(String phone) {
        String key = SMS_CODE_KEY_PREFIX + phone;
        Object value = redisTemplate.opsForValue().get(key);
        return value != null ? value.toString() : null;
    }

    @Override
    public boolean deleteSmsCode(String phone) {
        String key = SMS_CODE_KEY_PREFIX + phone;
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }
}
