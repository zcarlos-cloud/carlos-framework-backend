package com.carlos.system.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.carlos.core.enums.AppEnum;
import com.carlos.core.enums.BaseEnum;
import com.carlos.core.response.CommonErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 操作类型
 *
 * @author carlos
 * @date 2026-05-27
 */
@Getter
@AllArgsConstructor
@AppEnum(code = "OperationType")
public enum OperationType implements BaseEnum<Integer> {

    /**
     * 按钮
     */
    BUTTON(1, "按钮"),

    /**
     * API
     */
    API(2, "API"),
    ;

    @EnumValue
    private final Integer code;

    private final String desc;

    public static OperationType ofCode(final Integer code) {
        final OperationType[] values = OperationType.values();
        for (final OperationType value : values) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        throw CommonErrorCode.NOT_FOUND.exception("Can't find enum of code " + code);
    }
}
