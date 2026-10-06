package com.carlos.auth.provider;

import com.carlos.auth.idp.RoleProvider;
import com.carlos.core.response.Result;
import com.carlos.org.api.ApiOrgUser;
import com.carlos.org.api.pojo.ao.OrgUserAO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于 carlos-org 远程服务的用户提供者
 *
 * <p>通过 Feign 调用 carlos-org 的 {@link ApiOrgUser} 接口加载用户信息，
 * 替代开发环境的 {@link DefaultUserProvider}（内存中的测试用户）。</p>
 *
 * <h3>数据映射：</h3>
 * <table>
 *   <tr><th>OrgUserAO</th><th>UserInfo</th></tr>
 *   <tr><td>id</td><td>userId</td></tr>
 *   <tr><td>account</td><td>username</td></tr>
 *   <tr><td>email</td><td>email</td></tr>
 *   <tr><td>phone</td><td>phone</td></tr>
 *   <tr><td>pwd</td><td>password（BCrypt 哈希，原值透传）</td></tr>
 *   <tr><td>userStatus (0/1/2)</td><td>status (DISABLE/ENABLE/LOCKED)</td></tr>
 *   <tr><td>mainDeptId</td><td>deptId</td></tr>
 *   <tr><td>tenantId</td><td>tenantId</td></tr>
 * </table>
 *
 * <h3>写操作说明：</h3>
 * <p>密码修改、状态更新等写操作由 carlos-org 服务自己管理。
 * 本类中的写方法为 no-op（仅记录警告日志），不影响核心认证流程。</p>
 *
 * @author Carlos
 * @version 3.0.0
 * @since 2026-06-20
 * @see UserProvider
 * @see DefaultUserProvider
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrgRemoteUserProvider implements UserProvider {

    private final ApiOrgUser apiOrgUser;
    private final RoleProvider roleProvider;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserInfo loadUserByIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return null;
        }

        try {
            Result<OrgUserAO> result = apiOrgUser.getUserByIdentifier(identifier.trim());
            if (!result.isSuccess() || result.getData() == null) {
                log.debug("User not found by identifier: {}", identifier);
                return null;
            }

            OrgUserAO orgUser = result.getData();
            return convertToUserInfo(orgUser);

        } catch (Exception e) {
            log.error("Failed to load user by identifier={}: {}", identifier, e.getMessage(), e);
            return null;
        }
    }

    @Override
    public UserInfo loadUserByIdentifierAndTenant(String identifier, Long tenantId) {
        if (identifier == null || identifier.isBlank() || tenantId == null) {
            return null;
        }

        try {
            Result<OrgUserAO> result = apiOrgUser.getUserByIdentifierAndTenant(identifier.trim(), tenantId);
            if (!result.isSuccess() || result.getData() == null) {
                log.debug("User not found by identifier={} in tenant={}", identifier, tenantId);
                return null;
            }

            OrgUserAO orgUser = result.getData();
            return convertToUserInfo(orgUser);

        } catch (Exception e) {
            log.error("Failed to load user by identifier={} tenant={}: {}", identifier, tenantId, e.getMessage(), e);
            return null;
        }
    }

    @Override
    public UserInfo loadUserById(Long userId) {
        if (userId == null) {
            return null;
        }

        // carlos-org 当前不暴露按 ID 查询的 Feign 端点，使用 identifier 降级
        // 注意：String.valueOf(userId) 不太可能匹配 account/phone/email，
        // 调用方应优先使用 loadUserByIdentifier()
        log.warn("loadUserById({}) called, but carlos-org has no getById endpoint; "
                + "falling back to identifier lookup (may return null)", userId);
        return loadUserByIdentifier(String.valueOf(userId));
    }

    @Override
    public List<String> getUserRoleCodes(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return new ArrayList<>(roleProvider.getRoleCodesByUserId(userId));
    }

    @Override
    public boolean verifyPassword(Long userId, String password) {
        if (userId == null || password == null || password.isBlank()) {
            return false;
        }

        UserInfo userInfo = loadUserById(userId);
        if (userInfo == null || userInfo.getPassword() == null) {
            return false;
        }

        return passwordEncoder.matches(password, userInfo.getPassword());
    }

    // =========================== 写操作 ===========================
    // 说明：
    //   - updateUserStatus: 通过 Feign 调用 carlos-org 同步数据库状态（P2 修复）
    //   - incrementLoginAttempts / resetLoginAttempts / updateLockTime:
    //     由 LoginAttemptManager (Redis) 管理，不再同步写入数据库
    //   - updatePassword / updateMfaStatus:
    //     密码修改由 carlos-org 自己的 /org/user/changePassword 端点处理；
    //     MFA 由 auth 模块 MfaManager 独立管理

    @Override
    public boolean updatePassword(Long userId, String newPassword) {
        // 密码修改应通过 carlos-org 的 /org/user/changePassword 端点处理
        log.debug("updatePassword() delegated to carlos-org; auth module does not proxy password changes");
        return false;
    }

    @Override
    public boolean updateUserStatus(Long userId, String status) {
        if (userId == null || status == null || status.isBlank()) {
            return false;
        }
        try {
            Integer stateCode = convertStatusToState(status);
            if (stateCode == null) {
                log.warn("Unknown status '{}' for userId={}, cannot sync to carlos-org", status, userId);
                return false;
            }
            com.carlos.org.api.pojo.param.ApiOrgUserChangeStateParam param =
                new com.carlos.org.api.pojo.param.ApiOrgUserChangeStateParam()
                    .setId(userId)
                    .setUserStatus(stateCode);
            apiOrgUser.changeState(param);
            log.info("User status synced to carlos-org: userId={}, status={} (code={})", userId, status, stateCode);
            return true;
        } catch (Exception e) {
            log.error("Failed to sync user status to carlos-org: userId={}, status={}", userId, status, e);
            return false;
        }
    }

    @Override
    public int incrementLoginAttempts(Long userId) {
        // 登录尝试次数由 LoginAttemptManager (Redis) 管理
        log.debug("incrementLoginAttempts() no-op: managed by LoginAttemptManager (Redis)");
        return 0;
    }

    @Override
    public void resetLoginAttempts(Long userId) {
        // 登录尝试次数由 LoginAttemptManager (Redis) 管理
        log.debug("resetLoginAttempts() no-op: managed by LoginAttemptManager (Redis)");
    }

    @Override
    public void updateLockTime(Long userId, LocalDateTime lockTime) {
        // 锁定时间由 LoginAttemptManager (Redis) 管理
        log.debug("updateLockTime() no-op: managed by LoginAttemptManager (Redis)");
    }

    @Override
    public boolean updateMfaStatus(Long userId, boolean mfaEnabled, String mfaSecret) {
        // MFA 状态由 auth 模块 MfaManager 独立管理
        log.debug("updateMfaStatus() no-op: MFA managed by auth MfaManager");
        return false;
    }

    // ==================== 私有转换方法 ====================

    /**
     * 将 carlos-org 的 OrgUserAO 转换为 auth 模块的 UserInfo
     *
     * @param orgUser carlos-org 用户数据
     * @return auth 模块内部用户信息
     */
    private UserInfo convertToUserInfo(OrgUserAO orgUser) {
        return UserInfo.builder()
            .userId(orgUser.getId())
            .username(orgUser.getAccount())
            .email(orgUser.getEmail())
            .phone(orgUser.getPhone())
            .password(orgUser.getPwd())
            .status(convertStateToStatus(orgUser.getUserStatus()))
            .pwdLastModify(orgUser.getPwdLastModify())
            .deptId(orgUser.getMainDeptId())
            .tenantId(orgUser.getTenantId())
            .roleCodes(null) // 懒加载，由调用方通过 getUserRoleCodes() 获取
            .authorities(null)
            .mfaEnabled(false)
            .build();
    }

    /**
     * 将 carlos-org 用户状态码转换为 auth 模块状态字符串
     *
     * @param state 状态码：0-禁用，1-启用，2-锁定
     * @return 状态字符串：DISABLE / ENABLE / LOCKED
     */
    private String convertStateToStatus(Integer state) {
        if (state == null) {
            return "ENABLE";
        }
        return switch (state) {
            case 0 -> "DISABLE";
            case 2 -> "LOCKED";
            default -> "ENABLE";
        };
    }

    /**
     * 将 auth 模块状态字符串转换为 carlos-org 状态码（P2 修复，用于写操作同步）
     *
     * @param status 状态字符串：ENABLE / DISABLE / LOCKED
     * @return 状态码：0-禁用，1-启用，2-锁定；无法识别时返回 null
     */
    private Integer convertStatusToState(String status) {
        if (status == null) {
            return null;
        }
        return switch (status.toUpperCase()) {
            case "DISABLE" -> 0;
            case "ENABLE" -> 1;
            case "LOCKED" -> 2;
            default -> null;
        };
    }
}
