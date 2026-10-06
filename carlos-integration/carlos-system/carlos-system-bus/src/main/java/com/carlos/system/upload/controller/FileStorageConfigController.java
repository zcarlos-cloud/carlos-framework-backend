package com.carlos.system.upload.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.carlos.core.param.ParamIdSet;
import com.carlos.core.response.Result;
import com.carlos.log.annotation.Log;
import com.carlos.log.enums.BusinessType;
import com.carlos.system.upload.pojo.dto.FileStorageConfigDTO;
import com.carlos.system.upload.pojo.param.FileStorageConfigCreateParam;
import com.carlos.system.upload.pojo.param.FileStorageConfigPageParam;
import com.carlos.system.upload.pojo.param.FileStorageConfigUpdateParam;
import com.carlos.system.upload.pojo.vo.FileStorageConfigVO;
import com.carlos.system.upload.service.FileStorageConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


/**
 * 文件存储配置 Controller
 *
 * @author Carlos
 * @date 2026-05-30
 */
@Tag(name = "文件存储配置管理")
@RestController
@RequiredArgsConstructor
@RequestMapping("/sys/storage-config")
public class FileStorageConfigController {

    private final FileStorageConfigService configService;

    @Operation(summary = "分页查询")
    @PreAuthorize("hasPermission('manage:storage-config', 'view')")
    @GetMapping("/page")
    public Result<IPage<FileStorageConfigVO>> page(FileStorageConfigPageParam param) {
        return Result.success(configService.page(param));
    }

    @Operation(summary = "获取详情")
    @PreAuthorize("hasPermission('manage:storage-config', 'view')")
    @GetMapping("/detail")
    public Result<FileStorageConfigVO> getById(@RequestParam Long id) {
        FileStorageConfigVO vo = configService.getById(id);
        if (vo == null) {
            return Result.error("配置不存在");
        }
        return Result.success(vo);
    }

    @Log(
        title = "存储配置",
        logType = "SYS_STORAGE_CONFIG_CREATE",
        businessType = BusinessType.INSERT,
        operation = "'新增存储配置: ' + #param.configName",
        targetType = "SYS_STORAGE_CONFIG",
        riskLevel = 60)
    @PreAuthorize("hasPermission('manage:storage-config', 'create')")
    @Operation(summary = "创建配置")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody FileStorageConfigCreateParam param) {
        configService.create(param);
        return Result.success();
    }

    @Log(
        title = "存储配置",
        logType = "SYS_STORAGE_CONFIG_UPDATE",
        businessType = BusinessType.UPDATE,
        operation = "'修改存储配置: ' + #param.configName",
        targetType = "SYS_STORAGE_CONFIG",
        targetId = "#param.id",
        riskLevel = 60)
    @PreAuthorize("hasPermission('manage:storage-config', 'update')")
    @Operation(summary = "更新配置")
    @PostMapping("/update")
    public Result<Void> update(@Valid @RequestBody FileStorageConfigUpdateParam param) {
        configService.update(param);
        return Result.success();
    }

    @Log(
        title = "存储配置",
        logType = "SYS_STORAGE_CONFIG_DELETE",
        businessType = BusinessType.DELETE,
        operation = "'删除存储配置: ' + #param.ids",
        targetType = "SYS_STORAGE_CONFIG",
        targetId = "#param.ids",
        riskLevel = 70)
    @PreAuthorize("hasPermission('manage:storage-config', 'delete')")
    @Operation(summary = "批量删除")
    @PostMapping("/delete")
    public Result<Void> delete(@RequestBody @Validated ParamIdSet<Long> param) {
        configService.delete(param.getIds());
        return Result.success();
    }

    @Log(
        title = "存储配置",
        logType = "SYS_STORAGE_CONFIG_SET_DEFAULT",
        businessType = BusinessType.UPDATE_STATUS,
        operation = "'设为默认存储配置: ' + #id",
        targetType = "SYS_STORAGE_CONFIG",
        targetId = "#id",
        riskLevel = 60)
    @PreAuthorize("hasPermission('manage:storage-config', 'setDefault')")
    @Operation(summary = "设为默认配置")
    @PostMapping("/default")
    public Result<Void> setDefault(@RequestParam Long id) {
        configService.setDefault(id);
        return Result.success();
    }

    @Operation(summary = "获取当前默认配置")
    @PreAuthorize("hasPermission('manage:storage-config', 'view')")
    @GetMapping("/default")
    public Result<FileStorageConfigDTO> getDefault() {
        return Result.success(configService.getDefault());
    }

    @Log(
        title = "存储配置",
        logType = "SYS_STORAGE_CONFIG_TEST",
        businessType = BusinessType.OTHER,
        operation = "'测试存储配置连通性: ' + #id",
        targetType = "SYS_STORAGE_CONFIG",
        targetId = "#id",
        riskLevel = 20)
    @PreAuthorize("hasPermission('manage:storage-config', 'testConnection')")
    @Operation(summary = "测试连通性")
    @PostMapping("/test")
    public Result<Boolean> testConnection(@RequestParam Long id) {
        boolean connected = configService.testConnection(id);
        return Result.success(connected);
    }
}
