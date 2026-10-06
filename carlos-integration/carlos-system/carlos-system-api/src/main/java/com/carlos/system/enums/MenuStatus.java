package com.carlos.system.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.carlos.core.enums.AppEnum;
import com.carlos.core.enums.BaseEnum;
import com.carlos.core.enums.EnumColor;
import com.carlos.core.response.CommonErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 菜单状态
 *
 * @author carlos
 * @date 2026-05-27
 */
@Getter
@AllArgsConstructor
@AppEnum(code = "MenuStatus")
public enum MenuStatus implements BaseEnum<Integer> {

    /**
     * 启用
     */
    ENABLED(1, "启用", "Enabled", EnumColor.SUCCESS),

    /**
     * 禁用
     */
    DISABLED(2, "禁用", "Disabled", EnumColor.WARNING),
    ;

    @EnumValue
    private final Integer code;

    private final String desc;

    /** 英文描述：随 /app/enums 下发，前端按当前语言取用 */
    private final String descEn;

    /** 语义色：随 /app/enums 下发，避免前端为每个枚举值维护颜色表 */
    private final EnumColor color;

    public static MenuStatus ofCode(final Integer code) {
        final MenuStatus[] values = MenuStatus.values();
        for (final MenuStatus value : values) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        throw CommonErrorCode.NOT_FOUND.exception("Can't find enum of code " + code);
    }
}
