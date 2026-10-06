package com.carlos.monitor.collect.db.adapter;

import com.carlos.monitor.collect.spi.MetricPoint;
import com.carlos.monitor.collect.spi.SlowSqlRow;
import com.carlos.monitor.db.pojo.dto.CapacityRow;
import com.carlos.monitor.db.pojo.dto.DbColumnRow;
import com.carlos.monitor.db.pojo.dto.DbLockRow;
import com.carlos.monitor.db.pojo.dto.DbSessionRow;
import com.carlos.monitor.db.pojo.dto.DbTableRow;
import com.carlos.monitor.enums.InstanceTypeEnum;

import java.sql.Connection;
import java.util.List;

/**
 * <p>
 * 数据库类型适配器（按库型从系统视图采集实例指标、慢 SQL、会话/锁、元数据与容量）
 * 默认实现返回空/不支持，由各库方言覆盖
 * </p>
 *
 * @author Carlos
 * @date 2026年10月2日
 */
public interface DbTypeAdapter {

    /**
     * 是否支持该实例类型
     *
     * @param instanceType 实例类型
     * @return true=支持
     */
    boolean support(InstanceTypeEnum instanceType);

    /**
     * 采集实例级指标（QPS/TPS 计数器、连接数、慢查询数等，原始累计值）
     *
     * @param instanceId 实例ID
     * @param connection JDBC 连接
     * @return 指标点列表（失败返回空列表）
     */
    List<MetricPoint> collectInstanceMetrics(Long instanceId, Connection connection);

    /**
     * 采集慢 SQL 明细（统计窗口内聚合视图）
     *
     * @param instanceId 实例ID
     * @param dbName     库名
     * @param connection JDBC 连接
     * @return 慢 SQL 行（能力不可用时返回空列表）
     */
    List<SlowSqlRow> collectSlowSql(Long instanceId, String dbName, Connection connection);

    /**
     * 会话列表
     *
     * @param instanceId 实例ID
     * @param connection JDBC 连接
     * @return 会话行列表
     */
    default List<DbSessionRow> listSessions(Long instanceId, Connection connection) {
        return List.of();
    }

    /**
     * 终止会话（高危操作；sessionId 合法性由实现严格校验）
     *
     * @param instanceId 实例ID
     * @param connection JDBC 连接
     * @param sessionId  会话ID
     * @return 是否成功
     */
    default boolean killSession(Long instanceId, Connection connection, String sessionId) {
        return false;
    }

    /**
     * 锁等待/阻塞链
     *
     * @param instanceId 实例ID
     * @param connection JDBC 连接
     * @return 锁行列表
     */
    default List<DbLockRow> listLocks(Long instanceId, Connection connection) {
        return List.of();
    }

    /**
     * schema 列表
     */
    default List<String> listSchemas(Connection connection) {
        return List.of();
    }

    /**
     * 表列表（含视图）
     */
    default List<DbTableRow> listTables(Connection connection, String schemaName) {
        return List.of();
    }

    /**
     * 列列表（含主键标记）
     */
    default List<DbColumnRow> listColumns(Connection connection, String schemaName, String tableName) {
        return List.of();
    }

    /**
     * 建表 DDL 查看（尽量方言生成；不支持返回空串）
     */
    default String getTableDdl(Connection connection, String schemaName, String tableName) {
        return "";
    }

    /**
     * 库表容量采集（每日快照）
     */
    default List<CapacityRow> collectCapacity(Long instanceId, Connection connection) {
        return List.of();
    }
}
