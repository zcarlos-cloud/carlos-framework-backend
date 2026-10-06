package com.carlos.monitor.servicecollector;

import com.alibaba.nacos.api.naming.pojo.Instance;
import com.carlos.monitor.collect.spi.CollectorContext;
import com.carlos.monitor.collect.spi.MetricCollector;
import com.carlos.monitor.collect.spi.MetricPoint;
import com.carlos.monitor.constant.MetricNameConstant;
import com.carlos.monitor.enums.InstanceTypeEnum;
import com.carlos.monitor.enums.MetricSourceEnum;
import com.carlos.monitor.servicecollector.nacos.MonitorNacosDiscovery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 微服务指标采集器
 * Nacos getAllInstances(serviceName) → 并发（虚拟线程）拉各实例 actuator
 * /health 与 /metrics/{name} → 指标点（node=ip:port）+ 服务级聚合点（node=''）；
 * 单实例失败只影响该实例 health 点
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceMetricCollector implements MetricCollector {

    /**
     * 拉取的 actuator 指标名（防御性解析，缺失跳过）
     */
    private static final String M_JVM_MEMORY_USED = "jvm.memory.used";

    private static final String M_JVM_MEMORY_MAX = "jvm.memory.max";

    private static final String M_JVM_THREADS_LIVE = "jvm.threads.live";

    private static final String M_JVM_GC_PAUSE = "jvm.gc.pause";

    private static final String M_PROCESS_CPU_USAGE = "process.cpu.usage";

    private static final String M_HTTP_REQUESTS = "http.server.requests";

    private static final String M_TOMCAT_THREADS = "tomcat.threads.current";

    private final MonitorNacosDiscovery serviceDiscovery;

    private final ActuatorClient actuatorClient;

    @Override
    public boolean support(InstanceTypeEnum instanceType) {
        return instanceType == InstanceTypeEnum.MICROSERVICE;
    }

    @Override
    public List<MetricPoint> collect(CollectorContext ctx) {
        Long instanceId = ctx.instanceId();
        String serviceName = ctx.instance().getAddresses();
        List<Instance> instances = serviceDiscovery.getAllInstances(serviceName);
        List<MetricPoint> points = new ArrayList<>();
        if (instances.isEmpty()) {
            // 无实例：服务级聚合计 0
            points.add(metric(instanceId, MetricNameConstant.SERVICE_INSTANCE_COUNT, "", 0));
            points.add(metric(instanceId, MetricNameConstant.SERVICE_INSTANCE_HEALTHY_COUNT, "", 0));
            points.add(metric(instanceId, MetricNameConstant.SERVICE_INSTANCE_UP, "", 0));
            return points;
        }

        // 并发拉取各实例 actuator（虚拟线程，单实例失败互不影响）
        List<InstanceMetrics> results = new ArrayList<>();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<InstanceMetrics>> futures = instances.stream()
                .map(instance -> executor.submit(() -> collectOne(instanceId, instance)))
                .toList();
            for (Future<InstanceMetrics> future : futures) {
                try {
                    InstanceMetrics result = future.get(10, TimeUnit.SECONDS);
                    if (result != null) {
                        results.add(result);
                    }
                } catch (Exception e) {
                    log.debug("actuator 采集任务异常: {}", e.getMessage());
                }
            }
        }

        long totalRequests = 0;
        long totalErrors = 0;
        int healthyCount = 0;
        for (InstanceMetrics result : results) {
            points.addAll(result.points());
            if (result.healthy()) {
                healthyCount++;
            }
            totalRequests += result.requestCount();
            totalErrors += result.errorCount();
        }
        // 服务级聚合点（node=''）
        points.add(metric(instanceId, MetricNameConstant.SERVICE_INSTANCE_COUNT, "", instances.size()));
        points.add(metric(instanceId, MetricNameConstant.SERVICE_INSTANCE_HEALTHY_COUNT, "", healthyCount));
        points.add(metric(instanceId, MetricNameConstant.SERVICE_INSTANCE_UP, "", healthyCount > 0 ? 1 : 0));
        if (totalRequests > 0) {
            points.add(metric(instanceId, MetricNameConstant.SERVICE_HTTP_ERROR_RATE, "",
                totalErrors * 100D / totalRequests));
        }
        return points;
    }

    /**
     * 单实例采集（health + 指标，缺失指标跳过）
     */
    private InstanceMetrics collectOne(Long instanceId, Instance instance) {
        String node = instance.getIp() + ":" + instance.getPort();
        List<MetricPoint> points = new ArrayList<>();
        boolean healthy = false;
        long requestCount = 0;
        long errorCount = 0;

        // health：UP=1/DOWN=0（拉取失败按 DOWN 记该实例点，不影响其他实例）
        Map<String, Object> health = actuatorClient.get(node, "/health");
        if (health != null && "UP".equalsIgnoreCase(String.valueOf(health.get("status")))) {
            healthy = true;
        }
        points.add(metric(instanceId, MetricNameConstant.SERVICE_HEALTH, node, healthy ? 1 : 0));

        // 单值指标
        addSimpleMetric(points, instanceId, node, M_JVM_MEMORY_USED, MetricNameConstant.SERVICE_JVM_MEMORY_USED);
        addSimpleMetric(points, instanceId, node, M_JVM_MEMORY_MAX, MetricNameConstant.SERVICE_JVM_MEMORY_MAX);
        addSimpleMetric(points, instanceId, node, M_JVM_THREADS_LIVE, MetricNameConstant.SERVICE_JVM_THREADS_LIVE);
        addSimpleMetric(points, instanceId, node, M_JVM_GC_PAUSE, MetricNameConstant.SERVICE_JVM_GC_PAUSE, "TOTAL_TIME");
        addSimpleMetric(points, instanceId, node, M_PROCESS_CPU_USAGE, MetricNameConstant.SERVICE_PROCESS_CPU_USAGE);
        addSimpleMetric(points, instanceId, node, M_TOMCAT_THREADS, MetricNameConstant.SERVICE_TOMCAT_THREADS_CURRENT);

        // http.server.requests：总数 + 5xx（服务端错误率；uri 高基数不带，只按 status 维度）
        Double total = metricValue(node, M_HTTP_REQUESTS, "COUNT");
        if (total != null) {
            requestCount = total.longValue();
            points.add(metric(instanceId, MetricNameConstant.SERVICE_HTTP_REQUESTS_COUNT, node, total));
            Double errors = metricValue(node, M_HTTP_REQUESTS, "COUNT", "status:5xx");
            if (errors != null) {
                errorCount = errors.longValue();
            }
            if (total > 0) {
                points.add(metric(instanceId, MetricNameConstant.SERVICE_HTTP_ERROR_RATE, node,
                    errorCount * 100D / total));
            }
        }
        return new InstanceMetrics(points, healthy, requestCount, errorCount);
    }

    private void addSimpleMetric(List<MetricPoint> points, Long instanceId, String node,
                                 String actuatorName, String metricName) {
        addSimpleMetric(points, instanceId, node, actuatorName, metricName, "VALUE");
    }

    private void addSimpleMetric(List<MetricPoint> points, Long instanceId, String node,
                                 String actuatorName, String metricName, String statistic) {
        Double value = metricValue(node, actuatorName, statistic);
        if (value != null) {
            points.add(metric(instanceId, metricName, node, value));
        }
    }

    /**
     * 读取 actuator 指标值（/metrics/{name} 的 measurements[statistic]；可带 tag 过滤）
     */
    private Double metricValue(String node, String actuatorName, String statistic, String... tags) {
        StringBuilder path = new StringBuilder("/metrics/").append(actuatorName);
        for (String tag : tags) {
            path.append(path.indexOf("?") < 0 ? "?" : "&").append("tag=").append(tag);
        }
        Map<String, Object> result = actuatorClient.get(node, path.toString());
        if (result == null) {
            return null;
        }
        Object measurements = result.get("measurements");
        if (!(measurements instanceof List<?> list)) {
            return null;
        }
        for (Object item : list) {
            if (!(item instanceof Map)) {
                continue;
            }
            Object stat = ((Map<?, ?>) item).get("statistic");
            Object value = ((Map<?, ?>) item).get("value");
            if (statistic.equalsIgnoreCase(String.valueOf(stat)) && value instanceof Number number) {
                return number.doubleValue();
            }
        }
        return null;
    }

    private MetricPoint metric(Long instanceId, String name, String node, double value) {
        return MetricPoint.of(instanceId, MetricSourceEnum.SERVICE, name, node, value);
    }

    /**
     * 单实例采集结果
     */
    private record InstanceMetrics(List<MetricPoint> points, boolean healthy,
                                   long requestCount, long errorCount) {
    }
}
