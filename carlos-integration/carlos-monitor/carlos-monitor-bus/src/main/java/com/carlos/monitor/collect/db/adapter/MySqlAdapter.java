package com.carlos.monitor.collect.db.adapter;

import com.alibaba.druid.DbType;
import com.carlos.monitor.collect.spi.MetricPoint;
import com.carlos.monitor.collect.spi.SlowSqlRow;
import com.carlos.monitor.collect.spi.SqlFingerprintUtil;
import com.carlos.monitor.constant.MetricNameConstant;
import com.carlos.monitor.db.pojo.dto.CapacityRow;
import com.carlos.monitor.db.pojo.dto.DbColumnRow;
import com.carlos.monitor.db.pojo.dto.DbLockRow;
import com.carlos.monitor.db.pojo.dto.DbSessionRow;
import com.carlos.monitor.db.pojo.dto.DbTableRow;
import com.carlos.monitor.enums.InstanceTypeEnum;
import com.carlos.monitor.enums.MetricSourceEnum;
import com.carlos.monitor.enums.MonitorErrorCode;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * MySQL 适配器
 * 实例指标：SHOW GLOBAL STATUS（Questions/Com_commit/Com_rollback/Threads_connected/Threads_running/Slow_queries）
 * + information_schema.processlist（按 COMMAND 聚合会话）；
 * 慢 SQL：performance_schema.events_statements_summary_by_digest（不可用降级空列表）
 * </p>
 *
 * @author Carlos
 * @date 2026年10月2日
 */
@Slf4j
@Component
public class MySqlAdapter implements DbTypeAdapter {

    /**
     * timer_wait 单位：皮秒 → 毫秒换算因子
     */
    private static final double PICO_TO_MILLI = 1_000_000_000D;

    /**
     * 慢 SQL 判定阈值（毫秒）
     */
    private static final double SLOW_SQL_THRESHOLD_MS = 1000D;

    /**
     * 单次采集慢 SQL 行数上限
     */
    private static final int SLOW_SQL_LIMIT = 200;

    @Override
    public boolean support(InstanceTypeEnum instanceType) {
        return instanceType == InstanceTypeEnum.MYSQL;
    }

    @Override
    public List<MetricPoint> collectInstanceMetrics(Long instanceId, Connection connection) {
        List<MetricPoint> points = new ArrayList<>();
        collectGlobalStatus(instanceId, connection, points);
        collectProcesslist(instanceId, connection, points);
        return points;
    }

    /**
     * SHOW GLOBAL STATUS：QPS/TPS/连接/慢查询计数器（累计值）
     */
    private void collectGlobalStatus(Long instanceId, Connection connection, List<MetricPoint> points) {
        String sql = "SHOW GLOBAL STATUS WHERE Variable_name IN "
            + "('Questions', 'Com_commit', 'Com_rollback', 'Threads_connected', 'Threads_running', 'Slow_queries')";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String name = rs.getString(1);
                double value = parseDouble(rs.getString(2));
                switch (name) {
                    case "Questions" ->
                        points.add(metric(instanceId, MetricNameConstant.DB_QUESTIONS, value));
                    case "Com_commit" ->
                        points.add(metric(instanceId, MetricNameConstant.DB_XACT_COMMIT, value));
                    case "Com_rollback" ->
                        points.add(metric(instanceId, MetricNameConstant.DB_XACT_ROLLBACK, value));
                    case "Threads_connected" ->
                        points.add(metric(instanceId, MetricNameConstant.DB_THREADS_CONNECTED, value));
                    case "Threads_running" ->
                        points.add(metric(instanceId, MetricNameConstant.DB_THREADS_RUNNING, value));
                    case "Slow_queries" ->
                        points.add(metric(instanceId, MetricNameConstant.DB_SLOW_QUERIES, value));
                    default -> {
                        // 不在采集清单内，忽略
                    }
                }
            }
        } catch (Exception e) {
            log.warn("采集 MySQL GLOBAL STATUS 失败, instanceId:{}, error:{}", instanceId, e.getMessage());
        }
    }

    /**
     * information_schema.processlist：按 COMMAND 聚合会话数（tags={state:xxx}）
     */
    private void collectProcesslist(Long instanceId, Connection connection, List<MetricPoint> points) {
        String sql = "SELECT COALESCE(COMMAND, 'unknown') AS state, count(*) AS cnt "
            + "FROM information_schema.processlist GROUP BY COMMAND";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, String> tags = new HashMap<>();
                tags.put("state", rs.getString("state"));
                points.add(MetricPoint.of(instanceId, MetricSourceEnum.DB,
                    MetricNameConstant.DB_SESSIONS, "", tags, rs.getDouble("cnt")));
            }
        } catch (Exception e) {
            log.warn("采集 MySQL processlist 失败, instanceId:{}, error:{}", instanceId, e.getMessage());
        }
    }

    /**
     * performance_schema digest 汇总：慢 SQL（performance_schema 不可用降级空列表）
     */
    @Override
    public List<SlowSqlRow> collectSlowSql(Long instanceId, String dbName, Connection connection) {
        List<SlowSqlRow> rows = new ArrayList<>();
        String sql = "SELECT SCHEMA_NAME, DIGEST_TEXT, COUNT_STAR, AVG_TIMER_WAIT, MAX_TIMER_WAIT, LAST_SEEN "
            + "FROM performance_schema.events_statements_summary_by_digest "
            + "WHERE DIGEST_TEXT IS NOT NULL AND AVG_TIMER_WAIT >= ? "
            + "ORDER BY AVG_TIMER_WAIT DESC LIMIT ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setDouble(1, SLOW_SQL_THRESHOLD_MS * PICO_TO_MILLI);
            ps.setInt(2, SLOW_SQL_LIMIT);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String sampleSql = SqlFingerprintUtil.sampleSql(rs.getString("DIGEST_TEXT"), DbType.mysql);
                    String schema = rs.getString("SCHEMA_NAME");
                    rows.add(new SlowSqlRow(instanceId,
                        SqlFingerprintUtil.fingerprint(sampleSql, DbType.mysql),
                        sampleSql, schema == null ? dbName : schema, rs.getLong("COUNT_STAR"),
                        rs.getDouble("AVG_TIMER_WAIT") / PICO_TO_MILLI,
                        rs.getDouble("MAX_TIMER_WAIT") / PICO_TO_MILLI,
                        parseLastSeen(rs), Instant.now()));
                }
            }
        } catch (Exception e) {
            log.warn("采集 MySQL digest 慢 SQL 失败, instanceId:{}, error:{}", instanceId, e.getMessage());
        }
        return rows;
    }

    private Instant parseLastSeen(ResultSet rs) {
        try {
            Timestamp ts = rs.getTimestamp("LAST_SEEN");
            return ts == null ? Instant.now() : ts.toInstant();
        } catch (Exception e) {
            return Instant.now();
        }
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return 0D;
        }
    }

    private MetricPoint metric(Long instanceId, String name, double value) {
        return MetricPoint.of(instanceId, MetricSourceEnum.DB, name, "", value);
    }

    // ==================== 会话与锁 ====================

    @Override
    public List<DbSessionRow> listSessions(Long instanceId, Connection connection) {
        List<DbSessionRow> rows = new ArrayList<>();
        String sql = "SELECT ID, USER, COALESCE(DB, ''), COMMAND, LEFT(COALESCE(INFO, ''), 500) AS query, "
            + "TIME AS duration_sec, HOST FROM information_schema.processlist "
            + "WHERE ID <> CONNECTION_ID() ORDER BY TIME DESC LIMIT 500";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new DbSessionRow(String.valueOf(rs.getLong("ID")), rs.getString("USER"),
                    rs.getString("DB"), rs.getString("COMMAND"), rs.getString("query"),
                    rs.getDouble("duration_sec"), rs.getString("HOST"), null));
            }
        } catch (Exception e) {
            log.warn("查询 MySQL 会话失败, instanceId:{}, error:{}", instanceId, e.getMessage());
        }
        return rows;
    }

    @Override
    public boolean killSession(Long instanceId, Connection connection, String sessionId) {
        // sessionId 只允许数字（processlist id），杜绝注入
        if (sessionId == null || !sessionId.matches("\\d+")) {
            throw MonitorErrorCode.MONITOR_CONNECTION_UNAVAILABLE.exception("非法的会话ID：" + sessionId);
        }
        // KILL 不支持参数绑定；sessionId 已按纯数字校验，安全拼接
        try (PreparedStatement ps = connection.prepareStatement("KILL " + Long.parseLong(sessionId))) {
            ps.execute();
            return true;
        } catch (Exception e) {
            log.warn("终止 MySQL 会话失败, instanceId:{}, id:{}, error:{}", instanceId, sessionId, e.getMessage());
            throw MonitorErrorCode.MONITOR_CONNECTION_UNAVAILABLE
                .exception("终止会话失败：" + StrUtil.maxLength(e.getMessage(), 200));
        }
    }

    @Override
    public List<DbLockRow> listLocks(Long instanceId, Connection connection) {
        List<DbLockRow> rows = new ArrayList<>();
        String sql = "SELECT w.REQUESTING_ENGINE_TRANSACTION_ID AS req_trx, "
            + "w.BLOCKING_ENGINE_TRANSACTION_ID AS blk_trx, "
            + "rp.ID AS req_pid, bp.ID AS blk_pid, "
            + "LEFT(COALESCE(rp.INFO, ''), 300) AS req_query "
            + "FROM performance_schema.data_lock_waits w "
            + "LEFT JOIN information_schema.innodb_trx rt ON rt.trx_id = w.REQUESTING_ENGINE_TRANSACTION_ID "
            + "LEFT JOIN information_schema.innodb_trx bt ON bt.trx_id = w.BLOCKING_ENGINE_TRANSACTION_ID "
            + "LEFT JOIN information_schema.processlist rp ON rp.ID = rt.trx_mysql_thread_id "
            + "LEFT JOIN information_schema.processlist bp ON bp.ID = bt.trx_mysql_thread_id "
            + "LIMIT 500";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new DbLockRow("innodb_lock_wait", "",
                    rs.getString("req_trx"), rs.getString("req_pid"),
                    false, rs.getString("blk_pid"), rs.getString("req_query")));
            }
        } catch (Exception e) {
            log.warn("查询 MySQL 锁等待失败, instanceId:{}, error:{}", instanceId, e.getMessage());
        }
        return rows;
    }

    // ==================== 元数据 ====================

    @Override
    public List<String> listSchemas(Connection connection) {
        List<String> schemas = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
            "SELECT schema_name FROM information_schema.schemata ORDER BY schema_name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                schemas.add(rs.getString(1));
            }
        } catch (Exception e) {
            log.warn("查询 MySQL schema 列表失败, error:{}", e.getMessage());
        }
        return schemas;
    }

    @Override
    public List<DbTableRow> listTables(Connection connection, String schemaName) {
        List<DbTableRow> rows = new ArrayList<>();
        String sql = "SELECT table_schema, table_name, table_type, COALESCE(table_comment, '') AS table_comment, "
            + "COALESCE(table_rows, 0) AS est_rows FROM information_schema.tables "
            + "WHERE table_schema = ? ORDER BY table_name";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, schemaName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new DbTableRow(rs.getString("table_schema"), rs.getString("table_name"),
                        rs.getString("table_type"), rs.getString("table_comment"), rs.getLong("est_rows")));
                }
            }
        } catch (Exception e) {
            log.warn("查询 MySQL 表列表失败, schema:{}, error:{}", schemaName, e.getMessage());
        }
        return rows;
    }

    @Override
    public List<DbColumnRow> listColumns(Connection connection, String schemaName, String tableName) {
        List<DbColumnRow> rows = new ArrayList<>();
        String sql = "SELECT column_name, column_type, is_nullable, COALESCE(column_default, '') AS column_default, "
            + "COALESCE(column_comment, '') AS column_comment, column_key FROM information_schema.columns "
            + "WHERE table_schema = ? AND table_name = ? ORDER BY ordinal_position";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, schemaName);
            ps.setString(2, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new DbColumnRow(rs.getString("column_name"), rs.getString("column_type"),
                        "YES".equalsIgnoreCase(rs.getString("is_nullable")), rs.getString("column_default"),
                        rs.getString("column_comment"), "PRI".equalsIgnoreCase(rs.getString("column_key"))));
                }
            }
        } catch (Exception e) {
            log.warn("查询 MySQL 列列表失败, table:{}.{}, error:{}", schemaName, tableName, e.getMessage());
        }
        return rows;
    }

    @Override
    public String getTableDdl(Connection connection, String schemaName, String tableName) {
        String safeSchema = schemaName.replace("`", "");
        String safeTable = tableName.replace("`", "");
        try (PreparedStatement ps = connection.prepareStatement(
            "SHOW CREATE TABLE `" + safeSchema + "`.`" + safeTable + "`");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getString(2);
            }
        } catch (Exception e) {
            log.warn("查询 MySQL 建表 DDL 失败, table:{}.{}, error:{}", schemaName, tableName, e.getMessage());
        }
        return "";
    }

    // ==================== 容量 ====================

    @Override
    public List<CapacityRow> collectCapacity(Long instanceId, Connection connection) {
        List<CapacityRow> rows = new ArrayList<>();
        String sql = "SELECT table_schema, table_name, COALESCE(table_rows, 0) AS table_rows, "
            + "COALESCE(data_length, 0) AS table_size, COALESCE(index_length, 0) AS index_size "
            + "FROM information_schema.tables WHERE table_type = 'BASE TABLE' "
            + "AND table_schema NOT IN ('mysql', 'sys', 'information_schema', 'performance_schema')";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new CapacityRow(rs.getString("table_schema"), rs.getString("table_name"),
                    rs.getLong("table_rows"), rs.getLong("table_size"), rs.getLong("index_size")));
            }
        } catch (Exception e) {
            log.warn("采集 MySQL 容量失败, instanceId:{}, error:{}", instanceId, e.getMessage());
        }
        return rows;
    }
}
