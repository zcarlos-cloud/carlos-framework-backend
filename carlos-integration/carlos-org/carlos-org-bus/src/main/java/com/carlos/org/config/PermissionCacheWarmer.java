package com.carlos.org.config;

import com.carlos.org.manager.OrgRoleManager;
import com.carlos.org.pojo.dto.OrgRoleDTO;
import com.carlos.security.permission.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * 角色权限缓存预热器
 * </p>
 *
 * <p>启动时把每个启用角色的权限码写入共享权限缓存（Redis {@code auth:permissions:{roleCode}}）。</p>
 *
 * <p><b>为什么需要预热：</b>权限缓存以角色编码为 key，由 Redis 承载并在各服务间共享，但
 * 「无 DB 委托」的下游服务（如 carlos-system）在 Redis 未命中时只能得到空集合 ——
 * 于是菜单过滤、按钮鉴权全部失效。授权变更路径已由
 * {@link com.carlos.org.service.OrgRoleService#assignPermissions} 写通，
 * 但历史数据、直接改库或其它未覆盖路径产生的授权不会自动进缓存，故在启动时兜底全量预热一次。</p>
 *
 * <p>预热失败不影响应用启动（仅记录告警）。</p>
 *
 * @author carlos
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionCacheWarmer implements ApplicationRunner {

    private final OrgRoleManager roleManager;

    private final DbPermissionProvider dbPermissionProvider;

    private final PermissionService permissionService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            List<OrgRoleDTO> roles = roleManager.listEnabled();
            if (roles == null || roles.isEmpty()) {
                log.info("权限缓存预热跳过：无启用角色");
                return;
            }

            int warmed = 0;
            for (OrgRoleDTO role : roles) {
                if (role == null || role.getRoleCode() == null) {
                    continue;
                }
                Set<String> permCodes = dbPermissionProvider.getPermissions(role.getRoleCode());
                // 不广播：本节点刚启动，其它节点的本地缓存仍是新的，无需额外失效
                permissionService.updateRolePermissions(
                    role.getRoleCode(),
                    permCodes == null ? Collections.emptySet() : permCodes,
                    false);
                warmed++;
            }
            log.info("权限缓存预热完成：角色数={}", warmed);
        } catch (Exception e) {
            log.warn("权限缓存预热失败（不影响启动）：{}", e.getMessage());
        }
    }
}
