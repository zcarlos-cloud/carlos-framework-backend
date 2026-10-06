package com.carlos.auth.security.ext.handler;

import cn.hutool.core.util.StrUtil;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 短信验证码授权类型参数处理器
 *
 * @author Carlos
 * @since 3.0.0
 */
@Component
public class SmsCodeGrantTypeHandler implements GrantTypeHandler {

    @Override
    public String getGrantType() {
        return "sms_code";
    }

    @Override
    public void validate(Map<String, String> params) throws OAuth2AuthenticationException {
        String phone = params.get("phone");
        String smsCode = params.get("sms_code");

        if (StrUtil.isBlank(phone)) {
            throw new OAuth2AuthenticationException(
                new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST, "手机号不能为空", ""));
        }
        if (StrUtil.isBlank(smsCode)) {
            throw new OAuth2AuthenticationException(
                new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST, "短信验证码不能为空", ""));
        }
        if (!isValidPhone(phone)) {
            throw new OAuth2AuthenticationException(
                new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST, "手机号格式不正确", ""));
        }
    }

    @Override
    public Object extractPrincipal(Map<String, String> params) {
        return params.get("phone");
    }

    @Override
    public Object extractCredentials(Map<String, String> params) {
        return params.get("sms_code");
    }

    /**
     * 验证手机号格式（中国大陆）
     */
    private boolean isValidPhone(String phone) {
        if (StrUtil.isBlank(phone)) {
            return false;
        }
        return phone.matches("^1[3-9]\\d{9}$");
    }
}
