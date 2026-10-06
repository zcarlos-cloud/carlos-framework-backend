package com.carlos.devkit.controller;

import com.carlos.core.pagination.Paging;
import com.carlos.core.response.Result;
import com.carlos.devkit.convert.TemplateGroupConvert;
import com.carlos.devkit.pojo.dto.TemplateGroupDTO;
import com.carlos.devkit.pojo.param.DevkitIdsParam;
import com.carlos.devkit.pojo.param.TemplateGroupCopyParam;
import com.carlos.devkit.pojo.param.TemplateGroupCreateParam;
import com.carlos.devkit.pojo.param.TemplateGroupPageParam;
import com.carlos.devkit.pojo.param.TemplateGroupStatusParam;
import com.carlos.devkit.pojo.param.TemplateGroupUpdateParam;
import com.carlos.devkit.pojo.vo.TemplateGroupVO;
import com.carlos.devkit.service.TemplateGroupService;
import com.carlos.devkit.support.PageMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 开发工具 - 模板组管理 rest 服务接口
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("devkit/template/group")
@Tag(name = "开发工具-模板组管理")
public class TemplateGroupController {

    private final TemplateGroupService templateGroupService;

    @PreAuthorize("hasPermission('devkit:template', 'view')")
    @GetMapping("/page")
    @Operation(summary = "模板组分页")
    public Result<Paging<TemplateGroupVO>> page(final TemplateGroupPageParam param) {
        return Result.success(PageMapper.map(
            templateGroupService.getPage(param), TemplateGroupConvert.INSTANCE::toVO));
    }

    @PreAuthorize("hasPermission('devkit:template', 'view')")
    @GetMapping("/list")
    @Operation(summary = "模板组列表", description = "下拉用，可按模板类型过滤")
    public Result<List<TemplateGroupVO>> list(
        @RequestParam(value = "templateType", required = false) final Integer templateType) {
        return Result.success(TemplateGroupConvert.INSTANCE.toVO(
            templateGroupService.listByType(templateType)));
    }

    @PreAuthorize("hasPermission('devkit:template', 'view')")
    @GetMapping("/{id}")
    @Operation(summary = "模板组详情")
    public Result<TemplateGroupVO> detail(@PathVariable final Long id) {
        return Result.success(TemplateGroupConvert.INSTANCE.toVO(templateGroupService.getDetail(id)));
    }

    @PreAuthorize("hasPermission('devkit:template', 'add')")
    @PostMapping("/add")
    @Operation(summary = "新增模板组")
    public Result<Long> add(@RequestBody @Validated final TemplateGroupCreateParam param) {
        TemplateGroupDTO dto = TemplateGroupConvert.INSTANCE.toDTO(param);
        return Result.success(templateGroupService.addGroup(dto));
    }

    @PreAuthorize("hasPermission('devkit:template', 'edit')")
    @PostMapping("/update")
    @Operation(summary = "编辑模板组")
    public Result<Boolean> update(@RequestBody @Validated final TemplateGroupUpdateParam param) {
        TemplateGroupDTO dto = TemplateGroupConvert.INSTANCE.toDTO(param);
        return Result.success(templateGroupService.updateGroup(dto));
    }

    @PreAuthorize("hasPermission('devkit:template', 'delete')")
    @PostMapping("/delete")
    @Operation(summary = "删除模板组", description = "连同模板文件一并逻辑删除；内置模板组不可删除")
    public Result<Boolean> delete(@RequestBody @Validated final DevkitIdsParam param) {
        boolean allSuccess = true;
        for (Long id : param.getIds()) {
            allSuccess &= templateGroupService.deleteGroup(id);
        }
        return Result.success(allSuccess);
    }

    @PreAuthorize("hasPermission('devkit:template', 'add')")
    @PostMapping("/copy")
    @Operation(summary = "复制模板组")
    public Result<Long> copy(@RequestBody @Validated final TemplateGroupCopyParam param) {
        return Result.success(templateGroupService.copyGroup(param));
    }

    @PreAuthorize("hasPermission('devkit:template', 'edit')")
    @PostMapping("/toggle-status")
    @Operation(summary = "切换模板组启用状态")
    public Result<Boolean> toggleStatus(@RequestBody @Validated final TemplateGroupStatusParam param) {
        return Result.success(templateGroupService.toggleStatus(param.getId(), param.getStatus()));
    }
}
