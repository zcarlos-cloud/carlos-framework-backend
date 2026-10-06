package com.carlos.auth.oauth2.repository;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

// 定义 Mixin 类解决 AuthorizationGrantType 反序列化
// 注意：不使用 Visibility.ANY，仅通过 @JsonCreator 自定义反序列化
public abstract class AuthorizationGrantTypeMixin {
    @JsonCreator
    public static AuthorizationGrantType create(@JsonProperty("value") String value) {
        return new AuthorizationGrantType(value);
    }
}
