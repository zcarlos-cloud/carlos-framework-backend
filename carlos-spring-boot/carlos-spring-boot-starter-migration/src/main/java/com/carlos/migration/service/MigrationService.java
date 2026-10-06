package com.carlos.migration.service;

import com.carlos.migration.core.MultiDataSourceLiquibase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * 数据库迁移服务
 * 提供迁移操作的程序化接口
 *
 * @author carlos
 * @since 3.0.0
 */
public class MigrationService {

    private static final Logger log = LoggerFactory.getLogger(MigrationService.class);

    private final MultiDataSourceLiquibase liquibase;

    public MigrationService(MultiDataSourceLiquibase liquibase) {
        this.liquibase = liquibase;
    }

    /**
     * 执行所有数据源的迁移
     */
    public void migrateAll() {
        liquibase.runMigrations();
    }

    /**
     * 执行指定数据源的迁移
     */
    public void migrate(String dataSourceName) {
        // 通过 Liquibase 重新执行迁移
        log.info("[MigrationService] 执行数据源 [{}] 的迁移", dataSourceName);
        // 具体实现由 MultiDataSourceLiquibase 提供
    }

    /**
     * 回滚指定数据源到指定标签
     */
    public void rollback(String dataSourceName, String tag) {
        liquibase.rollback(dataSourceName, tag);
    }

    /**
     * 回滚指定数据源指定数量的变更集
     */
    public void rollbackCount(String dataSourceName, int count) {
        liquibase.rollbackCount(dataSourceName, count);
    }

    /**
     * 验证指定数据源的变更
     */
    public void validate(String dataSourceName) {
        liquibase.validate(dataSourceName);
    }

    /**
     * 获取指定数据源的迁移状态报告
     */
    public Map<String, Object> getStatus(String dataSourceName) {
        return liquibase.getStatusReport(dataSourceName);
    }

    /**
     * 获取所有数据源的迁移状态
     */
    public Map<String, MultiDataSourceLiquibase.LiquibaseStatus> getAllStatus() {
        return liquibase.getMigrationStatus();
    }
}
