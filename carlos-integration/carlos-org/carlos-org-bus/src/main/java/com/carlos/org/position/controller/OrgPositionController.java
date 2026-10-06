package com.carlos.org.position.controller;

import com.carlos.core.pagination.Paging;
import com.carlos.core.param.ParamIdSet;
import com.carlos.log.annotation.Log;
import com.carlos.log.enums.BusinessType;
import com.carlos.org.position.convert.OrgPositionConvert;
import com.carlos.org.position.manager.OrgPositionManager;
import com.carlos.org.position.pojo.dto.OrgPositionDTO;
import com.carlos.org.position.pojo.param.OrgPositionCreateParam;
import com.carlos.org.position.pojo.param.OrgPositionPageParam;
import com.carlos.org.position.pojo.param.OrgPositionUpdateParam;
import com.carlos.org.position.pojo.vo.OrgPositionVO;
import com.carlos.org.position.service.OrgPositionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


/**
 * <p>
 * 岗位表 rest服务接口
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("org/position")
@Tag(name = "岗位表")
public class OrgPositionController {

    public static final String BASE_NAME = "岗位表";

    private final OrgPositionService positionService;

    private final OrgPositionManager positionManager;


    @Log(
        title = "岗位管理",
        logType = "ORG_POSITION_CREATE",
        businessType = BusinessType.INSERT,
        operation = "'新增岗位: ' + #param.positionName",
        targetType = "ORG_POSITION",
        riskLevel = 30
    )
    @PreAuthorize("hasPermission('org:position', 'create')")
    @PostMapping("add")
    @Operation(summary = "新增" + BASE_NAME)
    public void add(@RequestBody @Validated OrgPositionCreateParam param) {
        OrgPositionDTO dto = OrgPositionConvert.INSTANCE.toDTO(param);
        positionService.addOrgPosition(dto);
    }


    @Log(
        title = "岗位管理",
        logType = "ORG_POSITION_DELETE",
        businessType = BusinessType.DELETE,
        operation = "'删除岗位: ' + #param.ids",
        targetId = "#param.ids",
        targetType = "ORG_POSITION",
        riskLevel = 60
    )
    @PreAuthorize("hasPermission('org:position', 'delete')")
    @PostMapping("delete")
    @Operation(summary = "删除" + BASE_NAME)
    public void delete(@RequestBody @Validated ParamIdSet<Long> param) {
        positionService.deleteOrgPosition(param.getIds());
    }


    @Log(
        title = "岗位管理",
        logType = "ORG_POSITION_UPDATE",
        businessType = BusinessType.UPDATE,
        operation = "'修改岗位: ' + #param.id",
        targetId = "#param.id",
        targetType = "ORG_POSITION",
        riskLevel = 30
    )
    @PreAuthorize("hasPermission('org:position', 'update')")
    @PostMapping("update")
    @Operation(summary = "更新" + BASE_NAME)
    public void update(@RequestBody @Validated OrgPositionUpdateParam param) {
        OrgPositionDTO dto = OrgPositionConvert.INSTANCE.toDTO(param);
        positionService.updateOrgPosition(dto);
    }


    @PreAuthorize("hasPermission('org:position', 'view')")
    @GetMapping("detail")
    @Operation(summary = BASE_NAME + "详情")
    public OrgPositionVO detail(Long id) {
        return OrgPositionConvert.INSTANCE.toVO(positionManager.getDtoById(id));
    }


    @PreAuthorize("hasPermission('org:position', 'view')")
    @GetMapping("page")
    @Operation(summary = BASE_NAME + "分页列表")
    public Paging<OrgPositionVO> page(OrgPositionPageParam param) {
        return positionManager.getPage(param);
    }
}
