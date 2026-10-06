package com.carlos.monitor.collect.db.adapter;

import com.carlos.monitor.collect.spi.MetricPoint;
import com.carlos.monitor.collect.spi.SlowSqlRow;
import com.carlos.monitor.enums.InstanceTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.util.List;

/**
 * <p>
 * 达梦（DM）适配器
 * M5 排期实现（设计文档第 11 节待确认问题 4），首期不支持：返回空结果
 * </p>
 *
 * @author Carlos
 * @date 2026年10月2日
 */
@Slf4j
@Component
public class DmAdapter implements DbTypeAdapter {

    @Override
    public boolean support(InstanceTypeEnum instanceType) {
        return instanceType == InstanceTypeEnum.DM;
    }

    @Override
    public List<MetricPoint> collectInstanceMetrics(Long instanceId, Connection connection) {
        log.debug("DM 实例指标采集暂未实现（M5 排期）, instanceId:{}", instanceId);
        return List.of();
    }

    @Override
    public List<SlowSqlRow> collectSlowSql(Long instanceId, String dbName, Connection connection) {
        log.debug("DM 慢 SQL 采集暂未实现（M5 排期）, instanceId:{}", instanceId);
        return List.of();
    }
}
