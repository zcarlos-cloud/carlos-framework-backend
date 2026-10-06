package com.carlos.system.fallback;

import com.carlos.core.base.RegionInfo;
import com.carlos.core.response.Result;
import com.carlos.system.api.ApiRegion;
import com.carlos.system.pojo.ao.SysRegionAO;
import com.carlos.system.pojo.param.ApiSysRegionAddParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

import java.util.List;
import java.util.Set;


/**
 * <p>
 * 用户降级服务
 * </p>
 *
 * @author Carlos
 * @date 2022/11/16 10:57
 */

@Slf4j
public class FeignRegionFallbackFactory implements FallbackFactory<ApiRegion> {

    @Override
    public ApiRegion create(Throwable throwable) {
        String message = throwable.getMessage();
        log.error("区域编码服务调用失败: message:{}", message);
        return new ApiRegion() {


            @Override
            public Result<List<String>> previewRegionName(String regionCode, Integer limit) {
                return Result.error("区域名称获取失败");
            }

            @Override
            public Result<Set<String>> getSubRegionCodes(String regionCode) {
                return Result.error("区域信息获取失败");
            }

            @Override
            public Result<RegionInfo> getRegionInfo(String regionCode, Integer limit) {
                return Result.error("区域信息获取失败");
            }

            @Override
            public Result<List<SysRegionAO>> getRegionTree() {
                return Result.error("区域树形列表获取失败");
            }

            @Override
            public Result<List<SysRegionAO>> all() {
                return Result.error("区域列表获取失败");
            }

            @Override
            public Result<SysRegionAO> addRegion(ApiSysRegionAddParam dto) {
                return Result.error("区域新增失败");
            }

            @Override
            public Result<List<String>> ancestors(String regionCode) {
                return Result.error("区域信息获取失败");
            }
        };
    }
}
