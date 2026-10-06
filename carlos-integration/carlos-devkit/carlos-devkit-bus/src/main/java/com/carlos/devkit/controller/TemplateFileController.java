package com.carlos.devkit.controller;

import com.carlos.boot.web.annotation.NoWrap;
import com.carlos.core.response.Result;
import com.carlos.devkit.convert.TemplateFileConvert;
import com.carlos.devkit.pojo.param.DevkitIdsParam;
import com.carlos.devkit.pojo.param.TemplateFileCreateParam;
import com.carlos.devkit.pojo.param.TemplateFileRenameParam;
import com.carlos.devkit.pojo.param.TemplateFileSaveParam;
import com.carlos.devkit.pojo.vo.TemplateFileNodeVO;
import com.carlos.devkit.pojo.vo.TemplateFileVO;
import com.carlos.devkit.service.TemplateFileService;
import com.carlos.devkit.support.ZipPacker;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
 * 开发工具 - 模板文件管理 rest 服务接口
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("devkit/template/file")
@Tag(name = "开发工具-模板文件管理")
public class TemplateFileController {

    private final TemplateFileService templateFileService;

    @PreAuthorize("hasPermission('devkit:template', 'view')")
    @GetMapping("/tree")
    @Operation(summary = "模板文件树")
    public Result<List<TemplateFileNodeVO>> tree(@RequestParam("groupId") final Long groupId) {
        return Result.success(templateFileService.listTree(groupId));
    }

    @PreAuthorize("hasPermission('devkit:template', 'view')")
    @GetMapping("/{id}")
    @Operation(summary = "模板文件详情", description = "含正文，供在线编辑")
    public Result<TemplateFileVO> detail(@PathVariable final Long id) {
        return Result.success(TemplateFileConvert.INSTANCE.toDetailVO(
            templateFileService.getDetail(id)));
    }

    @PreAuthorize("hasPermission('devkit:template', 'add')")
    @PostMapping("/add")
    @Operation(summary = "新增模板文件")
    public Result<Long> add(@RequestBody @Validated final TemplateFileCreateParam param) {
        return Result.success(templateFileService.createFile(param));
    }

    @PreAuthorize("hasPermission('devkit:template', 'edit')")
    @PostMapping("/save")
    @Operation(summary = "保存模板正文", description = "保存前自动落一版历史，可回滚")
    public Result<Boolean> save(@RequestBody @Validated final TemplateFileSaveParam param) {
        return Result.success(templateFileService.saveFile(param));
    }

    @PreAuthorize("hasPermission('devkit:template', 'edit')")
    @PostMapping("/rename")
    @Operation(summary = "重命名 / 移动模板文件")
    public Result<Boolean> rename(@RequestBody @Validated final TemplateFileRenameParam param) {
        return Result.success(templateFileService.renameFile(param));
    }

    @PreAuthorize("hasPermission('devkit:template', 'delete')")
    @PostMapping("/delete")
    @Operation(summary = "批量删除模板文件")
    public Result<Boolean> delete(@RequestBody @Validated final DevkitIdsParam param) {
        return Result.success(templateFileService.deleteFiles(param.getIds()));
    }

    /**
     * 导出模板组为 zip
     *
     * <p>{@code @NoWrap} 必不可少：去掉后 {@code ResponseEntity<byte[]>} 会被
     * {@code ResponseWrapAdvice} 包成 {@code Result} 信封，表现为 HTTP 200、文件名正常、
     * 但下载下来的 zip 打不开。参照 carlos-workflow 的导出端点。</p>
     *
     * @param groupId 模板组ID
     * @return zip 响应
     */
    @NoWrap
    @PreAuthorize("hasPermission('devkit:template', 'view')")
    @GetMapping("/download")
    @Operation(summary = "导出模板组", description = "zip 直出，不走 Result 信封")
    public ResponseEntity<byte[]> download(@RequestParam("groupId") final Long groupId) {
        byte[] content = templateFileService.exportGroup(groupId);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, ZipPacker.contentDisposition("template-" + groupId))
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(content);
    }
}
