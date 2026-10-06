package com.carlos.auth.security.manager;

import cn.hutool.extra.servlet.JakartaServletUtil;
import com.carlos.auth.constant.AuthRedisKey;
import com.carlos.auth.security.SecurityProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * <p>
 * IP封禁管理器
 * </p>
 *
 * <p>管理IP地址的登录失败次数，防止同一IP暴力破解</p>
 *
 * @author Carlos
 * @date 2026-02-26
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IpBlockManager {

    /**
     * RedisTemplate
     */
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 安全配置属性
     */
    private final SecurityProperties securityProperties;

    /**
     * 记录IP登录失败
     *
     * @param request HTTP请求
     */
    public void recordLoginFailure(HttpServletRequest request) {
        String ipAddress = getClientIp(request);
        if (ipAddress == null || ipAddress.isBlank()) {
            log.warn("Cannot determine client IP address");
            return;
        }

        recordLoginFailure(ipAddress);
    }

    /**
     * 记录IP登录失败
     *
     * @param ipAddress IP地址
     */
    public void recordLoginFailure(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return;
        }

        SecurityProperties.IpBlockProperties props = securityProperties.getIpBlock();
        if (!props.isEnabled()) {
            return;
        }

        String attemptKey = AuthRedisKey.ipAttempt(ipAddress);
        String blockKey = AuthRedisKey.ipLock(ipAddress);

        // 获取当前失败次数
        Long attempts = redisTemplate.opsForValue().increment(attemptKey);

        if (attempts == 1) {
            // 第一次失败，设置过期时间
            redisTemplate.expire(attemptKey, props.getTimeWindow());
            log.debug("First login failure from IP: {}", ipAddress);
        }

        log.warn("Login failure recorded from IP: {}, attempts={}", ipAddress, attempts);

        // 检查是否达到封禁阈值
        if (attempts >= props.getMaxAttempts()) {
            // 封禁IP
            blockIp(ipAddress);
            log.warn("IP blocked due to too many failed attempts: {}", ipAddress);
        }
    }

    /**
     * 记录IP登录成功（重置计数）
     *
     * @param request HTTP请求
     */
    public void recordLoginSuccess(HttpServletRequest request) {
        String ipAddress = getClientIp(request);
        if (ipAddress == null || ipAddress.isBlank()) {
            return;
        }

        recordLoginSuccess(ipAddress);
    }

    /**
     * 记录IP登录成功（重置计数）
     *
     * @param ipAddress IP地址
     */
    public void recordLoginSuccess(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return;
        }

        String attemptKey = AuthRedisKey.ipAttempt(ipAddress);
        String blockKey = AuthRedisKey.ipLock(ipAddress);

        // 删除失败次数记录
        redisTemplate.delete(attemptKey);

        // 解除封禁（如果之前被封禁）
        if (Boolean.TRUE.equals(redisTemplate.hasKey(blockKey))) {
            redisTemplate.delete(blockKey);
            log.info("IP unblocked after successful login: {}", ipAddress);
        }

        log.debug("Login success recorded from IP: {}", ipAddress);
    }

    /**
     * 检查IP是否被封禁
     *
     * @param request HTTP请求
     * @return true-被封禁，false-未封禁
     */
    public boolean isIpBlocked(HttpServletRequest request) {
        String ipAddress = getClientIp(request);
        if (ipAddress == null || ipAddress.isBlank()) {
            return false;
        }

        return isIpBlocked(ipAddress);
    }

    /**
     * 检查IP是否被封禁
     *
     * @param ipAddress IP地址
     * @return true-被封禁，false-未封禁
     */
    public boolean isIpBlocked(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return false;
        }

        String blockKey = AuthRedisKey.ipLock(ipAddress);
        Boolean isBlocked = redisTemplate.hasKey(blockKey);

        if (Boolean.TRUE.equals(isBlocked)) {
            Long ttl = redisTemplate.getExpire(blockKey, TimeUnit.SECONDS);
            log.warn("IP is blocked: {}, TTL={} seconds", ipAddress, ttl);
            return true;
        }

        return false;
    }

    /**
     * 封禁IP
     *
     * @param ipAddress IP地址
     */
    public void blockIp(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return;
        }

        SecurityProperties.IpBlockProperties props = securityProperties.getIpBlock();
        String blockKey = AuthRedisKey.ipLock(ipAddress);
        redisTemplate.opsForValue().set(blockKey, "BLOCKED", props.getBlockDuration());

        log.info("IP blocked: {}, duration={} hours", ipAddress, props.getBlockDuration().toHours());
    }

    /**
     * 获取剩余封禁时间（秒）
     *
     * @param ipAddress IP地址
     * @return 剩余封禁时间（秒），如果未封禁返回 0
     */
    public long getRemainingBlockTime(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return 0;
        }

        String blockKey = AuthRedisKey.ipLock(ipAddress);
        if (!Boolean.TRUE.equals(redisTemplate.hasKey(blockKey))) {
            return 0;
        }

        Long ttl = redisTemplate.getExpire(blockKey, TimeUnit.SECONDS);
        return ttl != null && ttl > 0 ? ttl : 0;
    }

    /**
     * 获取当前IP的失败次数
     *
     * @param ipAddress IP地址
     * @return 失败次数
     */
    public int getCurrentAttempts(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return 0;
        }

        String attemptKey = AuthRedisKey.ipAttempt(ipAddress);
        Object value = redisTemplate.opsForValue().get(attemptKey);

        if (value == null) {
            return 0;
        }

        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            log.error("Failed to parse IP login attempts for IP: {}", ipAddress, e);
            return 0;
        }
    }

    /**
     * 获取客户端IP地址
     *
     * @param request HTTP请求
     * @return IP地址
     */
    public String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        // 优先从 hutool 获取（支持代理头）
        String ip = JakartaServletUtil.getClientIP(request);

        // 回退方案
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        return ip;
    }

    /**
     * 重置IP的尝试次数（手动解封）
     *
     * @param ipAddress IP地址
     */
    public void resetAttempts(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return;
        }

        String attemptKey = AuthRedisKey.ipAttempt(ipAddress);
        String blockKey = AuthRedisKey.ipLock(ipAddress);

        redisTemplate.delete(attemptKey);
        redisTemplate.delete(blockKey);

        log.info("IP login attempts reset for IP: {}", ipAddress);
    }
}
