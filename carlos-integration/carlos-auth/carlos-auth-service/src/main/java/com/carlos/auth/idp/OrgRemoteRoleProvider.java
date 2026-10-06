package com.carlos.auth.idp;

import com.carlos.core.response.Result;
import com.carlos.org.api.ApiOrgRole;
import com.carlos.org.api.ApiOrgUser;
import com.carlos.org.api.ApiOrgUserRole;
import com.carlos.org.api.pojo.ao.OrgRoleAO;
import com.carlos.org.api.pojo.ao.OrgUserAO;
import com.carlos.org.api.pojo.ao.OrgUserRoleAO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 基于 carlos-org 的角色提供者
 *
 * <p>通过 Feign 调用 carlos-org 服务查询用户角色信息。</p>
 *
 * <h3>角色查询链路：</h3>
 * <ol>
 *   <li>根据用户ID查询用户角色关联（{@code org_user_role}）</li>
 *   <li>根据角色ID查询角色编码（{@code org_role.role_code}）</li>
 * </ol>
 *
 * <p>替代 {@link DefaultRoleProvider}（仅返回空集合），为 Path A 和 Path B 提供真实角色数据。</p>
 *
 * @author Carlos
 * @version 3.0.0
 * @since 2026-06-20
 * @see RoleProvider
 * @see DefaultRoleProvider
 */
@Slf4j
@Component
@Primary
@RequiredArgsConstructor
public class OrgRemoteRoleProvider implements RoleProvider {

    private final ApiOrgUserRole apiOrgUserRole;
    private final ApiOrgRole apiOrgRole;
    private final ApiOrgUser apiOrgUser;

    @Override
    public Set<String> getRoleCodesByUserId(Long userId) {
        if (userId == null) {
            return Collections.emptySet();
        }

        try {
            Result<List<OrgUserRoleAO>> result = apiOrgUserRole.listByUserId(userId);
            if (!result.isSuccess() || result.getData() == null || result.getData().isEmpty()) {
                log.debug("No roles found for userId: {}", userId);
                return Collections.emptySet();
            }

            Set<String> roleCodes = new HashSet<>();
            for (OrgUserRoleAO userRole : result.getData()) {
                Long roleId = userRole.getRoleId();
                if (roleId == null) {
                    continue;
                }
                try {
                    Result<OrgRoleAO> roleResult = apiOrgRole.getRoleById(roleId);
                    if (roleResult.isSuccess() && roleResult.getData() != null) {
                        String roleCode = roleResult.getData().getRoleCode();
                        if (roleCode != null && !roleCode.isBlank()) {
                            roleCodes.add(roleCode);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to fetch role detail for roleId={}: {}", roleId, e.getMessage());
                }
            }

            log.debug("Resolved {} role codes for userId: {}", roleCodes.size(), userId);
            return roleCodes;

        } catch (Exception e) {
            log.error("Failed to query roles for userId={}: {}", userId, e.getMessage(), e);
            return Collections.emptySet();
        }
    }

    @Override
    public Set<String> getRoleCodesByIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return Collections.emptySet();
        }

        try {
            Result<OrgUserAO> userResult = apiOrgUser.getUserByIdentifier(identifier);
            if (!userResult.isSuccess() || userResult.getData() == null) {
                log.debug("User not found by identifier: {}", identifier);
                return Collections.emptySet();
            }

            Long userId = userResult.getData().getId();
            return getRoleCodesByUserId(userId);

        } catch (Exception e) {
            log.error("Failed to resolve roles for identifier={}: {}", identifier, e.getMessage(), e);
            return Collections.emptySet();
        }
    }
}
