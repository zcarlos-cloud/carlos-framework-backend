package com.carlos.auth.api.pojo.ao;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * Token 检查响应 AO
 *
 * <p>标准化 /auth/token/checkToken 接口的响应结构，供 Feign 调用方解析。</p>
 *
 * @author Carlos
 * @version 3.0.0
 * @since 2026-04-29
 */
@Data
@Builder
@Schema(description = "Token 检查响应")
public class CheckTokenAO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Token 是否有效
     */
    @Schema(description = "Token 是否有效", example = "true")
    private boolean active;

    /**
     * Token 类型：user_token / client_token
     */
    @Schema(description = "Token 类型", example = "user_token")
    private String tokenType;

    /**
     * 用户ID（user_token 时有值）
     */
    @Schema(description = "用户ID", example = "1001")
    private Long userId;

    /**
     * 用户名（user_token 时有值）
     */
    @Schema(description = "用户名", example = "admin")
    private String username;

    /**
     * 租户ID
     */
    @Schema(description = "租户ID", example = "1")
    private Long tenantId;

    /**
     * 部门ID
     */
    @Schema(description = "部门ID", example = "10")
    private Long deptId;

    /**
     * 角色ID列表
     */
    @Schema(description = "角色ID列表", example = "[1, 2]")
    private Set<Long> roleIds;

    /**
     * 权限列表
     */
    @Schema(description = "权限列表", example = "[\"sys:user:list\"]")
    private List<String> authorities;

    /**
     * 客户端ID
     */
    @Schema(description = "客户端ID", example = "web-client")
    private String clientId;

    /**
     * 授权范围
     */
    @Schema(description = "授权范围", example = "[\"read\", \"write\"]")
    private Set<String> scopes;

    /**
     * 剩余有效期（秒）
     */
    @Schema(description = "剩余有效期（秒）", example = "7200")
    private Long expiresIn;

    /**
     * 签发时间（时间戳）
     */
    @Schema(description = "签发时间", example = "1710638258000")
    private Long issuedAt;

    /**
     * 过期时间（时间戳）
     */
    @Schema(description = "过期时间", example = "1710645458000")
    private Long expiresAt;

    /**
     * 错误信息（Token 无效时）
     */
    @Schema(description = "错误信息", example = "Token 已过期")
    private String error;
}
