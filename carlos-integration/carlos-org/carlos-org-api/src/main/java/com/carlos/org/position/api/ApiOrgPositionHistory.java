package com.carlos.org.position.api;

import com.carlos.org.ServiceNameConstant;
import com.carlos.org.position.api.fallback.ApiOrgPositionHistoryFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * <p>
 * 岗位变更历史表 feign 提供接口
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
@FeignClient(value = ServiceNameConstant.SERVICE_NAME, contextId = "positionHistory", path = "/api/org/position/history", fallbackFactory = ApiOrgPositionHistoryFallbackFactory.class)
public interface ApiOrgPositionHistory {


}
