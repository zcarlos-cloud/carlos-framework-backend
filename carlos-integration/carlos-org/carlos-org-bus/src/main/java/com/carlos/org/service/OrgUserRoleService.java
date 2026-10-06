package com.carlos.org.service;

import com.carlos.org.manager.OrgUserRoleManager;
import com.carlos.org.pojo.dto.OrgUserRoleDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Set;


/**
 * <p>
 * 用户角色 业务
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrgUserRoleService {

    private final OrgUserRoleManager userRoleManager;

    /**
     * 新增用户角色
     *
     * @param dto 用户角色数据
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    public void addOrgUserRole(OrgUserRoleDTO dto) {
        boolean success = userRoleManager.add(dto);
        if (!success) {
            // 保存失败的应对措施
            return;
        }
        Serializable id = dto.getId();
        log.info("Insert 'OrgUserRole' data: id:{}", id);
        // 保存完成的后续业务
    }

    /**
     * 删除用户角色
     *
     * @param ids 用户角色id
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    public void deleteOrgUserRole(Set<Long> ids) {
        for (Serializable id : ids) {
            boolean success = userRoleManager.delete(id);
            if (!success) {
                // 删除失败的措施
                continue;
            }
            // 删除成功的后续业务
        }
    }

    /**
     * 修改用户角色信息
     *
     * @param dto 对象信息
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    public void updateOrgUserRole(OrgUserRoleDTO dto) {
        boolean success = userRoleManager.modify(dto);
        if (!success) {
            // 修改失败操作
            return;
        }
        // 修改成功的后续操作
        log.info("Update 'OrgUserRole' data: id:{}", dto.getId());
    }

}
