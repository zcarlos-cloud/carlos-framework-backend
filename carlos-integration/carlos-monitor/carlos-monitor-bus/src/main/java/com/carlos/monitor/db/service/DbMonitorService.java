package com.carlos.monitor.db.service;

import com.carlos.monitor.collect.sink.MonitorCkQueryClient;
import com.carlos.monitor.db.pojo.dto.DbLockRow;
import com.carlos.monitor.db.pojo.dto.DbSessionRow;
import com.carlos.monitor.db.pojo.vo.CapacitySnapshotVO;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 数据库监控查询 业务接口
 * </p>
 *
 * @author Carlos
 * @date 2026年10月4日
 */
public interface DbMonitorService {

    /**
     * 当前指标快照
     */
    Map<String, String> getCurrentMetrics(Long instanceId);

    /**
     * 指标时序（range<=6h 原始点，>6h 小时聚合）
     */
    List<?> getMetricHistory(Long instanceId, String metricName, String range, String node);

    /**
     * 慢 SQL 分页（指纹可选：不带为聚合视图，带为明细钻取）
     */
    List<MonitorCkQueryClient.SlowSqlQueryRow> pageSlowSql(Long instanceId, String fingerprint,
                                                           Integer hours, int offset, int limit);

    /**
     * 会话列表
     */
    List<DbSessionRow> listSessions(Long instanceId);

    /**
     * 终止会话（高危：confirm-token）
     */
    boolean killSession(Long instanceId, String sessionId, String confirmToken);

    /**
     * 锁等待/阻塞链
     */
    List<DbLockRow> listLocks(Long instanceId);

    /**
     * 容量快照 Top N（date 为空取最新日期）
     */
    List<CapacitySnapshotVO> getCapacityTop(Long instanceId, LocalDate date, int topN);
}
