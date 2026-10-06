package com.carlos.system.api;

import com.carlos.core.response.Result;
import com.carlos.system.ServiceNameConstant;
import com.carlos.system.fallback.FeignSystemConfigFallbackFactory;
import com.carlos.system.pojo.ao.SysConfigAO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * <p>
 * 系统菜单 feign 提供接口
 * </p>
 *
 * @author carlos
 * @date 2021-12-28 15:26:57
 */
@FeignClient(value = ServiceNameConstant.SYSTEM, contextId = "systemconfig", path = "/api/sys/config", fallbackFactory = FeignSystemConfigFallbackFactory.class)
public interface ApiSystemConfig {

    /**
     * 根据配置code获取配置信息
     *
     * @param code 参数0
     * @return com.carlos.core.response.Result<com.carlos.system.pojo.ao.SysConfigAO>
     * @author Carlos
     * @date 2023/8/24 11:13
     */
    @GetMapping("code")
    Result<SysConfigAO> getByCode(@RequestParam("code") String code);
}
