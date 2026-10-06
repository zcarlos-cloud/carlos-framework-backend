package com.carlos.auth.idp;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 身份源认证请求上下文
 *
 * @author Carlos
 * @version 3.0.0
 * @since 2026-04-14
 */
@Data
@Builder
public class IdentityProviderRequest {

    /**
     * 认证主体（用户名、手机号、邮箱等）
     */
    private Object principal;

    /**
     * 凭证（密码、验证码等）
     */
    private Object credentials;

    /**
     * 客户端 ID
     */
    private String clientId;

    /**
     * 扩展参数
     */
    private Map<String, String> additionalParameters;
}
