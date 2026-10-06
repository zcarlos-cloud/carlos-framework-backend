package com.carlos.audit.config;

/**
 * 审计日志缓冲区背压策略
 *
 * @author Carlos
 * @date 2026-06-12
 */
public enum BackPressureStrategy {
    /**
     * 丢弃最旧数据
     */
    DROP_OLDEST,
    /**
     * 丢弃最新数据（当前这一条）
     */
    DROP_NEWEST,
    /**
     * 阻塞等待直到缓冲区有空间
     */
    BLOCK,
    /**
     * 按采样率保留数据
     */
    SAMPLE
}
