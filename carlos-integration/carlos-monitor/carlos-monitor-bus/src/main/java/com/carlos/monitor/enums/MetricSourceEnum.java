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
 * 指标来源
 *
 * @author Carlos
 * @date 2026年10月1日
 */
@Getter
@AllArgsConstructor
@AppEnum(code = "MonitorMetricSource")
public enum MetricSourceEnum implements BaseEnum<String> {

    /**
     * Redis 指标
     */
    REDIS("REDIS", "Redis"),

    /**
     * 数据库指标
     */
    DB("DB", "数据库"),

    /**
     * 微服务指标
     */
    SERVICE("SERVICE", "微服务"),
    ;

    @JsonValue
    @EnumValue
    private final String code;

    private final String desc;

    @JsonCreator
    public static MetricSourceEnum ofCode(final String code) {
        final MetricSourceEnum[] values = MetricSourceEnum.values();
        for (final MetricSourceEnum value : values) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        for (final MetricSourceEnum value : values) {
            if (value.name().equalsIgnoreCase(code)) {
                return value;
            }
        }
        throw CommonErrorCode.NOT_FOUND.exception("Can't find enum of code " + code);
    }
}
