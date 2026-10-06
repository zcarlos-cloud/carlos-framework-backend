package com.carlos.auth.idp;

import com.carlos.auth.oauth2.grant.CustomGrantTypes;
import com.carlos.auth.security.TenantLoginChecker;
import com.carlos.auth.security.password.PasswordProperties;
import com.carlos.core.response.Result;
import com.carlos.org.api.ApiOrgUser;
import com.carlos.org.api.pojo.ao.OrgUserAO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;

/**
 * 本地身份源提供者
 *
 * <p>支持用户名密码登录认证。从 carlos-org 获取用户信息，密码校验统一在 auth 模块完成。</p>
 *
 * <h3>密码迁移机制：</h3>
 * <p>当用户使用遗留格式密码（SM4 对称加密、明文）登录成功后，自动用 Argon2id 重新哈希并
 * 通过 {@link ApiOrgUser} 回写 org 模块，逐步消除对称加密存储风险。</p>
 *
 * @author Carlos
 * @version 3.0.0
 * @since 2026-04-14
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocalIdentityProvider implements IdentityProvider {

    private final PasswordEncoder passwordEncoder;
    private final ApiOrgUser apiOrgUser;
    private final RoleProvider roleProvider;
    private final PasswordProperties passwordProperties;

    @Override
    public String getProviderId() {
        return "local";
    }

    @Override
    public String getProviderType() {
        return "local";
    }

    @Override
    public Set<String> getSupportedGrantTypes() {
        return Collections.singleton(CustomGrantTypes.PASSWORD.getValue());
    }

    @Override
    public UserIdentity authenticate(IdentityProviderRequest request) {
        String username = (String) request.getPrincipal();
        String password = (String) request.getCredentials();

        // S2 租户管理：开启租户校验时 additionalParameters 携带已校验的 tenantId，
        // 在目标租户内定位用户；否则维持原查询（org 侧租户拦截器默认过滤平台租户 0）
        Long tenantId = resolveTenantId(request);
        Result<OrgUserAO> userResult = tenantId != null
            ? apiOrgUser.getUserByIdentifierAndTenant(username, tenantId)
            : apiOrgUser.getUserByIdentifier(username);

        if (!userResult.isSuccess() || userResult.getData() == null) {
            throw new BadCredentialsException("用户名或密码错误");
        }

        OrgUserAO orgUser = userResult.getData();
        String encodedPwd = orgUser.getPwd();

        if (encodedPwd == null || encodedPwd.isBlank()) {
            log.warn("用户 [{}] 密码为空，拒绝登录", username);
            throw new BadCredentialsException("用户名或密码错误");
        }

        // 统一使用 DelegatingPasswordEncoder 校验，支持 argon2/bcrypt/sm4 多前缀
        boolean matched = passwordEncoder.matches(password, encodedPwd);

        if (!matched) {
            throw new BadCredentialsException("用户名或密码错误");
        }

        // 检测是否需要密码升级（SM4 / 明文 / 旧 bcrypt → Argon2id）
        if (passwordProperties.isSm4MigrationEnabled() && passwordEncoder.upgradeEncoding(encodedPwd)) {
            upgradePasswordToArgon2(orgUser, password);
        }

        return convertToUserIdentity(orgUser);
    }

    /**
     * 将密码升级为 Argon2id 并回写
     *
     * <p>异步处理失败不影响登录，仅记录告警日志。</p>
     *
     * @param orgUser     用户信息
     * @param rawPassword 明文密码（已通过校验）
     */
    private void upgradePasswordToArgon2(OrgUserAO orgUser, String rawPassword) {
        try {
            String newHash = passwordEncoder.encode(rawPassword);
            apiOrgUser.updatePassword(orgUser.getId(), newHash);
            log.info("用户 [{}] 密码已从遗留格式迁移到 Argon2id", orgUser.getAccount());
        } catch (Exception e) {
            log.warn("用户 [{}] 密码迁移失败，将在下次登录时重试: {}", orgUser.getAccount(), e.getMessage());
        }
    }

    /**
     * 从认证请求的扩展参数中解析租户ID
     *
     * @param request 认证请求
     * @return 租户ID，未携带时返回 null
     */
    private Long resolveTenantId(IdentityProviderRequest request) {
        if (request.getAdditionalParameters() == null) {
            return null;
        }
        String tenantId = request.getAdditionalParameters().get(TenantLoginChecker.PARAM_TENANT_ID);
        if (tenantId == null || tenantId.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(tenantId);
        } catch (NumberFormatException e) {
            log.warn("非法的租户ID参数: {}", tenantId);
            return null;
        }
    }

    private UserIdentity convertToUserIdentity(OrgUserAO user) {
        Set<String> roleCodes;
        try {
            roleCodes = roleProvider.getRoleCodesByUserId(user.getId());
        } catch (Exception e) {
            log.warn("Failed to load roles for user {}: {}", user.getId(), e.getMessage());
            roleCodes = Collections.emptySet();
        }

        // 将org模块状态码转换为auth模块状态字符串
        String status = convertStateToStatus(user.getUserStatus());

        return UserIdentity.builder()
            .providerId(getProviderId())
            .providerUserId(String.valueOf(user.getId()))
            .username(user.getAccount())
            .email(user.getEmail())
            .phone(user.getPhone())
            .status(status)
            .pwdLastModify(user.getPwdLastModify())
            .roleCodes(roleCodes)
            .tenantId(user.getTenantId())
            .isNewUser(false)
            .build();
    }

    /**
     * 将org模块用户状态码转换为auth模块状态字符串
     *
     * @param state 状态码：0-禁用，1-启用，2-锁定
     * @return 状态字符串
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
}
