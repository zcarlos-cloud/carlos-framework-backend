package com.carlos.auth.idp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;

/**
 * 默认角色提供者实现
 *
 * <p>默认返回空角色集合。生产环境应提供自定义实现，从权限中心加载用户角色。</p>
 *
 * <p><strong>注意：</strong>此实现仅作为占位符，确保系统在没有角色服务时也能正常运行。
 * 建议在启动日志中明确提示未配置角色提供者。</p>
 *
 * @author Carlos
 * @version 3.0.0
 * @since 2026-04-29
 * @see RoleProvider
 */
@Slf4j
@Component
@ConditionalOnMissingBean(RoleProvider.class)
public class DefaultRoleProvider implements RoleProvider {

    public DefaultRoleProvider() {
        log.warn("=================================================================");
        log.warn(" Using DefaultRoleProvider - returning empty role sets!          ");
        log.warn("=================================================================");
        log.warn("Please implement RoleProvider for production use!");
        log.warn("Roles are required for proper authorization control.");
        log.warn("=================================================================");
    }

    @Override
    public Set<String> getRoleCodesByUserId(Long userId) {
        return Collections.emptySet();
    }

    @Override
    public Set<String> getRoleCodesByIdentifier(String identifier) {
        return Collections.emptySet();
    }
}
