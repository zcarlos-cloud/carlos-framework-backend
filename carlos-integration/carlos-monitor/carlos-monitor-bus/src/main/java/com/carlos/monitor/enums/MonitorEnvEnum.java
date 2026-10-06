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
 * 监控环境（字典 monitor_env）
 *
 * @author Carlos
 * @date 2026年10月1日
 */
@Getter
@AllArgsConstructor
@AppEnum(code = "MonitorEnv")
public enum MonitorEnvEnum implements BaseEnum<String> {

    /**
     * 开发环境
     */
    DEV("dev", "开发环境"),

    /**
     * 测试环境
     */
    TEST("test", "测试环境"),

    /**
     * 生产环境
     */
    PROD("prod", "生产环境"),
    ;

    @JsonValue
    @EnumValue
    private final String code;

    private final String desc;

    @JsonCreator
    public static MonitorEnvEnum ofCode(final String code) {
        final MonitorEnvEnum[] values = MonitorEnvEnum.values();
        for (final MonitorEnvEnum value : values) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        for (final MonitorEnvEnum value : values) {
            if (value.name().equalsIgnoreCase(code)) {
                return value;
            }
        }
        throw CommonErrorCode.NOT_FOUND.exception("Can't find enum of code " + code);
    }
}
