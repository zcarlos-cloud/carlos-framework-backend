package com.carlos.monitor.collect.sink;

import com.carlos.core.exception.BusinessException;
import com.carlos.monitor.enums.MonitorErrorCode;
import com.clickhouse.client.ClickHouseClient;
import com.clickhouse.client.ClickHouseNode;
import com.clickhouse.client.ClickHouseResponse;
import com.clickhouse.data.ClickHouseFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 监控 ClickHouse 只读查询封装
 * SQL 固化在代码内，不对外暴露；输入参数全部经类型化绑定（数字直拼、字符串转义、时间格式化），
 * 供 N4/N5/N6 的时序/慢日志/慢 SQL 查询使用
 * </p>
 *
 * @author Carlos
 * @date 2026年10月2日
 */
@Slf4j
@Component
public class MonitorCkQueryClient {

    /**
     * 查询超时与行数上限
     */
    private static final int MAX_EXECUTION_TIME_SECONDS = 5;

    private static final long MAX_ROWS_TO_READ = 10_000_000L;

    private static final DateTimeFormatter CK_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    /** 秒级时间（hour DateTime 列不接受毫秒字符串字面量） */
    private static final DateTimeFormatter CK_DATETIME_SECOND_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * JSONEachRow 每行一个 JSON 对象，由 Jackson 映射为行对象
     * （不能用 response.records(Class)：JSONEachRow 响应没有列元数据）
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final ClickHouseClient clickHouseClient;

    private final ClickHouseNode clickHouseNode;

    public MonitorCkQueryClient(@Qualifier("monitorClickHouseClient") ClickHouseClient clickHouseClient,
                                @Qualifier("monitorClickHouseNode") ClickHouseNode clickHouseNode) {
        this.clickHouseClient = clickHouseClient;
        this.clickHouseNode = clickHouseNode;
    }

    // ==================== 固化 SQL ====================

    private static final String SQL_METRIC_POINT =
        "SELECT instance_id, metric_source, metric_name, node, tags, value, ts "
            + "FROM monitor_metric_point "
            + "WHERE instance_id = %d AND metric_name = %s AND ts >= %s AND ts <= %s %s"
            + "ORDER BY ts LIMIT %d";

    private static final String SQL_METRIC_HOURLY =
        "SELECT toStartOfHour(hour) AS hour, avgMerge(avg_value) AS avg_value, maxMerge(max_value) AS max_value, "
            + "minMerge(min_value) AS min_value, quantileMerge(0.95)(p95_value) AS p95_value, "
            + "countMerge(point_count) AS point_count "
            + "FROM monitor_metric_point_hourly "
            + "WHERE instance_id = %d AND metric_name = %s AND hour >= %s AND hour <= %s %s"
            + "GROUP BY hour ORDER BY hour";

    private static final String SQL_SLOWLOG_PAGE =
        "SELECT instance_id, node, slowlog_id, ts, duration_us, command, client_addr "
            + "FROM monitor_redis_slowlog "
            + "WHERE instance_id = %d AND ts >= %s AND ts <= %s %s"
            + "ORDER BY ts DESC LIMIT %d OFFSET %d";

    private static final String SQL_SLOW_SQL_PAGE =
        "SELECT instance_id, fingerprint, sample_sql, db_name, exec_count, avg_ms, max_ms, last_exec_time, collect_time "
            + "FROM monitor_db_slow_sql "
            + "WHERE instance_id = %d AND collect_time >= %s AND collect_time <= %s %s"
            + "ORDER BY collect_time DESC LIMIT %d OFFSET %d";

    // ==================== 查询方法 ====================

    /**
     * 原始指标点时序查询（1h/6h 范围）
     *
     * @param node 节点地址，null/空 不过滤
     */
    public List<MetricPointRow> queryMetricPoints(Long instanceId, String metricName, String node,
                                                  Instant start, Instant end, int limit) {
        String nodeFilter = node == null || node.isEmpty() ? "" : "AND node = " + sqlString(node) + " ";
        String sql = String.format(SQL_METRIC_POINT,
            instanceId, sqlString(metricName), sqlDateTime(start), sqlDateTime(end), nodeFilter, limit);
        return queryForList(sql, MetricPointRow.class);
    }

    /**
     * 小时聚合指标查询（24h/7d/30d 范围，avgMerge/maxMerge/quantileMerge）
     *
     * @param node 节点地址，null/空 不过滤
     */
    public List<MetricHourlyRow> queryMetricHourly(Long instanceId, String metricName, String node,
                                                   Instant start, Instant end) {
        String nodeFilter = node == null || node.isEmpty() ? "" : "AND node = " + sqlString(node) + " ";
        String sql = String.format(SQL_METRIC_HOURLY,
            instanceId, sqlString(metricName), sqlDateTimeSecond(start), sqlDateTimeSecond(end), nodeFilter);
        return queryForList(sql, MetricHourlyRow.class);
    }

    /**
     * Redis 慢日志分页
     *
     * @param node          节点地址，null/空 不过滤
     * @param minDurationUs 最小耗时（微秒），null 不过滤
     */
    public List<SlowlogQueryRow> pageSlowlogs(Long instanceId, String node,
                                              Instant start, Instant end, Long minDurationUs,
                                              int offset, int limit) {
        String nodeFilter = node == null || node.isEmpty() ? "" : "AND node = " + sqlString(node) + " ";
        if (minDurationUs != null) {
            nodeFilter += "AND duration_us >= " + minDurationUs + " ";
        }
        String sql = String.format(SQL_SLOWLOG_PAGE,
            instanceId, sqlDateTime(start), sqlDateTime(end), nodeFilter, limit, offset);
        return queryForList(sql, SlowlogQueryRow.class);
    }

    /**
     * 慢 SQL 分页
     *
     * @param fingerprint SQL 指纹，null/空 不过滤
     */
    public List<SlowSqlQueryRow> pageSlowSqls(Long instanceId, String fingerprint,
                                              Instant start, Instant end, int offset, int limit) {
        String fpFilter = fingerprint == null || fingerprint.isEmpty()
            ? "" : "AND fingerprint = " + sqlString(fingerprint) + " ";
        String sql = String.format(SQL_SLOW_SQL_PAGE,
            instanceId, sqlDateTime(start), sqlDateTime(end), fpFilter, limit, offset);
        return queryForList(sql, SlowSqlQueryRow.class);
    }

    // ==================== 通用执行 ====================

    private <T> List<T> queryForList(String sql, Class<T> rowType) {
        String limitedSql = sql + " SETTINGS max_execution_time = " + MAX_EXECUTION_TIME_SECONDS
            + ", max_rows_to_read = " + MAX_ROWS_TO_READ;
        try (ClickHouseResponse response = clickHouseClient.read(clickHouseNode)
            .query(limitedSql)
            .format(ClickHouseFormat.JSONEachRow)
            .executeAndWait();
             BufferedReader reader = new BufferedReader(
                 new InputStreamReader(response.getInputStream(), StandardCharsets.UTF_8))) {
            List<T> result = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    result.add(OBJECT_MAPPER.readValue(line, rowType));
                }
            }
            return result;
        } catch (Exception e) {
            log.error("ClickHouse query failed, sql:{}", sql, e);
            throw new BusinessException(MonitorErrorCode.MONITOR_CLICKHOUSE_WRITE_FAILED, "ClickHouse 查询失败", e);
        }
    }

    // ==================== 类型化参数绑定（SQL 固化，仅绑定值） ====================

    private static String sqlString(String value) {
        return "'" + value.replace("\\", "\\\\").replace("'", "\\'") + "'";
    }

    private static String sqlDateTime(Instant instant) {
        return "'" + LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).format(CK_DATETIME_FORMATTER) + "'";
    }

    private static String sqlDateTimeSecond(Instant instant) {
        return "'" + LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).format(CK_DATETIME_SECOND_FORMATTER) + "'";
    }

    // ==================== 查询结果行 ====================

    /**
     * 原始指标点行
     */
    public record MetricPointRow(
        @JsonProperty("instance_id") Long instanceId,
        @JsonProperty("metric_source") String metricSource,
        @JsonProperty("metric_name") String metricName,
        @JsonProperty("node") String node,
        @JsonProperty("tags") Map<String, String> tags,
        @JsonProperty("value") Double value,
        @JsonProperty("ts") String ts) {
    }

    /**
     * 小时聚合指标行
     */
    public record MetricHourlyRow(
        @JsonProperty("hour") String hour,
        @JsonProperty("avg_value") Double avgValue,
        @JsonProperty("max_value") Double maxValue,
        @JsonProperty("min_value") Double minValue,
        @JsonProperty("p95_value") Double p95Value,
        @JsonProperty("point_count") Long pointCount) {
    }

    /**
     * Redis 慢日志行
     */
    public record SlowlogQueryRow(
        @JsonProperty("instance_id") Long instanceId,
        @JsonProperty("node") String node,
        @JsonProperty("slowlog_id") Long slowlogId,
        @JsonProperty("ts") String ts,
        @JsonProperty("duration_us") Long durationUs,
        @JsonProperty("command") String command,
        @JsonProperty("client_addr") String clientAddr) {
    }

    /**
     * 慢 SQL 行
     */
    public record SlowSqlQueryRow(
        @JsonProperty("instance_id") Long instanceId,
        @JsonProperty("fingerprint") String fingerprint,
        @JsonProperty("sample_sql") String sampleSql,
        @JsonProperty("db_name") String dbName,
        @JsonProperty("exec_count") Long execCount,
        @JsonProperty("avg_ms") Double avgMs,
        @JsonProperty("max_ms") Double maxMs,
        @JsonProperty("last_exec_time") String lastExecTime,
        @JsonProperty("collect_time") String collectTime) {
    }
}
