package com.carlos.auth.sso.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SSO Token 交换响应
 *
 * @author Carlos
 * @version 3.0.0
 * @since 2026-05-19
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "SSO Token 交换响应")
public class SsoTokenExchangeVO {

    /**
     * 访问令牌
     */
    @Schema(description = "访问令牌", example = "eyJhbGciOiJSUzI1NiJ9...")
    private String accessToken;

    /**
     * 刷新令牌
     */
    @Schema(description = "刷新令牌", example = "eyJhbGciOiJSUzI1NiJ9...")
    private String refreshToken;

    /**
     * 令牌类型
     */
    @Schema(description = "令牌类型", example = "Bearer")
    private String tokenType;

    /**
     * 过期时间（秒）
     */
    @Schema(description = "过期时间（秒）", example = "7200")
    private Long expiresIn;

    /**
     * 授权范围
     */
    @Schema(description = "授权范围", example = "read write")
    private String scope;

    /**
     * 目标客户端ID
     */
    @Schema(description = "目标客户端ID", example = "carlos-app")
    private String targetClientId;

    /**
     * 用户名
     */
    @Schema(description = "用户名", example = "admin")
    private String username;
}
