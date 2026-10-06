package com.carlos.auth.idp;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.carlos.auth.oauth2.grant.CustomGrantTypes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * 社交登录统一身份源提供者
 *
 * <p>处理 {@code grant_type=social} 的通用第三方登录请求，根据 {@code social_type}
 * 分发给对应的 {@link IdentityProvider}（钉钉、微信等）。</p>
 *
 * <p>principal 格式：{@code social_type:social_code}，例如 {@code dingtalk:abc123}</p>
 *
 * @author Carlos
 * @version 3.0.0
 * @since 2026-05-19
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SocialIdentityProvider implements IdentityProvider {

    @Override
    public String getProviderId() {
        return "social";
    }

    @Override
    public String getProviderType() {
        return "oauth2";
    }

    @Override
    public Set<String> getSupportedGrantTypes() {
        return Collections.singleton(CustomGrantTypes.SOCIAL.getValue());
    }

    @Override
    public UserIdentity authenticate(IdentityProviderRequest request) {
        String principal = (String) request.getPrincipal();
        if (StrUtil.isBlank(principal)) {
            throw new BadCredentialsException("第三方登录参数为空");
        }

        // 解析 social_type:social_code
        String socialType;
        String socialCode;
        int idx = principal.indexOf(':');
        if (idx > 0) {
            socialType = principal.substring(0, idx);
            socialCode = principal.substring(idx + 1);
        } else {
            // 尝试从 additionalParameters 获取
            socialType = request.getAdditionalParameters() != null
                ? request.getAdditionalParameters().get("social_type")
                : null;
            socialCode = principal;
        }

        if (StrUtil.isBlank(socialType)) {
            throw new BadCredentialsException("第三方登录类型不能为空");
        }
        if (StrUtil.isBlank(socialCode)) {
            throw new BadCredentialsException("第三方授权码不能为空");
        }

        log.info("Social login dispatch: type={}, code={}", socialType, socialCode);

        IdentityProviderRegistry providerRegistry = SpringUtil.getBean(IdentityProviderRegistry.class);
        IdentityProvider provider = providerRegistry.findByProviderId(socialType);
        if (provider == null) {
            throw new BadCredentialsException("不支持的第三方登录类型: " + socialType);
        }

        // 委托给具体 provider 处理
        IdentityProviderRequest delegatedRequest = IdentityProviderRequest.builder()
            .principal(socialCode)
            .credentials(request.getCredentials())
            .clientId(request.getClientId())
            .additionalParameters(request.getAdditionalParameters())
            .build();

        return provider.authenticate(delegatedRequest);
    }

    @Override
    public String buildAuthorizeUrl(String state, String redirectUri) {
        throw new UnsupportedOperationException(
            "社交登录统一入口不支持直接生成授权 URL，请使用具体身份源的 buildAuthorizeUrl");
    }

    @Override
    public UserIdentity handleCallback(Map<String, String> callbackParams) {
        throw new UnsupportedOperationException(
            "社交登录统一入口不支持直接处理回调，请使用具体身份源的 handleCallback");
    }
}
