package com.carlos.org.position.api;

import com.carlos.org.ServiceNameConstant;
import com.carlos.org.position.api.fallback.ApiOrgPositionCategoryFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * <p>
 * 岗位类别表（职系） feign 提供接口
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
@FeignClient(value = ServiceNameConstant.SERVICE_NAME, contextId = "positionCategory", path = "/api/org/position/category", fallbackFactory = ApiOrgPositionCategoryFallbackFactory.class)
public interface ApiOrgPositionCategory {


}
