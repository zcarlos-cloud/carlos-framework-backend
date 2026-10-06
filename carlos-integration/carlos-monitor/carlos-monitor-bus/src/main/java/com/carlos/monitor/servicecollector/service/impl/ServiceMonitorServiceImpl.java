package com.carlos.monitor.servicecollector.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.carlos.json.JsonUtils;
import com.carlos.monitor.collect.sink.MonitorCkQueryClient;
import com.carlos.monitor.config.MonitorProperties;
import com.carlos.monitor.constant.MetricNameConstant;
import com.carlos.monitor.enums.InstanceTypeEnum;
import com.carlos.monitor.enums.MonitorErrorCode;
import com.carlos.monitor.instance.manager.MonitorInstanceManager;
import com.carlos.monitor.instance.pojo.dto.MonitorInstanceDTO;
import com.carlos.monitor.redis.manager.RedisMetricsManager;
import com.carlos.monitor.redis.proxy.RedisWriteGuard;
import com.carlos.monitor.servicecollector.ActuatorClient;
import com.carlos.monitor.servicecollector.LoggerLevelManager;
import com.carlos.monitor.servicecollector.nacos.MonitorNacosDiscovery;
import com.carlos.monitor.servicecollector.pojo.vo.ServiceInstanceVO;
import com.carlos.monitor.servicecollector.pojo.vo.ServiceSummaryVO;
import com.carlos.monitor.servicecollector.service.ServiceMonitorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 微服务监控 业务接口实现类
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceMonitorServiceImpl implements ServiceMonitorService {

    private static final int RAW_POINT_LIMIT = 10_000;

    private final MonitorNacosDiscovery serviceDiscovery;

    private final MonitorInstanceManager instanceManager;

    private final RedisMetricsManager redisMetricsManager;

    private final MonitorCkQueryClient ckQueryClient;

    private final ActuatorClient actuatorClient;

    private final LoggerLevelManager loggerLevelManager;

    private final RedisWriteGuard writeGuard;

    private final MonitorProperties monitorProperties;

    @Override
    public List<ServiceSummaryVO> listServices() {
        List<ServiceSummaryVO> result = new ArrayList<>();
        for (MonitorInstanceDTO instance : instanceManager.listAllInstances()) {
            if (instance.getInstanceType() != InstanceTypeEnum.MICROSERVICE) {
                continue;
            }
            ServiceSummaryVO vo = new ServiceSummaryVO();
            vo.setServiceName(instance.getInstanceName());
            vo.setInstanceId(instance.getId());
            vo.setHealthStatus(instance.getHealthStatus());
            vo.setLastCollectTime(instance.getLastCollectTime());
            vo.setDisabled(Boolean.FALSE.equals(instance.getCollectEnabled()));
            // 实例数/健康数取当前快照（Nacos 实时代价高，快照 30s 级新鲜度足够）
            Map<String, String> snapshot = redisMetricsManager.getCurrentMetrics(instance.getId());
            vo.setInstanceCount(snapshotInt(snapshot, MetricNameConstant.SERVICE_INSTANCE_COUNT));
            vo.setHealthyCount(snapshotInt(snapshot, MetricNameConstant.SERVICE_INSTANCE_HEALTHY_COUNT));
            result.add(vo);
        }
        return result;
    }

    @Override
    public List<ServiceInstanceVO> listInstances(String serviceName) {
        List<Instance> instances = serviceDiscovery.getAllInstances(serviceName);
        List<ServiceInstanceVO> result = new ArrayList<>();
        for (Instance instance : instances) {
            ServiceInstanceVO vo = new ServiceInstanceVO();
            vo.setIp(instance.getIp());
            vo.setPort(instance.getPort());
            vo.setHealthy(instance.isHealthy());
            vo.setWeight(instance.getWeight());
            vo.setMetadata(instance.getMetadata());
            // actuator health 合并
            Map<String, Object> health = actuatorClient.get(
                instance.getIp() + ":" + instance.getPort(), "/health");
            vo.setActuatorHealth(health == null ? "UNREACHABLE" : String.valueOf(health.get("status")));
            result.add(vo);
        }
        return result;
    }

    @Override
    public List<?> metricHistory(String serviceName, String metric, String range, String node) {
        MonitorInstanceDTO instance = checkService(serviceName);
        long hours = switch (StrUtil.nullToDefault(range, "1h").toLowerCase()) {
            case "6h" -> 6L;
            case "24h" -> 24L;
            case "7d" -> 24L * 7;
            default -> 1L;
        };
        Instant end = Instant.now();
        Instant start = end.minus(hours, ChronoUnit.HOURS);
        if (hours > 6) {
            return ckQueryClient.queryMetricHourly(instance.getId(), metric, node, start, end);
        }
        return ckQueryClient.queryMetricPoints(instance.getId(), metric, node, start, end, RAW_POINT_LIMIT);
    }

    @Override
    public void updateLoggerLevel(String serviceName, String address, String loggerName,
                                  String level, long ttlSeconds, String confirmToken) {
        MonitorInstanceDTO instance = checkService(serviceName);
        String levelUpper = level.toUpperCase();
        if (!List.of("TRACE", "DEBUG", "INFO", "WARN", "ERROR", "OFF").contains(levelUpper)) {
            throw MonitorErrorCode.MONITOR_INSTANCE_TYPE_UNSUPPORTED
                .exception("不支持的日志级别：" + level);
        }
        writeGuard.consumeConfirmToken(instance.getId(),
            "service:logger:" + serviceName + ":" + address + ":" + loggerName + ":" + levelUpper,
            confirmToken);
        loggerLevelManager.applyLevel(serviceName, address, loggerName, levelUpper, ttlSeconds);
    }

    @Override
    public String skywalkingUrl(String serviceName) {
        String template = monitorProperties.getServiceMonitor().getSkywalkingUrlTemplate();
        if (StrUtil.isBlank(template)) {
            return "";
        }
        return template.replace("{service}", serviceName);
    }

    private MonitorInstanceDTO checkService(String serviceName) {
        for (MonitorInstanceDTO instance : instanceManager.listAllInstances()) {
            if (instance.getInstanceType() == InstanceTypeEnum.MICROSERVICE
                && instance.getInstanceName().equals(serviceName)) {
                return instance;
            }
        }
        throw MonitorErrorCode.MONITOR_INSTANCE_NOT_FOUND.exception("服务未登记：" + serviceName);
    }

    /**
     * 快照中取整数值（{v,ts} JSON 中的 v）
     */
    private Integer snapshotInt(Map<String, String> snapshot, String metric) {
        String json = snapshot.get(metric);
        if (StrUtil.isBlank(json) || !JsonUtils.isValidJson(json)) {
            return 0;
        }
        Object v = JsonUtils.toMap(json).get("v");
        return v instanceof Number number ? number.intValue() : 0;
    }
}
