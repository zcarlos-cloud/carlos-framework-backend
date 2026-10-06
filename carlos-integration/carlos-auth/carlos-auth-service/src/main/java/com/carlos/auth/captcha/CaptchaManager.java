package com.carlos.auth.captcha;

import com.carlos.auth.constant.AuthRedisKey;
import com.carlos.redis.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * <p>
 * 验证码数据管理器
 * </p>
 *
 * <p>封装验证码相关的 Redis 缓存操作，遵循 Carlos Framework 分层规范：
 * Service 层不直接操作 Redis，统一通过 Manager 层处理。
 * 底层使用 {@link RedisUtil} 进行 Redis 操作。</p>
 *
 * @author Carlos
 * @date 2026-05-19
 */
@Slf4j
@Component
public class CaptchaManager {

    /**
     * 保存验证码到 Redis
     *
     * @param type       类型：sms/email
     * @param target     手机号或邮箱
     * @param captcha    验证码
     * @param expireTime 过期时间（秒）
     */
    public void saveCaptcha(String type, String target, String captcha, long expireTime) {
        String key = buildCaptchaKey(type, target);
        RedisUtil.setValue(key, captcha, expireTime);
        log.debug("Saved captcha to Redis with key: {}, expireTime: {}", key, expireTime);
    }

    /**
     * 从 Redis 获取验证码
     *
     * @param type   类型：sms/email
     * @param target 手机号或邮箱
     * @return 验证码，不存在或过期返回 null
     */
    public String getCaptcha(String type, String target) {
        String key = buildCaptchaKey(type, target);
        Object value = RedisUtil.getValue(key);
        return value != null ? value.toString() : null;
    }

    /**
     * 删除验证码
     *
     * @param type   类型：sms/email
     * @param target 手机号或邮箱
     */
    public void deleteCaptcha(String type, String target) {
        String key = buildCaptchaKey(type, target);
        RedisUtil.delete(key);
        log.debug("Deleted captcha with key: {}", key);
    }

    /**
     * 检查是否可以发送验证码（每日上限 10 条）
     *
     * @param type   类型：sms/email
     * @param target 手机号/邮箱
     * @return true-可以发送，false-达到限制
     */
    public boolean canSendCaptcha(String type, String target) {
        String limitKey = buildLimitKey(type, target);
        Object count = RedisUtil.getValue(limitKey);
        if (count == null) {
            return true;
        }
        int sentCount = Integer.parseInt(count.toString());
        return sentCount < 10;
    }

    /**
     * 增加发送计数
     *
     * @param type   类型：sms/email
     * @param target 手机号/邮箱
     */
    public void incrementSendCount(String type, String target) {
        String limitKey = buildLimitKey(type, target);
        Long count = RedisUtil.incrementValue(limitKey, 1);
        if (count != null && count == 1) {
            RedisUtil.setExpire(limitKey, Duration.ofDays(1));
        }
    }

    /**
     * 删除发送计数
     *
     * @param type   类型：sms/email
     * @param target 手机号/邮箱
     */
    public void clearSendCount(String type, String target) {
        String limitKey = buildLimitKey(type, target);
        RedisUtil.delete(limitKey);
    }

    private String buildCaptchaKey(String type, String target) {
        return AuthRedisKey.captcha(type, target);
    }

    private String buildLimitKey(String type, String target) {
        return AuthRedisKey.captchaLimit(type, target);
    }
}
