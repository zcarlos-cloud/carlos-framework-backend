package com.carlos.auth.oauth2.grant;

import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * 自定义授权对象
 * </p>
 *
 * @author Carlos
 * @date 2022/11/9 15:47
 */
@Getter
@Setter
public abstract class BaseAuthenticationToken extends AbstractAuthenticationToken {

    /**
     * 授权类型
     */
    private AuthorizationGrantType grantType;

    /**
     * 客户端信息
     */
    private Authentication client;

    /**
     * scope
     */
    private Set<String> scopes;

    /**
     * 参数
     */
    private Map<String, String> parameters;


    public BaseAuthenticationToken(AuthorizationGrantType grantType,
                                   Authentication client,
                                   @Nullable Set<String> scopes,
                                   @Nullable Map<String, String> parameters) {
        super(Collections.emptyList());
        Assert.notNull(grantType, "authorizationGrantType cannot be null");
        Assert.notNull(client, "clientPrincipal cannot be null");
        this.grantType = grantType;
        this.client = client;
        this.scopes = Set.copyOf(scopes != null ? scopes : Collections.emptySet());
        this.parameters = Map.copyOf(parameters != null ? parameters : Collections.emptyMap());
    }

    /**
     * 扩展模式一般不需要密码
     */
    @Override
    public Object getCredentials() {
        return "";
    }

    /**
     * 获取用户名
     */
    @Override
    public Object getPrincipal() {
        return this.client;
    }

}
