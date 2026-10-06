package com.carlos.system.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.carlos.core.enums.AppEnum;
import com.carlos.core.enums.BaseEnum;
import com.carlos.core.response.CommonErrorCode;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 系统配置值类型
 *
 * @author Carlos
 * @date 2022/12/22 11:31
 */
@Getter
@AllArgsConstructor
@AppEnum(code = "ConfigValueType")
public enum ConfigValueType implements BaseEnum<Integer> {

    /**
     * 系统配置值类型
     */
    TEXT(1, "文本"),
    FILE(2, "文件"),
    ;

    @EnumValue
    private final Integer code;

    private final String desc;

    @JsonValue
    public String getJsonCode() {
        return String.valueOf(code);
    }

    @JsonCreator
    public static ConfigValueType ofJsonCode(final String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        try {
            return ofCode(Integer.valueOf(code));
        } catch (NumberFormatException e) {
            throw CommonErrorCode.NOT_FOUND.exception("Can't find enum of code " + code);
        }
    }

    public static ConfigValueType ofCode(final Integer code) {
        final ConfigValueType[] values = ConfigValueType.values();
        for (final ConfigValueType value : values) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        throw CommonErrorCode.NOT_FOUND.exception("Can't find enum of code " + code);
    }
}
