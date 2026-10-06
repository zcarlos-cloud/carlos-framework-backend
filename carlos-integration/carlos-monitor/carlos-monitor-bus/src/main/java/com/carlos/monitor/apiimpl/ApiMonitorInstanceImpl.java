package com.carlos.monitor.apiimpl;

import com.carlos.core.response.Result;
import com.carlos.monitor.api.ApiMonitorInstance;
import com.carlos.monitor.api.pojo.ao.MonitorInstanceHealthAO;
import com.carlos.monitor.api.pojo.ao.MonitorUnresolvedAlarmAO;
import com.carlos.monitor.alarm.manager.MonitorAlarmRecordManager;
import com.carlos.monitor.instance.convert.MonitorInstanceConvert;
import com.carlos.monitor.instance.service.MonitorInstanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 监控实例 Feign 接口实现
 * 提供给其他微服务/门户查询实例健康状态与未恢复告警计数
 * </p>
 *
 * @author Carlos
 * @date 2026年10月1日
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/monitor/instance")
@Tag(name = "监控实例Feign接口", description = "供其他微服务查询实例健康状态与告警计数")
public class ApiMonitorInstanceImpl implements ApiMonitorInstance {

    private final MonitorInstanceService instanceService;

    private final MonitorAlarmRecordManager alarmRecordManager;

    @Override
    @GetMapping("/health")
    @Operation(summary = "查询应用关联实例健康状态", description = "按应用名返回其全部关联实例健康状态")
    public Result<List<MonitorInstanceHealthAO>> health(@RequestParam("appName") String appName) {
        return Result.success(MonitorInstanceConvert.INSTANCE.toAO(instanceService.listByAppName(appName)));
    }

    @Override
    @GetMapping("/alarm/unresolved-count")
    @Operation(summary = "查询未恢复告警计数", description = "供门户角标展示")
    public Result<MonitorUnresolvedAlarmAO> unresolvedCount() {
        MonitorUnresolvedAlarmAO ao = new MonitorUnresolvedAlarmAO();
        ao.setUnresolvedCount(alarmRecordManager.countUnresolved());
        return Result.success(ao);
    }
}
