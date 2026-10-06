package com.carlos.auth.login.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * <p>
 * 前端用户信息响应
 * </p>
 *
 * <p>适配 SoybeanAdmin 前端框架的 <code>Api.Auth.UserInfo</code> 类型</p>
 *
 * @author Carlos
 * @date 2026-05-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "前端用户信息响应")
public class UserInfoResponse {

    /**
     * 用户ID
     */
    @Schema(description = "用户ID", example = "1001")
    private Long userId;

    /**
     * 用户名
     */
    @Schema(description = "用户名", example = "admin")
    private String userName;

    /**
     * 角色编码列表
     */
    @Schema(description = "角色编码列表", example = "[\"ADMIN\", \"USER\"]")
    private List<String> roles;

    /**
     * 按钮权限编码列表
     */
    @Schema(description = "按钮权限编码列表", example = "[\"btn:add\", \"btn:edit\"]")
    private List<String> buttons;
}
