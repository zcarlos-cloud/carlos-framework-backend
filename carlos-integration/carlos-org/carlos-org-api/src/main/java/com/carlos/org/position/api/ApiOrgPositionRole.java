package com.carlos.org.position.api;

import com.carlos.org.ServiceNameConstant;
import com.carlos.org.position.api.fallback.ApiOrgPositionRoleFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * <p>
 * 岗位角色关联表（岗位默认权限配置） feign 提供接口
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
@FeignClient(value = ServiceNameConstant.SERVICE_NAME, contextId = "positionRole", path = "/api/org/position/role", fallbackFactory = ApiOrgPositionRoleFallbackFactory.class)
public interface ApiOrgPositionRole {


}
