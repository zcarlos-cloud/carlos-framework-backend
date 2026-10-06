package com.carlos.fx.utils;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.db.Db;
import cn.hutool.db.meta.MetaUtil;
import com.carlos.fx.codege.config.DatabaseInfo;
import com.carlos.fx.codege.entity.TableBaseInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 *   数据库工具
 * </p>
 *
 * @author Carlos
 * @date 2026-02-14 21:05
 */
@Slf4j
public class DatabaseUtil {

    /**
     * 测试数据库连接 - 基础版本
     *
     * @return 连接测试结果
     */
    public static ConnectionTestResult testConnection(DataSource dataSource) {
        Connection connection = null;
        long startTime = System.currentTimeMillis();
        Db db = null;
        try {
            db = Db.use(dataSource);
            // 尝试建立连接
            connection = db.getConnection();

            long responseTime = System.currentTimeMillis() - startTime;
            // 获取数据库元数据
            var metaData = connection.getMetaData();
            return ConnectionTestResult.success(
                metaData.getDatabaseProductName(),
                metaData.getDatabaseProductVersion(),
                responseTime
            );

        } catch (Throwable e) {
            log.error("连接测试失败: {}", e.getMessage(), e);
            return ConnectionTestResult.failure("连接失败: " + e.getMessage());
        } finally {
            // 确保连接关闭
            if (connection != null) {
                db.closeConnection(connection);
            }
        }
    }


    /**
     * 连接测试结果记录类
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ConnectionTestResult {
        private boolean success;
        private String databaseName;
        private String databaseVersion;
        private long responseTimeMs;
        private String errorMessage;

        public static ConnectionTestResult success(String name, String version, long time) {
            return new ConnectionTestResult(true, name, version, time, null);
        }

        public static ConnectionTestResult failure(String error) {
            return new ConnectionTestResult(false, null, null, -1, error);
        }

        @Override
        public String toString() {
            if (success) {
                return String.format("✅ 连接成功 [%s %s] 响应时间: %dms",
                    databaseName, databaseVersion, responseTimeMs);
            } else {
                return "❌ 连接失败: " + errorMessage;
            }
        }
    }

    /**
     * 当前数据库中具有哪些数据库(当前操作连接数据库后将数据库进行了关闭)
     *
     * @return List<String> 返回数据库中的表名列表
     * @author Carlos
     * @date 2019/4/14 0:05
     */
    public static List<String> getSchemas(DatabaseInfo databaseInfo) throws SQLException {
        Connection connection = databaseInfo.getConnection();
        DatabaseMetaData metaData = MetaUtil.getMetaData(connection);
        // 得到数据库列表信息
        ResultSet rs = metaData.getCatalogs();
        List<String> list = new ArrayList<>();
        String[] sysTable = databaseInfo.getDbType().getSysTable();
        while (rs.next()) {
            String databaseName = rs.getString(1);
            if (ArrayUtil.isNotEmpty(sysTable) && ArrayUtil.contains(sysTable, databaseName)) {
                continue;
            }
            list.add(databaseName);
        }
        release(connection, rs);
        return list;
    }

    /**
     * 获取库中的所有表信息
     *
     * @return List<Table> 返回数据库中所有的表信息
     * @author Carlos
     * @date 2019/4/12 17:01
     */
    public static List<TableBaseInfo> getTables(DatabaseInfo databaseInfo) throws SQLException {
        // 连接数据库
        Connection connection = databaseInfo.getConnection();
        // 检索DatabaseMetaData对象包含有关哪个这个数据库的元数据Connection对象表示的连接。 元数据包括有关数据库表，其支持的SQL语法，其存储过程，此连接的功能等的信息。
        // 关于整个数据库的综合信息。
        DatabaseMetaData metaData = connection.getMetaData();
        String schema = connection.getSchema();
        String catalog = connection.getCatalog();
        // 针对Oracle的特殊处理
        if ((databaseInfo.getDbType() != null) && (databaseInfo.getDbType().getDescribe().toUpperCase().contains("ORACLE"))) {
            schema = databaseInfo.getUser().toUpperCase();
            catalog = connection.getCatalog();
        }
        ResultSet tables = metaData.getTables(catalog, schema, "%", new String[]{"TABLE"});

        List<TableBaseInfo> list = new ArrayList<>();
        TableBaseInfo table;
        // 表名
        String tableName;
        String tableRemark;
        while (tables.next()) {
            table = new TableBaseInfo();
            tableName = tables.getString("TABLE_NAME");
            tableRemark = tables.getString("REMARKS");

            table.setComment(tableRemark);
            if (tableName.contains("=") || tableName.contains("$")) {
                continue;
            }
            if (tableName.toUpperCase().equals(tableName)) {
                table.setName(tableName.toLowerCase());
            } else {
                table.setName(tableName);
            }
            list.add(table);
        }
        release(connection, tables);
        return list;
    }


    /**
     * @param conn 连接对象
     * @author Carlos
     * @date 2019/12/25 17:32
     */

    public static void release(Connection conn, Statement st) {
        closeSt(st);
        closeConn(conn);
    }

    public static void release(Connection conn, ResultSet rs) {
        closeRs(rs);
        closeConn(conn);
    }

    private static void closeRs(ResultSet rs) {
        try {
            if (rs != null) {
                rs.close();
            }
        } catch (SQLException e) {
            log.error("关闭ResultSet失败", e);
        }
    }

    private static void closeSt(Statement st) {
        try {
            if (st != null) {
                st.close();
            }
        } catch (SQLException e) {
            log.error("关闭Statement失败", e);
        }
    }

    private static void closeConn(Connection conn) {
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException e) {
            log.error("关闭Connection失败", e);
        }
    }
}
