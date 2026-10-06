package com.carlos.org.manager;

import com.carlos.datasource.base.BaseService;
import com.carlos.org.pojo.entity.OrgRoleOperation;

import java.util.List;
import java.util.Set;

/**
 * <p>
 * 角色-操作权限关联 查询封装接口
 * </p>
 *
 * @author carlos
 * @date 2026-05-27
 */
public interface OrgRoleOperationManager extends BaseService<OrgRoleOperation> {

    /**
     * 根据角色ID查询操作ID列表
     *
     * @param roleId 角色ID
     * @return 操作ID列表
     */
    List<Long> listOperationIdsByRoleId(Long roleId);

    /**
     * 根据角色ID集合查询操作ID列表
     *
     * @param roleIds 角色ID集合
     * @return 操作ID列表
     */
    List<Long> listOperationIdsByRoleIds(Set<Long> roleIds);

    /**
     * 保存角色权限（全量覆盖）
     *
     * @param roleId       角色ID
     * @param operationIds 操作ID集合
     */
    void saveRoleOperations(Long roleId, Set<Long> operationIds);

}
