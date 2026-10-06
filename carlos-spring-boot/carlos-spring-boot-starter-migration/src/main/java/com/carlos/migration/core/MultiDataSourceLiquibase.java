package com.carlos.migration.core;

import com.carlos.migration.config.MigrationProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.ResourceAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ResourceLoader;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 * 多数据源 Liquibase 管理器
 * 支持主数据源和多数据源的自动化迁移管理
 *
 * @author carlos
 * @since 3.0.0
 */
public class MultiDataSourceLiquibase implements InitializingBean, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(MultiDataSourceLiquibase.class);

    private MigrationProperties migrationProperties;
    private ResourceLoader resourceLoader;
    private DataSource primaryDataSource;
    private Map<String, DataSource> multiDataSources = new HashMap<>();
    private ExecutorService executorService;
    private final Map<String, LiquibaseStatus> migrationStatus = new ConcurrentHashMap<>();

    /**
     * 迁移状态枚举
     */
    public enum LiquibaseStatus {
        PENDING,      // 待执行
        RUNNING,      // 执行中
        SUCCESS,      // 成功
        FAILED,       // 失败
        ROLLED_BACK   // 已回滚
    }

    @PostConstruct
    public void init() {
        if (migrationProperties.isAsync()) {
            executorService = Executors.newFixedThreadPool(
                migrationProperties.getAsyncThreadPoolSize(),
                new ThreadFactory() {
                    private int count = 0;

                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "liquibase-migration-" + (++count));
                    }
                }
            );
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (migrationProperties.isEnabled()) {
            if (migrationProperties.getPrimary().isRunOnStartup()) {
                runMigrations();
            }
        }
    }

    @PreDestroy
    @Override
    public void destroy() throws Exception {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        }
    }

    /**
     * 执行所有数据源的迁移
     */
    public void runMigrations() {
        log.info("[Migration] 开始执行数据库迁移...");

        // 迁移主数据源
        if (primaryDataSource != null && migrationProperties.getPrimary().isEnabled()) {
            runMigration("primary", primaryDataSource, migrationProperties.getPrimary());
        }

        // 迁移多数据源
        if (multiDataSources != null && !multiDataSources.isEmpty()) {
            for (Map.Entry<String, DataSource> entry : multiDataSources.entrySet()) {
                String dsName = entry.getKey();
                MigrationProperties.DataSourceConfig config = migrationProperties.getMulti().get(dsName);

                if (config == null) {
                    config = new MigrationProperties.DataSourceConfig();
                }

                if (config.isEnabled()) {
                    runMigration(dsName, entry.getValue(), config);
                }
            }
        }

        log.info("[Migration] 数据库迁移执行完成");
    }

    /**
     * 执行单个数据源的迁移
     */
    public void runMigration(String dataSourceName, DataSource dataSource,
                             MigrationProperties.DataSourceConfig config) {
        if (migrationProperties.isAsync()) {
            executorService.submit(() -> doMigrate(dataSourceName, dataSource, config));
        } else {
            doMigrate(dataSourceName, dataSource, config);
        }
    }

    /**
     * 实际执行迁移
     */
    private void doMigrate(String dataSourceName, DataSource dataSource,
                           MigrationProperties.DataSourceConfig config) {
        migrationStatus.put(dataSourceName, LiquibaseStatus.RUNNING);

        try (Connection connection = dataSource.getConnection()) {
            log.info("[Migration] 数据源 [{}] 开始执行 Liquibase 迁移，变更日志: {}",
                dataSourceName, config.getChangeLog());

            JdbcConnection jdbcConnection = new JdbcConnection(connection);
            Database database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(jdbcConnection);

            // 设置默认 schema
            if (config.getDefaultSchema() != null && !config.getDefaultSchema().isEmpty()) {
                database.setDefaultSchemaName(config.getDefaultSchema());
            }

            // 设置变更日志表名
            if (config.getDatabaseChangeLogTable() != null) {
                database.setDatabaseChangeLogTableName(config.getDatabaseChangeLogTable());
            }
            if (config.getDatabaseChangeLogLockTable() != null) {
                database.setDatabaseChangeLogLockTableName(config.getDatabaseChangeLogLockTable());
            }

            ResourceAccessor resourceAccessor = new ClassLoaderResourceAccessor(
                Thread.currentThread().getContextClassLoader());

            try (Liquibase liquibase = new Liquibase(config.getChangeLog(), resourceAccessor, database)) {

                // 设置参数
                if (config.getParameters() != null) {
                    config.getParameters().forEach(liquibase::setChangeLogParameter);
                }

                // 设置上下文和标签
                Contexts contexts = new Contexts(config.getContexts());
                LabelExpression labels = new LabelExpression(config.getLabels());

                // 先清空数据库（如果是开发环境且配置了dropFirst）
                if (config.isDropFirst()) {
                    log.warn("[Migration] 数据源 [{}] 执行 dropAll 操作！", dataSourceName);
                    liquibase.dropAll();
                }

                // 执行迁移
                liquibase.update(contexts, labels);

                migrationStatus.put(dataSourceName, LiquibaseStatus.SUCCESS);
                log.info("[Migration] 数据源 [{}] Liquibase 迁移执行成功", dataSourceName);
            }

        } catch (SQLException | LiquibaseException e) {
            migrationStatus.put(dataSourceName, LiquibaseStatus.FAILED);
            log.error("[Migration] 数据源 [{}] Liquibase 迁移执行失败: {}", dataSourceName, e.getMessage(), e);
            throw new MigrationException("数据源 [" + dataSourceName + "] 迁移失败", e);
        }
    }

    /**
     * 回滚指定数据源到指定版本
     */
    public void rollback(String dataSourceName, String tag) {
        DataSource dataSource = getDataSource(dataSourceName);
        MigrationProperties.DataSourceConfig config = getConfig(dataSourceName);

        try (Connection connection = dataSource.getConnection()) {
            log.info("[Migration] 数据源 [{}] 开始回滚到标签: {}", dataSourceName, tag);

            JdbcConnection jdbcConnection = new JdbcConnection(connection);
            Database database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(jdbcConnection);

            ResourceAccessor resourceAccessor = new ClassLoaderResourceAccessor();

            try (Liquibase liquibase = new Liquibase(config.getChangeLog(), resourceAccessor, database)) {
                liquibase.rollback(tag, new Contexts(config.getContexts()));
                migrationStatus.put(dataSourceName, LiquibaseStatus.ROLLED_BACK);
                log.info("[Migration] 数据源 [{}] 回滚到标签 [{}] 成功", dataSourceName, tag);
            }
        } catch (SQLException | LiquibaseException e) {
            log.error("[Migration] 数据源 [{}] 回滚失败: {}", dataSourceName, e.getMessage(), e);
            throw new MigrationException("回滚失败", e);
        }
    }

    /**
     * 回滚指定数据源指定数量的变更集
     */
    public void rollbackCount(String dataSourceName, int count) {
        DataSource dataSource = getDataSource(dataSourceName);
        MigrationProperties.DataSourceConfig config = getConfig(dataSourceName);

        try (Connection connection = dataSource.getConnection()) {
            log.info("[Migration] 数据源 [{}] 开始回滚 [{}] 个变更集", dataSourceName, count);

            JdbcConnection jdbcConnection = new JdbcConnection(connection);
            Database database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(jdbcConnection);

            ResourceAccessor resourceAccessor = new ClassLoaderResourceAccessor();

            try (Liquibase liquibase = new Liquibase(config.getChangeLog(), resourceAccessor, database)) {
                liquibase.rollback(count, config.getContexts());
                log.info("[Migration] 数据源 [{}] 回滚 [{}] 个变更集成功", dataSourceName, count);
            }
        } catch (SQLException | LiquibaseException e) {
            log.error("[Migration] 数据源 [{}] 回滚失败: {}", dataSourceName, e.getMessage(), e);
            throw new MigrationException("回滚失败", e);
        }
    }

    /**
     * 验证待执行的变更
     */
    public void validate(String dataSourceName) {
        DataSource dataSource = getDataSource(dataSourceName);
        MigrationProperties.DataSourceConfig config = getConfig(dataSourceName);

        try (Connection connection = dataSource.getConnection()) {
            JdbcConnection jdbcConnection = new JdbcConnection(connection);
            Database database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(jdbcConnection);

            ResourceAccessor resourceAccessor = new ClassLoaderResourceAccessor();

            try (Liquibase liquibase = new Liquibase(config.getChangeLog(), resourceAccessor, database)) {
                liquibase.validate();
                log.info("[Migration] 数据源 [{}] 变更验证通过", dataSourceName);
            }
        } catch (SQLException | LiquibaseException e) {
            log.error("[Migration] 数据源 [{}] 变更验证失败: {}", dataSourceName, e.getMessage(), e);
            throw new MigrationException("验证失败", e);
        }
    }

    /**
     * 获取迁移状态报告
     */
    public Map<String, Object> getStatusReport(String dataSourceName) {
        Map<String, Object> report = new HashMap<>();
        report.put("dataSourceName", dataSourceName);
        report.put("status", migrationStatus.getOrDefault(dataSourceName, LiquibaseStatus.PENDING));

        DataSource dataSource = getDataSource(dataSourceName);
        MigrationProperties.DataSourceConfig config = getConfig(dataSourceName);

        try (Connection connection = dataSource.getConnection()) {
            JdbcConnection jdbcConnection = new JdbcConnection(connection);
            Database database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(jdbcConnection);

            ResourceAccessor resourceAccessor = new ClassLoaderResourceAccessor();

            try (Liquibase liquibase = new Liquibase(config.getChangeLog(), resourceAccessor, database)) {
                report.put("unrunChangesets", liquibase.listUnrunChangeSets(
                    new Contexts(config.getContexts()), new LabelExpression(config.getLabels())));
            }
        } catch (SQLException | LiquibaseException e) {
            log.error("获取状态报告失败", e);
            report.put("error", e.getMessage());
        }

        return report;
    }

    /**
     * 生成变更日志差异（未来功能）
     */
    public void generateDiff(String referenceUrl, String targetUrl, String outputFile) {
        // 使用 Liquibase diff 命令生成差异
        log.info("[Migration] 生成数据库差异报告...");
    }

    // ============ Getter/Setter ============

    public void setMigrationProperties(MigrationProperties migrationProperties) {
        this.migrationProperties = migrationProperties;
    }

    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public void setPrimaryDataSource(DataSource primaryDataSource) {
        this.primaryDataSource = primaryDataSource;
    }

    public void setMultiDataSources(Map<String, DataSource> multiDataSources) {
        this.multiDataSources = multiDataSources;
    }

    private DataSource getDataSource(String dataSourceName) {
        if ("primary".equals(dataSourceName)) {
            return primaryDataSource;
        }
        return multiDataSources.get(dataSourceName);
    }

    private MigrationProperties.DataSourceConfig getConfig(String dataSourceName) {
        if ("primary".equals(dataSourceName)) {
            return migrationProperties.getPrimary();
        }
        return migrationProperties.getMulti().getOrDefault(dataSourceName,
            new MigrationProperties.DataSourceConfig());
    }

    public Map<String, LiquibaseStatus> getMigrationStatus() {
        return new HashMap<>(migrationStatus);
    }
}
