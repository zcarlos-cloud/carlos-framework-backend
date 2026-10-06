package com.carlos.devkit.controller;

import com.carlos.boot.web.annotation.NoWrap;
import com.carlos.core.response.Result;
import com.carlos.devkit.convert.TemplateGroupConvert;
import com.carlos.devkit.pojo.param.ProjectGenRunParam;
import com.carlos.devkit.pojo.vo.GeneratedResultVO;
import com.carlos.devkit.pojo.vo.TemplateGroupVO;
import com.carlos.devkit.service.ProjectGenService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 开发工具 - 工程生成 rest 服务接口
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("devkit/projectgen")
@Tag(name = "开发工具-工程生成")
public class ProjectGenController {

    private final ProjectGenService projectGenService;

    @PreAuthorize("hasPermission('devkit:project', 'view')")
    @GetMapping("/template/list")
    @Operation(summary = "可用的工程模板组")
    public Result<List<TemplateGroupVO>> templates() {
        return Result.success(TemplateGroupConvert.INSTANCE.toVO(projectGenService.listTemplates()));
    }

    @PreAuthorize("hasPermission('devkit:project', 'preview')")
    @PostMapping("/preview")
    @Operation(summary = "工程生成预览")
    public Result<GeneratedResultVO> preview(@RequestBody @Validated final ProjectGenRunParam param) {
        return Result.success(GeneratedResultMapper.toVO(projectGenService.preview(param)));
    }

    /**
     * 工程生成并打包下载
     *
     * <p>{@code @NoWrap} 必不可少，否则 zip 会被包成 {@code Result} 信封导致下载文件损坏。</p>
     *
     * @param param 生成参数
     * @return zip 响应
     */
    @NoWrap
    @PreAuthorize("hasPermission('devkit:project', 'generate')")
    @PostMapping("/generate")
    @Operation(summary = "工程生成下载", description = "zip 直出，不走 Result 信封")
    public ResponseEntity<byte[]> generate(@RequestBody @Validated final ProjectGenRunParam param) {
        byte[] content = projectGenService.generate(param);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, ZipPacker.contentDisposition(param.getArtifactId()))
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(content);
    }
}
