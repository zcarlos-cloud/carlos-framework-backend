package com.carlos.migration.config;

import com.carlos.migration.core.MultiDataSourceLiquibase;
import com.carlos.migration.listener.MigrationEventListener;
import com.carlos.migration.service.MigrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ResourceLoader;

import javax.sql.DataSource;
import java.util.Map;

/**
 * Liquibase 自动配置类
 * 提供数据库迁移功能的自动配置
 *
 * @author carlos
 * @since 3.0.0
 */
@AutoConfiguration(after = DataSourceAutoConfiguration.class)
@ConditionalOnClass({liquibase.integration.spring.SpringLiquibase.class})
@ConditionalOnProperty(prefix = "carlos.migration", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(MigrationProperties.class)
public class LiquibaseAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(LiquibaseAutoConfiguration.class);

    private final MigrationProperties migrationProperties;
    private final ResourceLoader resourceLoader;

    public LiquibaseAutoConfiguration(MigrationProperties migrationProperties, ResourceLoader resourceLoader) {
        this.migrationProperties = migrationProperties;
        this.resourceLoader = resourceLoader;
    }

    /**
     * 多数据源 Liquibase 配置
     */
    @Bean
    @ConditionalOnMissingBean
    public MultiDataSourceLiquibase multiDataSourceLiquibase(
        ObjectProvider<DataSource> dataSourceProvider,
        ObjectProvider<Map<String, DataSource>> multiDataSourcesProvider) {

        MultiDataSourceLiquibase liquibase = new MultiDataSourceLiquibase();
        liquibase.setMigrationProperties(migrationProperties);
        liquibase.setResourceLoader(resourceLoader);

        // 设置主数据源
        DataSource primaryDataSource = dataSourceProvider.getIfAvailable();
        if (primaryDataSource != null) {
            liquibase.setPrimaryDataSource(primaryDataSource);
        }

        // 设置多数据源
        Map<String, DataSource> multiDataSources = multiDataSourcesProvider.getIfAvailable();
        if (multiDataSources != null) {
            liquibase.setMultiDataSources(multiDataSources);
        }

        log.info("[Migration] Liquibase 多数据源迁移配置已初始化");
        return liquibase;
    }

    /**
     * 迁移服务
     */
    @Bean
    @ConditionalOnMissingBean
    public MigrationService migrationService(MultiDataSourceLiquibase multiDataSourceLiquibase) {
        return new MigrationService(multiDataSourceLiquibase);
    }

    /**
     * 迁移事件监听器
     */
    @Bean
    @ConditionalOnMissingBean
    public MigrationEventListener migrationEventListener() {
        return new MigrationEventListener();
    }
}
