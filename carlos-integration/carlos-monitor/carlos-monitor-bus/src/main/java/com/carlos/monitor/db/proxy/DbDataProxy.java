package com.carlos.monitor.db.proxy;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLLimit;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLIntegerExpr;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.carlos.core.exception.BusinessException;
import com.carlos.monitor.collect.db.adapter.DbTypeAdapter;
import com.carlos.monitor.config.MonitorProperties;
import com.carlos.monitor.db.pojo.dto.DbColumnRow;
import com.carlos.monitor.db.pojo.dto.DbLockRow;
import com.carlos.monitor.db.pojo.dto.DbSessionRow;
import com.carlos.monitor.db.pojo.dto.DbTableRow;
import com.carlos.monitor.db.pojo.dto.SqlExecResult;
import com.carlos.monitor.enums.InstanceTypeEnum;
import com.carlos.monitor.enums.MonitorErrorCode;
import com.carlos.monitor.instance.connection.DbManagedConnection;
import com.carlos.monitor.instance.connection.InstanceConnectionRegistry;
import com.carlos.monitor.instance.connection.ManagedConnection;
import com.carlos.monitor.instance.manager.MonitorInstanceManager;
import com.carlos.monitor.instance.pojo.dto.MonitorInstanceDTO;
import com.carlos.monitor.mask.DataMasker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * 数据库数据代理
 * 元数据查询（DbTypeAdapter 方言）、表数据分页浏览（强制 LIMIT/OFFSET、
 * 排序/条件列名白名单校验防列名注入）、受控执行（statement_timeout 可配、
 * SELECT 自动包裹 LIMIT、max-limit 硬上限内读取）
 * </p>
 *
 * @author Carlos
 * @date 2026年10月4日
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DbDataProxy {

    /**
     * 表数据浏览默认页大小
     */
    private static final int TABLE_DATA_DEFAULT_SIZE = 100;

    /**
     * 表数据浏览页大小上限
     */
    private static final int TABLE_DATA_MAX_SIZE = 1000;

    /**
     * MySQL/ClickHouse 系统库（未配置 dbName 时兜底过滤）
     */
    private static final Set<String> SYSTEM_DBS = Set.of(
        "information_schema", "mysql", "performance_schema", "sys");

    /**
     * DM 系统 schema（未配置 dbName 时兜底过滤）
     */
    private static final Set<String> DM_SYSTEM_SCHEMAS = Set.of(
        "sys", "sysdba", "sysobject", "sysauditor", "syssso", "ctisys");

    private final InstanceConnectionRegistry connectionRegistry;

    private final MonitorInstanceManager instanceManager;

    private final List<DbTypeAdapter> dbTypeAdapters;

    private final DataMasker dataMasker;

    private final MonitorProperties monitorProperties;

    /**
     * 表数据查询条件（列等值或 LIKE）
     */
    public record TableCondition(String column, String op, String value) {
    }

    // ==================== 元数据 ====================

    /**
     * schema 列表（仅实例配置范围内：MySQL/ClickHouse/DM 只返回注册库 dbName；
     * PostgreSQL 剔除系统 schema 保留业务 schema）
     */
    public List<String> listSchemas(Long instanceId) {
        MonitorInstanceDTO instance = checkManageEnabled(instanceId);
        try (Connection connection = dbConnection(instanceId).getDataSource().getConnection()) {
            List<String> schemas = adapterOf(instance.getInstanceType()).listSchemas(connection);
            return filterConfiguredSchemas(instance, schemas);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw connectionError("查询 schema 列表失败", e);
        }
    }

    /**
     * 按实例注册配置收敛 schema 列表，避免把实例上所有库/系统 schema 暴露给数据浏览
     */
    private List<String> filterConfiguredSchemas(MonitorInstanceDTO instance, List<String> schemas) {
        if (CollUtil.isEmpty(schemas)) {
            return schemas;
        }
        InstanceTypeEnum type = instance.getInstanceType();
        String dbName = instance.getDbName();
        if (type == InstanceTypeEnum.MYSQL || type == InstanceTypeEnum.CLICKHOUSE
            || type == InstanceTypeEnum.DM) {
            if (StrUtil.isNotBlank(dbName)) {
                return List.of(dbName);
            }
            return schemas.stream().filter(schema -> !isSystemSchema(type, schema)).toList();
        }
        if (type == InstanceTypeEnum.POSTGRESQL) {
            return schemas.stream().filter(schema -> !isSystemSchema(type, schema)).toList();
        }
        return schemas;
    }

    private boolean isSystemSchema(InstanceTypeEnum type, String schema) {
        String name = schema.toLowerCase();
        if (type == InstanceTypeEnum.POSTGRESQL) {
            return name.startsWith("pg_") || "information_schema".equals(name);
        }
        if (type == InstanceTypeEnum.MYSQL || type == InstanceTypeEnum.CLICKHOUSE) {
            return SYSTEM_DBS.contains(name);
        }
        if (type == InstanceTypeEnum.DM) {
            return DM_SYSTEM_SCHEMAS.contains(name);
        }
        return false;
    }

    /**
     * 表列表
     */
    public List<DbTableRow> listTables(Long instanceId, String schemaName) {
        MonitorInstanceDTO instance = checkManageEnabled(instanceId);
        try (Connection connection = dbConnection(instanceId).getDataSource().getConnection()) {
            return adapterOf(instance.getInstanceType()).listTables(connection, schemaName);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw connectionError("查询表列表失败", e);
        }
    }

    /**
     * 列列表
     */
    public List<DbColumnRow> listColumns(Long instanceId, String schemaName, String tableName) {
        MonitorInstanceDTO instance = checkManageEnabled(instanceId);
        try (Connection connection = dbConnection(instanceId).getDataSource().getConnection()) {
            return adapterOf(instance.getInstanceType()).listColumns(connection, schemaName, tableName);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw connectionError("查询列列表失败", e);
        }
    }

    /**
     * 建表 DDL 查看
     */
    public String getTableDdl(Long instanceId, String schemaName, String tableName) {
        MonitorInstanceDTO instance = checkManageEnabled(instanceId);
        try (Connection connection = dbConnection(instanceId).getDataSource().getConnection()) {
            return adapterOf(instance.getInstanceType()).getTableDdl(connection, schemaName, tableName);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw connectionError("查询建表 DDL 失败", e);
        }
    }

    // ==================== 表数据浏览 ====================

    /**
     * 表数据分页浏览（强制分页；条件仅支持列等值与 LIKE，列名必须在元数据中存在）
     *
     * @param orderColumn 排序列（必须在元数据中存在，可空）
     * @param orderDesc   是否倒序
     */
    public SqlExecResult tableData(Long instanceId, String schemaName, String tableName,
                                   List<TableCondition> conditions, String orderColumn, boolean orderDesc,
                                   int page, int size) {
        MonitorInstanceDTO instance = checkManageEnabled(instanceId);
        DbTypeAdapter adapter = adapterOf(instance.getInstanceType());
        InstanceTypeEnum type = instance.getInstanceType();
        int effectiveSize = size <= 0 ? TABLE_DATA_DEFAULT_SIZE : Math.min(size, TABLE_DATA_MAX_SIZE);
        int offset = Math.max(0, page - 1) * effectiveSize;
        try (Connection connection = dbConnection(instanceId).getDataSource().getConnection()) {
            // 列名白名单：排序/条件列必须在元数据中存在（防列名注入）
            List<DbColumnRow> columns = adapter.listColumns(connection, schemaName, tableName);
            if (columns.isEmpty()) {
                throw MonitorErrorCode.MONITOR_INSTANCE_NOT_FOUND
                    .exception("表不存在或无权限：" + schemaName + "." + tableName);
            }
            Set<String> validColumns = new HashSet<>();
            columns.forEach(column -> validColumns.add(column.columnName().toLowerCase(Locale.ROOT)));

            StringBuilder sql = new StringBuilder("SELECT * FROM ")
                .append(quoteIdent(type, schemaName)).append('.').append(quoteIdent(type, tableName));
            List<Object> params = new ArrayList<>();
            if (CollUtil.isNotEmpty(conditions)) {
                List<String> whereParts = new ArrayList<>();
                for (TableCondition condition : conditions) {
                    if (!validColumns.contains(condition.column().toLowerCase(Locale.ROOT))) {
                        throw MonitorErrorCode.MONITOR_SQL_BLOCKED
                            .exception("条件列不在表结构中：" + condition.column());
                    }
                    String quoted = quoteIdent(type, condition.column());
                    if ("like".equalsIgnoreCase(condition.op())) {
                        whereParts.add(quoted + " LIKE ?");
                        params.add("%" + condition.value() + "%");
                    } else {
                        whereParts.add(quoted + " = ?");
                        params.add(condition.value());
                    }
                }
                sql.append(" WHERE ").append(String.join(" AND ", whereParts));
            }
            if (StrUtil.isNotBlank(orderColumn)) {
                if (!validColumns.contains(orderColumn.toLowerCase(Locale.ROOT))) {
                    throw MonitorErrorCode.MONITOR_SQL_BLOCKED.exception("排序列不在表结构中：" + orderColumn);
                }
                sql.append(" ORDER BY ").append(quoteIdent(type, orderColumn))
                    .append(orderDesc ? " DESC" : " ASC");
            }
            sql.append(" LIMIT ").append(effectiveSize).append(" OFFSET ").append(offset);
            return executeQuery(connection, sql.toString(), params, instance, schemaName, tableName);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw connectionError("查询表数据失败", e);
        }
    }

    // ==================== 受控执行 ====================

    /**
     * 只读执行（SELECT/EXPLAIN，SELECT 自动包裹 LIMIT）
     */
    public SqlExecResult executeReadonly(Long instanceId, String sql, SqlGuard.SqlType sqlType) {
        MonitorInstanceDTO instance = checkManageEnabled(instanceId);
        String effectiveSql = sqlType == SqlGuard.SqlType.SELECT
            ? wrapSelectLimit(sql, instance.getInstanceType()) : sql;
        try (Connection connection = dbConnection(instanceId).getDataSource().getConnection();
             Statement statement = connection.createStatement()) {
            applyStatementTimeout(statement);
            try (ResultSet rs = statement.executeQuery(effectiveSql)) {
                return readRows(rs, instance, null, null);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw connectionError("SQL 执行失败", e);
        }
    }

    /**
     * 写执行（UPDATE/DELETE）：setAutoCommit=false，影响行数超阈值回滚并抛 21305
     *
     * @return 影响行数
     */
    public int executeWrite(Long instanceId, String sql) {
        checkManageEnabled(instanceId);
        Connection connection = null;
        try {
            connection = dbConnection(instanceId).getDataSource().getConnection();
            connection.setAutoCommit(false);
            int affected;
            try (Statement statement = connection.createStatement()) {
                applyStatementTimeout(statement);
                affected = statement.executeUpdate(sql);
            }
            int maxAffected = monitorProperties.getSqlConsole().getWriteMaxAffectedRows();
            if (affected > maxAffected) {
                connection.rollback();
                throw MonitorErrorCode.MONITOR_SQL_AFFECTED_ROWS_EXCEEDED
                    .exception("影响行数 " + affected + " 超出保护阈值 " + maxAffected + "，已回滚");
            }
            connection.commit();
            return affected;
        } catch (BusinessException e) {
            rollbackQuietly(connection, instanceId);
            throw e;
        } catch (Exception e) {
            rollbackQuietly(connection, instanceId);
            throw connectionError("写 SQL 执行失败（已回滚）", e);
        } finally {
            closeQuietly(connection);
        }
    }

    // ==================== 内部工具 ====================

    /**
     * SELECT 自动包裹 LIMIT：未写 limit 追加 default-limit；写了超过 max-limit 截断
     * （LIMIT 子句处理走 Druid AST，不涉及语句类型判断）
     */
    private String wrapSelectLimit(String sql, InstanceTypeEnum type) {
        int defaultLimit = monitorProperties.getSqlConsole().getDefaultLimit();
        int maxLimit = monitorProperties.getSqlConsole().getMaxLimit();
        String trimmed = sql.trim().replaceAll(";+$", "");
        Integer existingLimit = readLimit(trimmed, type);
        if (existingLimit == null) {
            return trimmed + " LIMIT " + defaultLimit;
        }
        if (existingLimit > maxLimit) {
            return rewriteLimit(trimmed, type, maxLimit);
        }
        return trimmed;
    }

    private Integer readLimit(String sql, InstanceTypeEnum type) {
        try {
            List<SQLStatement> statements = SQLUtils.parseStatements(sql, toDruidDbType(type));
            if (statements.size() == 1
                && statements.getFirst() instanceof SQLSelectStatement selectStatement
                && selectStatement.getSelect().getQueryBlock() != null
                && selectStatement.getSelect().getQueryBlock().getLimit() != null
                && selectStatement.getSelect().getQueryBlock().getLimit().getRowCount()
                    instanceof SQLIntegerExpr rowCount) {
                return rowCount.getNumber().intValue();
            }
        } catch (Exception e) {
            log.debug("读取 LIMIT 失败（按未包裹处理）: {}", e.getMessage());
        }
        return null;
    }

    private String rewriteLimit(String sql, InstanceTypeEnum type, int maxLimit) {
        try {
            List<SQLStatement> statements = SQLUtils.parseStatements(sql, toDruidDbType(type));
            SQLStatement statement = statements.getFirst();
            if (statement instanceof SQLSelectStatement selectStatement
                && selectStatement.getSelect().getQueryBlock() != null) {
                selectStatement.getSelect().getQueryBlock().setLimit(new SQLLimit(maxLimit));
                return SQLUtils.toSQLString(statement, toDruidDbType(type));
            }
        } catch (Exception e) {
            log.warn("重写 LIMIT 失败，按原 SQL 执行, error:{}", e.getMessage());
        }
        return sql;
    }

    private SqlExecResult executeQuery(Connection connection, String sql, List<Object> params,
                                       MonitorInstanceDTO instance,
                                       String schemaName, String tableName) throws Exception {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            applyStatementTimeout(ps);
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                return readRows(rs, instance, schemaName, tableName);
            }
        }
    }

    /**
     * 行读取 + 列级脱敏（max-limit 硬上限，超出截断并标记 truncated）
     */
    private SqlExecResult readRows(ResultSet rs, MonitorInstanceDTO instance,
                                   String schemaName, String tableName) throws Exception {
        int maxLimit = monitorProperties.getSqlConsole().getMaxLimit();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        List<String> columns = new ArrayList<>(columnCount);
        for (int i = 1; i <= columnCount; i++) {
            columns.add(metaData.getColumnLabel(i));
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        boolean truncated = false;
        while (rs.next()) {
            if (rows.size() >= maxLimit) {
                truncated = true;
                break;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                Object value = rs.getObject(i);
                String text = value == null ? null : String.valueOf(value);
                row.put(columns.get(i - 1), maskColumn(instance, schemaName, tableName, columns.get(i - 1), text));
            }
            rows.add(row);
        }
        return new SqlExecResult(columns, rows, null, truncated);
    }

    /**
     * 列级脱敏（按 DB 规则匹配 schema/table/column）
     */
    private String maskColumn(MonitorInstanceDTO instance, String schemaName, String tableName,
                              String column, String value) {
        if (value == null) {
            return null;
        }
        return dataMasker.maskDbValue(instance.getId(), schemaName, tableName, column, value);
    }

    private void applyStatementTimeout(Statement statement) throws Exception {
        statement.setQueryTimeout(monitorProperties.getSqlConsole().getStatementTimeoutSec());
    }

    private MonitorInstanceDTO checkManageEnabled(Long instanceId) {
        MonitorInstanceDTO instance = instanceManager.getDtoById(instanceId);
        if (instance == null) {
            throw MonitorErrorCode.MONITOR_INSTANCE_NOT_FOUND.exception();
        }
        if (instance.getInstanceType() == InstanceTypeEnum.MICROSERVICE) {
            throw MonitorErrorCode.MONITOR_INSTANCE_TYPE_UNSUPPORTED
                .exception("微服务实例不支持数据库数据管理");
        }
        if (Boolean.FALSE.equals(instance.getManageEnabled())) {
            throw MonitorErrorCode.MONITOR_MANAGE_DISABLED.exception();
        }
        return instance;
    }

    private DbManagedConnection dbConnection(Long instanceId) {
        ManagedConnection connection = connectionRegistry.getConnection(instanceId);
        if (!(connection instanceof DbManagedConnection dbConnection)) {
            throw MonitorErrorCode.MONITOR_CONNECTION_UNAVAILABLE.exception("目标实例不是数据库类型或连接不可用");
        }
        return dbConnection;
    }

    private DbTypeAdapter adapterOf(InstanceTypeEnum type) {
        for (DbTypeAdapter adapter : dbTypeAdapters) {
            if (adapter.support(type)) {
                return adapter;
            }
        }
        throw MonitorErrorCode.MONITOR_INSTANCE_TYPE_UNSUPPORTED.exception("不支持的实例类型：" + type);
    }

    /**
     * 标识符引用（防注入：双引号 PG/CK，反引号 MySQL；内部同名字符转义）
     */
    private String quoteIdent(InstanceTypeEnum type, String ident) {
        if (type == InstanceTypeEnum.MYSQL) {
            return "`" + ident.replace("`", "``") + "`";
        }
        return "\"" + ident.replace("\"", "\"\"") + "\"";
    }

    private DbType toDruidDbType(InstanceTypeEnum type) {
        return switch (type) {
            case POSTGRESQL -> DbType.postgresql;
            case MYSQL -> DbType.mysql;
            case DM -> DbType.dm;
            case CLICKHOUSE -> DbType.clickhouse;
            default -> null;
        };
    }

    private void rollbackQuietly(Connection connection, Long instanceId) {
        if (connection != null) {
            try {
                connection.rollback();
            } catch (Exception e) {
                log.warn("写执行回滚失败, instanceId:{}, error:{}", instanceId, e.getMessage());
            }
        }
    }

    private void closeQuietly(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (Exception e) {
                log.debug("关闭连接失败: {}", e.getMessage());
            }
        }
    }

    private BusinessException connectionError(String action, Exception e) {
        return new BusinessException(MonitorErrorCode.MONITOR_CONNECTION_UNAVAILABLE,
            action + "：" + StrUtil.maxLength(e.getMessage(), 200));
    }

    // ==================== 会话与锁 ====================

    /**
     * 会话列表
     */
    public List<DbSessionRow> listSessions(Long instanceId) {
        MonitorInstanceDTO instance = checkManageEnabled(instanceId);
        try (Connection connection = dbConnection(instanceId).getDataSource().getConnection()) {
            return adapterOf(instance.getInstanceType()).listSessions(instanceId, connection);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw connectionError("查询会话列表失败", e);
        }
    }

    /**
     * 终止会话（高危；调用方负责 confirm-token 与审计）
     */
    public boolean killSession(Long instanceId, String sessionId) {
        MonitorInstanceDTO instance = checkManageEnabled(instanceId);
        try (Connection connection = dbConnection(instanceId).getDataSource().getConnection()) {
            return adapterOf(instance.getInstanceType()).killSession(instanceId, connection, sessionId);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw connectionError("终止会话失败", e);
        }
    }

    /**
     * 锁等待/阻塞链
     */
    public List<DbLockRow> listLocks(Long instanceId) {
        MonitorInstanceDTO instance = checkManageEnabled(instanceId);
        try (Connection connection = dbConnection(instanceId).getDataSource().getConnection()) {
            return adapterOf(instance.getInstanceType()).listLocks(instanceId, connection);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw connectionError("查询锁等待失败", e);
        }
    }
}
