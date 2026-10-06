package com.carlos.auth.oauth2.grant;

import cn.hutool.extra.servlet.JakartaServletUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 自定义登录信息转换器
 * </p>
 *
 * @author carlos
 * @date 2022/11/4 17:39
 */
public abstract class BaseAuthenticationConverter<T extends BaseAuthenticationToken> implements AuthenticationConverter {

    /**
     * 是否支持此convert
     *
     * @param grantType 授权类型
     */
    public abstract boolean support(String grantType);

    /**
     * 校验参数
     *
     * @param request 请求
     */
    public void checkParams(Map<String, String> request) {

    }


    /**
     * buildToken
     *
     * @param clientPrincipal      参数0
     * @param requestedScopes      参数1
     * @param additionalParameters 参数2
     * @return T
     * @author Carlos
     * @date 2022/11/9 18:48
     */
    public abstract T buildToken(Authentication clientPrincipal, Set<String> requestedScopes,
                                 Map<String, String> additionalParameters);

    @Override
    public Authentication convert(HttpServletRequest request) {

        // grant_type (REQUIRED)
        Map<String, String> params = JakartaServletUtil.getParamMap(request);
        String grantType = params.get(OAuth2ParameterNames.GRANT_TYPE);
        if (!support(grantType)) {
            return null;
        }

        // scope (OPTIONAL)
        String scope = params.get(OAuth2ParameterNames.SCOPE);
        // if (StringUtils.hasText(scope) && parameters.get(OAuth2ParameterNames.SCOPE).size() != 1) {
        //     throwError(OAuth2ErrorCodes.INVALID_REQUEST, OAuth2ParameterNames.SCOPE,
        //             OAuth2EndpointUtils.ACCESS_TOKEN_REQUEST_ERROR_URI);
        // }

        Set<String> requestedScopes = null;
        if (StringUtils.hasText(scope)) {
            requestedScopes = new HashSet<>(Arrays.asList(StringUtils.delimitedListToStringArray(scope, " ")));
        }

        // 校验个性化参数
        this.checkParams(params);

        // 获取当前已经认证的客户端信息
        Authentication clientPrincipal = SecurityContextHolder.getContext().getAuthentication();
        if (clientPrincipal == null) {
            throwError(OAuth2ErrorCodes.INVALID_REQUEST, OAuth2ErrorCodes.INVALID_CLIENT,
                "");
        }

        // 扩展信息
        Map<String, String> additionalParameters = params.entrySet()
            .stream()
            .filter(e ->
                !e.getKey().equals(OAuth2ParameterNames.GRANT_TYPE)
                    && !e.getKey().equals(OAuth2ParameterNames.SCOPE))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        // 创建token
        return buildToken(clientPrincipal, requestedScopes, additionalParameters);

    }

    public void throwError(String errorCode, String parameterName, String errorUri) {
        OAuth2Error error = new OAuth2Error(errorCode, "OAuth 2.0 Parameter: " + parameterName, errorUri);
        throw new OAuth2AuthenticationException(error);
    }

}
