package com.carlos.gateway.oauth2.provider;

import com.carlos.core.constant.RedisKeys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Set;

/**
 * <p>
 * 默认权限提供者实现
 * 从 Redis 获取用户权限
 * </p>
 *
 * @author carlos
 * @date 2026/3/16
 */
@Slf4j
public class DefaultPermissionProvider implements PermissionProvider {

    private final ReactiveStringRedisTemplate redisTemplate;

    public DefaultPermissionProvider(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Set<String>> getUserPermissions(String userId) {
        String key = RedisKeys.AUTH_PERMISSIONS + userId;
        return redisTemplate.opsForSet()
            .members(key)
            .collectList()
            .map(members -> {
                Set<String> permissions = new java.util.HashSet<>(members);
                log.debug("User {} permissions: {}", userId, permissions);
                return permissions;
            })
            .switchIfEmpty(Mono.just(new java.util.HashSet<>()));
    }

    @Override
    public Mono<Boolean> evaluateAbacPolicy(Map<String, Object> attributes) {
        // 简化的 ABAC 评估：基于时间和部门的策略示例
        Long tenantId = (Long) attributes.get("tenantId");
        Long deptId = (Long) attributes.get("deptId");
        String path = (String) attributes.get("path");

        log.debug("Evaluating ABAC policy for tenant: {}, dept: {}, path: {}",
            tenantId, deptId, path);

        // 这里可以实现复杂的策略评估逻辑
        // 例如：检查用户是否有权限访问特定部门的资源

        return Mono.just(true);
    }
}
