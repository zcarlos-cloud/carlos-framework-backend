package com.carlos.system.menu.controller;

import com.carlos.core.pagination.Paging;
import com.carlos.core.param.ParamIdList;
import com.carlos.log.annotation.Log;
import com.carlos.log.enums.BusinessType;
import com.carlos.system.menu.convert.SysModuleConvert;
import com.carlos.system.menu.manager.SysModuleManager;
import com.carlos.system.menu.pojo.param.SysModuleCreateParam;
import com.carlos.system.menu.pojo.param.SysModulePageParam;
import com.carlos.system.menu.pojo.param.SysModuleUpdateParam;
import com.carlos.system.menu.pojo.vo.SysModuleVO;
import com.carlos.system.menu.service.SysModuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 功能模块 Rest服务接口
 * </p>
 *
 * @author carlos
 * @date 2026-05-26
 */
@RestController
@RequestMapping("/sys/module")
@RequiredArgsConstructor
@Tag(name = "功能模块")
public class SysModuleController {

    public static final String BASE_NAME = "功能模块";

    private final SysModuleService moduleService;
    private final SysModuleManager moduleManager;

    @Log(
        title = "模块管理",
        logType = "SYS_MODULE_CREATE",
        businessType = BusinessType.INSERT,
        operation = "'新增模块: ' + #param.moduleName",
        targetType = "SYS_MODULE",
        riskLevel = 40)
    @PreAuthorize("hasPermission('manage:module', 'create')")
    @PostMapping("add")
    @Operation(summary = "新增" + BASE_NAME)
    public void add(@RequestBody @Validated SysModuleCreateParam param) {
        moduleService.add(param);
    }

    @Log(
        title = "模块管理",
        logType = "SYS_MODULE_DELETE",
        businessType = BusinessType.DELETE,
        operation = "'删除模块: ' + #param.ids",
        targetType = "SYS_MODULE",
        targetId = "#param.ids",
        riskLevel = 60)
    @PreAuthorize("hasPermission('manage:module', 'delete')")
    @PostMapping("delete")
    @Operation(summary = "删除" + BASE_NAME)
    public void delete(@RequestBody @Validated ParamIdList<Long> param) {
        moduleService.delete(param.getIds());
    }

    @Log(
        title = "模块管理",
        logType = "SYS_MODULE_UPDATE",
        businessType = BusinessType.UPDATE,
        operation = "'修改模块: ' + #param.moduleName",
        targetType = "SYS_MODULE",
        targetId = "#param.id",
        riskLevel = 40)
    @PreAuthorize("hasPermission('manage:module', 'update')")
    @PostMapping("update")
    @Operation(summary = "更新" + BASE_NAME)
    public void update(@RequestBody @Validated SysModuleUpdateParam param) {
        moduleService.update(param);
    }

    @PreAuthorize("hasPermission('manage:module', 'view')")
    @GetMapping("detail")
    @Operation(summary = BASE_NAME + "详情")
    public SysModuleVO detail(@RequestParam Long id) {
        return SysModuleConvert.INSTANCE.toVO(moduleManager.getDtoById(id));
    }

    @PreAuthorize("hasPermission('manage:module', 'view')")
    @GetMapping("page")
    @Operation(summary = BASE_NAME + "分页列表")
    public Paging<SysModuleVO> page(SysModulePageParam param) {
        return moduleManager.getPage(param);
    }

    @PreAuthorize("hasPermission('manage:module', 'view')")
    @GetMapping("list")
    @Operation(summary = BASE_NAME + "列表")
    public List<SysModuleVO> list() {
        return SysModuleConvert.INSTANCE.toVO(
            SysModuleConvert.INSTANCE.toDTO(moduleManager.listEnabled())
        );
    }

    /**
     * 全部模块（含禁用）
     *
     * <p>
     * `/sys/module/list` 走的是 `listEnabled()`，只返回启用中的模块；
     * 管理后台的模块树与「上级模块」下拉都必须看到禁用模块，否则禁用一个模块后
     * 它就彻底从界面上消失，连改回来都做不到。
     * </p>
     */
    @PreAuthorize("hasPermission('manage:module', 'view')")
    @GetMapping("all")
    @Operation(summary = BASE_NAME + "全部列表（含禁用）")
    public List<SysModuleVO> all() {
        return SysModuleConvert.INSTANCE.toVO(
            SysModuleConvert.INSTANCE.toDTO(moduleManager.listAll())
        );
    }

}
