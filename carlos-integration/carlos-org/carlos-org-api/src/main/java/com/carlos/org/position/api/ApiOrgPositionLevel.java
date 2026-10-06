package com.carlos.org.position.api;

import com.carlos.org.ServiceNameConstant;
import com.carlos.org.position.api.fallback.ApiOrgPositionLevelFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * <p>
 * 职级表 feign 提供接口
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
@FeignClient(value = ServiceNameConstant.SERVICE_NAME, contextId = "positionLevel", path = "/api/org/position/level", fallbackFactory = ApiOrgPositionLevelFallbackFactory.class)
public interface ApiOrgPositionLevel {


}
