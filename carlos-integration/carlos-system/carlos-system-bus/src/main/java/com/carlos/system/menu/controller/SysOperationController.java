package com.carlos.system.menu.controller;

import com.carlos.core.pagination.Paging;
import com.carlos.core.param.ParamIdSet;
import com.carlos.core.response.Result;
import com.carlos.log.annotation.Log;
import com.carlos.log.enums.BusinessType;
import com.carlos.system.menu.convert.SysOperationConvert;
import com.carlos.system.menu.manager.SysOperationManager;
import com.carlos.system.menu.pojo.dto.SysOperationDTO;
import com.carlos.system.menu.pojo.param.SysOperationCreateParam;
import com.carlos.system.menu.pojo.param.SysOperationMoveParam;
import com.carlos.system.menu.pojo.param.SysOperationPageParam;
import com.carlos.system.menu.pojo.param.SysOperationUpdateParam;
import com.carlos.system.menu.pojo.vo.SysOperationVO;
import com.carlos.system.menu.service.SysOperationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 统一操作表 rest服务接口
 * </p>
 *
 * @author carlos
 * @date 2026-05-27
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/sys/operation")
@Tag(name = "统一操作权限")
public class SysOperationController {

    public static final String BASE_NAME = "统一操作";

    private final SysOperationService sysOperationService;
    private final SysOperationManager sysOperationManager;

    @Log(
        title = "操作权限",
        logType = "SYS_OPERATION_CREATE",
        businessType = BusinessType.INSERT,
        operation = "'新增操作权限: ' + #param.opName",
        targetType = "SYS_OPERATION",
        riskLevel = 40)
    @PreAuthorize("hasPermission('manage:operation', 'create')")
    @PostMapping("add")
    @Operation(summary = "新增" + BASE_NAME)
    public void add(@RequestBody @Validated SysOperationCreateParam param) {
        SysOperationDTO dto = SysOperationConvert.INSTANCE.toDTO(param);
        sysOperationService.addOperation(dto);
    }

    @Log(
        title = "操作权限",
        logType = "SYS_OPERATION_DELETE",
        businessType = BusinessType.DELETE,
        operation = "'删除操作权限: ' + #param.ids",
        targetType = "SYS_OPERATION",
        targetId = "#param.ids",
        riskLevel = 60)
    @PreAuthorize("hasPermission('manage:operation', 'delete')")
    @PostMapping("delete")
    @Operation(summary = "删除" + BASE_NAME)
    public void delete(@RequestBody @Validated ParamIdSet<String> param) {
        sysOperationService.deleteOperation(param.getIds());
    }

    @Log(
        title = "操作权限",
        logType = "SYS_OPERATION_UPDATE",
        businessType = BusinessType.UPDATE,
        operation = "'修改操作权限: ' + #param.opName",
        targetType = "SYS_OPERATION",
        targetId = "#param.id",
        riskLevel = 40)
    @PreAuthorize("hasPermission('manage:operation', 'update')")
    @PostMapping("update")
    @Operation(summary = "更新" + BASE_NAME)
    public void update(@RequestBody @Validated SysOperationUpdateParam param) {
        SysOperationDTO dto = SysOperationConvert.INSTANCE.toDTO(param);
        sysOperationService.updateOperation(dto);
    }

    @Log(
        title = "操作权限",
        logType = "SYS_OPERATION_MOVE",
        businessType = BusinessType.UPDATE,
        operation = "'移动操作权限: ' + #param.id + ' 到菜单: ' + #param.menuId",
        targetType = "SYS_OPERATION",
        targetId = "#param.id",
        riskLevel = 40)
    @PreAuthorize("hasPermission('manage:operation', 'move')")
    @PostMapping("move")
    @Operation(summary = "移动" + BASE_NAME + "到指定菜单（menuId 为空则摘除菜单，成为独立接口）")
    public Result<Boolean> move(@RequestBody @Validated SysOperationMoveParam param) {
        return Result.success(sysOperationService.moveOperation(param.getId(), param.getMenuId()));
    }

    @PreAuthorize("hasPermission('manage:operation', 'view')")
    @GetMapping("detail")
    @Operation(summary = BASE_NAME + "详情")
    public SysOperationVO detail(@RequestParam Long id) {
        return SysOperationConvert.INSTANCE.toVO(sysOperationManager.getDtoById(id));
    }

    @PreAuthorize("hasPermission('manage:operation', 'view')")
    @GetMapping("page")
    @Operation(summary = BASE_NAME + "分页列表")
    public Paging<SysOperationVO> page(SysOperationPageParam param) {
        return sysOperationManager.getPage(param);
    }

    @PreAuthorize("hasPermission('manage:operation', 'view')")
    @GetMapping("listByMenuId")
    @Operation(summary = "根据菜单ID查询" + BASE_NAME + "列表")
    public List<SysOperationVO> listByMenuId(@RequestParam Long menuId) {
        List<SysOperationDTO> list = sysOperationService.listByMenuId(menuId);
        return SysOperationConvert.INSTANCE.toVOS(list);
    }

}
