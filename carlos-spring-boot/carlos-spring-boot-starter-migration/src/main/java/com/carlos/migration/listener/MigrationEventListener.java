package com.carlos.migration.listener;

import com.carlos.migration.core.MultiDataSourceLiquibase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

import java.util.Map;

/**
 * 数据库迁移事件监听器
 * 监听应用生命周期事件，执行迁移相关操作
 *
 * @author carlos
 * @since 3.0.0
 */
public class MigrationEventListener implements
    ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(MigrationEventListener.class);

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        // 应用启动完成后，输出迁移状态
        Map<String, MultiDataSourceLiquibase> beans =
            event.getApplicationContext().getBeansOfType(MultiDataSourceLiquibase.class);

        if (!beans.isEmpty()) {
            beans.values().forEach(liquibase -> {
                Map<String, MultiDataSourceLiquibase.LiquibaseStatus> status = liquibase.getMigrationStatus();
                log.info("[Migration] 数据库迁移状态: {}", status);
            });
        }
    }
}
