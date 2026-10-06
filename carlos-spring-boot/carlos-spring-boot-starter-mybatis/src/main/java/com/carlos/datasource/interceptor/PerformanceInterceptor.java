package com.carlos.datasource.interceptor;

import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * SQL 性能监控拦截器
 * </p>
 *
 * <p>
 * 用于监控 SQL 执行时间，识别慢 SQL
 * 实现 InnerInterceptor 接口，集成到 Mybatis Plus 拦截器链中
 * </p>
 *
 * @author carlos
 * @date 2025/01/15
 */
@Slf4j
public class PerformanceInterceptor implements InnerInterceptor {

    /**
     * 慢 SQL 阈值（毫秒）
     */
    private long slowSqlThreshold = 1000;

    /**
     * 是否打印所有 SQL
     */
    private boolean printAllSql = false;

    /**
     * 设置慢 SQL 阈值
     *
     * @param slowSqlThreshold 阈值（毫秒）
     */
    public void setSlowSqlThreshold(long slowSqlThreshold) {
        this.slowSqlThreshold = slowSqlThreshold;
    }

    /**
     * 设置是否打印所有 SQL
     *
     * @param printAllSql 是否打印
     */
    public void setPrintAllSql(boolean printAllSql) {
        this.printAllSql = printAllSql;
    }

    // 具体的性能监控逻辑可以通过 AOP 或日志实现
    // 这里提供一个简单的配置类，实际使用时可以通过配置文件开启
}
