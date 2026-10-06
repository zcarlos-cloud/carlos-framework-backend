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
 * 告警状态（字典 monitor_alarm_status）
 * <p>
 * 状态机：FIRING → CONFIRMED → RECOVERED；FIRING 也可直接 RECOVERED
 * </p>
 *
 * @author Carlos
 * @date 2026年10月1日
 */
@Getter
@AllArgsConstructor
@AppEnum(code = "MonitorAlarmStatus")
public enum AlarmStatusEnum implements BaseEnum<String> {

    /**
     * 触发中
     */
    FIRING("FIRING", "触发中"),

    /**
     * 已确认
     */
    CONFIRMED("CONFIRMED", "已确认"),

    /**
     * 已恢复
     */
    RECOVERED("RECOVERED", "已恢复"),
    ;

    @JsonValue
    @EnumValue
    private final String code;

    private final String desc;

    @JsonCreator
    public static AlarmStatusEnum ofCode(final String code) {
        final AlarmStatusEnum[] values = AlarmStatusEnum.values();
        for (final AlarmStatusEnum value : values) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        for (final AlarmStatusEnum value : values) {
            if (value.name().equalsIgnoreCase(code)) {
                return value;
            }
        }
        throw CommonErrorCode.NOT_FOUND.exception("Can't find enum of code " + code);
    }
}
