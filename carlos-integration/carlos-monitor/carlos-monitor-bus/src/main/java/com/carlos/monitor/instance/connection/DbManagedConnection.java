package com.carlos.monitor.instance.connection;

import com.alibaba.druid.pool.DruidDataSource;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * 数据库受管连接（Druid 监控专用小池，maxActive 取 carlos.monitor.connection.db-pool-max-active）
 * </p>
 *
 * @author Carlos
 * @date 2026年10月1日
 */
@Slf4j
@Getter
public final class DbManagedConnection implements ManagedConnection {

    private final Long instanceId;

    private final DruidDataSource dataSource;

    public DbManagedConnection(Long instanceId, DruidDataSource dataSource) {
        this.instanceId = instanceId;
        this.dataSource = dataSource;
    }

    @Override
    public void close() {
        try {
            if (dataSource != null) {
                dataSource.close();
            }
        } catch (Exception e) {
            log.warn("Close db managed connection fail, instanceId:{}, error:{}", instanceId, e.getMessage());
        }
    }
}
