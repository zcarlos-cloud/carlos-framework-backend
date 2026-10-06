package com.carlos.org.position.apiimpl;


import com.carlos.core.response.Result;
import com.carlos.org.api.pojo.ao.OrgUserAO;
import com.carlos.org.convert.OrgUserConvert;
import com.carlos.org.manager.OrgUserManager;
import com.carlos.org.pojo.dto.OrgUserDTO;
import com.carlos.org.position.api.ApiOrgUserPosition;
import com.carlos.org.position.manager.OrgUserPositionManager;
import com.carlos.org.position.pojo.dto.OrgUserPositionDTO;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

/**
 * <p>
 * 用户岗位职级关联表（核心任职信息） api接口
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("api/org/user/position")
@Tag(name = "用户岗位职级关联表（核心任职信息）Feign接口")
public class ApiOrgUserPositionImpl implements ApiOrgUserPosition {

    private final OrgUserPositionManager orgUserPositionManager;
    private final OrgUserManager orgUserManager;

    @Override
    public Result<List<OrgUserAO>> listSubordinatesByUserId(@RequestParam("userId") Long userId) {
        if (userId == null) {
            return Result.success(List.of());
        }
        // report_to = userId 的任职记录 → 去重后的下级 user_id 集合 → 批量取用户，避免逐条查询
        List<Long> subordinateIds = orgUserPositionManager.listByReportTo(userId).stream()
            .map(OrgUserPositionDTO::getUserId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        List<OrgUserDTO> users = orgUserManager.listDtoByIds(subordinateIds);
        return Result.success(OrgUserConvert.INSTANCE.toAOList(users));
    }

    @Override
    public Result<List<OrgUserAO>> listSubordinatesByUserIdAndTenant(@RequestParam("userId") Long userId,
                                                                     @RequestParam("tenantId") Long tenantId) {
        if (userId == null || tenantId == null) {
            return Result.success(List.of());
        }
        // report_to = userId 的任职记录（限定租户）→ 去重后的下级 user_id 集合 → 批量取用户，避免逐条查询
        List<Long> subordinateIds = orgUserPositionManager.listByReportToAndTenant(userId, tenantId).stream()
            .map(OrgUserPositionDTO::getUserId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        List<OrgUserDTO> users = orgUserManager.listDtoByIdsAndTenant(subordinateIds, tenantId);
        return Result.success(OrgUserConvert.INSTANCE.toAOList(users));
    }

    @Override
    public Result<OrgUserAO> getSuperiorByUserId(@RequestParam("userId") Long userId) {
        if (userId == null) {
            return Result.success(null);
        }
        // 读该用户自己的 report_to（与 listSubordinatesByUserId 方向相反）→ 按 id 取上级
        Long superiorId = orgUserPositionManager.getReportToByUserId(userId);
        if (superiorId == null) {
            return Result.success(null);
        }
        OrgUserDTO superior = orgUserManager.getDtoById(superiorId);
        return Result.success(OrgUserConvert.INSTANCE.toAO(superior));
    }

    @Override
    public Result<OrgUserAO> getSuperiorByUserIdAndTenant(@RequestParam("userId") Long userId,
                                                          @RequestParam("tenantId") Long tenantId) {
        if (userId == null || tenantId == null) {
            return Result.success(null);
        }
        // 任职记录与用户查询都必须限定同一租户，否则会解析出另一个租户的上级
        Long superiorId = orgUserPositionManager.getReportToByUserIdAndTenant(userId, tenantId);
        if (superiorId == null) {
            return Result.success(null);
        }
        OrgUserDTO superior = orgUserManager.getDtoByIdAndTenant(superiorId, tenantId);
        return Result.success(OrgUserConvert.INSTANCE.toAO(superior));
    }

    @Override
    public Result<List<OrgUserAO>> listUsersByPositionId(@RequestParam("positionId") Long positionId) {
        if (positionId == null) {
            return Result.success(List.of());
        }
        // 先取岗位上的用户ID（去重），再批量取用户，避免逐条查询
        List<Long> userIds = orgUserPositionManager.listUserIdsByPositionId(positionId);
        List<OrgUserDTO> users = orgUserManager.listDtoByIds(userIds);
        return Result.success(OrgUserConvert.INSTANCE.toAOList(users));
    }

    @Override
    public Result<List<OrgUserAO>> listUsersByPositionIdAndTenant(@RequestParam("positionId") Long positionId,
                                                                  @RequestParam("tenantId") Long tenantId) {
        if (positionId == null || tenantId == null) {
            return Result.success(List.of());
        }
        // 两段查询使用同一个 tenantId，避免岗位用户与用户资料取自不同租户
        List<Long> userIds = orgUserPositionManager.listUserIdsByPositionIdAndTenant(positionId, tenantId);
        List<OrgUserDTO> users = orgUserManager.listDtoByIdsAndTenant(userIds, tenantId);
        return Result.success(OrgUserConvert.INSTANCE.toAOList(users));
    }

}
