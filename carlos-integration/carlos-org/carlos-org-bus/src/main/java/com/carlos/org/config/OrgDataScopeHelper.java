package com.carlos.org.config;

import cn.hutool.extra.spring.SpringUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carlos.org.manager.OrgDepartmentManager;
import com.carlos.org.manager.OrgRoleManager;
import com.carlos.org.pojo.dto.OrgRoleDTO;
import com.carlos.org.pojo.entity.OrgDepartment;
import com.carlos.org.pojo.entity.OrgUser;
import com.carlos.org.pojo.enums.OrgDataScopeEnum;
import com.carlos.security.auth.UserContext;
import com.carlos.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;

/**
 * <p>
 * 数据权限范围辅助类
 * </p>
 * <p>基于当前用户角色的 data_scope 动态添加查询条件</p>
 *
 * @author Carlos
 * @date 2026年5月19日
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrgDataScopeHelper {

    private final OrgRoleManager roleManager;

    /**
     * 获取当前用户的数据权限范围编码
     *
     * @return data_scope 编码，null 表示未获取到用户上下文
     */
    public Integer getCurrentDataScope() {
        UserContext userContext = getUserContext();
        if (userContext == null) {
            return null;
        }

        Set<String> roleCodes = userContext.getRoleCodes();
        if (roleCodes == null || roleCodes.isEmpty()) {
            return OrgDataScopeEnum.SELF_ONLY.getCode();
        }

        // 取所有角色中最宽泛的权限（code 最小）
        Integer minScope = null;
        for (String roleCode : roleCodes) {
            OrgRoleDTO role = roleManager.getByRoleCode(roleCode);
            if (role != null && role.getDataScope() != null) {
                Integer scopeCode = role.getDataScope().getCode();
                if (minScope == null || scopeCode < minScope) {
                    minScope = scopeCode;
                }
            }
        }

        return minScope != null ? minScope : OrgDataScopeEnum.SELF_ONLY.getCode();
    }

    /**
     * 获取当前用户ID
     *
     * @return 用户ID
     */
    public Long getCurrentUserId() {
        UserContext userContext = getUserContext();
        if (userContext == null) {
            return null;
        }
        return userContext.getUserId();
    }

    /**
     * 获取当前部门ID
     *
     * @return 部门ID
     */
    public Long getCurrentDeptId() {
        UserContext userContext = getUserContext();
        if (userContext == null) {
            return null;
        }
        return userContext.getDeptId();
    }

    /**
     * 对用户查询应用数据权限范围
     *
     * @param wrapper LambdaQueryWrapper
     */
    public void applyUserQueryScope(LambdaQueryWrapper<OrgUser> wrapper) {
        Integer scope = getCurrentDataScope();
        if (scope == null || scope.equals(OrgDataScopeEnum.ALL.getCode())) {
            return;
        }

        Long userId = getCurrentUserId();
        Long deptId = getCurrentDeptId();

        if (scope.equals(OrgDataScopeEnum.SELF_ONLY.getCode())) {
            if (userId != null) {
                wrapper.eq(OrgUser::getId, userId);
            }
        } else if (scope.equals(OrgDataScopeEnum.DEPT_ONLY.getCode())) {
            if (deptId != null) {
                wrapper.eq(OrgUser::getMainDeptId, deptId);
            }
        } else if (scope.equals(OrgDataScopeEnum.DEPT_AND_CHILD.getCode())) {
            if (deptId != null) {
                OrgDepartmentManager departmentManager = SpringUtil.getBean(OrgDepartmentManager.class);
                Set<Long> deptIds = departmentManager.getDescIdsFromCache(deptId);
                if (deptIds == null || deptIds.isEmpty()) {
                    deptIds = Collections.singleton(deptId);
                } else {
                    deptIds.add(deptId);
                }
                wrapper.in(OrgUser::getMainDeptId, deptIds);
            }
        } else if (scope.equals(OrgDataScopeEnum.CUSTOM.getCode())) {
            // 自定义规则暂时按本人处理
            if (userId != null) {
                wrapper.eq(OrgUser::getId, userId);
            }
        }
    }

    /**
     * 对部门查询应用数据权限范围
     *
     * @param wrapper LambdaQueryWrapper
     */
    public void applyDeptQueryScope(LambdaQueryWrapper<OrgDepartment> wrapper) {
        Integer scope = getCurrentDataScope();
        if (scope == null || scope.equals(OrgDataScopeEnum.ALL.getCode())) {
            return;
        }

        Long deptId = getCurrentDeptId();

        if (scope.equals(OrgDataScopeEnum.SELF_ONLY.getCode())) {
            // 仅本人时只能看到所属部门
            if (deptId != null) {
                wrapper.eq(OrgDepartment::getId, deptId);
            }
        } else if (scope.equals(OrgDataScopeEnum.DEPT_ONLY.getCode())) {
            if (deptId != null) {
                wrapper.eq(OrgDepartment::getId, deptId);
            }
        } else if (scope.equals(OrgDataScopeEnum.DEPT_AND_CHILD.getCode())) {
            if (deptId != null) {
                OrgDepartmentManager departmentManager = SpringUtil.getBean(OrgDepartmentManager.class);
                Set<Long> deptIds = departmentManager.getDescIdsFromCache(deptId);
                if (deptIds == null || deptIds.isEmpty()) {
                    deptIds = Collections.singleton(deptId);
                } else {
                    deptIds.add(deptId);
                }
                wrapper.in(OrgDepartment::getId, deptIds);
            }
        } else if (scope.equals(OrgDataScopeEnum.CUSTOM.getCode())) {
            // 自定义规则暂时按所属部门处理
            if (deptId != null) {
                wrapper.eq(OrgDepartment::getId, deptId);
            }
        }
    }

    /**
     * 获取当前用户上下文
     *
     * @return UserContext
     */
    private UserContext getUserContext() {
        try {
            return SecurityUtils.getCurrentUser();
        } catch (Exception e) {
            log.debug("无法获取当前用户上下文: {}", e.getMessage());
            return null;
        }
    }
}
