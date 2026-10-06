package com.carlos.org.service;

import com.carlos.org.manager.OrgRoleOperationManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * <p>
 * 角色-操作权限关联 业务
 * </p>
 *
 * @author carlos
 * @date 2026-05-27
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrgRoleOperationService {

    private final OrgRoleOperationManager orgRoleOperationManager;

    /**
     * 根据角色ID查询操作ID列表
     *
     * @param roleId 角色ID
     * @return 操作ID列表
     */
    public List<Long> listOperationIdsByRoleId(Long roleId) {
        return orgRoleOperationManager.listOperationIdsByRoleId(roleId);
    }

    /**
     * 根据角色ID集合查询操作ID列表
     *
     * @param roleIds 角色ID集合
     * @return 操作ID列表
     */
    public List<Long> listOperationIdsByRoleIds(Set<Long> roleIds) {
        return orgRoleOperationManager.listOperationIdsByRoleIds(roleIds);
    }

    /**
     * 保存角色操作权限（全量覆盖）
     *
     * @param roleId       角色ID
     * @param operationIds 操作ID集合
     */
    public void saveRoleOperations(Long roleId, Set<Long> operationIds) {
        orgRoleOperationManager.saveRoleOperations(roleId, operationIds);
    }

}
