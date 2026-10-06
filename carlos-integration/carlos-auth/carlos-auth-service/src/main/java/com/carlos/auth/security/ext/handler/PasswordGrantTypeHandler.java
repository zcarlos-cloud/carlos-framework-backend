package com.carlos.auth.security.ext.handler;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * 密码授权类型参数处理器
 *
 * @author Carlos
 * @since 3.0.0
 */
@Component
public class PasswordGrantTypeHandler implements GrantTypeHandler {

    @Override
    public String getGrantType() {
        return "password";
    }

    @Override
    public void validate(Map<String, String> params) throws OAuth2AuthenticationException {
        String username = params.get("username");
        String password = params.get("password");

        if (!StringUtils.hasText(username)) {
            throw new OAuth2AuthenticationException(
                new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST, "用户名不能为空", ""));
        }
        if (!StringUtils.hasText(password)) {
            throw new OAuth2AuthenticationException(
                new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST, "密码不能为空", ""));
        }
    }

    @Override
    public Object extractPrincipal(Map<String, String> params) {
        return params.get("username");
    }

    @Override
    public Object extractCredentials(Map<String, String> params) {
        return params.get("password");
    }
}
