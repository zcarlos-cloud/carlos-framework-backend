package com.carlos.gateway.oauth2.provider;

import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Set;

/**
 * 权限提供者接口
 */
public interface PermissionProvider {

    /**
     * 获取用户权限列表
     */
    Mono<Set<String>> getUserPermissions(String userId);

    /**
     * 评估 ABAC 策略
     */
    Mono<Boolean> evaluateAbacPolicy(Map<String, Object> attributes);
}
