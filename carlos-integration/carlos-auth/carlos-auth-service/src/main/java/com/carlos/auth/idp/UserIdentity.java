package com.carlos.auth.idp;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

/**
 * 统一用户身份信息
 *
 * <p>无论用户来自哪个身份源，最终都归一化为该对象。</p>
 *
 * @author Carlos
 * @version 3.0.0
 * @since 2026-04-14
 */
@Data
@Builder
public class UserIdentity {

    /**
     * 身份源唯一标识
     */
    private String providerId;

    /**
     * 身份源中的用户唯一标识
     */
    private String providerUserId;

    /**
     * 映射到本系统的用户名
     */
    private String username;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 用户状态: ENABLE-启用、DISABLE-禁用、LOCKED-锁定
     */
    private String status;

    /**
     * 密码最后修改时间
     */
    private LocalDateTime pwdLastModify;

    /**
     * 角色编码集合
     */
    private Set<String> roleCodes;

    /**
     * 租户ID（S2 租户管理：来自 org_user.tenant_id，用于 JWT tenant_id claim）
     */
    private Long tenantId;

    /**
     * 扩展属性（如钉钉的 deptId、SAML 的部门等）
     */
    private Map<String, Object> extraAttrs;

    /**
     * 是否为新用户（需要自动注册）
     */
    private boolean isNewUser;
}
