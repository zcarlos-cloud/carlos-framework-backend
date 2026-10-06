package com.carlos.org.api;

import com.carlos.core.response.Result;
import com.carlos.org.ServiceNameConstant;
import com.carlos.org.api.fallback.ApiOrgTenantFallbackFactory;
import com.carlos.org.api.pojo.ao.OrgTenantAO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * <p>
 * 租户 feign 提供接口
 * </p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
@FeignClient(value = ServiceNameConstant.SERVICE_NAME, contextId = "tenant", path = "/api/org/tenant", fallbackFactory = ApiOrgTenantFallbackFactory.class)
public interface ApiOrgTenant {

    /**
     * 根据租户编码查询租户
     *
     * <p>供 carlos-auth 登录流程做租户状态/有效期校验使用。</p>
     *
     * @param tenantCode 租户编码
     * @return 租户信息（不存在时 data 为 null）
     */
    @GetMapping("/tenant/by-code")
    Result<OrgTenantAO> getByCode(@RequestParam("tenantCode") String tenantCode);

}
