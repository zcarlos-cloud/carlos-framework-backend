package com.carlos.system.region.controller;


import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.StrUtil;
import cn.idev.excel.EasyExcel;
import com.carlos.core.pagination.Paging;
import com.carlos.core.param.ParamIdSet;
import com.carlos.core.response.CommonErrorCode;
import com.carlos.core.response.Result;
import com.carlos.log.annotation.Log;
import com.carlos.log.enums.BusinessType;
import com.carlos.system.region.convert.SysRegionConvert;
import com.carlos.system.region.listener.RegionExcelListener;
import com.carlos.system.region.manager.SysRegionManager;
import com.carlos.system.region.pojo.dto.SysRegionDTO;
import com.carlos.system.region.pojo.excel.RegionExcel;
import com.carlos.system.region.pojo.param.SysRegionConvertParam;
import com.carlos.system.region.pojo.param.SysRegionCreateParam;
import com.carlos.system.region.pojo.param.SysRegionPageParam;
import com.carlos.system.region.pojo.param.SysRegionUpdateParam;
import com.carlos.system.region.pojo.vo.SysRegionVO;
import com.carlos.system.region.service.SysRegionService;
import com.carlos.util.easyexcel.ExcelUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;


/**
 * <p>
 * 行政区域划分 rest服务接口
 * </p>
 *
 * @author carlos
 * @date 2022-11-8 19:30:24
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/sys/region")
@Tag(name = "行政区域管理")
public class SysRegionController {

    public static final String BASE_NAME = "行政区域";

    private final SysRegionService regionService;

    private final SysRegionManager regionManager;


    @Log(
        title = "行政区域",
        logType = "SYS_REGION_CREATE",
        businessType = BusinessType.INSERT,
        operation = "'新增区域: ' + #param.regionName",
        targetType = "SYS_REGION",
        riskLevel = 20)
    @PreAuthorize("hasPermission('manage:region', 'create')")
    @PostMapping
    @Operation(summary = "新增" + BASE_NAME)

    public void add(@RequestBody @Validated final SysRegionCreateParam param) {
        final SysRegionDTO dto = SysRegionConvert.INSTANCE.toDTO(param);
        this.regionService.addSysRegion(dto);
    }


    @Log(
        title = "行政区域",
        logType = "SYS_REGION_DELETE",
        businessType = BusinessType.DELETE,
        operation = "'删除区域: ' + #param.ids",
        targetType = "SYS_REGION",
        targetId = "#param.ids",
        riskLevel = 50)
    @PreAuthorize("hasPermission('manage:region', 'delete')")
    @PostMapping("delete")
    @Operation(summary = "删除" + BASE_NAME)

    public void delete(@RequestBody @Validated final ParamIdSet<Long> param) {
        this.regionService.deleteSysRegion(param.getIds());
    }


    @Log(
        title = "行政区域",
        logType = "SYS_REGION_UPDATE",
        businessType = BusinessType.UPDATE,
        operation = "'修改区域: ' + #param.regionName",
        targetType = "SYS_REGION",
        targetId = "#param.id",
        riskLevel = 20)
    @PreAuthorize("hasPermission('manage:region', 'update')")
    @PostMapping("update")
    @Operation(summary = "更新" + BASE_NAME)

    public void update(@RequestBody @Validated final SysRegionUpdateParam param) {
        final SysRegionDTO dto = SysRegionConvert.INSTANCE.toDTO(param);
        this.regionService.updateSysRegion(dto);
    }

    @PreAuthorize("hasPermission('manage:region', 'view')")
    @GetMapping("detail")
    @Operation(summary = BASE_NAME + "详情")
    public SysRegionVO detail(@RequestParam final Long id) {
        return SysRegionConvert.INSTANCE.toVO(this.regionManager.getDtoById(id));
    }

    @PreAuthorize("hasPermission('manage:region', 'view')")
    @GetMapping("page")
    @Operation(summary = BASE_NAME + "分页列表")
    public Paging<SysRegionVO> page(final SysRegionPageParam param) {
        return this.regionManager.getPage(param);
    }


    @PreAuthorize("hasPermission('manage:region', 'view')")
    @GetMapping("tree/list")
    @Operation(summary = "行政区域树形列表", description = "一次性全部加载")
    @Parameters({
        @Parameter(name = "regionCode", description = "当前区域code"),
        @Parameter(name = "regionName", description = "当前区域名称"),
        @Parameter(name = "codeTop", description = "是否以传入的code作为顶级树的开始")
    })
    public Result<List<SysRegionVO>> treeList(final String parentId, final String regionName, final String regionCode, final boolean codeTop) {
        return Result.success(this.regionService.getRegionTree(parentId, regionName, regionCode, true, codeTop));
    }

    @PreAuthorize("hasPermission('manage:region', 'view')")
    @GetMapping("loadStep")
    @Operation(summary = "逐级加载行政区域")
    @Parameters({@Parameter(name = "regionCode", description = "当前区域code")})
    public List<SysRegionVO> lodeStep(final String regionCode) {
        final List<SysRegionDTO> dtos = this.regionService.loadRegionStep(regionCode);
        return SysRegionConvert.INSTANCE.dto2vo(dtos);
    }


    @Log(
        title = "行政区域",
        logType = "SYS_REGION_IMPORT",
        businessType = BusinessType.IMPORT,
        operation = "'导入区域'",
        targetType = "SYS_REGION",
        saveRequestParams = false,
        riskLevel = 40)
    @PreAuthorize("hasPermission('manage:region', 'import')")
    @PostMapping("import")
    @Operation(summary = "0-导入区域")

    public Result<?> importData(@RequestPart final MultipartFile file) {
        final RegionExcelListener listener = new RegionExcelListener(regionService);
        try {
            final String filename = FileNameUtil.getName(file.getOriginalFilename());
            if (StrUtil.isBlank(filename)) {
                throw CommonErrorCode.PARAM_MISSING.exception("文件名不能为空");
            }
            ExcelUtil.checkExcel(filename);
            EasyExcel.read(file.getInputStream(), RegionExcel.class, listener).sheet().doRead();
        } catch (final IOException e) {
            return Result.error("数据导入失败！");
        }
        return Result.success("区域数据初始化导入成功！");
    }


    @Log(
        title = "行政区域",
        logType = "SYS_REGION_IMPORT_V2",
        businessType = BusinessType.IMPORT,
        operation = "'导入区域V2'",
        targetType = "SYS_REGION",
        saveRequestParams = false,
        riskLevel = 40)
    @PreAuthorize("hasPermission('manage:region', 'import')")
    @PostMapping("importV2")
    @Operation(summary = "0-导入区域V2", description = "支持自定义表字段和多sheet")
    public Result<?> id2code(@Validated SysRegionConvertParam param) {
        MultipartFile file = param.getFile();
        String fileName = FileNameUtil.getName(file.getOriginalFilename());
        if (fileName == null) {
            return Result.error("请选择文件！");
        }
        regionService.importCustomRegions(param);

        return Result.success("区域数据初始化导入成功！");
    }


    @Log(
        title = "行政区域",
        logType = "SYS_REGION_EXPORT",
        businessType = BusinessType.EXPORT,
        operation = "'导出区域数据'",
        targetType = "SYS_REGION",
        riskLevel = 30)
    @PreAuthorize("hasPermission('manage:region', 'export')")
    @GetMapping("export")
    @Operation(summary = "导出数据")

    public void exportUser(final HttpServletResponse response) {
        regionService.export(response, false);
    }


    @PreAuthorize("hasPermission('manage:region', 'export')")
    @GetMapping("export/template")
    @Operation(summary = "导出模板")

    public void exportTemplate(final HttpServletResponse response) {
        regionService.export(response, true);
    }


    @Log(
        title = "行政区域",
        logType = "SYS_REGION_PARENTS_RESET",
        businessType = BusinessType.REFRESH,
        operation = "'重建区域父级编码'",
        targetType = "SYS_REGION",
        riskLevel = 60)
    @PreAuthorize("hasPermission('manage:region', 'resetParents')")
    @PostMapping("parents/reset")
    @Operation(summary = "0-初始化父级编码")
    public void resetParents() {
        regionService.resetParents();
    }

    @Log(
        title = "行政区域",
        logType = "SYS_REGION_CACHE_INIT",
        businessType = BusinessType.REFRESH,
        operation = "'初始化区域缓存'",
        targetType = "SYS_REGION",
        riskLevel = 50)
    @PreAuthorize("hasPermission('manage:region', 'cacheInit')")
    @PostMapping("cache/init")
    @Operation(summary = "0-缓存初始化")

    public void cacheInit() {
        this.regionManager.initCache();
    }


    @Log(
        title = "行政区域",
        logType = "SYS_REGION_CACHE_CLEAR",
        businessType = BusinessType.CLEAN,
        operation = "'清空区域缓存'",
        targetType = "SYS_REGION",
        riskLevel = 50)
    @PreAuthorize("hasPermission('manage:region', 'cacheClear')")
    @PostMapping("cache/clear")
    @Operation(summary = "0-缓存清空")
    public void cacheClear() {
        regionManager.clearCache();
    }

    @PreAuthorize("hasPermission('manage:region', 'view')")
    @GetMapping("/getAllParentRegions")
    @Operation(summary = "根据code，获取上级区域,传入C,并按照A、B、C层级返回")
    public List<SysRegionVO> getAllParentRegions(@RequestParam(required = false) String regionCode) {
        List<SysRegionDTO> dto = regionService.getAllParentRegions(regionCode);
        return SysRegionConvert.INSTANCE.dto2vo(dto);
    }


}
