package com.carlos.auth.oauth2.repository;

import cn.hutool.core.util.StrUtil;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

public class AuthorizationGrantTypeDeserializer
    extends ValueDeserializer<AuthorizationGrantType> {

    @Override
    public AuthorizationGrantType deserialize(
        JsonParser jsonParser,
        DeserializationContext context
    ) {
        // 从JSON中读取授权类型字符串
        String grantTypeValue = jsonParser.getValueAsString();
        if (StrUtil.isBlank(grantTypeValue)) {
            return null;
        }

        // 匹配标准授权类型
        if (AuthorizationGrantType.AUTHORIZATION_CODE.getValue().equals(grantTypeValue)) {
            return AuthorizationGrantType.AUTHORIZATION_CODE;
        } else if (AuthorizationGrantType.CLIENT_CREDENTIALS.getValue().equals(grantTypeValue)) {
            return AuthorizationGrantType.CLIENT_CREDENTIALS;
        } else if (AuthorizationGrantType.REFRESH_TOKEN.getValue().equals(grantTypeValue)) {
            return AuthorizationGrantType.REFRESH_TOKEN;
        }

        // 处理自定义授权类型（如设备码流程）
        return new AuthorizationGrantType(grantTypeValue);
    }
}
