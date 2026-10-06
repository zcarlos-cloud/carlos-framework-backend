package com.carlos.auth.security.ext.handler;

import cn.hutool.core.util.StrUtil;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 邮箱验证码授权类型参数处理器
 *
 * @author Carlos
 * @since 3.0.0
 */
@Component
public class EmailCodeGrantTypeHandler implements GrantTypeHandler {

    @Override
    public String getGrantType() {
        return "email_code";
    }

    @Override
    public void validate(Map<String, String> params) throws OAuth2AuthenticationException {
        String email = params.get("email");
        String emailCode = params.get("email_code");

        if (StrUtil.isBlank(email)) {
            throw new OAuth2AuthenticationException(
                new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST, "邮箱不能为空", ""));
        }
        if (StrUtil.isBlank(emailCode)) {
            throw new OAuth2AuthenticationException(
                new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST, "邮箱验证码不能为空", ""));
        }
    }

    @Override
    public Object extractPrincipal(Map<String, String> params) {
        return params.get("email");
    }

    @Override
    public Object extractCredentials(Map<String, String> params) {
        return params.get("email_code");
    }
}
