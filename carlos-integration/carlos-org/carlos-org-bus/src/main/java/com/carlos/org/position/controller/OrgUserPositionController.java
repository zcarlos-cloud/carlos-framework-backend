package com.carlos.org.position.controller;

import com.carlos.core.pagination.Paging;
import com.carlos.core.param.ParamIdSet;
import com.carlos.core.response.Result;
import com.carlos.log.annotation.Log;
import com.carlos.log.enums.BusinessType;
import com.carlos.org.position.convert.OrgUserPositionConvert;
import com.carlos.org.position.manager.OrgUserPositionManager;
import com.carlos.org.position.pojo.dto.OrgUserPositionDTO;
import com.carlos.org.position.pojo.param.OrgUserPositionAssignParam;
import com.carlos.org.position.pojo.param.OrgUserPositionCreateParam;
import com.carlos.org.position.pojo.param.OrgUserPositionPageParam;
import com.carlos.org.position.pojo.param.OrgUserPositionUpdateParam;
import com.carlos.org.position.pojo.vo.OrgUserPositionVO;
import com.carlos.org.position.service.OrgUserPositionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


/**
 * <p>
 * 用户岗位职级关联表（核心任职信息） rest服务接口
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("org/user/position")
@Tag(name = "用户岗位职级关联表（核心任职信息）")
public class OrgUserPositionController {

    public static final String BASE_NAME = "用户岗位职级关联表（核心任职信息）";

    private final OrgUserPositionService userPositionService;

    private final OrgUserPositionManager userPositionManager;


    @Log(
        title = "用户岗位",
        logType = "ORG_USER_POSITION_CREATE",
        businessType = BusinessType.INSERT,
        operation = "'新增用户任职: 用户 ' + #param.userId + ' -> 岗位 ' + #param.positionId",
        targetType = "ORG_USER_POSITION",
        riskLevel = 50
    )
    @PreAuthorize("hasPermission('org:user:position', 'create')")
    @PostMapping("add")
    @Operation(summary = "新增" + BASE_NAME)
    public void add(@RequestBody @Validated OrgUserPositionCreateParam param) {
        OrgUserPositionDTO dto = OrgUserPositionConvert.INSTANCE.toDTO(param);
        userPositionService.addOrgUserPosition(dto);
    }


    @Log(
        title = "用户岗位",
        logType = "ORG_USER_POSITION_DELETE",
        businessType = BusinessType.DELETE,
        operation = "'删除用户任职: ' + #param.ids",
        targetId = "#param.ids",
        targetType = "ORG_USER_POSITION",
        riskLevel = 60
    )
    @PreAuthorize("hasPermission('org:user:position', 'delete')")
    @PostMapping("delete")
    @Operation(summary = "删除" + BASE_NAME)
    public void delete(@RequestBody @Validated ParamIdSet<Long> param) {
        userPositionService.deleteOrgUserPosition(param.getIds());
    }


    @Log(
        title = "用户岗位",
        logType = "ORG_USER_POSITION_UPDATE",
        businessType = BusinessType.UPDATE,
        operation = "'修改用户任职: ' + #param.id",
        targetId = "#param.id",
        targetType = "ORG_USER_POSITION",
        riskLevel = 50
    )
    @PreAuthorize("hasPermission('org:user:position', 'update')")
    @PostMapping("update")
    @Operation(summary = "更新" + BASE_NAME)
    public void update(@RequestBody @Validated OrgUserPositionUpdateParam param) {
        OrgUserPositionDTO dto = OrgUserPositionConvert.INSTANCE.toDTO(param);
        userPositionService.updateOrgUserPosition(dto);
    }


    @PreAuthorize("hasPermission('org:user:position', 'view')")
    @GetMapping("detail")
    @Operation(summary = BASE_NAME + "详情")
    public OrgUserPositionVO detail(Long id) {
        return OrgUserPositionConvert.INSTANCE.toVO(userPositionManager.getDtoById(id));
    }


    @PreAuthorize("hasPermission('org:user:position', 'view')")
    @GetMapping("page")
    @Operation(summary = BASE_NAME + "分页列表")
    public Paging<OrgUserPositionVO> page(OrgUserPositionPageParam param) {
        return userPositionManager.getPage(param);
    }

    /**
     * 用户分配岗位
     *
     * @param param 分配参数
     * @return 结果
     */
    @Log(
        title = "用户岗位",
        logType = "ORG_USER_POSITION_ASSIGN",
        businessType = BusinessType.GRANT,
        operation = "'用户分配岗位: 用户 ' + #param.userId + ' -> 岗位 ' + #param.positionId",
        targetId = "#param.userId",
        targetType = "ORG_USER_POSITION",
        riskLevel = 60
    )
    @PreAuthorize("hasPermission('org:user:position', 'assignPosition')")
    @PostMapping("assign")
    @Operation(summary = "用户分配岗位")
    public Result<Boolean> assignPosition(@RequestBody @Validated OrgUserPositionAssignParam param) {
        userPositionService.assignPosition(param);
        return Result.success(true);
    }

    /**
     * 用户卸任岗位
     *
     * @param userId     用户ID
     * @param positionId 岗位ID
     * @return 结果
     */
    @Log(
        title = "用户岗位",
        logType = "ORG_USER_POSITION_RESIGN",
        businessType = BusinessType.GRANT,
        operation = "'用户卸任岗位: 用户 ' + #userId + ' -> 岗位 ' + #positionId",
        targetId = "#userId",
        targetType = "ORG_USER_POSITION",
        riskLevel = 60
    )
    @PreAuthorize("hasPermission('org:user:position', 'resignPosition')")
    @PostMapping("resign")
    @Operation(summary = "用户卸任岗位")
    public Result<Boolean> resignPosition(@RequestParam Long userId, @RequestParam Long positionId) {
        userPositionService.resignPosition(userId, positionId);
        return Result.success(true);
    }
}
