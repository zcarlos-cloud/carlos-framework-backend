package com.carlos.org.position.service;

import com.carlos.org.position.manager.OrgPositionLevelManager;
import com.carlos.org.position.pojo.dto.OrgPositionLevelDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Set;


/**
 * <p>
 * 职级表 业务
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrgPositionLevelService {

    private final OrgPositionLevelManager positionLevelManager;

    /**
     * 新增职级表
     *
     * @param dto 职级表数据
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    public void addOrgPositionLevel(OrgPositionLevelDTO dto) {
        boolean success = positionLevelManager.add(dto);
        if (!success) {
            // 保存失败的应对措施
            return;
        }
        Serializable id = dto.getId();
        log.info("Insert 'OrgPositionLevel' data: id:{}", id);
        // 保存完成的后续业务
    }

    /**
     * 删除职级表
     *
     * @param ids 职级表id
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    public void deleteOrgPositionLevel(Set<Long> ids) {
        for (Serializable id : ids) {
            boolean success = positionLevelManager.delete(id);
            if (!success) {
                // 删除失败的措施
                continue;
            }
            // 删除成功的后续业务
        }
    }

    /**
     * 修改职级表信息
     *
     * @param dto 对象信息
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    public void updateOrgPositionLevel(OrgPositionLevelDTO dto) {
        boolean success = positionLevelManager.modify(dto);
        if (!success) {
            // 修改失败操作
            return;
        }
        // 修改成功的后续操作
        log.info("Update 'OrgPositionLevel' data: id:{}", dto.getId());
    }

}
