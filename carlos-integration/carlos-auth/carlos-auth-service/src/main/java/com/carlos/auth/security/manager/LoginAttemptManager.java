package com.carlos.auth.security.manager;

import com.carlos.auth.constant.AuthRedisKey;
import com.carlos.auth.provider.UserInfo;
import com.carlos.auth.provider.UserProvider;
import com.carlos.redis.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * <p>
 * 登录尝试管理器
 * </p>
 *
 * <p>管理用户登录尝试次数，防止暴力破解密码</p>
 *
 * @author Carlos
 * @date 2026-02-26
 */
@Slf4j
@Component
public class LoginAttemptManager {

    /**
     * 默认失败阈值（5次）
     */
    private static final int DEFAULT_MAX_ATTEMPTS = 5;

    /**
     * 默认锁定时长（30分钟）
     */
    private static final Duration DEFAULT_LOCK_DURATION = Duration.ofMinutes(30);

    /**
     * 用户服务（懒加载，兼容生产环境自定义实现）
     */
    private final ObjectProvider<UserProvider> userProviderProvider;

    public LoginAttemptManager(ObjectProvider<UserProvider> userProviderProvider) {
        this.userProviderProvider = userProviderProvider;
    }

    /**
     * 记录登录失败
     *
     * @param username 用户名
     */
    public void recordLoginFailure(String username) {
        if (username == null || username.isBlank()) {
            return;
        }

        String countKey = AuthRedisKey.userAttempt(username);
        String lockKey = AuthRedisKey.userLock(username);

        // 获取当前失败次数
        Long attempts = RedisUtil.incrementValue(countKey, 1L);

        if (attempts == 1) {
            // 第一次失败，设置过期时间（24小时）
            RedisUtil.setExpire(countKey, Duration.ofHours(24));
            log.debug("First login failure for user: {}", username);
        }

        log.warn("Login failure recorded for user: {}, attempts={}", username, attempts);

        // 检查是否达到锁定阈值
        if (attempts >= DEFAULT_MAX_ATTEMPTS) {
            // 锁定账号
            lockAccount(username);
            log.warn("Account locked due to too many failed attempts: {}", username);
        }
    }

    /**
     * 记录登录成功
     *
     * @param username 用户名
     */
    public void recordLoginSuccess(String username) {
        if (username == null || username.isBlank()) {
            return;
        }

        String countKey = AuthRedisKey.userAttempt(username);
        String lockKey = AuthRedisKey.userLock(username);

        // 删除失败次数记录
        RedisUtil.delete(countKey);

        // 解除锁定（如果之前被锁定）
        if (Boolean.TRUE.equals(RedisUtil.hasKey(lockKey))) {
            RedisUtil.delete(lockKey);
            // 同步解锁数据库状态
            unlockUserInDb(username);
            log.info("Account unlocked after successful login: {}", username);
        }

        log.debug("Login success recorded for user: {}", username);
    }

    /**
     * 检查账号是否被锁定
     *
     * @param username 用户名
     * @return true-被锁定，false-未锁定
     */
    public boolean isAccountLocked(String username) {
        if (username == null || username.isBlank()) {
            return false;
        }

        String lockKey = AuthRedisKey.userLock(username);
        Boolean isLocked = RedisUtil.hasKey(lockKey);

        if (Boolean.TRUE.equals(isLocked)) {
            Long ttl = RedisUtil.getExpire(lockKey);
            log.warn("Account is locked: {}, TTL={} seconds", username, ttl);
            return true;
        }

        return false;
    }

    /**
     * 锁定账号
     *
     * @param username 用户名
     */
    public void lockAccount(String username) {
        if (username == null || username.isBlank()) {
            return;
        }

        String lockKey = AuthRedisKey.userLock(username);
        RedisUtil.setValue(lockKey, "LOCKED", DEFAULT_LOCK_DURATION);

        // 同步锁定数据库状态
        lockUserInDb(username);

        log.info("Account locked: {}, duration={} minutes", username, DEFAULT_LOCK_DURATION.toMinutes());
    }

    /**
     * 同步锁定数据库用户状态
     *
     * @param username 用户名
     */
    private void lockUserInDb(String username) {
        try {
            UserProvider userProvider = userProviderProvider.getIfAvailable();
            if (userProvider == null) {
                return;
            }
            UserInfo userInfo = userProvider.loadUserByIdentifier(username);
            if (userInfo != null && userInfo.getUserId() != null) {
                userProvider.updateUserStatus(userInfo.getUserId(), "LOCKED");
                log.debug("User status updated to LOCKED in database: {}", username);
            }
        } catch (Exception e) {
            log.warn("Failed to update user lock status in database for: {}", username, e);
        }
    }

    /**
     * 同步解锁数据库用户状态
     *
     * @param username 用户名
     */
    private void unlockUserInDb(String username) {
        try {
            UserProvider userProvider = userProviderProvider.getIfAvailable();
            if (userProvider == null) {
                return;
            }
            UserInfo userInfo = userProvider.loadUserByIdentifier(username);
            if (userInfo != null && userInfo.getUserId() != null) {
                userProvider.updateUserStatus(userInfo.getUserId(), "ENABLE");
                log.debug("User status updated to ENABLE in database: {}", username);
            }
        } catch (Exception e) {
            log.warn("Failed to update user unlock status in database for: {}", username, e);
        }
    }

    /**
     * 获取剩余锁定时间（秒）
     *
     * @param username 用户名
     * @return 剩余锁定时间（秒），如果未锁定返回 0
     */
    public long getRemainingLockTime(String username) {
        if (username == null || username.isBlank()) {
            return 0;
        }

        String lockKey = AuthRedisKey.userLock(username);
        if (!Boolean.TRUE.equals(RedisUtil.hasKey(lockKey))) {
            return 0;
        }

        Long ttl = RedisUtil.getExpire(lockKey);
        return ttl != null && ttl > 0 ? ttl : 0;
    }

    /**
     * 获取当前失败次数
     *
     * @param username 用户名
     * @return 失败次数
     */
    public int getCurrentAttempts(String username) {
        if (username == null || username.isBlank()) {
            return 0;
        }
        String countKey = AuthRedisKey.userAttempt(username);
        Integer value = RedisUtil.getValue(countKey);
        if (value == null) {
            return 0;
        }
        return value;
    }

    /**
     * 重置失败次数（手动解锁）
     *
     * @param username 用户名
     */
    public void resetAttempts(String username) {
        if (username == null || username.isBlank()) {
            return;
        }

        String countKey = AuthRedisKey.userAttempt(username);
        String lockKey = AuthRedisKey.userLock(username);

        RedisUtil.delete(countKey);
        RedisUtil.delete(lockKey);

        // 同步解锁数据库状态
        unlockUserInDb(username);

        log.info("Login attempts reset for user: {}", username);
    }
}
