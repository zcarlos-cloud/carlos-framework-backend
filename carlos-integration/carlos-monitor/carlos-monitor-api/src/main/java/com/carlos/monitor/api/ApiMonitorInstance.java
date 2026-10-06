package com.carlos.monitor.api;

import com.carlos.core.response.Result;
import com.carlos.monitor.ServiceNameConstant;
import com.carlos.monitor.api.fallback.ApiMonitorInstanceFallbackFactory;
import com.carlos.monitor.api.pojo.ao.MonitorInstanceHealthAO;
import com.carlos.monitor.api.pojo.ao.MonitorUnresolvedAlarmAO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * <p>
 * 监控实例 Feign 接口
 * 提供给其他微服务/门户查询实例健康状态与未恢复告警计数
 * </p>
 *
 * @author Carlos
 * @date 2026年10月1日
 */
@FeignClient(value = ServiceNameConstant.SERVICE_NAME, contextId = "monitorInstance", path = "/api/monitor/instance", fallbackFactory = ApiMonitorInstanceFallbackFactory.class)
@Tag(name = "监控实例Feign接口", description = "供其他微服务查询实例健康状态与告警计数")
public interface ApiMonitorInstance {

    /**
     * 按应用名查询其全部关联实例的健康状态
     * <p>
     * 供门户/运维页聚合展示
     *
     * @param appName 应用名（微服务 spring.application.name）
     * @return 关联实例健康状态列表
     */
    @GetMapping("/health")
    @Operation(summary = "查询应用关联实例健康状态", description = "按应用名返回其全部关联实例健康状态")
    Result<List<MonitorInstanceHealthAO>> health(@RequestParam("appName") String appName);

    /**
     * 查询未恢复告警计数
     * <p>
     * 供门户角标展示
     *
     * @return 未恢复告警计数
     */
    @GetMapping("/alarm/unresolved-count")
    @Operation(summary = "查询未恢复告警计数", description = "供门户角标展示")
    Result<MonitorUnresolvedAlarmAO> unresolvedCount();
}
