package com.carlos.org.position.service;

import com.carlos.core.response.CommonErrorCode;
import com.carlos.org.api.enums.OrgErrorCode;
import com.carlos.org.position.manager.OrgUserPositionManager;
import com.carlos.org.position.pojo.dto.OrgUserPositionDTO;
import com.carlos.org.position.pojo.param.OrgUserPositionAssignParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Set;


/**
 * <p>
 * 用户岗位职级关联表（核心任职信息） 业务
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrgUserPositionService {

    private final OrgUserPositionManager userPositionManager;

    /**
     * 新增用户岗位职级关联表（核心任职信息）
     *
     * @param dto 用户岗位职级关联表（核心任职信息）数据
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    public void addOrgUserPosition(OrgUserPositionDTO dto) {
        boolean success = userPositionManager.add(dto);
        if (!success) {
            // 保存失败的应对措施
            return;
        }
        Serializable id = dto.getId();
        log.info("Insert 'OrgUserPosition' data: id:{}", id);
        // 保存完成的后续业务
    }

    /**
     * 删除用户岗位职级关联表（核心任职信息）
     *
     * @param ids 用户岗位职级关联表（核心任职信息）id
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    public void deleteOrgUserPosition(Set<Long> ids) {
        for (Serializable id : ids) {
            boolean success = userPositionManager.delete(id);
            if (!success) {
                // 删除失败的措施
                continue;
            }
            // 删除成功的后续业务
        }
    }

    /**
     * 修改用户岗位职级关联表（核心任职信息）信息
     *
     * @param dto 对象信息
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    public void updateOrgUserPosition(OrgUserPositionDTO dto) {
        boolean success = userPositionManager.modify(dto);
        if (!success) {
            // 修改失败操作
            return;
        }
        // 修改成功的后续操作
        log.info("Update 'OrgUserPosition' data: id:{}", dto.getId());
    }

    /**
     * 用户分配岗位
     *
     * @param param 分配参数
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignPosition(OrgUserPositionAssignParam param) {
        // 检查用户是否已存在该岗位
        OrgUserPositionDTO exist = userPositionManager.getByUserIdAndPositionIdAndDeptId(
            param.getUserId(),
            param.getPositionId(),
            param.getDepartmentId());

        if (exist != null) {
            throw OrgErrorCode.ORG_USER_ROLE_ALREADY_EXISTS.exception();
        }

        // 如果设置为主岗位，需要将其他岗位设为非主岗位
        if (Boolean.TRUE.equals(param.getIsMain())) {
            userPositionManager.clearMainPosition(param.getUserId());
        }

        // 创建用户岗位关联
        OrgUserPositionDTO userPosition = new OrgUserPositionDTO();
        userPosition.setUserId(param.getUserId());
        userPosition.setPositionId(param.getPositionId());
        userPosition.setLevelId(param.getLevelId());
        userPosition.setDepartmentId(param.getDepartmentId());
        userPosition.setMain(param.getIsMain());
        userPosition.setPositionStatus(1); // 在职
        userPosition.setAppointDate(param.getAppointDate() != null ? param.getAppointDate() : LocalDate.now());

        boolean success = userPositionManager.add(userPosition);
        if (!success) {
            throw CommonErrorCode.BUSINESS_ERROR.exception("分配岗位失败");
        }

        log.info("用户分配岗位成功：userId={}, positionId={}", param.getUserId(), param.getPositionId());
    }

    /**
     * 用户卸任岗位
     *
     * @param userId     用户ID
     * @param positionId 岗位ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void resignPosition(Long userId, Long positionId) {
        boolean success = userPositionManager.updatePositionStatus(
            userId, positionId, 5, LocalDate.now());

        if (!success) {
            throw CommonErrorCode.BUSINESS_ERROR.exception("卸任岗位失败");
        }

        log.info("用户卸任岗位成功：userId={}, positionId={}", userId, positionId);
    }

}
