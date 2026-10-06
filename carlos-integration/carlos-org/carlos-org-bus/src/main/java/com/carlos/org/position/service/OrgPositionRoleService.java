package com.carlos.org.position.service;

import com.carlos.org.position.manager.OrgPositionRoleManager;
import com.carlos.org.position.pojo.dto.OrgPositionRoleDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Set;


/**
 * <p>
 * 岗位角色关联表（岗位默认权限配置） 业务
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrgPositionRoleService {

    private final OrgPositionRoleManager positionRoleManager;

    /**
     * 新增岗位角色关联表（岗位默认权限配置）
     *
     * @param dto 岗位角色关联表（岗位默认权限配置）数据
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    public void addOrgPositionRole(OrgPositionRoleDTO dto) {
        boolean success = positionRoleManager.add(dto);
        if (!success) {
            // 保存失败的应对措施
            return;
        }
        Serializable id = dto.getId();
        log.info("Insert 'OrgPositionRole' data: id:{}", id);
        // 保存完成的后续业务
    }

    /**
     * 删除岗位角色关联表（岗位默认权限配置）
     *
     * @param ids 岗位角色关联表（岗位默认权限配置）id
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    public void deleteOrgPositionRole(Set<Long> ids) {
        for (Serializable id : ids) {
            boolean success = positionRoleManager.delete(id);
            if (!success) {
                // 删除失败的措施
                continue;
            }
            // 删除成功的后续业务
        }
    }

    /**
     * 修改岗位角色关联表（岗位默认权限配置）信息
     *
     * @param dto 对象信息
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    public void updateOrgPositionRole(OrgPositionRoleDTO dto) {
        boolean success = positionRoleManager.modify(dto);
        if (!success) {
            // 修改失败操作
            return;
        }
        // 修改成功的后续操作
        log.info("Update 'OrgPositionRole' data: id:{}", dto.getId());
    }

}
