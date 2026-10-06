package com.carlos.org.position.service;

import com.carlos.org.position.manager.OrgPositionHistoryManager;
import com.carlos.org.position.pojo.dto.OrgPositionHistoryDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Set;


/**
 * <p>
 * 岗位变更历史表 业务
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrgPositionHistoryService {

    private final OrgPositionHistoryManager positionHistoryManager;

    /**
     * 新增岗位变更历史表
     *
     * @param dto 岗位变更历史表数据
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    public void addOrgPositionHistory(OrgPositionHistoryDTO dto) {
        boolean success = positionHistoryManager.add(dto);
        if (!success) {
            // 保存失败的应对措施
            return;
        }
        Serializable id = dto.getId();
        log.info("Insert 'OrgPositionHistory' data: id:{}", id);
        // 保存完成的后续业务
    }

    /**
     * 删除岗位变更历史表
     *
     * @param ids 岗位变更历史表id
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    public void deleteOrgPositionHistory(Set<Long> ids) {
        for (Serializable id : ids) {
            boolean success = positionHistoryManager.delete(id);
            if (!success) {
                // 删除失败的措施
                continue;
            }
            // 删除成功的后续业务
        }
    }

    /**
     * 修改岗位变更历史表信息
     *
     * @param dto 对象信息
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    public void updateOrgPositionHistory(OrgPositionHistoryDTO dto) {
        boolean success = positionHistoryManager.modify(dto);
        if (!success) {
            // 修改失败操作
            return;
        }
        // 修改成功的后续操作
        log.info("Update 'OrgPositionHistory' data: id:{}", dto.getId());
    }

}
