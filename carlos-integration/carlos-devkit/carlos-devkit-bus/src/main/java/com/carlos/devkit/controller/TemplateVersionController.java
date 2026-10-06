package com.carlos.devkit.controller;

import com.carlos.core.pagination.Paging;
import com.carlos.core.response.Result;
import com.carlos.devkit.convert.TemplateVersionConvert;
import com.carlos.devkit.pojo.param.TemplateVersionPageParam;
import com.carlos.devkit.pojo.vo.TemplateVersionVO;
import com.carlos.devkit.service.TemplateVersionService;
import com.carlos.devkit.support.PageMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 开发工具 - 模板版本历史 rest 服务接口
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("devkit/template/version")
@Tag(name = "开发工具-模板版本历史")
public class TemplateVersionController {

    private final TemplateVersionService templateVersionService;

    @PreAuthorize("hasPermission('devkit:template', 'view')")
    @GetMapping("/page")
    @Operation(summary = "版本分页", description = "不含正文快照")
    public Result<Paging<TemplateVersionVO>> page(@Validated final TemplateVersionPageParam param) {
        return Result.success(PageMapper.map(
            templateVersionService.getPage(param), TemplateVersionConvert.INSTANCE::toVO));
    }

    @PreAuthorize("hasPermission('devkit:template', 'view')")
    @GetMapping("/{id}")
    @Operation(summary = "版本详情", description = "含正文快照，供 diff 对比")
    public Result<TemplateVersionVO> detail(@PathVariable final Long id) {
        return Result.success(TemplateVersionConvert.INSTANCE.toDetailVO(
            templateVersionService.getDetail(id)));
    }

    @PreAuthorize("hasPermission('devkit:template', 'rollback')")
    @PostMapping("/rollback/{id}")
    @Operation(summary = "回滚到指定版本", description = "回滚前会把当前内容再落一版，回滚本身可回滚")
    public Result<Boolean> rollback(@PathVariable final Long id) {
        return Result.success(templateVersionService.rollback(id));
    }
}
