package com.carlos.auth.app.controller;

import com.carlos.auth.app.convert.AppCategoryConvert;
import com.carlos.auth.app.pojo.dto.AppCategoryDTO;
import com.carlos.auth.app.pojo.param.AppCategoryCreateParam;
import com.carlos.auth.app.pojo.param.AppCategoryUpdateParam;
import com.carlos.auth.app.pojo.vo.AppCategoryVO;
import com.carlos.auth.app.service.AppCategoryService;
import com.carlos.core.param.ParamId;
import com.carlos.core.response.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 应用分类管理
 *
 * @author Carlos
 * @date 2026-07-02
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("auth/app/category")
@Tag(name = "应用分类管理")
public class AppCategoryController {

    private final AppCategoryService appCategoryService;

    @PreAuthorize("hasPermission('manage:app-category', 'view')")
    @GetMapping("list")
    @Operation(summary = "分类列表")
    public Result<List<AppCategoryVO>> list() {
        List<AppCategoryDTO> dtoList = appCategoryService.listAll();
        return Result.success(AppCategoryConvert.INSTANCE.toVO(dtoList));
    }

    @PreAuthorize("hasPermission('manage:app-category', 'create')")
    @PostMapping("add")
    @Operation(summary = "新增分类")
    public Result<Void> add(@RequestBody @Valid AppCategoryCreateParam param) {
        AppCategoryDTO dto = AppCategoryConvert.INSTANCE.toDTO(param);
        appCategoryService.add(dto);
        return Result.success();
    }

    @PreAuthorize("hasPermission('manage:app-category', 'update')")
    @PostMapping("update")
    @Operation(summary = "更新分类")
    public Result<Void> update(@RequestBody @Valid AppCategoryUpdateParam param) {
        AppCategoryDTO dto = AppCategoryConvert.INSTANCE.toDTO(param);
        appCategoryService.modify(dto);
        return Result.success();
    }

    @PreAuthorize("hasPermission('manage:app-category', 'delete')")
    @PostMapping("delete")
    @Operation(summary = "删除分类")
    public Result<Void> delete(@RequestBody @Valid ParamId<Long> param) {
        appCategoryService.delete(param.getId());
        return Result.success();
    }

}
