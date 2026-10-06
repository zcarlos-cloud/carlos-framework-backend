package com.carlos.auth.security.ext.handler;

import cn.hutool.core.util.StrUtil;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 第三方登录授权类型参数处理器
 *
 * @author Carlos
 * @since 3.0.0
 */
@Component
public class SocialGrantTypeHandler implements GrantTypeHandler {

    @Override
    public String getGrantType() {
        return "social";
    }

    @Override
    public void validate(Map<String, String> params) throws OAuth2AuthenticationException {
        String socialType = params.get("social_type");
        String socialCode = params.get("social_code");

        if (StrUtil.isBlank(socialType)) {
            throw new OAuth2AuthenticationException(
                new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST, "第三方类型不能为空", ""));
        }
        if (StrUtil.isBlank(socialCode)) {
            throw new OAuth2AuthenticationException(
                new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST, "第三方授权码不能为空", ""));
        }
    }

    @Override
    public Object extractPrincipal(Map<String, String> params) {
        String socialType = params.get("social_type");
        String socialCode = params.get("social_code");
        return socialType + ":" + socialCode;
    }

    @Override
    public Object extractCredentials(Map<String, String> params) {
        return null;
    }
}
