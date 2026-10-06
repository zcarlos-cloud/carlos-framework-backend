package com.carlos.system.region.apiimpl;

import cn.hutool.core.collection.CollUtil;
import com.carlos.core.base.RegionInfo;
import com.carlos.core.response.Result;
import com.carlos.system.api.ApiRegion;
import com.carlos.system.pojo.ao.SysRegionAO;
import com.carlos.system.pojo.param.ApiSysRegionAddParam;
import com.carlos.system.region.convert.SysRegionConvert;
import com.carlos.system.region.manager.SysRegionManager;
import com.carlos.system.region.pojo.dto.SysRegionDTO;
import com.carlos.system.region.service.SysRegionService;
import com.google.common.collect.Lists;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * <p>
 * 系统资源 api接口
 * </p>
 *
 * @author carlos
 * @date 2021-12-28 15:26:57
 */
// @Hidden
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sys/region")
@Tag(name = "区域编码Feign接口")
@Hidden
public class ApiRegionImpl implements ApiRegion {

    private final SysRegionService regionService;

    private final SysRegionManager regionManager;

    @Override
    @GetMapping("name")
    @Operation(summary = "区域预览指定父级")
    public Result<List<String>> previewRegionName(@RequestParam("regionCode") String regionCode, @RequestParam(value = "limit", required = false) Integer limit) {
        List<String> strings = this.regionService.previewRegionName(regionCode, limit);
        return Result.success(strings);
    }

    @Override
    @GetMapping("ancestors")
    public Result<List<String>> ancestors(@RequestParam("regionCode") String regionCode) {
        List<String> parentCode = regionManager.getAncestorIdsFromCache(regionCode, 0);
        if (CollUtil.isEmpty(parentCode)) {
            parentCode = Lists.newArrayList();
        }
        parentCode.add(regionCode);
        return Result.success(parentCode);
    }

    @Override
    @GetMapping("subCodes")
    @Operation(summary = "获取当前区域及子级区域编码")
    public Result<Set<String>> getSubRegionCodes(@RequestParam("regionCode") String regionCode) {
        return Result.success(this.regionService.getRegionSubCodes(regionCode));
    }

    @Override
    @GetMapping("info")
    @Operation(summary = "获取区域信息")
    public Result<RegionInfo> getRegionInfo(@RequestParam("regionCode") String regionCode, @RequestParam("limit") Integer limit) {
        return Result.success(this.regionService.getRegionInfo(regionCode, limit));
    }

    @Override
    @GetMapping("/getRegionTree")
    public Result<List<SysRegionAO>> getRegionTree() {
        List<SysRegionDTO> regionTree = this.regionService.getRegionTree();
        List<SysRegionAO> aos = SysRegionConvert.INSTANCE.toAOList(regionTree);
        return Result.success(aos);
    }

    @Override
    @GetMapping("/all")
    public Result<List<SysRegionAO>> all() {
        List<SysRegionDTO> regionTree = this.regionService.listAll();
        List<SysRegionAO> aos = SysRegionConvert.INSTANCE.toAOList(regionTree);
        return Result.success(aos);
    }


    @PostMapping
    @Override
    public Result<SysRegionAO> addRegion(@RequestBody ApiSysRegionAddParam region) {
        try {
            SysRegionDTO dto = SysRegionConvert.INSTANCE.toDTO(region);
            this.regionService.addSysRegion(dto);
            return Result.success(SysRegionConvert.INSTANCE.toAO(dto));
        } catch (Exception e) {
            log.error("{}", e.getMessage(), e);
            return Result.error("新增区域失败");
        }
    }

}
