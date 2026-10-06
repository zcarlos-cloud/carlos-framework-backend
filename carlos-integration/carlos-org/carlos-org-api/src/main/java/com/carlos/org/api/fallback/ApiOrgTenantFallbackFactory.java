package com.carlos.org.api.fallback;

import com.carlos.core.response.Result;
import com.carlos.org.api.ApiOrgTenant;
import com.carlos.org.api.pojo.ao.OrgTenantAO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;


/**
 * <p>
 * 租户 api 降级
 * </p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
@Slf4j
public class ApiOrgTenantFallbackFactory implements FallbackFactory<ApiOrgTenant> {

    @Override
    public ApiOrgTenant create(Throwable throwable) {
        String message = throwable.getMessage();
        log.error("租户服务调用失败: message:{}", message);
        return tenantCode -> {
            log.error("租户服务调用失败(getByCode): tenantCode:{}, message:{}", tenantCode, message);
            return Result.error("租户服务调用失败");
        };
    }

}
