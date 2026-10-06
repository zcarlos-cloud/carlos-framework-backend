package com.carlos.sample.datacenter.controller;

import com.carlos.core.response.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据中心样例控制器
 * 演示数据字典查询、参数配置等接口
 *
 * @author carlos
 */
@Slf4j
@RestController
@RequestMapping("/api/datacenter")
@RequiredArgsConstructor
@Tag(name = "数据中心接口", description = "数据字典查询、参数配置等接口")
public class DatacenterController {

    /**
     * 获取数据字典列表
     */
    @GetMapping("/dict/list")
    @Operation(summary = "获取数据字典列表", description = "根据字典类型查询数据字典列表")
    public Result<List<Map<String, Object>>> getDictList(
        @Parameter(description = "字典类型") @RequestParam String dictType) {
        log.info("获取数据字典列表, dictType: {}", dictType);

        // 模拟数据字典数据
        List<Map<String, Object>> dictList = List.of(
            createDictItem("1", "启用", "enable", 1),
            createDictItem("2", "禁用", "disable", 2),
            createDictItem("3", "待审核", "pending", 3)
        );

        return Result.success(dictList);
    }

    /**
     * 根据字典编码获取字典详情
     */
    @GetMapping("/dict/{dictCode}")
    @Operation(summary = "获取字典详情", description = "根据字典编码获取字典详情")
    public Result<Map<String, Object>> getDictByCode(
        @Parameter(description = "字典编码") @PathVariable String dictCode) {
        log.info("获取字典详情, dictCode: {}", dictCode);

        Map<String, Object> dict = createDictItem("1", "启用", "enable", 1);
        dict.put("remark", "系统启用状态");
        dict.put("createTime", "2024-01-01 00:00:00");

        return Result.success(dict);
    }

    /**
     * 获取系统参数配置
     */
    @GetMapping("/config/{configKey}")
    @Operation(summary = "获取参数配置", description = "根据配置键获取系统参数配置值")
    public Result<String> getConfig(
        @Parameter(description = "配置键") @PathVariable String configKey) {
        log.info("获取参数配置, configKey: {}", configKey);

        // 模拟配置值
        Map<String, String> configMap = Map.of(
            "sys.name", "Carlos 数据中心",
            "sys.version", "3.0.0",
            "upload.maxSize", "10485760",
            "upload.allowedTypes", "jpg,png,gif,pdf"
        );

        String configValue = configMap.getOrDefault(configKey, "default_value");
        return Result.success(configValue);
    }

    /**
     * 获取所有参数配置
     */
    @GetMapping("/config/list")
    @Operation(summary = "获取所有参数配置", description = "获取系统所有参数配置列表")
    public Result<List<Map<String, Object>>> getConfigList() {
        log.info("获取所有参数配置");

        List<Map<String, Object>> configList = List.of(
            createConfigItem("sys.name", "Carlos 数据中心", "系统名称", "系统配置"),
            createConfigItem("sys.version", "3.0.0", "系统版本", "系统配置"),
            createConfigItem("upload.maxSize", "10485760", "上传文件最大大小(字节)", "上传配置"),
            createConfigItem("upload.allowedTypes", "jpg,png,gif,pdf", "允许上传的文件类型", "上传配置")
        );

        return Result.success(configList);
    }

    /**
     * 刷新数据字典缓存
     */
    @PostMapping("/dict/refresh")
    @Operation(summary = "刷新字典缓存", description = "刷新数据字典缓存")
    public Result<Void> refreshDictCache() {
        log.info("刷新数据字典缓存");
        // 模拟刷新缓存操作
        return Result.success();
    }

    /**
     * 刷新参数配置缓存
     */
    @PostMapping("/config/refresh")
    @Operation(summary = "刷新配置缓存", description = "刷新系统参数配置缓存")
    public Result<Void> refreshConfigCache() {
        log.info("刷新参数配置缓存");
        // 模拟刷新缓存操作
        return Result.success();
    }

    /**
     * 批量获取数据字典
     */
    @PostMapping("/dict/batch")
    @Operation(summary = "批量获取字典", description = "根据字典类型列表批量获取数据字典")
    public Result<Map<String, List<Map<String, Object>>>> getDictBatch(
        @Parameter(description = "字典类型列表") @RequestBody List<String> dictTypes) {
        log.info("批量获取字典, dictTypes: {}", dictTypes);

        Map<String, List<Map<String, Object>>> result = new HashMap<>();
        for (String dictType : dictTypes) {
            result.put(dictType, List.of(
                createDictItem("1", "选项1", "option1", 1),
                createDictItem("2", "选项2", "option2", 2)
            ));
        }

        return Result.success(result);
    }

    // ==================== 私有辅助方法 ====================

    private Map<String, Object> createDictItem(String id, String label, String value, Integer sort) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", id);
        item.put("label", label);
        item.put("value", value);
        item.put("sort", sort);
        return item;
    }

    private Map<String, Object> createConfigItem(String key, String value, String description, String category) {
        Map<String, Object> item = new HashMap<>();
        item.put("configKey", key);
        item.put("configValue", value);
        item.put("description", description);
        item.put("category", category);
        return item;
    }
}
