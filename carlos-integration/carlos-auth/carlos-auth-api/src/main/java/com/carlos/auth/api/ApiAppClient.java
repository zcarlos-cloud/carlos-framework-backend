package com.carlos.auth.api;

import com.carlos.auth.api.fallback.ApiAppClientFallbackFactory;
import com.carlos.auth.api.pojo.ao.AppClientAO;
import com.carlos.core.response.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * <p>
 * 应用信息 feign 提供接口
 * </p>
 *
 * @author Carlos
 * @date 2025-3-12 14:00:14
 */
@FeignClient(
    value = ServiceNameConstant.SERVICE_NAME, path = "api/auth/app/client",
    contextId = "auth-app",
    fallbackFactory = ApiAppClientFallbackFactory.class)
public interface ApiAppClient {


    /**
     * 获取应用信
     *
     * @param id 应用id
     * @return com.carlos.core.response.Result<com.carlos.auth.api.pojo.ao.AppClientAO>
     * @author Carlos
     * @date 2025-04-15 14:58
     */
    @GetMapping(value = "/id")
    Result<AppClientAO> getById(@RequestParam("id") Long id);

    /**
     * 获取应用信息
     *
     * @param appKey appKey
     * @return com.carlos.core.response.Result<com.carlos.auth.api.pojo.ao.AppClientAO>
     * @author Carlos
     * @date 2025-04-15 14:58
     */
    @GetMapping(value = "/appKey")
    Result<AppClientAO> getByAppKey(@RequestParam("appKey") String appKey);

}
