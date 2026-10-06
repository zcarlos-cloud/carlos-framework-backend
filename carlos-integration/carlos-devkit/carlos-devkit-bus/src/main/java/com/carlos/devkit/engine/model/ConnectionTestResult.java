package com.carlos.devkit.engine.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <p>
 * 目标数据库连通性测试结果
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConnectionTestResult {

    /**
     * 是否连通
     */
    private boolean success;

    /**
     * 数据库产品名
     */
    private String databaseName;

    /**
     * 数据库产品版本
     */
    private String databaseVersion;

    /**
     * 响应耗时（毫秒）
     */
    private long responseTimeMs;

    /**
     * 失败原因
     */
    private String errorMessage;

    /**
     * 构造成功结果
     *
     * @param name    产品名
     * @param version 产品版本
     * @param time    耗时（毫秒）
     * @return 测试结果
     */
    public static ConnectionTestResult success(final String name, final String version, final long time) {
        return new ConnectionTestResult(true, name, version, time, null);
    }

    /**
     * 构造失败结果
     *
     * @param error 失败原因
     * @return 测试结果
     */
    public static ConnectionTestResult failure(final String error) {
        return new ConnectionTestResult(false, null, null, -1L, error);
    }
}
