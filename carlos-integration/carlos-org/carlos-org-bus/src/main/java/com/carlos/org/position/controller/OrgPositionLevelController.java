package com.carlos.org.position.controller;

import com.carlos.core.pagination.Paging;
import com.carlos.core.param.ParamIdSet;
import com.carlos.log.annotation.Log;
import com.carlos.log.enums.BusinessType;
import com.carlos.org.position.convert.OrgPositionLevelConvert;
import com.carlos.org.position.manager.OrgPositionLevelManager;
import com.carlos.org.position.pojo.dto.OrgPositionLevelDTO;
import com.carlos.org.position.pojo.param.OrgPositionLevelCreateParam;
import com.carlos.org.position.pojo.param.OrgPositionLevelPageParam;
import com.carlos.org.position.pojo.param.OrgPositionLevelUpdateParam;
import com.carlos.org.position.pojo.vo.OrgPositionLevelVO;
import com.carlos.org.position.service.OrgPositionLevelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


/**
 * <p>
 * 职级表 rest服务接口
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("org/position/level")
@Tag(name = "职级表")
public class OrgPositionLevelController {

    public static final String BASE_NAME = "职级表";

    private final OrgPositionLevelService positionLevelService;

    private final OrgPositionLevelManager positionLevelManager;


    @Log(
        title = "职级管理",
        logType = "ORG_POSITION_LEVEL_CREATE",
        businessType = BusinessType.INSERT,
        operation = "'新增职级: ' + #param.levelName",
        targetType = "ORG_POSITION_LEVEL",
        riskLevel = 30
    )
    @PreAuthorize("hasPermission('org:position:level', 'create')")
    @PostMapping("add")
    @Operation(summary = "新增" + BASE_NAME)
    public void add(@RequestBody @Validated OrgPositionLevelCreateParam param) {
        OrgPositionLevelDTO dto = OrgPositionLevelConvert.INSTANCE.toDTO(param);
        positionLevelService.addOrgPositionLevel(dto);
    }


    @Log(
        title = "职级管理",
        logType = "ORG_POSITION_LEVEL_DELETE",
        businessType = BusinessType.DELETE,
        operation = "'删除职级: ' + #param.ids",
        targetId = "#param.ids",
        targetType = "ORG_POSITION_LEVEL",
        riskLevel = 60
    )
    @PreAuthorize("hasPermission('org:position:level', 'delete')")
    @PostMapping("delete")
    @Operation(summary = "删除" + BASE_NAME)
    public void delete(@RequestBody @Validated ParamIdSet<Long> param) {
        positionLevelService.deleteOrgPositionLevel(param.getIds());
    }


    @Log(
        title = "职级管理",
        logType = "ORG_POSITION_LEVEL_UPDATE",
        businessType = BusinessType.UPDATE,
        operation = "'修改职级: ' + #param.id",
        targetId = "#param.id",
        targetType = "ORG_POSITION_LEVEL",
        riskLevel = 30
    )
    @PreAuthorize("hasPermission('org:position:level', 'update')")
    @PostMapping("update")
    @Operation(summary = "更新" + BASE_NAME)
    public void update(@RequestBody @Validated OrgPositionLevelUpdateParam param) {
        OrgPositionLevelDTO dto = OrgPositionLevelConvert.INSTANCE.toDTO(param);
        positionLevelService.updateOrgPositionLevel(dto);
    }


    @PreAuthorize("hasPermission('org:position:level', 'view')")
    @GetMapping("detail")
    @Operation(summary = BASE_NAME + "详情")
    public OrgPositionLevelVO detail(Long id) {
        return OrgPositionLevelConvert.INSTANCE.toVO(positionLevelManager.getDtoById(id));
    }


    @PreAuthorize("hasPermission('org:position:level', 'view')")
    @GetMapping("page")
    @Operation(summary = BASE_NAME + "分页列表")
    public Paging<OrgPositionLevelVO> page(OrgPositionLevelPageParam param) {
        return positionLevelManager.getPage(param);
    }
}
