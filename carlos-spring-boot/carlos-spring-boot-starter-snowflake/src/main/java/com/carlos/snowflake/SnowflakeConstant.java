package com.carlos.snowflake;

/**
 * <p>
 * 雪花算法相关常量
 * </p>
 *
 * @author carlos
 * @date 2021/12/13 11:46
 */
public interface SnowflakeConstant {

    // 最大支持机器节点数0~31，一共32个
    long MAX_WORKER_ID = 31;
    // 最大支持数据中心节点数0~31，一共32个
    long MAX_DATA_CENTER_ID = 31;


    /**
     * 缓存空间（Redis Key 顶层命名规范 {@code {模块}:{业务}:{标识}} 中的 framework: 段）
     * <p>禁止各服务在 yml 里覆盖 {@code carlos.snowflake.namespace}——历史上
     * carlos-audit / carlos-monitor 曾把它改成 {@code carlos}，导致同一实例上
     * {@code snowflake:{w}_{d}} 与 {@code carlos:{w}_{d}} 两套 workerId 注册键并存。</p>
     */
    String NAMESPACE = "framework:snowflake";
}
