package com.carlos.auth.provider;

import java.util.List;

/**
 * <p>
 * 用户信息提供者接口
 * </p>
 *
 * <p>用于与外部用户系统集成，提供用户基本信息查询</p>
 *
 * @author Carlos
 * @date 2026-02-26
 */
public interface UserProvider {

    /**
     * 根据用户名/邮箱/手机号查询用户信息
     *
     * @param identifier 用户名、邮箱或手机号
     * @return 用户信息，不存在返回null
     */
    UserInfo loadUserByIdentifier(String identifier);

    /**
     * 根据用户名/手机号查询指定租户内的用户信息
     *
     * <p>S2 租户管理：开启登录租户校验（carlos.auth.tenant.enabled=true）时使用。
     * 默认实现退化为 {@link #loadUserByIdentifier}（不支持租户隔离的用户源保持原行为）。</p>
     *
     * @param identifier 用户名或手机号
     * @param tenantId   租户ID
     * @return 用户信息，不存在返回null
     */
    default UserInfo loadUserByIdentifierAndTenant(String identifier, Long tenantId) {
        return loadUserByIdentifier(identifier);
    }

    /**
     * 根据用户ID查询用户信息
     *
     * @param userId 用户ID
     * @return 用户信息，不存在返回null
     */
    UserInfo loadUserById(Long userId);

    /**
     * 获取用户的角色编码列表
     *
     * @param userId 用户ID
     * @return 角色编码列表（如：ADMIN, USER）
     */
    List<String> getUserRoleCodes(Long userId);

    /**
     * 验证用户密码
     *
     * @param userId   用户ID
     * @param password 明文密码
     * @return true-验证通过，false-验证失败
     */
    boolean verifyPassword(Long userId, String password);

    /**
     * 更新用户密码
     *
     * @param userId      用户ID
     * @param newPassword 新密码（已加密）
     * @return true-更新成功，false-更新失败
     */
    boolean updatePassword(Long userId, String newPassword);

    /**
     * 更新用户状态
     *
     * @param userId 用户ID
     * @param status 状态（ENABLE/DISABLE/LOCKED）
     * @return true-更新成功，false-更新失败
     */
    boolean updateUserStatus(Long userId, String status);

    /**
     * 增加登录失败次数
     *
     * @param userId 用户ID
     * @return 更新后的失败次数
     */
    int incrementLoginAttempts(Long userId);

    /**
     * 重置登录失败次数
     *
     * @param userId 用户ID
     */
    void resetLoginAttempts(Long userId);

    /**
     * 更新账号锁定时间
     *
     * @param userId    用户ID
     * @param lockTime  锁定时间，null表示解锁
     */
    void updateLockTime(Long userId, java.time.LocalDateTime lockTime);

    /**
     * 更新MFA启用状态
     *
     * @param userId     用户ID
     * @param mfaEnabled MFA是否启用
     * @param mfaSecret  MFA密钥，禁用时可传入null
     * @return true-更新成功，false-更新失败
     */
    boolean updateMfaStatus(Long userId, boolean mfaEnabled, String mfaSecret);
}
