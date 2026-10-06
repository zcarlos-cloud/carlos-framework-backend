package com.carlos.auth.security.ext;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.JakartaServletUtil;
import com.carlos.auth.oauth2.grant.CustomGrantTypes;
import com.carlos.auth.security.ext.handler.GrantTypeHandler;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * 扩展认证转换器
 *
 * <p>支持多种认证方式的请求转换，将 HTTP 请求转换为统一的 ExtendAuthenticationToken。</p>
 * <p><strong>注意：</strong>密码认证已通过表单登录（/login）端点实现，不再通过扩展授权流程处理。</p>
 *
 * <h3>支持的认证方式：</h3>
 * <ul>
 *   <li>sms_code - 短信验证码认证</li>
 *   <li>email_code - 邮箱验证码认证</li>
 *   <li>qr_code - 扫码认证</li>
 *   <li>social - 第三方登录</li>
 * </ul>
 *
 * <h3>扩展方式：</h3>
 * <p>如需支持更多认证方式，实现 {@link GrantTypeHandler} 接口并注册为 Spring Bean 即可自动识别。</p>
 *
 * @author Carlos
 * @version 3.0.0
 * @since 2026-04-08
 * @see ExtendAuthenticationToken
 * @see ExtendAuthenticationProvider
 * @see GrantTypeHandler
 */
@Slf4j
public class ExtendAuthenticationConverter implements AuthenticationConverter {

    /**
     * 支持的授权类型（已统一纳入标准 OAuth2 token 端点）
     */
    protected static final Set<String> SUPPORTED_GRANT_TYPES = new HashSet<>(Arrays.asList(
        CustomGrantTypes.PASSWORD.getValue(),
        CustomGrantTypes.SMS_CODE.getValue(),
        CustomGrantTypes.EMAIL_CODE.getValue(),
        CustomGrantTypes.QR_CODE.getValue(),
        CustomGrantTypes.SOCIAL.getValue()
    ));

    /**
     * 授权类型参数处理器映射
     */
    private final Map<String, GrantTypeHandler> handlerMap;

    public ExtendAuthenticationConverter() {
        this.handlerMap = Collections.emptyMap();
    }

    public ExtendAuthenticationConverter(List<GrantTypeHandler> handlers) {
        Map<String, GrantTypeHandler> map = new HashMap<>();
        if (!CollectionUtils.isEmpty(handlers)) {
            for (GrantTypeHandler handler : handlers) {
                map.put(handler.getGrantType(), handler);
                log.debug("Registered GrantTypeHandler: {}", handler.getGrantType());
            }
        }
        this.handlerMap = Collections.unmodifiableMap(map);
    }

    /**
     * 转换请求为认证令牌
     *
     * @param request HTTP 请求
     * @return 认证令牌，不支持返回 null
     * @throws OAuth2AuthenticationException 参数验证失败时抛出
     */
    @Override
    public Authentication convert(HttpServletRequest request) {
        // 提取请求参数
        Map<String, String> params = JakartaServletUtil.getParamMap(request);
        String grantType = params.get(OAuth2ParameterNames.GRANT_TYPE);

        // 检查是否支持
        if (!supports(grantType)) {
            log.debug("AuthenticationConverter [{}] does not support grant_type: {}",
                this.getClass().getSimpleName(), grantType);
            return null;
        }

        // 验证客户端已认证
        Authentication clientPrincipal = SecurityContextHolder.getContext().getAuthentication();
        if (clientPrincipal == null || !(clientPrincipal instanceof OAuth2ClientAuthenticationToken clientAuth)) {
            log.warn("Client authentication not found or invalid");
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_CLIENT);
        }

        // 提取 scope
        Set<String> scopes = extractScopes(params);

        // 获取并执行对应的处理器
        GrantTypeHandler handler = handlerMap.get(grantType);
        if (handler != null) {
            handler.validate(params);
        } else {
            // 没有专用处理器时使用默认验证
            log.warn("No GrantTypeHandler found for grant_type: {}, using default validation", grantType);
        }

        // 提取认证信息
        Object principal = extractPrincipal(params, grantType);
        Object credentials = extractCredentials(params, grantType);

        if (principal == null || (principal instanceof String s && StrUtil.isBlank(s))) {
            log.warn("Principal is empty for grant_type: {}", grantType);
            throw new OAuth2AuthenticationException(
                new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST, "认证主体不能为空", ""));
        }

        // 构建令牌
        ExtendAuthenticationToken token = new ExtendAuthenticationToken(
            principal,
            credentials,
            new AuthorizationGrantType(grantType),
            clientAuth,
            scopes,
            params
        );

        log.debug("Converted request to ExtendAuthenticationToken: grant_type={}, principal={}",
            grantType, principal);

        return token;
    }

    /**
     * 判断是否支持指定的授权类型
     *
     * @param grantType 授权类型
     * @return true-支持，false-不支持
     */
    protected boolean supports(String grantType) {
        return grantType != null && SUPPORTED_GRANT_TYPES.contains(grantType);
    }

    /**
     * 提取授权范围
     *
     * @param params 请求参数
     * @return 授权范围集合
     */
    protected Set<String> extractScopes(Map<String, String> params) {
        String scope = params.get(OAuth2ParameterNames.SCOPE);
        if (!StringUtils.hasText(scope)) {
            return new HashSet<>();
        }
        return new HashSet<>(Arrays.asList(StringUtils.delimitedListToStringArray(scope, " ")));
    }

    /**
     * 提取认证主体
     *
     * @param params 请求参数
     * @param grantType 授权类型
     * @return 认证主体
     */
    protected Object extractPrincipal(Map<String, String> params, String grantType) {
        GrantTypeHandler handler = handlerMap.get(grantType);
        if (handler != null) {
            return handler.extractPrincipal(params);
        }
        return params.get("username");
    }

    /**
     * 提取凭证
     *
     * @param params 请求参数
     * @param grantType 授权类型
     * @return 凭证
     */
    protected Object extractCredentials(Map<String, String> params, String grantType) {
        GrantTypeHandler handler = handlerMap.get(grantType);
        if (handler != null) {
            return handler.extractCredentials(params);
        }
        return null;
    }

    /**
     * 获取支持的授权类型
     *
     * @return 授权类型集合
     */
    public Set<String> getSupportedGrantTypes() {
        return new HashSet<>(SUPPORTED_GRANT_TYPES);
    }
}
