package com.carlos.auth.idp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 身份源注册中心
 *
 * <p>统一管理所有 {@link IdentityProvider} 实例，支持按 providerId 或 grantType 查找。</p>
 *
 * @author Carlos
 * @version 3.0.0
 * @since 2026-04-14
 */
@Slf4j
@Component
public class IdentityProviderRegistry {

    private final Map<String, IdentityProvider> providerMap = new ConcurrentHashMap<>();

    public IdentityProviderRegistry(List<IdentityProvider> providers) {
        if (providers != null) {
            for (IdentityProvider provider : providers) {
                register(provider);
            }
        }
        log.info("Initialized IdentityProviderRegistry with {} providers", providerMap.size());
    }

    /**
     * 注册身份源提供者
     *
     * @param provider 身份源提供者
     */
    public void register(IdentityProvider provider) {
        providerMap.put(provider.getProviderId(), provider);
        log.info("Registered IdentityProvider: {}, type={}, grantTypes={}",
            provider.getProviderId(), provider.getProviderType(), provider.getSupportedGrantTypes());
    }

    /**
     * 根据授权类型查找身份源提供者
     *
     * @param grantType 授权类型，如 password、sms_code
     * @return 身份源提供者
     * @throws IllegalArgumentException 未找到时抛出
     */
    public IdentityProvider findByGrantType(String grantType) {
        return providerMap.values().stream()
            .filter(p -> p.getSupportedGrantTypes().contains(grantType))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("不支持的认证方式: " + grantType));
    }

    /**
     * 根据身份源标识查找提供者
     *
     * @param providerId 身份源标识，如 local、dingtalk
     * @return 身份源提供者，不存在返回 null
     */
    public IdentityProvider findByProviderId(String providerId) {
        return providerMap.get(providerId);
    }
}
