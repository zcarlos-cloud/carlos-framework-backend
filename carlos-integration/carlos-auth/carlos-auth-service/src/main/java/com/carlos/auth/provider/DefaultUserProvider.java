package com.carlos.auth.provider;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 * 默认用户提供者实现
 * </p>
 *
 * <p>基于内存的默认实现，仅用于开发和测试环境。</p>
 *
 * <p><strong>注意：</strong>生产环境必须提供自定义实现，从实际用户系统（如数据库、LDAP等）加载用户信息。</p>
 *
 * <h3>默认测试用户：</h3>
 * <ul>
 *   <li>用户名：admin，密码：123456，角色：ADMIN</li>
 *   <li>用户名：user，密码：123456，角色：USER</li>
 *   <li>用户名：test，密码：123456，角色：USER</li>
 * </ul>
 *
 * @author Carlos
 * @date 2026-04-08
 * @see com.carlos.auth.provider.UserProvider
 */
@Slf4j
@Profile("!prod")
public class DefaultUserProvider implements UserProvider {

    private final PasswordEncoder passwordEncoder;
    private final Map<String, UserInfo> userStore = new ConcurrentHashMap<>();
    private final Map<Long, UserInfo> userIdStore = new ConcurrentHashMap<>();

    /**
     * 默认用户ID计数器
     */
    private static long userIdCounter = 1000L;

    public DefaultUserProvider(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
        initDefaultUsers();
        log.warn("DefaultUserProvider initialized (FOR DEVELOPMENT ONLY)");
        log.warn("Please implement custom UserProvider for production environment");
    }

    /**
     * 初始化默认测试用户
     */
    private void initDefaultUsers() {
        // 创建默认管理员用户
        createDefaultUser("admin", "123456", "admin@example.com", "13800138000",
            List.of("ADMIN", "USER"));

        // 创建普通用户
        createDefaultUser("user", "123456", "user@example.com", "13800138001",
            List.of("USER"));

        // 创建测试用户
        createDefaultUser("test", "123456", "test@example.com", "13800138002",
            List.of("USER"));

        log.info("Initialized {} default users: admin, user, test (password: 123456)", userStore.size());
    }

    /**
     * 创建默认用户
     */
    private void createDefaultUser(String username, String password, String email, String phone,
                                   List<String> roleCodes) {
        long userId = ++userIdCounter;
        String encodedPassword = passwordEncoder.encode(password);

        UserInfo userInfo = UserInfo.builder()
            .userId(userId)
            .username(username)
            .email(email)
            .phone(phone)
            .password(encodedPassword)
            .status("ENABLE")
            .loginAttempts(0)
            .mfaEnabled(false)
            .roleCodes(roleCodes)
            .authorities(new ArrayList<>(roleCodes))
            .deptId(1L)
            .tenantId(1L)
            .createTime(LocalDateTime.now())
            .updateTime(LocalDateTime.now())
            .build();

        userStore.put(username.toLowerCase(), userInfo);
        userStore.put(email.toLowerCase(), userInfo);
        userStore.put(phone, userInfo);
        userIdStore.put(userId, userInfo);
    }

    @Override
    public UserInfo loadUserByIdentifier(String identifier) {
        if (StrUtil.isBlank(identifier)) {
            return null;
        }

        // 支持用户名、邮箱、手机号查询
        String key = identifier.toLowerCase().trim();
        UserInfo userInfo = userStore.get(key);

        if (userInfo != null) {
            log.debug("Found user by identifier '{}': {}", identifier, userInfo.getUsername());
        } else {
            log.debug("User not found by identifier: {}", identifier);
        }

        return userInfo;
    }

    @Override
    public UserInfo loadUserById(Long userId) {
        if (userId == null) {
            return null;
        }

        UserInfo userInfo = userIdStore.get(userId);

        if (userInfo != null) {
            log.debug("Found user by id '{}': {}", userId, userInfo.getUsername());
        } else {
            log.debug("User not found by id: {}", userId);
        }

        return userInfo;
    }

    @Override
    public List<String> getUserRoleCodes(Long userId) {
        UserInfo userInfo = loadUserById(userId);
        if (userInfo == null || userInfo.getRoleCodes() == null) {
            return List.of();
        }
        return new ArrayList<>(userInfo.getRoleCodes());
    }

    @Override
    public boolean verifyPassword(Long userId, String password) {
        if (userId == null || StrUtil.isBlank(password)) {
            return false;
        }

        UserInfo userInfo = loadUserById(userId);
        if (userInfo == null || userInfo.getPassword() == null) {
            return false;
        }

        boolean matches = passwordEncoder.matches(password, userInfo.getPassword());
        log.debug("Password verification for user '{}': {}", userInfo.getUsername(), matches ? "SUCCESS" : "FAILED");

        return matches;
    }

    @Override
    public boolean updatePassword(Long userId, String newPassword) {
        if (userId == null || StrUtil.isBlank(newPassword)) {
            return false;
        }

        UserInfo userInfo = loadUserById(userId);
        if (userInfo == null) {
            log.warn("Cannot update password: user not found, userId={}", userId);
            return false;
        }

        String encodedPassword = passwordEncoder.encode(newPassword);
        userInfo.setPassword(encodedPassword);
        userInfo.setUpdateTime(LocalDateTime.now());

        log.info("Password updated for user: {}", userInfo.getUsername());
        return true;
    }

    @Override
    public boolean updateUserStatus(Long userId, String status) {
        if (userId == null || StrUtil.isBlank(status)) {
            return false;
        }

        UserInfo userInfo = loadUserById(userId);
        if (userInfo == null) {
            log.warn("Cannot update status: user not found, userId={}", userId);
            return false;
        }

        String oldStatus = userInfo.getStatus();
        userInfo.setStatus(status);
        userInfo.setUpdateTime(LocalDateTime.now());

        // 如果是解锁操作，清除锁定时间
        if ("ENABLE".equals(status)) {
            userInfo.setLockTime(null);
            userInfo.setLoginAttempts(0);
        }

        log.info("User status updated: {} from '{}' to '{}'", userInfo.getUsername(), oldStatus, status);
        return true;
    }

    @Override
    public int incrementLoginAttempts(Long userId) {
        if (userId == null) {
            return 0;
        }

        UserInfo userInfo = loadUserById(userId);
        if (userInfo == null) {
            return 0;
        }

        int attempts = userInfo.getLoginAttempts() == null ? 0 : userInfo.getLoginAttempts();
        attempts++;
        userInfo.setLoginAttempts(attempts);
        userInfo.setUpdateTime(LocalDateTime.now());

        log.debug("Login attempts incremented for user '{}': {}", userInfo.getUsername(), attempts);
        return attempts;
    }

    @Override
    public void resetLoginAttempts(Long userId) {
        if (userId == null) {
            return;
        }

        UserInfo userInfo = loadUserById(userId);
        if (userInfo == null) {
            return;
        }

        userInfo.setLoginAttempts(0);
        userInfo.setUpdateTime(LocalDateTime.now());

        log.debug("Login attempts reset for user: {}", userInfo.getUsername());
    }

    @Override
    public void updateLockTime(Long userId, LocalDateTime lockTime) {
        if (userId == null) {
            return;
        }

        UserInfo userInfo = loadUserById(userId);
        if (userInfo == null) {
            log.warn("Cannot update lock time: user not found, userId={}", userId);
            return;
        }

        userInfo.setLockTime(lockTime);
        userInfo.setUpdateTime(LocalDateTime.now());

        if (lockTime != null) {
            userInfo.setStatus("LOCKED");
            log.info("User locked: {}, lockTime={}", userInfo.getUsername(), lockTime);
        } else {
            userInfo.setStatus("ENABLE");
            log.info("User unlocked: {}", userInfo.getUsername());
        }
    }

    @Override
    public boolean updateMfaStatus(Long userId, boolean mfaEnabled, String mfaSecret) {
        if (userId == null) {
            return false;
        }

        UserInfo userInfo = loadUserById(userId);
        if (userInfo == null) {
            log.warn("Cannot update MFA status: user not found, userId={}", userId);
            return false;
        }

        userInfo.setMfaEnabled(mfaEnabled);
        userInfo.setMfaSecret(mfaSecret);
        userInfo.setUpdateTime(LocalDateTime.now());

        log.info("MFA status updated for user '{}': enabled={}", userInfo.getUsername(), mfaEnabled);
        return true;
    }
}
