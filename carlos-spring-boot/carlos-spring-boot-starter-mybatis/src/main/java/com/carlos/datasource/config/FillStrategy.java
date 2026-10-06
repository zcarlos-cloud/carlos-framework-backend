package com.carlos.datasource.config;

/**
 * <p>
 * fill 注解字段的填充策略
 * </p>
 *
 * @author carlos
 * @since 3.0.0
 */
enum FillStrategy {

    /**
     * 填充当前时间（时间类型字段）
     */
    TIME,

    /**
     * 填充当前用户 ID（命名为 createBy/updateBy 的字段）
     */
    USER,

    /**
     * 无策略：保持 null，需要业务显式赋值
     */
    UNKNOWN
}
