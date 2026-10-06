package com.carlos.org.position.service;

import com.carlos.org.position.manager.OrgPositionCategoryManager;
import com.carlos.org.position.pojo.dto.OrgPositionCategoryDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Set;


/**
 * <p>
 * 岗位类别表（职系） 业务
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrgPositionCategoryService {

    private final OrgPositionCategoryManager positionCategoryManager;

    /**
     * 新增岗位类别表（职系）
     *
     * @param dto 岗位类别表（职系）数据
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    public void addOrgPositionCategory(OrgPositionCategoryDTO dto) {
        boolean success = positionCategoryManager.add(dto);
        if (!success) {
            // 保存失败的应对措施
            return;
        }
        Serializable id = dto.getId();
        log.info("Insert 'OrgPositionCategory' data: id:{}", id);
        // 保存完成的后续业务
    }

    /**
     * 删除岗位类别表（职系）
     *
     * @param ids 岗位类别表（职系）id
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    public void deleteOrgPositionCategory(Set<Long> ids) {
        for (Serializable id : ids) {
            boolean success = positionCategoryManager.delete(id);
            if (!success) {
                // 删除失败的措施
                continue;
            }
            // 删除成功的后续业务
        }
    }

    /**
     * 修改岗位类别表（职系）信息
     *
     * @param dto 对象信息
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    public void updateOrgPositionCategory(OrgPositionCategoryDTO dto) {
        boolean success = positionCategoryManager.modify(dto);
        if (!success) {
            // 修改失败操作
            return;
        }
        // 修改成功的后续操作
        log.info("Update 'OrgPositionCategory' data: id:{}", dto.getId());
    }

}
