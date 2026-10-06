package com.carlos.org.position.service;

import com.carlos.org.position.manager.OrgPositionManager;
import com.carlos.org.position.pojo.dto.OrgPositionDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Set;


/**
 * <p>
 * 岗位表 业务
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrgPositionService {

    private final OrgPositionManager positionManager;

    private final com.carlos.org.position.manager.OrgUserPositionManager userPositionManager;

    /**
     * 新增岗位表
     *
     * @param dto 岗位表数据
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    public void addOrgPosition(OrgPositionDTO dto) {
        // 初始化编制统计
        if (dto.getHeadcount() == null) {
            dto.setHeadcount(0);
        }
        dto.setCurrentCount(0);
        dto.setVacancyCount(dto.getHeadcount());

        boolean success = positionManager.add(dto);
        if (!success) {
            return;
        }
        log.info("Insert 'OrgPosition' data: id:{}", dto.getId());
    }

    /**
     * 删除岗位表
     *
     * @param ids 岗位表id
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    public void deleteOrgPosition(Set<Long> ids) {
        for (Serializable id : ids) {
            boolean success = positionManager.delete(id);
            if (!success) {
                // 删除失败的措施
                continue;
            }
            // 删除成功的后续业务
        }
    }

    /**
     * 修改岗位表信息
     *
     * @param dto 对象信息
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    public void updateOrgPosition(OrgPositionDTO dto) {
        // 如果修改了编制，刷新统计
        if (dto.getHeadcount() != null && dto.getId() != null) {
            int occupied = userPositionManager.countOccupiedByPositionId(dto.getId());
            dto.setCurrentCount(occupied);
            dto.setVacancyCount(Math.max(0, dto.getHeadcount() - occupied));

            // 超编预警
            if (dto.getHeadcount() > 0 && occupied > dto.getHeadcount()) {
                log.warn("岗位超编预警：positionId={}, headcount={}, occupied={}", dto.getId(), dto.getHeadcount(), occupied);
            }
        }

        boolean success = positionManager.modify(dto);
        if (!success) {
            return;
        }
        log.info("Update 'OrgPosition' data: id:{}", dto.getId());
    }

}
