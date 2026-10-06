package com.carlos.monitor.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.carlos.core.enums.AppEnum;
import com.carlos.core.enums.BaseEnum;
import com.carlos.core.response.CommonErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 监控实例类型（字典 monitor_instance_type）
 *
 * @author Carlos
 * @date 2026年10月1日
 */
@Getter
@AllArgsConstructor
@AppEnum(code = "MonitorInstanceType")
public enum InstanceTypeEnum implements BaseEnum<String> {

    /**
     * Redis 单机
     */
    REDIS_STANDALONE("REDIS_STANDALONE", "Redis单机"),

    /**
     * Redis 集群
     */
    REDIS_CLUSTER("REDIS_CLUSTER", "Redis集群"),

    /**
     * Redis 哨兵
     */
    REDIS_SENTINEL("REDIS_SENTINEL", "Redis哨兵"),

    /**
     * PostgreSQL
     */
    POSTGRESQL("POSTGRESQL", "PostgreSQL"),

    /**
     * MySQL
     */
    MYSQL("MYSQL", "MySQL"),

    /**
     * 达梦
     */
    DM("DM", "达梦数据库"),

    /**
     * ClickHouse
     */
    CLICKHOUSE("CLICKHOUSE", "ClickHouse"),

    /**
     * 微服务（Nacos 自动登记）
     */
    MICROSERVICE("MICROSERVICE", "微服务"),
    ;

    @JsonValue
    @EnumValue
    private final String code;

    private final String desc;

    @JsonCreator
    public static InstanceTypeEnum ofCode(final String code) {
        final InstanceTypeEnum[] values = InstanceTypeEnum.values();
        for (final InstanceTypeEnum value : values) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        for (final InstanceTypeEnum value : values) {
            if (value.name().equalsIgnoreCase(code)) {
                return value;
            }
        }
        throw CommonErrorCode.NOT_FOUND.exception("Can't find enum of code " + code);
    }
}
