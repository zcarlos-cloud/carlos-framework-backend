package com.carlos.monitor.db.controller;

import com.carlos.core.response.Result;
import com.carlos.monitor.audit.MonitorAudit;
import com.carlos.monitor.collect.sink.MonitorCkQueryClient;
import com.carlos.monitor.db.pojo.dto.DbLockRow;
import com.carlos.monitor.db.pojo.dto.DbSessionRow;
import com.carlos.monitor.db.pojo.vo.CapacitySnapshotVO;
import com.carlos.monitor.db.pojo.vo.SlowSqlPageVO;
import com.carlos.monitor.db.service.DbMonitorService;
import com.carlos.monitor.redis.pojo.vo.HistoryPointVO;
import com.carlos.monitor.redis.pojo.vo.MetricCurrentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 数据库监控 rest服务接口（指标/慢SQL/会话/锁/容量）
 * </p>
 *
 * @author Carlos
 * @date 2026年10月4日
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/monitor/db")
@Tag(name = "数据库监控")
public class DbMetricsController {

    private final DbMonitorService dbMonitorService;

    @PreAuthorize("hasPermission('monitor:db', 'view')")
    @GetMapping("/{id}/metrics/current")
    @Operation(summary = "当前指标快照")
    public Result<MetricCurrentVO> current(@PathVariable("id") Long id) {
        MetricCurrentVO vo = new MetricCurrentVO();
        vo.setMetrics(dbMonitorService.getCurrentMetrics(id));
        return Result.success(vo);
    }

    @PreAuthorize("hasPermission('monitor:db', 'view')")
    @GetMapping("/{id}/metrics/history")
    @Operation(summary = "指标时序", description = "range<=6h 查原始点，>6h 查小时聚合")
    public Result<List<HistoryPointVO>> history(@PathVariable("id") Long id,
                                                @RequestParam("metric") String metric,
                                                @RequestParam(value = "range", defaultValue = "1h") String range,
                                                @RequestParam(value = "node", required = false) String node) {
        List<?> rows = dbMonitorService.getMetricHistory(id, metric, range, node);
        List<HistoryPointVO> result = new ArrayList<>(rows.size());
        for (Object row : rows) {
            HistoryPointVO point = new HistoryPointVO();
            if (row instanceof MonitorCkQueryClient.MetricPointRow raw) {
                point.setTs(raw.ts());
                point.setValue(raw.value());
            } else if (row instanceof MonitorCkQueryClient.MetricHourlyRow hourly) {
                point.setTs(hourly.hour());
                point.setAvgValue(hourly.avgValue());
                point.setMaxValue(hourly.maxValue());
                point.setMinValue(hourly.minValue());
                point.setP95Value(hourly.p95Value());
                point.setPointCount(hourly.pointCount());
            }
            result.add(point);
        }
        return Result.success(result);
    }

    @PreAuthorize("hasPermission('monitor:db', 'view')")
    @GetMapping("/{id}/slow-sql/page")
    @Operation(summary = "慢 SQL 分页", description = "不带指纹为聚合视图，带指纹为明细钻取")
    public Result<SlowSqlPageVO> slowSqlPage(@PathVariable("id") Long id,
                                             @RequestParam(value = "fingerprint", required = false) String fingerprint,
                                             @RequestParam(value = "hours", required = false) Integer hours,
                                             @RequestParam(value = "offset", defaultValue = "0") int offset,
                                             @RequestParam(value = "size", defaultValue = "50") int size) {
        List<MonitorCkQueryClient.SlowSqlQueryRow> rows =
            dbMonitorService.pageSlowSql(id, fingerprint, hours, offset, size);
        SlowSqlPageVO vo = new SlowSqlPageVO();
        List<SlowSqlPageVO.SlowSqlVO> records = rows.stream().map(row -> {
            SlowSqlPageVO.SlowSqlVO item = new SlowSqlPageVO.SlowSqlVO();
            item.setFingerprint(row.fingerprint());
            item.setSampleSql(row.sampleSql());
            item.setDbName(row.dbName());
            item.setExecCount(row.execCount());
            item.setAvgMs(row.avgMs());
            item.setMaxMs(row.maxMs());
            item.setLastExecTime(row.lastExecTime());
            item.setCollectTime(row.collectTime());
            return item;
        }).toList();
        vo.setRecords(records);
        vo.setHasMore(records.size() >= size);
        vo.setNextOffset(offset + records.size());
        return Result.success(vo);
    }

    @PreAuthorize("hasPermission('monitor:db', 'view')")
    @GetMapping("/{id}/sessions")
    @Operation(summary = "会话列表")
    public Result<List<DbSessionRow>> sessions(@PathVariable("id") Long id) {
        return Result.success(dbMonitorService.listSessions(id));
    }

    @MonitorAudit(action = "DB_SESSION_KILL", target = "#id + ':' + #sessionId", snapshot = false, riskLevel = 80)
    @PreAuthorize("hasPermission('monitor:db:session', 'kill')")
    @PostMapping("/{id}/sessions/kill")
    @Operation(summary = "终止会话", description = "高危：必须携带 X-Confirm-Token")
    public Result<Boolean> killSession(@PathVariable("id") Long id,
                                       @RequestParam("sessionId") String sessionId,
                                       @RequestHeader(value = "X-Confirm-Token", required = false) String confirmToken) {
        return Result.success(dbMonitorService.killSession(id, sessionId, confirmToken));
    }

    @PreAuthorize("hasPermission('monitor:db', 'view')")
    @GetMapping("/{id}/locks")
    @Operation(summary = "锁等待/阻塞链")
    public Result<List<DbLockRow>> locks(@PathVariable("id") Long id) {
        return Result.success(dbMonitorService.listLocks(id));
    }

    @PreAuthorize("hasPermission('monitor:db', 'capacity')")
    @GetMapping("/{id}/capacity")
    @Operation(summary = "容量快照 Top N", description = "date 为空取最新快照日期")
    public Result<List<CapacitySnapshotVO>> capacity(@PathVariable("id") Long id,
                                                     @RequestParam(value = "date", required = false)
                                                     @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                                     @RequestParam(value = "topN", defaultValue = "20") int topN) {
        return Result.success(dbMonitorService.getCapacityTop(id, date, topN));
    }
}
