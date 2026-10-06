package com.carlos.monitor.api.fallback;

import com.carlos.core.response.Result;
import com.carlos.monitor.api.ApiMonitorInstance;
import com.carlos.monitor.api.pojo.ao.MonitorInstanceHealthAO;
import com.carlos.monitor.api.pojo.ao.MonitorUnresolvedAlarmAO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

import java.util.Collections;
import java.util.List;

/**
 * <p>
 * 监控实例 Feign 接口降级工厂
 * </p>
 *
 * @author Carlos
 * @date 2026年10月1日
 */
@Slf4j
public class ApiMonitorInstanceFallbackFactory implements FallbackFactory<ApiMonitorInstance> {

    @Override
    public ApiMonitorInstance create(Throwable throwable) {
        log.error("监控服务调用失败", throwable);

        return new ApiMonitorInstance() {

            @Override
            public Result<List<MonitorInstanceHealthAO>> health(String appName) {
                log.warn("查询应用关联实例健康状态降级处理，appName: {}", appName);
                // 降级时返回空列表，不影响门户主流程
                return Result.success(Collections.emptyList());
            }

            @Override
            public Result<MonitorUnresolvedAlarmAO> unresolvedCount() {
                log.warn("查询未恢复告警计数降级处理");
                MonitorUnresolvedAlarmAO ao = new MonitorUnresolvedAlarmAO();
                ao.setUnresolvedCount(0L);
                return Result.success(ao);
            }
        };
    }
}
