package com.carlos.org.config;

import com.carlos.org.manager.OrgDepartmentManager;
import com.carlos.org.pojo.enums.OrgDataScopeEnum;
import com.carlos.security.auth.UserContext;
import com.carlos.security.permission.DataScopeProvider;
import com.carlos.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;

/**
 * <p>
 * 组织模块的数据权限实现（{@link DataScopeProvider} 的数据维度）
 * </p>
 *
 * <p>把「当前用户角色上的 {@code org_role.data_scope}」翻译成行级可访问性判定，
 * 供 {@code @PreAuthorize("@ps.canAccess(#entity)")} 之类的表达式使用。</p>
 *
 * <p>判定顺序：认证 → 超管放行 → 租户隔离 → 数据范围（全部 / 本部门及子部门 / 仅本部门 / 仅本人 / 自定义）。
 * 任一环节无法判定都**保守拒绝**。</p>
 *
 * @author carlos
 * @since 3.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrgDataScopeProvider implements DataScopeProvider {

    private final OrgDataScopeHelper dataScopeHelper;

    private final OrgDepartmentManager departmentManager;

    @Override
    public Integer currentScopeCode() {
        return dataScopeHelper.getCurrentDataScope();
    }

    @Override
    public boolean canAccessData(Long ownerId, Long deptId, Long tenantId) {
        UserContext user = SecurityUtils.getCurrentUserOpt().orElse(null);
        if (user == null) {
            return false;
        }
        if (user.isSuperAdmin()) {
            return true;
        }

        // 租户隔离：数据带租户且与当前用户不同租户 → 拒绝（数据无租户视为公共数据）
        if (tenantId != null && !Objects.equals(tenantId, user.getTenantId())) {
            return false;
        }

        Integer scope = dataScopeHelper.getCurrentDataScope();
        if (scope == null) {
            return false;
        }

        // 全部数据
        if (Objects.equals(scope, OrgDataScopeEnum.ALL.getCode())) {
            return true;
        }

        // 仅本人：归属人必须是当前用户
        if (Objects.equals(scope, OrgDataScopeEnum.SELF_ONLY.getCode())) {
            return ownerId != null && Objects.equals(ownerId, user.getUserId());
        }

        // 仅本部门
        if (Objects.equals(scope, OrgDataScopeEnum.DEPT_ONLY.getCode())) {
            return deptId != null && Objects.equals(deptId, user.getDeptId());
        }

        // 本部门及子部门
        if (Objects.equals(scope, OrgDataScopeEnum.DEPT_AND_CHILD.getCode())) {
            Long myDept = user.getDeptId();
            if (myDept == null || deptId == null) {
                return false;
            }
            if (Objects.equals(deptId, myDept)) {
                return true;
            }
            Set<Long> descendants = departmentManager.getDescIdsFromCache(myDept);
            return descendants != null && descendants.contains(deptId);
        }

        // 自定义规则：当前按「仅本人」兜底（与 OrgDataScopeHelper 的查询过滤口径一致）
        return ownerId != null && Objects.equals(ownerId, user.getUserId());
    }
}
