package com.carlos.org.service;

import com.carlos.org.manager.OrgUserDepartmentManager;
import com.carlos.org.pojo.dto.OrgUserDepartmentDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Set;


/**
 * <p>
 * 用户部门 业务
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrgUserDepartmentService {

    private final OrgUserDepartmentManager userDepartmentManager;

    /**
     * 新增用户部门
     *
     * @param dto 用户部门数据
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    public void addOrgUserDepartment(OrgUserDepartmentDTO dto) {
        boolean success = userDepartmentManager.add(dto);
        if (!success) {
            // 保存失败的应对措施
            return;
        }
        Serializable id = dto.getId();
        log.info("Insert 'OrgUserDepartment' data: id:{}", id);
        // 保存完成的后续业务
    }

    /**
     * 删除用户部门
     *
     * @param ids 用户部门id
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    public void deleteOrgUserDepartment(Set<Long> ids) {
        for (Serializable id : ids) {
            boolean success = userDepartmentManager.delete(id);
            if (!success) {
                // 删除失败的措施
                continue;
            }
            // 删除成功的后续业务
        }
    }

    /**
     * 修改用户部门信息
     *
     * @param dto 对象信息
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    public void updateOrgUserDepartment(OrgUserDepartmentDTO dto) {
        boolean success = userDepartmentManager.modify(dto);
        if (!success) {
            // 修改失败操作
            return;
        }
        // 修改成功的后续操作
        log.info("Update 'OrgUserDepartment' data: id:{}", dto.getId());
    }

}
