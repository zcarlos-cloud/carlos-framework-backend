package com.carlos.monitor.servicecollector;

import com.carlos.job.client.annotation.AutoRegisterJob;
import com.carlos.job.client.annotation.ScheduleJob;
import com.carlos.monitor.enums.InstanceTypeEnum;
import com.carlos.monitor.enums.MonitorEnvEnum;
import com.carlos.monitor.instance.manager.MonitorInstanceManager;
import com.carlos.monitor.instance.pojo.dto.MonitorInstanceDTO;
import com.carlos.monitor.instance.pojo.param.InstanceStatusParam;
import com.carlos.monitor.servicecollector.nacos.MonitorNacosDiscovery;
import com.carlos.monitor.util.MonitorCacheUtil;
import com.carlos.redis.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * 服务自动登记同步任务（60s）
 * Nacos 服务列表 → upsert monitor_instance（type=MICROSERVICE），
 * 使微服务复用采集/告警/大盘/历史查询全链路；
 * 已从 Nacos 消失且 30 分钟未再发现的服务实例标记停用（collect_enabled=false，不删除）；
 * 顺带执行日志级别到期恢复
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "carlos.monitor.collect", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ServiceRegistrySyncTask {

    /**
     * 服务最近发现时间键前缀
     */
    private static final String SEEN_PREFIX = "monitor:service:seen:";

    /**
     * 消失停用阈值（毫秒）：30 分钟
     */
    private static final long STALE_MS = 30L * 60 * 1000;

    /**
     * 默认采集间隔（秒）
     */
    private static final int DEFAULT_COLLECT_INTERVAL = 30;

    private final MonitorNacosDiscovery serviceDiscovery;

    private final MonitorInstanceManager instanceManager;

    private final LoggerLevelManager loggerLevelManager;

    private final Environment environment;

    /**
     * 服务注册同步（每 60s）
     */
    @ScheduleJob("monitorServiceRegistrySyncHandler")
    @AutoRegisterJob(cron = "0 * * * * ?", description = "微服务实例自动登记同步（Nacos → monitor_instance）", author = "carlos")
    public void syncRegistrations() {
        try {
            doSync();
        } catch (Exception e) {
            log.warn("服务自动登记同步失败: {}", e.getMessage());
        }
        // 日志级别到期恢复
        try {
            loggerLevelManager.restoreExpired();
        } catch (Exception e) {
            log.warn("日志级别恢复扫描失败: {}", e.getMessage());
        }
    }

    private void doSync() {
        List<String> services = serviceDiscovery.listServices();
        MonitorEnvEnum env = resolveEnv();
        Set<String> discovered = new HashSet<>(services);
        long now = System.currentTimeMillis();

        for (String serviceName : services) {
            RedisUtil.setValue(SEEN_PREFIX + serviceName, now, 3600L);
            MonitorInstanceDTO existing = instanceManager.getByNameEnv(serviceName, env.getCode());
            if (existing != null) {
                // 已存在：曾被停用的重新启用（按 (name,env) 查已有则更新不新建）
                if (Boolean.FALSE.equals(existing.getCollectEnabled())) {
                    InstanceStatusParam param = new InstanceStatusParam();
                    param.setCollectEnabled(true);
                    instanceManager.updateStatus(existing.getId(), param);
                    log.info("服务重新出现，恢复采集: {}", serviceName);
                }
                continue;
            }
            MonitorInstanceDTO instance = new MonitorInstanceDTO();
            instance.setInstanceName(serviceName);
            instance.setInstanceType(InstanceTypeEnum.MICROSERVICE);
            instance.setEnv(env);
            instance.setAddresses(serviceName);
            instance.setCollectEnabled(true);
            instance.setCollectInterval(DEFAULT_COLLECT_INTERVAL);
            instance.setConnectTimeout(3000);
            instance.setSocketTimeout(5000);
            instance.setManageEnabled(false);
            instance.setSqlWriteEnabled(false);
            instance.setHealthStatus("UNKNOWN");
            instanceManager.add(instance);
            log.info("微服务自动登记: {}, id:{}", serviceName, instance.getId());
        }

        // 消失停用：DB 中 MICROSERVICE 实例不在本次发现清单且 30 分钟未再发现 → 停用
        for (MonitorInstanceDTO instance : instanceManager.listAllInstances()) {
            if (instance.getInstanceType() != InstanceTypeEnum.MICROSERVICE
                || Boolean.FALSE.equals(instance.getCollectEnabled())) {
                continue;
            }
            if (discovered.contains(instance.getInstanceName())) {
                continue;
            }
            Long lastSeen = MonitorCacheUtil.getLong(SEEN_PREFIX + instance.getInstanceName());
            if (lastSeen == null || now - lastSeen > STALE_MS) {
                InstanceStatusParam param = new InstanceStatusParam();
                param.setCollectEnabled(false);
                instanceManager.updateStatus(instance.getId(), param);
                log.info("服务已从 Nacos 消失超过 30 分钟，标记停用: {}", instance.getInstanceName());
            }
        }
    }

    /**
     * 环境解析：spring.profiles.active 含 prod→PROD、test→TEST，其余 DEV
     */
    private MonitorEnvEnum resolveEnv() {
        String active = environment.getProperty("spring.profiles.active", "prod").toLowerCase();
        if (active.contains("prod")) {
            return MonitorEnvEnum.PROD;
        }
        if (active.contains("test")) {
            return MonitorEnvEnum.TEST;
        }
        return MonitorEnvEnum.DEV;
    }
}
