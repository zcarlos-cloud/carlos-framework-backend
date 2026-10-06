package com.carlos.monitor.redis.controller;

import com.carlos.core.pagination.Paging;
import com.carlos.core.response.Result;
import com.carlos.monitor.collect.sink.MonitorCkQueryClient;
import com.carlos.monitor.redis.pojo.param.BigkeyReportPageParam;
import com.carlos.monitor.redis.pojo.vo.BigkeyReportItemVO;
import com.carlos.monitor.redis.pojo.vo.BigkeyReportVO;
import com.carlos.monitor.redis.pojo.vo.HistoryPointVO;
import com.carlos.monitor.redis.pojo.vo.MetricCurrentVO;
import com.carlos.monitor.redis.pojo.vo.RedisNodeVO;
import com.carlos.monitor.redis.pojo.vo.SlowlogPageVO;
import com.carlos.monitor.redis.service.BigkeyReportService;
import com.carlos.monitor.redis.service.RedisMetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Redis 监控 rest服务接口（指标/节点/慢日志/大key分析）
 * </p>
 *
 * @author Carlos
 * @date 2026年10月3日
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/monitor/redis")
@Tag(name = "Redis监控")
public class RedisMetricsController {

    private final RedisMetricsService redisMetricsService;

    private final BigkeyReportService bigkeyReportService;

    @PreAuthorize("hasPermission('monitor:redis', 'view')")
    @GetMapping("/{id}/metrics/current")
    @Operation(summary = "当前指标快照", description = "Caffeine 60s + Redis 5min 缓存，采集成功时由 sink 侧回写")
    public Result<MetricCurrentVO> current(@PathVariable("id") Long id) {
        MetricCurrentVO vo = new MetricCurrentVO();
        vo.setMetrics(redisMetricsService.getCurrentMetrics(id));
        return Result.success(vo);
    }

    @PreAuthorize("hasPermission('monitor:redis', 'view')")
    @GetMapping("/{id}/metrics/history")
    @Operation(summary = "指标时序", description = "range<=6h 查原始点，>6h 查小时聚合（avgMerge/maxMerge/quantileMerge）")
    public Result<List<HistoryPointVO>> history(@PathVariable("id") Long id,
                                                @RequestParam("metric") String metric,
                                                @RequestParam(value = "range", defaultValue = "1h") String range,
                                                @RequestParam(value = "node", required = false) String node) {
        List<?> rows = redisMetricsService.getMetricHistory(id, metric, range, node);
        return Result.success(toHistoryVO(rows));
    }

    @PreAuthorize("hasPermission('monitor:redis', 'view')")
    @GetMapping("/{id}/nodes")
    @Operation(summary = "节点与拓扑", description = "cluster/sentinel 节点（CLUSTER NODES / INFO replication 解析）")
    public Result<List<RedisNodeVO>> nodes(@PathVariable("id") Long id) {
        return Result.success(redisMetricsService.listNodes(id));
    }

    @PreAuthorize("hasPermission('monitor:redis', 'view')")
    @GetMapping("/{id}/slowlog/page")
    @Operation(summary = "慢日志分页", description = "时间/耗时/节点过滤")
    public Result<SlowlogPageVO> slowlogPage(@PathVariable("id") Long id,
                                             @RequestParam(value = "hours", required = false) Integer hours,
                                             @RequestParam(value = "minDurationUs", required = false) Long minDurationUs,
                                             @RequestParam(value = "node", required = false) String node,
                                             @RequestParam(value = "offset", defaultValue = "0") int offset,
                                             @RequestParam(value = "size", defaultValue = "50") int size) {
        List<MonitorCkQueryClient.SlowlogQueryRow> rows =
            redisMetricsService.pageSlowlogs(id, node, hours, minDurationUs, offset, size);
        SlowlogPageVO vo = new SlowlogPageVO();
        List<SlowlogPageVO.SlowlogVO> records = rows.stream().map(row -> {
            SlowlogPageVO.SlowlogVO item = new SlowlogPageVO.SlowlogVO();
            item.setNode(row.node());
            item.setSlowlogId(row.slowlogId());
            item.setTs(row.ts());
            item.setDurationUs(row.durationUs());
            item.setCommand(row.command());
            item.setClientAddr(row.clientAddr());
            return item;
        }).toList();
        vo.setRecords(records);
        vo.setHasMore(records.size() >= size);
        vo.setNextOffset(offset + records.size());
        return Result.success(vo);
    }

    @PreAuthorize("hasPermission('monitor:redis', 'analysis')")
    @PostMapping("/{id}/analysis/bigkey")
    @Operation(summary = "手动触发大 key 扫描", description = "异步执行，返回报告ID")
    public Result<Long> triggerBigkey(@PathVariable("id") Long id,
                                      @RequestParam(value = "reportType", defaultValue = "BIGKEY") String reportType) {
        return Result.success(bigkeyReportService.triggerScan(id, reportType));
    }

    @PreAuthorize("hasPermission('monitor:redis', 'view')")
    @GetMapping("/{id}/analysis/reports/page")
    @Operation(summary = "分析报告分页")
    public Result<Paging<BigkeyReportVO>> reportsPage(BigkeyReportPageParam param) {
        return Result.success(bigkeyReportService.getPage(param));
    }

    @PreAuthorize("hasPermission('monitor:redis', 'view')")
    @GetMapping("/{id}/analysis/reports/{reportId}")
    @Operation(summary = "分析报告明细", description = "按 memory_bytes 倒序")
    public Result<Paging<BigkeyReportItemVO>> reportDetail(@PathVariable("id") Long id,
                                                           @PathVariable("reportId") Long reportId,
                                                           @RequestParam(value = "current", defaultValue = "1") int current,
                                                           @RequestParam(value = "size", defaultValue = "50") int size) {
        return Result.success(bigkeyReportService.getItems(reportId, current, size));
    }

    /**
     * 原始点/聚合点统一转 VO
     */
    private List<HistoryPointVO> toHistoryVO(List<?> rows) {
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
        return result;
    }
}
