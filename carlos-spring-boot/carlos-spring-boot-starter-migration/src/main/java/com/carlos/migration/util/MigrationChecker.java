package com.carlos.migration.util;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.changelog.ChangeSet;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * 迁移检查工具
 * 用于检查数据库迁移状态和执行预检查
 *
 * @author carlos
 * @since 3.0.0
 */
public class MigrationChecker {

    private static final Logger log = LoggerFactory.getLogger(MigrationChecker.class);

    /**
     * 检查是否有待执行的变更
     */
    public static boolean hasPendingChanges(DataSource dataSource, String changeLogPath,
                                            String contexts, String labels) {
        try (Connection connection = dataSource.getConnection()) {
            JdbcConnection jdbcConnection = new JdbcConnection(connection);
            Database database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(jdbcConnection);

            try (Liquibase liquibase = new Liquibase(changeLogPath,
                new ClassLoaderResourceAccessor(), database)) {

                List<ChangeSet> unrunChangeSets = liquibase.listUnrunChangeSets(
                    new Contexts(contexts), new LabelExpression(labels));

                return !unrunChangeSets.isEmpty();
            }
        } catch (Exception e) {
            log.error("[MigrationChecker] 检查待执行变更失败", e);
            return false;
        }
    }

    /**
     * 获取待执行的变更集数量
     */
    public static int getPendingChangeSetCount(DataSource dataSource, String changeLogPath,
                                               String contexts, String labels) {
        try (Connection connection = dataSource.getConnection()) {
            JdbcConnection jdbcConnection = new JdbcConnection(connection);
            Database database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(jdbcConnection);

            try (Liquibase liquibase = new Liquibase(changeLogPath,
                new ClassLoaderResourceAccessor(), database)) {

                List<ChangeSet> unrunChangeSets = liquibase.listUnrunChangeSets(
                    new Contexts(contexts), new LabelExpression(labels));

                return unrunChangeSets.size();
            }
        } catch (Exception e) {
            log.error("[MigrationChecker] 获取待执行变更集数量失败", e);
            return -1;
        }
    }

    /**
     * 检查 Liquibase 是否已初始化
     */
    public static boolean isLiquibaseInitialized(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            var metaData = connection.getMetaData();
            try (var rs = metaData.getTables(null, null, "DATABASECHANGELOG", null)) {
                return rs.next();
            }
        } catch (SQLException e) {
            log.error("[MigrationChecker] 检查 Liquibase 初始化状态失败", e);
            return false;
        }
    }

    /**
     * 预检查：验证数据库连接和变更日志文件
     */
    public static boolean preflightCheck(DataSource dataSource, String changeLogPath) {
        try {
            // 检查数据库连接
            try (Connection connection = dataSource.getConnection()) {
                if (!connection.isValid(5)) {
                    log.error("[MigrationChecker] 数据库连接无效");
                    return false;
                }
            }

            // 检查变更日志文件是否存在
            var resource = Thread.currentThread().getContextClassLoader().getResource(changeLogPath);
            if (resource == null) {
                // 尝试添加 classpath: 前缀
                if (!changeLogPath.startsWith("classpath:")) {
                    resource = Thread.currentThread().getContextClassLoader()
                        .getResource("classpath:" + changeLogPath);
                }
                if (resource == null) {
                    log.warn("[MigrationChecker] 变更日志文件不存在: {}", changeLogPath);
                    // 文件不存在也返回 true，因为 Liquibase 会在运行时处理
                    return true;
                }
            }

            log.info("[MigrationChecker] 预检查通过");
            return true;

        } catch (SQLException e) {
            log.error("[MigrationChecker] 预检查失败: {}", e.getMessage());
            return false;
        }
    }
}
