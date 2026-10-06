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
 * 告警级别（字典 monitor_alarm_level）
 *
 * @author Carlos
 * @date 2026年10月1日
 */
@Getter
@AllArgsConstructor
@AppEnum(code = "MonitorAlarmLevel")
public enum AlarmLevelEnum implements BaseEnum<String> {

    /**
     * 提示
     */
    INFO("INFO", "提示"),

    /**
     * 警告
     */
    WARN("WARN", "警告"),

    /**
     * 严重
     */
    CRITICAL("CRITICAL", "严重"),
    ;

    @JsonValue
    @EnumValue
    private final String code;

    private final String desc;

    @JsonCreator
    public static AlarmLevelEnum ofCode(final String code) {
        final AlarmLevelEnum[] values = AlarmLevelEnum.values();
        for (final AlarmLevelEnum value : values) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        for (final AlarmLevelEnum value : values) {
            if (value.name().equalsIgnoreCase(code)) {
                return value;
            }
        }
        throw CommonErrorCode.NOT_FOUND.exception("Can't find enum of code " + code);
    }
}
