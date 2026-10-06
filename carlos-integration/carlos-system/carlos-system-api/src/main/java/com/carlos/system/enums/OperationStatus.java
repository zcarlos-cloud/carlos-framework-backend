package com.carlos.system.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.carlos.core.enums.AppEnum;
import com.carlos.core.enums.BaseEnum;
import com.carlos.core.response.CommonErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 操作状态
 *
 * @author carlos
 * @date 2026-05-27
 */
@Getter
@AllArgsConstructor
@AppEnum(code = "OperationStatus")
public enum OperationStatus implements BaseEnum<Integer> {

    /**
     * 禁用
     */
    DISABLED(0, "禁用"),

    /**
     * 启用
     */
    ENABLED(1, "启用"),
    ;

    @EnumValue
    private final Integer code;

    private final String desc;

    public static OperationStatus ofCode(final Integer code) {
        final OperationStatus[] values = OperationStatus.values();
        for (final OperationStatus value : values) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        throw CommonErrorCode.NOT_FOUND.exception("Can't find enum of code " + code);
    }
}
