package com.carlos.auth.session.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 在线用户会话 视图对象
 * </p>
 *
 * @author carlos
 * @since 3.0.0
 */
@Data
@Accessors(chain = true)
@Schema(description = "在线用户会话")
public class OnlineUserVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "会话ID（授权ID，踢下线用）")
    private String sessionId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "登录客户端")
    private String clientId;

    @Schema(description = "授权类型")
    private String grantType;

    @Schema(description = "登录IP（签发时写入，存量会话可能为空）")
    private String ip;

    @Schema(description = "User-Agent（签发时写入，存量会话可能为空）")
    private String userAgent;

    @Schema(description = "登录时间（access token 签发时间）")
    private LocalDateTime loginTime;

    @Schema(description = "会话过期时间（取刷新令牌过期时间，无则访问令牌过期时间）")
    private LocalDateTime expireTime;

}
