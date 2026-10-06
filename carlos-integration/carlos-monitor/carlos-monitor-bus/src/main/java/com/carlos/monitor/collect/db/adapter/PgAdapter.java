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
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * PostgreSQL 适配器
 * 实例指标：pg_stat_database（xact/tup/deadlocks/numbackends）+ pg_stat_activity（按 state 聚合会话）；
 * 慢 SQL：pg_stat_statements（扩展不可用时返回空列表）
 * </p>
 *
 * @author Carlos
 * @date 2026年10月2日
 */
@Slf4j
@Component
public class PgAdapter implements DbTypeAdapter {

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
        return instanceType == InstanceTypeEnum.POSTGRESQL;
    }

    @Override
    public List<MetricPoint> collectInstanceMetrics(Long instanceId, Connection connection) {
        List<MetricPoint> points = new ArrayList<>();
        collectDatabaseStats(instanceId, connection, points);
        collectActivityStats(instanceId, connection, points);
        return points;
    }

    /**
     * pg_stat_database：当前库的 xact/tup/deadlocks/连接数（累计值）
     */
    private void collectDatabaseStats(Long instanceId, Connection connection, List<MetricPoint> points) {
        String sql = "SELECT numbackends, xact_commit, xact_rollback, tup_returned, tup_fetched, "
            + "tup_inserted, tup_updated, tup_deleted, deadlocks "
            + "FROM pg_stat_database WHERE datname = current_database()";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                points.add(metric(instanceId, MetricNameConstant.DB_CONNECTIONS_ACTIVE, rs.getDouble("numbackends")));
                points.add(metric(instanceId, MetricNameConstant.DB_XACT_COMMIT, rs.getDouble("xact_commit")));
                points.add(metric(instanceId, MetricNameConstant.DB_XACT_ROLLBACK, rs.getDouble("xact_rollback")));
                points.add(metric(instanceId, MetricNameConstant.DB_TUP_RETURNED, rs.getDouble("tup_returned")));
                points.add(metric(instanceId, MetricNameConstant.DB_TUP_FETCHED, rs.getDouble("tup_fetched")));
                points.add(metric(instanceId, MetricNameConstant.DB_TUP_INSERTED, rs.getDouble("tup_inserted")));
                points.add(metric(instanceId, MetricNameConstant.DB_TUP_UPDATED, rs.getDouble("tup_updated")));
                points.add(metric(instanceId, MetricNameConstant.DB_TUP_DELETED, rs.getDouble("tup_deleted")));
                points.add(metric(instanceId, MetricNameConstant.DB_DEADLOCKS, rs.getDouble("deadlocks")));
            }
        } catch (Exception e) {
            log.warn("采集 pg_stat_database 失败, instanceId:{}, error:{}", instanceId, e.getMessage());
        }
    }

    /**
     * pg_stat_activity：按 state 聚合会话数（tags={state:xxx}）
     */
    private void collectActivityStats(Long instanceId, Connection connection, List<MetricPoint> points) {
        String sql = "SELECT COALESCE(state, 'unknown') AS state, count(*) AS cnt "
            + "FROM pg_stat_activity WHERE backend_type = 'client backend' GROUP BY state";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, String> tags = new HashMap<>();
                tags.put("state", rs.getString("state"));
                points.add(MetricPoint.of(instanceId, MetricSourceEnum.DB,
                    MetricNameConstant.DB_SESSIONS, "", tags, rs.getDouble("cnt")));
            }
        } catch (Exception e) {
            log.warn("采集 pg_stat_activity 失败, instanceId:{}, error:{}", instanceId, e.getMessage());
        }
    }

    /**
     * pg_stat_statements：按指纹聚合的慢 SQL（扩展不可用降级空列表）
     */
    @Override
    public List<SlowSqlRow> collectSlowSql(Long instanceId, String dbName, Connection connection) {
        if (!pgStatStatementsAvailable(connection)) {
            log.debug("pg_stat_statements 不可用，跳过慢 SQL 采集, instanceId:{}", instanceId);
            return List.of();
        }
        List<SlowSqlRow> rows = new ArrayList<>();
        String sql = "SELECT query, calls, mean_exec_time, max_exec_time "
            + "FROM pg_stat_statements WHERE mean_exec_time >= ? "
            + "ORDER BY mean_exec_time DESC LIMIT ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setDouble(1, SLOW_SQL_THRESHOLD_MS);
            ps.setInt(2, SLOW_SQL_LIMIT);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String sampleSql = SqlFingerprintUtil.sampleSql(rs.getString("query"), DbType.postgresql);
                    rows.add(new SlowSqlRow(instanceId,
                        SqlFingerprintUtil.fingerprint(sampleSql, DbType.postgresql),
                        sampleSql, dbName, rs.getLong("calls"),
                        rs.getDouble("mean_exec_time"), rs.getDouble("max_exec_time"),
                        Instant.now(), Instant.now()));
                }
            }
        } catch (Exception e) {
            log.warn("采集 pg_stat_statements 失败, instanceId:{}, error:{}", instanceId, e.getMessage());
        }
        return rows;
    }

    private boolean pgStatStatementsAvailable(Connection connection) {
        try (PreparedStatement ps = connection.prepareStatement(
            "SELECT count(*) FROM pg_extension WHERE extname = 'pg_stat_statements'");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() && rs.getLong(1) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private MetricPoint metric(Long instanceId, String name, double value) {
        return MetricPoint.of(instanceId, MetricSourceEnum.DB, name, "", value);
    }

    // ==================== 会话与锁 ====================

    @Override
    public List<DbSessionRow> listSessions(Long instanceId, Connection connection) {
        List<DbSessionRow> rows = new ArrayList<>();
        String sql = "SELECT pid, usename, datname, COALESCE(state, '') AS state, LEFT(COALESCE(query, ''), 500) AS query, "
            + "EXTRACT(EPOCH FROM (now() - query_start)) AS duration_sec, "
            + "COALESCE(client_addr::text, '') AS client_addr, COALESCE(backend_start::text, '') AS backend_start "
            + "FROM pg_stat_activity WHERE backend_type = 'client backend' AND pid <> pg_backend_pid() "
            + "ORDER BY query_start NULLS LAST LIMIT 500";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new DbSessionRow(String.valueOf(rs.getLong("pid")), rs.getString("usename"),
                    rs.getString("datname"), rs.getString("state"), rs.getString("query"),
                    rs.getDouble("duration_sec"), rs.getString("client_addr"), rs.getString("backend_start")));
            }
        } catch (Exception e) {
            log.warn("查询 PG 会话失败, instanceId:{}, error:{}", instanceId, e.getMessage());
        }
        return rows;
    }

    @Override
    public boolean killSession(Long instanceId, Connection connection, String sessionId) {
        // sessionId 只允许数字（pid），杜绝注入
        if (sessionId == null || !sessionId.chars().allMatch(Character::isDigit) || sessionId.isEmpty()) {
            throw MonitorErrorCode.MONITOR_CONNECTION_UNAVAILABLE.exception("非法的会话ID：" + sessionId);
        }
        try (PreparedStatement ps = connection.prepareStatement("SELECT pg_terminate_backend(?)")) {
            ps.setLong(1, Long.parseLong(sessionId));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }
        } catch (Exception e) {
            log.warn("终止 PG 会话失败, instanceId:{}, pid:{}, error:{}", instanceId, sessionId, e.getMessage());
            throw MonitorErrorCode.MONITOR_CONNECTION_UNAVAILABLE
                .exception("终止会话失败：" + StrUtil.maxLength(e.getMessage(), 200));
        }
    }

    @Override
    public List<DbLockRow> listLocks(Long instanceId, Connection connection) {
        List<DbLockRow> rows = new ArrayList<>();
        String sql = "SELECT l.locktype, l.mode, COALESCE(l.relation::regclass::text, '') AS relation, "
            + "l.pid, l.granted, COALESCE(pg_blocking_pids(l.pid)::text, '') AS blocking, "
            + "LEFT(COALESCE(a.query, ''), 300) AS query "
            + "FROM pg_locks l LEFT JOIN pg_stat_activity a ON a.pid = l.pid "
            + "WHERE NOT l.granted OR pg_blocking_pids(l.pid) <> '{}' "
            + "ORDER BY l.granted LIMIT 500";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new DbLockRow(rs.getString("locktype"), rs.getString("mode"),
                    rs.getString("relation"), String.valueOf(rs.getLong("pid")),
                    rs.getBoolean("granted"), rs.getString("blocking"), rs.getString("query")));
            }
        } catch (Exception e) {
            log.warn("查询 PG 锁失败, instanceId:{}, error:{}", instanceId, e.getMessage());
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
            log.warn("查询 PG schema 列表失败, error:{}", e.getMessage());
        }
        return schemas;
    }

    @Override
    public List<DbTableRow> listTables(Connection connection, String schemaName) {
        List<DbTableRow> rows = new ArrayList<>();
        String sql = "SELECT t.table_schema, t.table_name, t.table_type, "
            + "COALESCE(obj_description(c.oid), '') AS comment, COALESCE(c.reltuples, 0)::bigint AS est_rows "
            + "FROM information_schema.tables t "
            + "LEFT JOIN pg_class c ON c.relname = t.table_name "
            + "  AND c.relnamespace = (SELECT oid FROM pg_namespace WHERE nspname = t.table_schema) "
            + "WHERE t.table_schema = ? ORDER BY t.table_name";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, schemaName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new DbTableRow(rs.getString("table_schema"), rs.getString("table_name"),
                        rs.getString("table_type"), rs.getString("comment"), rs.getLong("est_rows")));
                }
            }
        } catch (Exception e) {
            log.warn("查询 PG 表列表失败, schema:{}, error:{}", schemaName, e.getMessage());
        }
        return rows;
    }

    @Override
    public List<DbColumnRow> listColumns(Connection connection, String schemaName, String tableName) {
        List<DbColumnRow> rows = new ArrayList<>();
        String sql = "SELECT col.column_name, col.data_type, col.is_nullable, COALESCE(col.column_default, '') AS column_default, "
            + "COALESCE(col_description(c.oid, col.ordinal_position), '') AS comment, "
            + "EXISTS (SELECT 1 FROM pg_index i WHERE i.indrelid = c.oid AND i.indisprimary "
            + "  AND col.ordinal_position = ANY (i.indkey::int[])) AS is_pk "
            + "FROM information_schema.columns col "
            + "LEFT JOIN pg_class c ON c.relname = col.table_name "
            + "  AND c.relnamespace = (SELECT oid FROM pg_namespace WHERE nspname = col.table_schema) "
            + "WHERE col.table_schema = ? AND col.table_name = ? ORDER BY col.ordinal_position";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, schemaName);
            ps.setString(2, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new DbColumnRow(rs.getString("column_name"), rs.getString("data_type"),
                        "YES".equalsIgnoreCase(rs.getString("is_nullable")), rs.getString("column_default"),
                        rs.getString("comment"), rs.getBoolean("is_pk")));
                }
            }
        } catch (Exception e) {
            log.warn("查询 PG 列列表失败, table:{}.{}, error:{}", schemaName, tableName, e.getMessage());
        }
        return rows;
    }

    @Override
    public String getTableDdl(Connection connection, String schemaName, String tableName) {
        // PG 无内置 SHOW CREATE，按 information_schema 近似重建（不含索引/约束细节）
        List<DbColumnRow> columns = listColumns(connection, schemaName, tableName);
        if (columns.isEmpty()) {
            return "";
        }
        StringBuilder ddl = new StringBuilder("CREATE TABLE \"")
            .append(schemaName).append("\".\"").append(tableName).append("\" (\n");
        List<String> pkColumns = new ArrayList<>();
        for (int i = 0; i < columns.size(); i++) {
            DbColumnRow column = columns.get(i);
            ddl.append("  \"").append(column.columnName()).append("\" ").append(column.dataType());
            if (StrUtil.isNotBlank(column.defaultValue())) {
                ddl.append(" DEFAULT ").append(column.defaultValue());
            }
            if (Boolean.FALSE.equals(column.nullable())) {
                ddl.append(" NOT NULL");
            }
            if (Boolean.TRUE.equals(column.primaryKey())) {
                pkColumns.add("\"" + column.columnName() + "\"");
            }
            ddl.append(i < columns.size() - 1 || !pkColumns.isEmpty() ? ",\n" : "\n");
        }
        if (!pkColumns.isEmpty()) {
            ddl.append("  PRIMARY KEY (").append(String.join(", ", pkColumns)).append(")\n");
        }
        return ddl.append(");").toString();
    }

    // ==================== 容量 ====================

    @Override
    public List<CapacityRow> collectCapacity(Long instanceId, Connection connection) {
        List<CapacityRow> rows = new ArrayList<>();
        String sql = "SELECT n.nspname AS schema_name, c.relname AS table_name, "
            + "COALESCE(c.reltuples, 0)::bigint AS table_rows, "
            + "pg_relation_size(c.oid) AS table_size, pg_indexes_size(c.oid) AS index_size "
            + "FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace "
            + "WHERE c.relkind = 'r' AND n.nspname NOT IN ('pg_catalog', 'information_schema', 'pg_toast')";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new CapacityRow(rs.getString("schema_name"), rs.getString("table_name"),
                    rs.getLong("table_rows"), rs.getLong("table_size"), rs.getLong("index_size")));
            }
        } catch (Exception e) {
            log.warn("采集 PG 容量失败, instanceId:{}, error:{}", instanceId, e.getMessage());
        }
        return rows;
    }
}
