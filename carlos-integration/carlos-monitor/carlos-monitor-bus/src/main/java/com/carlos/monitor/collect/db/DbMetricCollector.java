package com.carlos.monitor.collect.db;

import com.alibaba.druid.pool.DruidDataSource;
import com.carlos.monitor.collect.db.adapter.DbTypeAdapter;
import com.carlos.monitor.collect.spi.CollectorContext;
import com.carlos.monitor.collect.spi.MetricCollector;
import com.carlos.monitor.collect.spi.MetricPoint;
import com.carlos.monitor.constant.MetricNameConstant;
import com.carlos.monitor.enums.InstanceTypeEnum;
import com.carlos.monitor.enums.MetricSourceEnum;
import com.carlos.monitor.instance.connection.DbManagedConnection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 数据库指标采集器
 * 连接池指标（受管 DruidDataSource 自身：activeCount/poolingCount/waitThreadCount/maxActive）
 * + DbTypeAdapter 实例指标（QPS/TPS/连接数/慢查询数等）
 * </p>
 *
 * @author Carlos
 * @date 2026年10月2日
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DbMetricCollector implements MetricCollector {

    private final List<DbTypeAdapter> dbTypeAdapters;

    @Override
    public boolean support(InstanceTypeEnum instanceType) {
        return instanceType == InstanceTypeEnum.POSTGRESQL
            || instanceType == InstanceTypeEnum.MYSQL
            || instanceType == InstanceTypeEnum.DM
            || instanceType == InstanceTypeEnum.CLICKHOUSE;
    }

    @Override
    public List<MetricPoint> collect(CollectorContext ctx) throws Exception {
        if (!(ctx.connection() instanceof DbManagedConnection dbConnection)) {
            return List.of();
        }
        Long instanceId = ctx.instanceId();
        List<MetricPoint> points = new ArrayList<>();

        // 1. 连接池指标（受管小池自身状态）
        collectPoolMetrics(instanceId, dbConnection, points);

        // 2. 实例指标（DbTypeAdapter 系统视图；DM 首期空实现）
        DbTypeAdapter adapter = findAdapter(ctx.instance().getInstanceType());
        if (adapter != null) {
            try (Connection connection = dbConnection.getDataSource().getConnection()) {
                points.addAll(adapter.collectInstanceMetrics(instanceId, connection));
            } catch (Exception e) {
                // 适配器失败不影响连接池指标
                log.warn("DB 实例指标采集失败, instanceId:{}, error:{}", instanceId, e.getMessage());
            }
        }
        return points;
    }

    private void collectPoolMetrics(Long instanceId, DbManagedConnection dbConnection, List<MetricPoint> points) {
        try {
            DruidDataSource dataSource = dbConnection.getDataSource();
            points.add(metric(instanceId, MetricNameConstant.DB_POOL_ACTIVE_COUNT, dataSource.getActiveCount()));
            points.add(metric(instanceId, MetricNameConstant.DB_POOL_POOLING_COUNT, dataSource.getPoolingCount()));
            points.add(metric(instanceId, MetricNameConstant.DB_POOL_WAIT_THREAD_COUNT, dataSource.getWaitThreadCount()));
            points.add(metric(instanceId, MetricNameConstant.DB_POOL_MAX_ACTIVE, dataSource.getMaxActive()));
        } catch (Exception e) {
            log.warn("采集连接池指标失败, instanceId:{}, error:{}", instanceId, e.getMessage());
        }
    }

    /**
     * 查找适配器（ClickHouse 首期无适配器，只采连接池指标）
     */
    public DbTypeAdapter findAdapter(InstanceTypeEnum instanceType) {
        for (DbTypeAdapter adapter : dbTypeAdapters) {
            if (adapter.support(instanceType)) {
                return adapter;
            }
        }
        return null;
    }

    private MetricPoint metric(Long instanceId, String name, double value) {
        return MetricPoint.of(instanceId, MetricSourceEnum.DB, name, "", value);
    }
}
