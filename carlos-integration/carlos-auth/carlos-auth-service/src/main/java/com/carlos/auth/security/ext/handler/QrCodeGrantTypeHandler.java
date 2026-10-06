package com.carlos.auth.security.ext.handler;

import cn.hutool.core.util.StrUtil;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 扫码登录授权类型参数处理器
 *
 * @author Carlos
 * @since 3.0.0
 */
@Component
public class QrCodeGrantTypeHandler implements GrantTypeHandler {

    @Override
    public String getGrantType() {
        return "qr_code";
    }

    @Override
    public void validate(Map<String, String> params) throws OAuth2AuthenticationException {
        String qrToken = params.get("qr_token");

        if (StrUtil.isBlank(qrToken)) {
            throw new OAuth2AuthenticationException(
                new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST, "扫码令牌不能为空", ""));
        }
    }

    @Override
    public Object extractPrincipal(Map<String, String> params) {
        return params.get("qr_token");
    }

    @Override
    public Object extractCredentials(Map<String, String> params) {
        return null;
    }
}
