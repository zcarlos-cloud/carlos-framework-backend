package com.carlos.migration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * 数据库迁移配置属性
 * 支持 Liquibase 多数据源配置
 *
 * @author carlos
 * @since 3.0.0
 */
@ConfigurationProperties(prefix = "carlos.migration")
public class MigrationProperties {

    /**
     * 是否启用迁移功能
     */
    private boolean enabled = true;

    /**
     * 默认数据源配置
     */
    private DataSourceConfig primary = new DataSourceConfig();

    /**
     * 多数据源配置（key为数据源名称）
     */
    private Map<String, DataSourceConfig> multi = new HashMap<>();

    /**
     * 是否启用迁移管理端点（需要spring-web支持）
     */
    private boolean endpointEnabled = false;

    /**
     * 迁移端点路径前缀
     */
    private String endpointPath = "/migration";

    /**
     * 是否启用异步迁移
     */
    private boolean async = false;

    /**
     * 异步迁移线程池大小
     */
    private int asyncThreadPoolSize = 2;

    // Getters and Setters

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public DataSourceConfig getPrimary() {
        return primary;
    }

    public void setPrimary(DataSourceConfig primary) {
        this.primary = primary;
    }

    public Map<String, DataSourceConfig> getMulti() {
        return multi;
    }

    public void setMulti(Map<String, DataSourceConfig> multi) {
        this.multi = multi;
    }

    public boolean isEndpointEnabled() {
        return endpointEnabled;
    }

    public void setEndpointEnabled(boolean endpointEnabled) {
        this.endpointEnabled = endpointEnabled;
    }

    public String getEndpointPath() {
        return endpointPath;
    }

    public void setEndpointPath(String endpointPath) {
        this.endpointPath = endpointPath;
    }

    public boolean isAsync() {
        return async;
    }

    public void setAsync(boolean async) {
        this.async = async;
    }

    public int getAsyncThreadPoolSize() {
        return asyncThreadPoolSize;
    }

    public void setAsyncThreadPoolSize(int asyncThreadPoolSize) {
        this.asyncThreadPoolSize = asyncThreadPoolSize;
    }

    /**
     * 数据源配置
     */
    public static class DataSourceConfig {

        /**
         * 是否启用该数据源的迁移
         */
        private boolean enabled = true;

        /**
         * 变更日志主文件路径
         * 默认：db/changelog/db.changelog-master.yaml
         */
        private String changeLog = "db/changelog/db.changelog-master.yaml";

        /**
         * 数据库上下文环境（用于区分不同环境的变更）
         * 如：develop, test, production
         */
        private String contexts = "";

        /**
         * 标签过滤器
         */
        private String labels = "";

        /**
         * 默认 Schema
         */
        private String defaultSchema;

        /**
         * 是否先清空数据库（危险操作，仅用于开发环境）
         */
        private boolean dropFirst = false;

        /**
         * 是否校验变更集 checksum
         */
        private boolean validateOnMigrate = true;

        /**
         * 执行迁移时是否忽略失败的变更
         */
        private boolean ignoreFailedFutureMigrations = false;

        /**
         * 是否在启动时执行迁移
         */
        private boolean runOnStartup = true;

        /**
         * 回滚时保留的变更集数量
         */
        private int rollbackCount = 1;

        /**
         * 参数配置（用于替换变更日志中的变量）
         */
        private Map<String, String> parameters = new HashMap<>();

        /**
         * Liquibase 数据库变更日志表名
         */
        private String databaseChangeLogTable = "DATABASECHANGELOG";

        /**
         * Liquibase 数据库变更日志锁定表名
         */
        private String databaseChangeLogLockTable = "DATABASECHANGELOGLOCK";

        // Getters and Setters

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getChangeLog() {
            return changeLog;
        }

        public void setChangeLog(String changeLog) {
            this.changeLog = changeLog;
        }

        public String getContexts() {
            return contexts;
        }

        public void setContexts(String contexts) {
            this.contexts = contexts;
        }

        public String getLabels() {
            return labels;
        }

        public void setLabels(String labels) {
            this.labels = labels;
        }

        public String getDefaultSchema() {
            return defaultSchema;
        }

        public void setDefaultSchema(String defaultSchema) {
            this.defaultSchema = defaultSchema;
        }

        public boolean isDropFirst() {
            return dropFirst;
        }

        public void setDropFirst(boolean dropFirst) {
            this.dropFirst = dropFirst;
        }

        public boolean isValidateOnMigrate() {
            return validateOnMigrate;
        }

        public void setValidateOnMigrate(boolean validateOnMigrate) {
            this.validateOnMigrate = validateOnMigrate;
        }

        public boolean isIgnoreFailedFutureMigrations() {
            return ignoreFailedFutureMigrations;
        }

        public void setIgnoreFailedFutureMigrations(boolean ignoreFailedFutureMigrations) {
            this.ignoreFailedFutureMigrations = ignoreFailedFutureMigrations;
        }

        public boolean isRunOnStartup() {
            return runOnStartup;
        }

        public void setRunOnStartup(boolean runOnStartup) {
            this.runOnStartup = runOnStartup;
        }

        public int getRollbackCount() {
            return rollbackCount;
        }

        public void setRollbackCount(int rollbackCount) {
            this.rollbackCount = rollbackCount;
        }

        public Map<String, String> getParameters() {
            return parameters;
        }

        public void setParameters(Map<String, String> parameters) {
            this.parameters = parameters;
        }

        public String getDatabaseChangeLogTable() {
            return databaseChangeLogTable;
        }

        public void setDatabaseChangeLogTable(String databaseChangeLogTable) {
            this.databaseChangeLogTable = databaseChangeLogTable;
        }

        public String getDatabaseChangeLogLockTable() {
            return databaseChangeLogLockTable;
        }

        public void setDatabaseChangeLogLockTable(String databaseChangeLogLockTable) {
            this.databaseChangeLogLockTable = databaseChangeLogLockTable;
        }
    }
}
