package com.carlos.system.api;

import com.carlos.core.response.Result;
import com.carlos.system.ServiceNameConstant;
import com.carlos.system.fallback.FeignSysOperationFallbackFactory;
import com.carlos.system.pojo.ao.SysOperationAO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * <p>
 * 统一操作 Feign 接口
 * </p>
 *
 * @author carlos
 * @date 2026-07-11
 */
@FeignClient(value = ServiceNameConstant.SYSTEM, contextId = "operation", path = "/api/sys/operation", fallbackFactory = FeignSysOperationFallbackFactory.class)
public interface ApiSysOperation {

    /**
     * 获取所有启用的操作列表
     *
     * @return 操作列表
     */
    @GetMapping("/enabled")
    Result<List<SysOperationAO>> listEnabledOperations();

}
