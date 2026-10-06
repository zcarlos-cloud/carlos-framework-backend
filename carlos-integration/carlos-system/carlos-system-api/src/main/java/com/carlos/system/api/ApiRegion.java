package com.carlos.system.api;


import com.carlos.core.base.RegionInfo;
import com.carlos.core.response.Result;
import com.carlos.system.ServiceNameConstant;
import com.carlos.system.fallback.FeignRegionFallbackFactory;
import com.carlos.system.pojo.ao.SysRegionAO;
import com.carlos.system.pojo.param.ApiSysRegionAddParam;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Set;

/**
 * <p>
 * 行政区域 feign 提供接口
 * </p>
 *
 * @author carlos
 * @date 2021-11-22 14:49:00
 */
@FeignClient(value = ServiceNameConstant.SYSTEM, contextId = "region", fallbackFactory = FeignRegionFallbackFactory.class, path = "/api/sys/region")
public interface ApiRegion {


    /**
     * 获取指定级数的区域名称
     *
     * @param regionCode 区域代码
     * @param limit      级数限制 0代表不限制 null仅当前级  1代表一级 2代表二级 3代表三级 4代表四级 5代表五级 6代表六级 7代表七级 8代表八级 9代表九级 10代表十级
     * @return com.carlos.common.core.response.Result<java.util.LinkedList < java.lang.String>>
     * @author Carlos
     * @date 2022/12/5 13:37
     */
    @GetMapping("name")
    Result<List<String>> previewRegionName(@RequestParam("regionCode") String regionCode, @RequestParam(value = "limit", required = false) Integer limit);

    /**
     * 获取区域所有上级,包含当前区域
     *
     * @param regionCode 区域编码
     * @return com.carlos.common.core.response.Result<java.util.List < java.lang.String>>
     * @author Carlos
     * @date 2022/12/13 15:38
     */
    @GetMapping("ancestors")
    Result<List<String>> ancestors(@RequestParam("regionCode") String regionCode);

    /**
     * 获取子区域code信息
     *
     * @param regionCode 参数0
     * @return com.carlos.common.core.response.Result<java.util.Set < java.lang.String>>
     * @author Carlos
     * @date 2022/12/13 15:38
     */
    @GetMapping("subCodes")
    Result<Set<String>> getSubRegionCodes(@RequestParam("regionCode") String regionCode);

    /**
     * 获取区域信息
     *
     * @param regionCode 区域编码
     * @param limit      上级数目
     * @return com.carlos.common.core.response.Result<com.carlos.common.core.base.RegionInfo>
     * @author Carlos
     * @date 2022/12/30 13:56
     */
    @GetMapping("info")
    Result<RegionInfo> getRegionInfo(@RequestParam("regionCode") String regionCode, @RequestParam("limit") Integer limit);

    /**
     * 获取所有行政区域树形列表
     *
     * @return com.carlos.common.core.response.Result<java.util.List < com.carlos.common.dto.sys.SysRegionDTO>>
     * @author Carlos
     * @date 2023/1/4 19:41
     */
    @GetMapping("/getRegionTree")
    Result<List<SysRegionAO>> getRegionTree();

    @GetMapping("/all")
    Result<List<SysRegionAO>> all();

    /**
     * 新增行政区域
     *
     * @param region 区域信息
     * @return com.carlos.common.core.response.Result<com.carlos.common.dto.sys.SysRegionDTO>
     * @author Carlos
     * @date 2023/1/3 10:41
     */
    @PostMapping
    Result<SysRegionAO> addRegion(@RequestBody ApiSysRegionAddParam region);
}


