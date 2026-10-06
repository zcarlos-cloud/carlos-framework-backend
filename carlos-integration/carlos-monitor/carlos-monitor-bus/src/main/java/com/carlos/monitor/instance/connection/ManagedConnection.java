package com.carlos.monitor.instance.connection;

/**
 * <p>
 * 实例受管连接（sealed 接口）
 * 实现：{@link RedisManagedConnection}（Lettuce standalone/sentinel/cluster）
 * 与 {@link DbManagedConnection}（Druid 监控专用小池）
 * </p>
 *
 * @author Carlos
 * @date 2026年10月1日
 */
public sealed interface ManagedConnection extends AutoCloseable
    permits RedisManagedConnection, DbManagedConnection {

    /**
     * 所属实例ID
     *
     * @return 实例ID
     */
    Long getInstanceId();

    /**
     * 关闭连接并释放底层资源（Lettuce client shutdown / DruidDataSource close）
     */
    @Override
    void close();
}
