package com.carlos.mongodb.controller;

import com.carlos.core.pagination.Paging;
import com.carlos.core.param.ParamPage;
import com.carlos.core.response.Result;
import com.carlos.mongodb.entity.LogDocument;
import com.carlos.mongodb.service.LogDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * MongoDB CRUD 接口演示（基于 mongo-plus Lambda 查询）
 *
 * @author Carlos
 * @date 2026/3/15
 */
@RestController
@RequestMapping("/mongo/log")
@RequiredArgsConstructor
@Tag(name = "MongoDB 日志管理", description = "MongoDB 文档操作示例（mongo-plus）")
public class MongodbController {

    private final LogDocumentService logDocumentService;

    // ==================== 新增操作 ====================

    @PostMapping("/save")
    @Operation(summary = "保存日志", description = "新增或更新日志文档（自动填充 createTime/updateTime）")
    public Result<LogDocument> save(@RequestBody LogDocument document) {
        // createTime/updateTime 由 @CollectionField 自动填充，无需手动设置
        boolean saved = logDocumentService.save(document);
        return saved ? Result.success(document) : Result.error("保存失败");
    }

    @PostMapping("/saveBatch")
    @Operation(summary = "批量保存日志", description = "批量新增日志文档")
    public Result<List<LogDocument>> saveBatch(@RequestBody List<LogDocument> documents) {
        boolean saved = logDocumentService.saveBatch(documents);
        return saved ? Result.success(documents) : Result.error("批量保存失败");
    }

    // ==================== 查询操作 ====================

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询", description = "根据文档ID查询日志")
    @Parameter(name = "id", description = "文档ID", required = true)
    public Result<LogDocument> getById(@PathVariable String id) {
        LogDocument document = logDocumentService.getById(id);
        return Optional.ofNullable(document)
            .map(Result::success)
            .orElseGet(() -> Result.error("文档不存在"));
    }

    @GetMapping("/list")
    @Operation(summary = "查询所有", description = "查询所有日志文档")
    public Result<List<LogDocument>> list() {
        List<LogDocument> list = logDocumentService.list();
        return Result.success(list);
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询", description = "分页查询日志文档（使用 Carlos Paging）")
    public Result<Paging<LogDocument>> page(
        @RequestParam(defaultValue = "1") Integer pageNum,
        @RequestParam(defaultValue = "10") Integer pageSize) {
        ParamPage param = new ParamPage();
        param.setCurrent(pageNum);
        param.setSize(pageSize);
        Paging<LogDocument> page = logDocumentService.pageInfo(param);
        return Result.success(page);
    }

    @GetMapping("/byLevel")
    @Operation(summary = "根据级别查询", description = "使用 Lambda 查询根据日志级别查询")
    @Parameter(name = "level", description = "日志级别(INFO/WARN/ERROR)", required = true)
    public Result<List<LogDocument>> findByLevel(@RequestParam String level) {
        List<LogDocument> list = logDocumentService.findByLevel(level);
        return Result.success(list);
    }

    @GetMapping("/byModule")
    @Operation(summary = "根据模块查询", description = "使用 Lambda 模糊查询根据模块名称查询")
    @Parameter(name = "module", description = "模块名称", required = true)
    public Result<List<LogDocument>> findByModuleLike(@RequestParam String module) {
        List<LogDocument> list = logDocumentService.findByModuleLike(module);
        return Result.success(list);
    }

    @GetMapping("/byUserId")
    @Operation(summary = "根据用户查询", description = "使用 Lambda 查询根据用户ID查询")
    @Parameter(name = "userId", description = "用户ID", required = true)
    public Result<List<LogDocument>> findByUserId(@RequestParam Long userId) {
        List<LogDocument> list = logDocumentService.findByUserId(userId);
        return Result.success(list);
    }

    // ==================== 删除操作 ====================

    @DeleteMapping("/{id}")
    @Operation(summary = "根据ID删除", description = "根据文档ID删除日志")
    @Parameter(name = "id", description = "文档ID", required = true)
    public Result<Void> removeById(@PathVariable String id) {
        boolean removed = logDocumentService.removeById(id);
        return removed ? Result.success() : Result.error("删除失败或文档不存在");
    }

    @DeleteMapping("/batch")
    @Operation(summary = "批量删除", description = "批量删除日志文档")
    public Result<Void> removeBatch(@RequestBody List<LogDocument> documents) {
        boolean removed = logDocumentService.removeBatchByIds(
            documents.stream().map(LogDocument::getId).toList()
        );
        return removed ? Result.success() : Result.error("批量删除失败");
    }

    @DeleteMapping("/all")
    @Operation(summary = "删除所有", description = "清空所有日志文档(谨慎使用)")
    public Result<Void> removeAll() {
        // mongo-plus: 通过空条件删除所有
        logDocumentService.lambdaUpdate().remove();
        return Result.success();
    }

    // ==================== 统计操作 ====================

    @GetMapping("/count")
    @Operation(summary = "统计数量", description = "统计日志总数")
    public Result<Long> count() {
        long count = logDocumentService.count();
        return Result.success(count);
    }

    @GetMapping("/exists/{id}")
    @Operation(summary = "判断存在", description = "判断文档是否存在")
    @Parameter(name = "id", description = "文档ID", required = true)
    public Result<Boolean> existsById(@PathVariable String id) {
        boolean exists = logDocumentService.exist(id);
        return Result.success(exists);
    }
}
