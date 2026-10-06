package com.carlos.log.enums;

/**
 * 日志级别
 *
 * @author carlos
 * @since 3.0.0
 */
public enum LogLevel {

    /**
     * 调试
     */
    DEBUG("DEBUG", "调试"),

    /**
     * 信息
     */
    INFO("INFO", "信息"),

    /**
     * 警告
     */
    WARN("WARN", "警告"),

    /**
     * 错误
     */
    ERROR("ERROR", "错误"),

    /**
     * 严重错误
     */
    FATAL("FATAL", "严重错误");

    private final String code;
    private final String description;

    LogLevel(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
