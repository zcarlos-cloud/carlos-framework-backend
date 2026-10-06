package com.carlos.monitor.collect.schedule;

import com.carlos.monitor.collect.db.SlowSqlCollector;
import com.carlos.monitor.collect.redis.RedisSlowlogCollector;
import com.carlos.monitor.collect.sink.MetricSink;
import com.carlos.monitor.collect.spi.CollectorContext;
import com.carlos.monitor.collect.spi.MetricCollector;
import com.carlos.monitor.collect.spi.MetricPoint;
import com.carlos.monitor.config.MonitorProperties;
import com.carlos.monitor.constant.MetricNameConstant;
import com.carlos.monitor.enums.InstanceTypeEnum;
import com.carlos.monitor.enums.MetricSourceEnum;
import com.carlos.monitor.instance.connection.InstanceConnectionInvalidEvent;
import com.carlos.monitor.instance.connection.InstanceConnectionRegistry;
import com.carlos.monitor.instance.connection.ManagedConnection;
import com.carlos.monitor.instance.connection.ManagedConnectionFactory;
import com.carlos.monitor.instance.pojo.dto.MonitorInstanceDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 * 采集任务路由器
 * 按 instance_type 路由到 MetricCollector（SPI）；单实例全部包 try-catch 隔离，
 * 失败仅回写 healthStatus=DOWN 并产出 instance.unreachable 指标点，不影响其他实例；
 * 监听实例停采/删除事件跳过采集
 * </p>
 *
 * @author Carlos
 * @date 2026年10月2日
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CollectTaskRouter {

    private final List<MetricCollector> metricCollectors;

    private final RedisSlowlogCollector redisSlowlogCollector;

    private final SlowSqlCollector slowSqlCollector;

    private final InstanceConnectionRegistry connectionRegistry;

    private final InstanceHealthReporter healthReporter;

    private final MetricSink metricSink;

    private final MonitorProperties monitorProperties;

    /**
     * 停采/删除的实例（采集路由跳过；重新开启采集时按扫描结果自动解除）
     */
    private final Set<Long> pausedInstances = ConcurrentHashMap.newKeySet();

    /**
     * 监听 N2 的停采/删除事件：采集路由跳过
     */
    @EventListener
    public void onInstanceInvalid(InstanceConnectionInvalidEvent event) {
        pausedInstances.add(event.getInstanceId());
        log.info("实例采集已暂停, instanceId:{}, reason:{}", event.getInstanceId(), event.getReason());
    }

    /**
     * 执行单实例采集（调度层虚拟线程内调用）
     *
     * @param instance 实例 DTO（调度扫描的当时快照）
     */
    public void collect(MonitorInstanceDTO instance) {
        Long instanceId = instance.getId();
        // 重新开启采集的实例解除暂停标记
        if (Boolean.TRUE.equals(instance.getCollectEnabled())) {
            pausedInstances.remove(instanceId);
        }
        if (pausedInstances.contains(instanceId) || Boolean.FALSE.equals(instance.getCollectEnabled())) {
            return;
        }
        InstanceTypeEnum instanceType = instance.getInstanceType();
        try {
            // 微服务实例 actuator 直拉，不经过连接注册中心
            ManagedConnection connection = instanceType == InstanceTypeEnum.MICROSERVICE
                ? null
                : connectionRegistry.getConnection(instanceId);
            CollectorContext ctx = new CollectorContext(instance, connection, monitorProperties);

            // 1. 指标采集（SPI 路由；连接级失败上抛 → 实例判 DOWN）
            for (MetricCollector collector : metricCollectors) {
                if (!collector.support(instanceType)) {
                    continue;
                }
                try {
                    List<MetricPoint> points = collector.collect(ctx);
                    metricSink.offerMetrics(points);
                } catch (Exception e) {
                    log.warn("指标采集器执行失败, instanceId:{}, collector:{}, error:{}",
                        instanceId, collector.getClass().getSimpleName(), e.getMessage());
                    throw e;
                }
            }

            // 2. 慢日志/慢 SQL 明细（失败不影响指标点采集，不判 DOWN）
            collectDetails(ctx);

            healthReporter.reportUp(instanceId);
        } catch (Exception e) {
            log.warn("实例采集失败, instanceId:{}, type:{}, error:{}",
                instanceId, instanceType, e.getMessage());
            healthReporter.reportDown(instanceId);
            // 产出不可达指标点（供告警规则 instance.unreachable 判定）
            metricSink.offer(MetricPoint.of(instanceId, sourceOf(instanceType),
                MetricNameConstant.INSTANCE_UNREACHABLE, "", 1D));
        }
    }

    private void collectDetails(CollectorContext ctx) {
        InstanceTypeEnum instanceType = ctx.instance().getInstanceType();
        if (redisSlowlogCollector.support(instanceType)) {
            try {
                metricSink.offerSlowlogs(redisSlowlogCollector.collectSlowlogs(ctx));
            } catch (Exception e) {
                log.warn("慢日志采集失败, instanceId:{}, error:{}", ctx.instanceId(), e.getMessage());
            }
        }
        if (slowSqlCollector.support(instanceType)) {
            try {
                metricSink.offerSlowSqls(slowSqlCollector.collectSlowSql(ctx));
            } catch (Exception e) {
                log.warn("慢 SQL 采集失败, instanceId:{}, error:{}", ctx.instanceId(), e.getMessage());
            }
        }
    }

    private MetricSourceEnum sourceOf(InstanceTypeEnum instanceType) {
        if (instanceType == InstanceTypeEnum.MICROSERVICE) {
            return MetricSourceEnum.SERVICE;
        }
        return ManagedConnectionFactory.isRedisType(instanceType) ? MetricSourceEnum.REDIS : MetricSourceEnum.DB;
    }
}
