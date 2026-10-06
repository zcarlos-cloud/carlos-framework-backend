package com.carlos.devkit.controller;

import com.carlos.core.pagination.Paging;
import com.carlos.core.response.Result;
import com.carlos.devkit.convert.GenerateRecordConvert;
import com.carlos.devkit.pojo.param.DevkitIdsParam;
import com.carlos.devkit.pojo.param.GenerateRecordPageParam;
import com.carlos.devkit.pojo.vo.GenerateRecordVO;
import com.carlos.devkit.service.GenerateRecordService;
import com.carlos.devkit.support.PageMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 开发工具 - 生成记录 rest 服务接口
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("devkit/record")
@Tag(name = "开发工具-生成记录")
public class GenerateRecordController {

    private final GenerateRecordService generateRecordService;

    @PreAuthorize("hasPermission('devkit:record', 'view')")
    @GetMapping("/page")
    @Operation(summary = "生成记录分页")
    public Result<Paging<GenerateRecordVO>> page(final GenerateRecordPageParam param) {
        return Result.success(PageMapper.map(
            generateRecordService.getPage(param), GenerateRecordConvert.INSTANCE::toVO));
    }

    @PreAuthorize("hasPermission('devkit:record', 'delete')")
    @PostMapping("/delete")
    @Operation(summary = "批量删除生成记录")
    public Result<Boolean> delete(@RequestBody @Validated final DevkitIdsParam param) {
        return Result.success(generateRecordService.deleteRecords(param.getIds()));
    }
}
