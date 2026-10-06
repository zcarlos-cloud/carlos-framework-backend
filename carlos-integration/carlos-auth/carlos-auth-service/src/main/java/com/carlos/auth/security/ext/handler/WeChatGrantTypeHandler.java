package com.carlos.auth.security.ext.handler;

import cn.hutool.core.util.StrUtil;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 微信登录授权类型参数处理器
 *
 * @author Carlos
 * @version 3.0.0
 * @since 2026-05-19
 */
@Component
public class WeChatGrantTypeHandler implements GrantTypeHandler {

    @Override
    public String getGrantType() {
        return "wechat";
    }

    @Override
    public void validate(Map<String, String> params) throws OAuth2AuthenticationException {
        String code = params.get("code");
        if (StrUtil.isBlank(code)) {
            throw new OAuth2AuthenticationException(
                new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST, "微信授权码不能为空", ""));
        }
    }

    @Override
    public Object extractPrincipal(Map<String, String> params) {
        return params.get("code");
    }

    @Override
    public Object extractCredentials(Map<String, String> params) {
        return null;
    }
}
