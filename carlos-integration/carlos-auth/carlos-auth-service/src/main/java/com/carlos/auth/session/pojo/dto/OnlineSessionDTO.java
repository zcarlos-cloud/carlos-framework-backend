package com.carlos.auth.session.pojo.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 在线用户会话 数据传输对象（Manager → Service）
 * </p>
 *
 * @author carlos
 * @since 3.0.0
 */
@Data
@Accessors(chain = true)
public class OnlineSessionDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 会话ID（授权ID） */
    private String sessionId;

    /** 用户名 */
    private String username;

    /** 用户ID */
    private Long userId;

    /** 登录客户端 */
    private String clientId;

    /** 授权类型 */
    private String grantType;

    /** 登录IP（会话元数据，存量会话可能为空） */
    private String ip;

    /** User-Agent（会话元数据，存量会话可能为空） */
    private String userAgent;

    /** 登录时间（access token 签发时间） */
    private LocalDateTime loginTime;

    /** 会话过期时间（取刷新令牌过期时间，无则访问令牌过期时间） */
    private LocalDateTime expireTime;

}
