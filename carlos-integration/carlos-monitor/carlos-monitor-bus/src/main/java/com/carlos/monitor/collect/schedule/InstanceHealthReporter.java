package com.carlos.monitor.collect.schedule;

import com.carlos.monitor.instance.manager.MonitorInstanceManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * <p>
 * 实例健康回写器（采集结果 → monitor_instance.health_status/last_collect_time）
 * 实际落库走 Manager 层 Caffeine 合并节流（同实例同状态 10s 内只写一次库）
 * </p>
 *
 * @author Carlos
 * @date 2026年10月2日
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InstanceHealthReporter {

    private static final String HEALTH_UP = "UP";

    private static final String HEALTH_DOWN = "DOWN";

    private final MonitorInstanceManager instanceManager;

    /**
     * 采集成功：healthStatus=UP + lastCollectTime=now
     *
     * @param instanceId 实例ID
     */
    public void reportUp(Long instanceId) {
        instanceManager.updateHealth(instanceId, HEALTH_UP, LocalDateTime.now());
    }

    /**
     * 采集失败：healthStatus=DOWN（不刷新 lastCollectTime，它只记录最近成功采集时间）
     *
     * @param instanceId 实例ID
     */
    public void reportDown(Long instanceId) {
        instanceManager.updateHealth(instanceId, HEALTH_DOWN, null);
    }
}
