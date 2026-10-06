package com.carlos.boot.web.controller;

import com.carlos.boot.web.config.WebProperties;
import com.carlos.boot.web.enums.EnumDictService;
import com.carlos.boot.web.resource.ResourceScanService;
import com.carlos.core.response.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 应用信息控制器。
 * </p>
 * <p>
 * 提供枚举字典查询和资源扫描初始化等管理端点。
 * </p>
 *
 * @author carlos
 * @since 3.0.0
 */
@Slf4j
@RestController
@RequestMapping("/app")
@Tag(name = "框架内置接口")
public class ApplicationController {

    private final WebProperties webProperties;
    private final EnumDictService enumService;
    private final ResourceScanService resourceScanService;

    public ApplicationController(WebProperties webProperties,
                                 @Nullable EnumDictService enumService,
                                 @Nullable ResourceScanService resourceScanService) {
        this.webProperties = webProperties;
        this.enumService = enumService;
        this.resourceScanService = resourceScanService;
    }

    /**
     * 获取全部枚举字典列表。
     *
     * @return 枚举字典 Map
     */
    @GetMapping("/enums")
    @Operation(summary = "枚举字典列表")
    public Result<?> enumList() {
        if (enumService == null) {
            return Result.error("枚举功能未开启");
        }
        return Result.success(enumService.getDict());
    }

    /**
     * 获取指定枚举的详情。
     *
     * @param name 枚举标识名称
     * @return 枚举详情
     */
    @GetMapping("/enum")
    @Operation(summary = "枚举详情")
    public Result<?> enumInfo(@RequestParam("name") String name) {
        if (enumService == null) {
            return Result.error("枚举功能未开启");
        }
        return Result.success(enumService.getDictEntry(name));
    }

    /**
     * 触发资源扫描初始化。
     *
     * @return 扫描结果
     */
    @GetMapping("/init/resource")
    @Operation(summary = "初始化资源")
    public Result<?> initResource() {
        if (resourceScanService == null) {
            return Result.error("资源扫描功能未开启");
        }
        return Result.success(resourceScanService.scan());
    }

}
