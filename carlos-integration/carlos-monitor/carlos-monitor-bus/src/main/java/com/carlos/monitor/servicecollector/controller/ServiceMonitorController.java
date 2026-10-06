package com.carlos.monitor.servicecollector.controller;

import com.carlos.core.response.Result;
import com.carlos.monitor.audit.MonitorAudit;
import com.carlos.monitor.collect.sink.MonitorCkQueryClient;
import com.carlos.monitor.redis.pojo.vo.HistoryPointVO;
import com.carlos.monitor.servicecollector.pojo.param.LoggerUpdateParam;
import com.carlos.monitor.servicecollector.pojo.vo.ServiceInstanceVO;
import com.carlos.monitor.servicecollector.pojo.vo.ServiceSummaryVO;
import com.carlos.monitor.servicecollector.service.ServiceMonitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 微服务监控 rest服务接口
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/monitor/services")
@Tag(name = "微服务监控")
public class ServiceMonitorController {

    private final ServiceMonitorService serviceMonitorService;

    @PreAuthorize("hasPermission('monitor:service', 'view')")
    @GetMapping
    @Operation(summary = "服务清单", description = "服务名/实例数/健康实例数/最近采集时间")
    public Result<List<ServiceSummaryVO>> services() {
        return Result.success(serviceMonitorService.listServices());
    }

    @PreAuthorize("hasPermission('monitor:service', 'view')")
    @GetMapping("/{name}/instances")
    @Operation(summary = "实例明细", description = "Nacos 实时实例 + actuator health 合并")
    public Result<List<ServiceInstanceVO>> instances(@PathVariable("name") String name) {
        return Result.success(serviceMonitorService.listInstances(name));
    }

    @PreAuthorize("hasPermission('monitor:service', 'view')")
    @GetMapping("/{name}/metrics/history")
    @Operation(summary = "指标时序", description = "CK 时序，支持 node 维度多实例对比")
    public Result<List<HistoryPointVO>> metricsHistory(@PathVariable("name") String name,
                                                       @RequestParam("metric") String metric,
                                                       @RequestParam(value = "range", defaultValue = "1h") String range,
                                                       @RequestParam(value = "node", required = false) String node) {
        List<?> rows = serviceMonitorService.metricHistory(name, metric, range, node);
        List<HistoryPointVO> result = new ArrayList<>(rows.size());
        for (Object row : rows) {
            HistoryPointVO point = new HistoryPointVO();
            if (row instanceof MonitorCkQueryClient.MetricPointRow raw) {
                point.setTs(raw.ts());
                point.setValue(raw.value());
            } else if (row instanceof MonitorCkQueryClient.MetricHourlyRow hourly) {
                point.setTs(hourly.hour());
                point.setAvgValue(hourly.avgValue());
                point.setMaxValue(hourly.maxValue());
                point.setMinValue(hourly.minValue());
                point.setP95Value(hourly.p95Value());
                point.setPointCount(hourly.pointCount());
            }
            result.add(point);
        }
        return Result.success(result);
    }

    @MonitorAudit(action = "SERVICE_LOGGER_UPDATE", target = "#name + ':' + #param.instanceAddr + ':' + #param.loggerName", snapshot = false, riskLevel = 80)
    @PreAuthorize("hasPermission('monitor:service', 'logger')")
    @PutMapping("/{name}/loggers")
    @Operation(summary = "日志级别调整", description = "高危：X-Confirm-Token；TTL 到期自动恢复原级别")
    public Result<Boolean> updateLogger(@PathVariable("name") String name,
                                        @RequestBody @Validated LoggerUpdateParam param,
                                        @RequestHeader(value = "X-Confirm-Token", required = false)
                                        String confirmToken) {
        serviceMonitorService.updateLoggerLevel(name, param.getInstanceAddr(), param.getLoggerName(),
            param.getLevel(), param.getTtlSeconds(), confirmToken);
        return Result.success(true);
    }

    @PreAuthorize("hasPermission('monitor:service', 'view')")
    @GetMapping("/{name}/skywalking-url")
    @Operation(summary = "SkyWalking 跳转链接", description = "按配置模板拼接，未配置返回空串")
    public Result<String> skywalkingUrl(@PathVariable("name") String name) {
        return Result.success(serviceMonitorService.skywalkingUrl(name));
    }
}
