package com.carlos.devkit.controller;

import com.carlos.boot.web.annotation.NoWrap;
import com.carlos.core.response.Result;
import com.carlos.devkit.engine.model.ColumnMeta;
import com.carlos.devkit.engine.model.TableBaseInfo;
import com.carlos.devkit.pojo.dto.TemplateRenderPreviewDTO;
import com.carlos.devkit.pojo.param.CodeGenRunParam;
import com.carlos.devkit.pojo.param.TemplateRenderPreviewParam;
import com.carlos.devkit.pojo.vo.ColumnMetaVO;
import com.carlos.devkit.pojo.vo.GeneratedResultVO;
import com.carlos.devkit.pojo.vo.TableMetaVO;
import com.carlos.devkit.pojo.vo.TemplateRenderPreviewVO;
import com.carlos.devkit.service.CodeGenService;
import com.carlos.devkit.support.GeneratedResultMapper;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * <p>
 * 开发工具 - 代码生成 rest 服务接口
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("devkit/codegen")
@Tag(name = "开发工具-代码生成")
public class CodeGenController {

    private final CodeGenService codeGenService;

    @PreAuthorize("hasPermission('devkit:codegen', 'view')")
    @GetMapping("/schemas")
    @Operation(summary = "目标实例下的库 / schema 清单")
    public Result<List<String>> schemas(@RequestParam("dsId") final Long dsId) {
        return Result.success(codeGenService.listSchemas(dsId));
    }

    @PreAuthorize("hasPermission('devkit:codegen', 'view')")
    @GetMapping("/tables")
    @Operation(summary = "目标库下的表清单")
    public Result<List<TableMetaVO>> tables(@RequestParam("dsId") final Long dsId,
                                            @RequestParam(value = "schema", required = false) final String schema,
                                            @RequestParam(value = "keyword", required = false) final String keyword) {
        List<TableMetaVO> list = codeGenService.listTables(dsId, schema, keyword).stream()
            .map(item -> new TableMetaVO(item.getName(), item.getComment()))
            .toList();
        return Result.success(list);
    }

    @PreAuthorize("hasPermission('devkit:codegen', 'view')")
    @GetMapping("/columns")
    @Operation(summary = "目标表的列元数据")
    public Result<List<ColumnMetaVO>> columns(@RequestParam("dsId") final Long dsId,
                                              @RequestParam(value = "schema", required = false) final String schema,
                                              @RequestParam("tableName") final String tableName) {
        return Result.success(codeGenService.listColumns(dsId, schema, tableName).stream()
            .map(this::toColumnVO)
            .toList());
    }

    @PreAuthorize("hasPermission('devkit:codegen', 'preview')")
    @PostMapping("/preview")
    @Operation(summary = "代码生成预览", description = "产物在内存中返回，供文件树 + 代码高亮")
    public Result<GeneratedResultVO> preview(@RequestBody @Validated final CodeGenRunParam param) {
        return Result.success(GeneratedResultMapper.toVO(codeGenService.preview(param)));
    }

    /**
     * 代码生成并打包下载
     *
     * <p>{@code @NoWrap} 必不可少，否则 zip 会被包成 {@code Result} 信封导致下载文件损坏。</p>
     *
     * @param param 生成参数
     * @return zip 响应
     */
    @NoWrap
    @PreAuthorize("hasPermission('devkit:codegen', 'generate')")
    @PostMapping("/generate")
    @Operation(summary = "代码生成下载", description = "zip 直出，不走 Result 信封")
    public ResponseEntity<byte[]> generate(@RequestBody @Validated final CodeGenRunParam param) {
        byte[] content = codeGenService.generate(param);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, ZipPacker.contentDisposition(param.getProjectName()))
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(content);
    }

    @PreAuthorize("hasPermission('devkit:codegen', 'preview')")
    @PostMapping("/template-preview")
    @Operation(summary = "单模板渲染预览", description = "用样例表渲染模板正文，供在线编辑即时看效果")
    public Result<TemplateRenderPreviewVO> templatePreview(
        @RequestBody @Validated final TemplateRenderPreviewParam param) {
        TemplateRenderPreviewDTO dto = codeGenService.previewTemplate(param);
        return Result.success(new TemplateRenderPreviewVO()
            .setSuccess(dto.getSuccess())
            .setContent(dto.getContent())
            .setError(dto.getError()));
    }

    /**
     * 列元数据 → VO
     *
     * @param meta 列元数据
     * @return 列 VO
     */
    private ColumnMetaVO toColumnVO(final ColumnMeta meta) {
        return new ColumnMetaVO()
            .setColumnName(meta.getColumnName())
            .setColumnDbType(meta.getColumnDbType())
            .setJavaType(meta.getJavaType())
            .setColumnComment(meta.getColumnComment())
            .setPrimaryKey(meta.isPrimaryKey())
            .setNullable(meta.isNullable())
            .setAutoIncrement(meta.isAutoIncrement())
            .setLogicField(meta.isLogicField())
            .setVersionField(meta.isVersionField())
            .setCommonField(meta.isCommonField())
            .setPropertyName(meta.getPropertyName());
    }
}
