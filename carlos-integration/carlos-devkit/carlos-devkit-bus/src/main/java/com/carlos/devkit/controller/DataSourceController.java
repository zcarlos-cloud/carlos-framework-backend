package com.carlos.devkit.controller;

import com.carlos.core.pagination.Paging;
import com.carlos.core.response.Result;
import com.carlos.devkit.convert.DataSourceConvert;
import com.carlos.devkit.engine.model.ConnectionTestResult;
import com.carlos.devkit.pojo.dto.DataSourceDTO;
import com.carlos.devkit.pojo.param.DataSourceCreateParam;
import com.carlos.devkit.pojo.param.DataSourcePageParam;
import com.carlos.devkit.pojo.param.DataSourceTestParam;
import com.carlos.devkit.pojo.param.DataSourceUpdateParam;
import com.carlos.devkit.pojo.param.DevkitIdsParam;
import com.carlos.devkit.pojo.vo.DataSourceVO;
import com.carlos.devkit.pojo.vo.TestConnectionVO;
import com.carlos.devkit.service.DataSourceService;
import com.carlos.devkit.support.PageMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 开发工具 - 受管数据源 rest 服务接口
 * </p>
 * <p>
 * <b>密码永不回发</b>：列表与详情只回 {@code hasPassword}。
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("devkit/datasource")
@Tag(name = "开发工具-数据源管理")
public class DataSourceController {

    private final DataSourceService dataSourceService;

    @PreAuthorize("hasPermission('devkit:datasource', 'view')")
    @GetMapping("/page")
    @Operation(summary = "数据源分页")
    public Result<Paging<DataSourceVO>> page(final DataSourcePageParam param) {
        return Result.success(PageMapper.map(
            dataSourceService.getPage(param), this::toVO));
    }

    @PreAuthorize("hasPermission('devkit:datasource', 'view')")
    @GetMapping("/list")
    @Operation(summary = "启用中的数据源", description = "下拉用")
    public Result<List<DataSourceVO>> list() {
        return Result.success(dataSourceService.listEnabled().stream().map(this::toVO).toList());
    }

    @PreAuthorize("hasPermission('devkit:datasource', 'view')")
    @GetMapping("/{id}")
    @Operation(summary = "数据源详情", description = "不返回密码，只回 hasPassword")
    public Result<DataSourceVO> detail(@PathVariable final Long id) {
        return Result.success(toVO(dataSourceService.getDetail(id)));
    }

    @PreAuthorize("hasPermission('devkit:datasource', 'add')")
    @PostMapping("/add")
    @Operation(summary = "新增数据源")
    public Result<Long> add(@RequestBody @Validated final DataSourceCreateParam param) {
        return Result.success(dataSourceService.addDataSource(DataSourceConvert.INSTANCE.toDTO(param)));
    }

    @PreAuthorize("hasPermission('devkit:datasource', 'edit')")
    @PostMapping("/update")
    @Operation(summary = "编辑数据源", description = "密码留空表示保持原值")
    public Result<Boolean> update(@RequestBody @Validated final DataSourceUpdateParam param) {
        return Result.success(dataSourceService.updateDataSource(DataSourceConvert.INSTANCE.toDTO(param)));
    }

    @PreAuthorize("hasPermission('devkit:datasource', 'delete')")
    @PostMapping("/delete")
    @Operation(summary = "批量删除数据源")
    public Result<Boolean> delete(@RequestBody @Validated final DevkitIdsParam param) {
        return Result.success(dataSourceService.deleteDataSources(param.getIds()));
    }

    @PreAuthorize("hasPermission('devkit:datasource', 'test')")
    @PostMapping("/test")
    @Operation(summary = "测试连接", description = "传 id 且密码为空时使用库中已存密码")
    public Result<TestConnectionVO> test(@RequestBody @Validated final DataSourceTestParam param) {
        ConnectionTestResult result = dataSourceService.testConnection(
            DataSourceConvert.INSTANCE.toDTO(param));
        return Result.success(new TestConnectionVO()
            .setSuccess(result.isSuccess())
            .setDatabaseName(result.getDatabaseName())
            .setDatabaseVersion(result.getDatabaseVersion())
            .setResponseTimeMs(result.getResponseTimeMs())
            .setErrorMessage(result.getErrorMessage()));
    }

    /**
     * DTO → VO（不携带任何口令信息，只回 hasPassword 标记）
     *
     * <p>hasPassword 由 Service 回填：列表查询不取密文，详情查询才取。</p>
     *
     * @param dto 数据源 DTO
     * @return 数据源 VO
     */
    private DataSourceVO toVO(final DataSourceDTO dto) {
        DataSourceVO vo = DataSourceConvert.INSTANCE.toVO(dto);
        vo.setHasPassword(Boolean.TRUE.equals(dto.getHasPassword()));
        return vo;
    }
}
