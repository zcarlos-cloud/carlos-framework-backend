package com.carlos.org.position.controller;

import com.carlos.core.pagination.Paging;
import com.carlos.core.param.ParamIdSet;
import com.carlos.log.annotation.Log;
import com.carlos.log.enums.BusinessType;
import com.carlos.org.position.convert.OrgPositionCategoryConvert;
import com.carlos.org.position.manager.OrgPositionCategoryManager;
import com.carlos.org.position.pojo.dto.OrgPositionCategoryDTO;
import com.carlos.org.position.pojo.param.OrgPositionCategoryCreateParam;
import com.carlos.org.position.pojo.param.OrgPositionCategoryPageParam;
import com.carlos.org.position.pojo.param.OrgPositionCategoryUpdateParam;
import com.carlos.org.position.pojo.vo.OrgPositionCategoryVO;
import com.carlos.org.position.service.OrgPositionCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


/**
 * <p>
 * 岗位类别表（职系） rest服务接口
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("org/position/category")
@Tag(name = "岗位类别表（职系）")
public class OrgPositionCategoryController {

    public static final String BASE_NAME = "岗位类别表（职系）";

    private final OrgPositionCategoryService positionCategoryService;

    private final OrgPositionCategoryManager positionCategoryManager;


    @Log(
        title = "岗位类别管理",
        logType = "ORG_POSITION_CATEGORY_CREATE",
        businessType = BusinessType.INSERT,
        operation = "'新增岗位类别: ' + #param.categoryName",
        targetType = "ORG_POSITION_CATEGORY",
        riskLevel = 30
    )
    @PreAuthorize("hasPermission('org:position:category', 'create')")
    @PostMapping("add")
    @Operation(summary = "新增" + BASE_NAME)
    public void add(@RequestBody @Validated OrgPositionCategoryCreateParam param) {
        OrgPositionCategoryDTO dto = OrgPositionCategoryConvert.INSTANCE.toDTO(param);
        positionCategoryService.addOrgPositionCategory(dto);
    }


    @Log(
        title = "岗位类别管理",
        logType = "ORG_POSITION_CATEGORY_DELETE",
        businessType = BusinessType.DELETE,
        operation = "'删除岗位类别: ' + #param.ids",
        targetId = "#param.ids",
        targetType = "ORG_POSITION_CATEGORY",
        riskLevel = 60
    )
    @PreAuthorize("hasPermission('org:position:category', 'delete')")
    @PostMapping("delete")
    @Operation(summary = "删除" + BASE_NAME)
    public void delete(@RequestBody @Validated ParamIdSet<Long> param) {
        positionCategoryService.deleteOrgPositionCategory(param.getIds());
    }


    @Log(
        title = "岗位类别管理",
        logType = "ORG_POSITION_CATEGORY_UPDATE",
        businessType = BusinessType.UPDATE,
        operation = "'修改岗位类别: ' + #param.id",
        targetId = "#param.id",
        targetType = "ORG_POSITION_CATEGORY",
        riskLevel = 30
    )
    @PreAuthorize("hasPermission('org:position:category', 'update')")
    @PostMapping("update")
    @Operation(summary = "更新" + BASE_NAME)
    public void update(@RequestBody @Validated OrgPositionCategoryUpdateParam param) {
        OrgPositionCategoryDTO dto = OrgPositionCategoryConvert.INSTANCE.toDTO(param);
        positionCategoryService.updateOrgPositionCategory(dto);
    }


    @PreAuthorize("hasPermission('org:position:category', 'view')")
    @GetMapping("detail")
    @Operation(summary = BASE_NAME + "详情")
    public OrgPositionCategoryVO detail(Long id) {
        return OrgPositionCategoryConvert.INSTANCE.toVO(positionCategoryManager.getDtoById(id));
    }


    @PreAuthorize("hasPermission('org:position:category', 'view')")
    @GetMapping("page")
    @Operation(summary = BASE_NAME + "分页列表")
    public Paging<OrgPositionCategoryVO> page(OrgPositionCategoryPageParam param) {
        return positionCategoryManager.getPage(param);
    }
}
