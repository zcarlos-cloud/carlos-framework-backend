package com.carlos.auth.provider;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 *
 * <p>
 * 用户信息DTO
 * </p>
 *
 * <p>包含用户基本信息、角色、权限等</p>
 *
 * @author Carlos
 * @date 2026-02-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户信息DTO")
public class UserInfo {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "密码（加密后）")
    private String password;

    @Schema(description = "用户状态: ENABLE-启用、DISABLE-禁用、LOCKED-锁定")
    private String status;

    @Schema(description = "登录失败次数")
    private Integer loginAttempts;

    @Schema(description = "锁定时间")
    private LocalDateTime lockTime;

    @Schema(description = "密码最后修改时间")
    private LocalDateTime pwdLastModify;

    @Schema(description = "MFA是否启用")
    private Boolean mfaEnabled;

    @Schema(description = "MFA密钥")
    private String mfaSecret;

    @Schema(description = "角色编码列表")
    private List<String> roleCodes;

    @Schema(description = "权限列表")
    private List<String> authorities;

    @Schema(description = "部门ID")
    private Long deptId;

    @Schema(description = "租户ID")
    private Long tenantId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /**
     * 判断账号是否已锁定
     *
     * @return true-已锁定，false-未锁定
     */
    public boolean isAccountLocked() {
        return "LOCKED".equals(this.status);
    }

    /**
     * 判断账号是否启用
     *
     * @return true-已启用，false-未启用
     */
    public boolean isAccountEnabled() {
        return "ENABLE".equals(this.status);
    }

    /**
     * 判断MFA是否启用
     *
     * @return true-已启用，false-未启用
     */
    public boolean isMfaEnabled() {
        return Boolean.TRUE.equals(this.mfaEnabled);
    }
}
