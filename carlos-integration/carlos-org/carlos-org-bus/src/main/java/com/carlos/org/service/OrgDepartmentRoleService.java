package com.carlos.org.service;

import com.carlos.org.manager.OrgDepartmentRoleManager;
import com.carlos.org.pojo.dto.OrgDepartmentRoleDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Set;


/**
 * <p>
 * 部门角色 业务
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrgDepartmentRoleService {

    private final OrgDepartmentRoleManager departmentRoleManager;

    /**
     * 新增部门角色
     *
     * @param dto 部门角色数据
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    public void addOrgDepartmentRole(OrgDepartmentRoleDTO dto) {
        boolean success = departmentRoleManager.add(dto);
        if (!success) {
            // 保存失败的应对措施
            return;
        }
        Serializable id = dto.getId();
        log.info("Insert 'OrgDepartmentRole' data: id:{}", id);
        // 保存完成的后续业务
    }

    /**
     * 删除部门角色
     *
     * @param ids 部门角色id
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    public void deleteOrgDepartmentRole(Set<Long> ids) {
        for (Serializable id : ids) {
            boolean success = departmentRoleManager.delete(id);
            if (!success) {
                // 删除失败的措施
                continue;
            }
            // 删除成功的后续业务
        }
    }

    /**
     * 修改部门角色信息
     *
     * @param dto 对象信息
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    public void updateOrgDepartmentRole(OrgDepartmentRoleDTO dto) {
        boolean success = departmentRoleManager.modify(dto);
        if (!success) {
            // 修改失败操作
            return;
        }
        // 修改成功的后续操作
        log.info("Update 'OrgDepartmentRole' data: id:{}", dto.getId());
    }

}
