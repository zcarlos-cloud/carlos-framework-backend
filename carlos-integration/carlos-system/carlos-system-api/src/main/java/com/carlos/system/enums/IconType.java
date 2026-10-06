package com.carlos.system.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.carlos.core.enums.AppEnum;
import com.carlos.core.enums.BaseEnum;
import com.carlos.core.response.CommonErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 图标类型
 *
 * @author carlos
 * @date 2026-05-27
 */
@Getter
@AllArgsConstructor
@AppEnum(code = "IconType")
public enum IconType implements BaseEnum<Integer> {

    /**
     * iconify
     */
    ICONIFY(1, "iconify", "iconify"),

    /**
     * local
     */
    LOCAL(2, "local", "local"),
    ;

    @EnumValue
    private final Integer code;

    private final String desc;

    /** 英文描述：随 /app/enums 下发，前端按当前语言取用（本枚举中英文同形） */
    private final String descEn;

    public static IconType ofCode(final Integer code) {
        final IconType[] values = IconType.values();
        for (final IconType value : values) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        throw CommonErrorCode.NOT_FOUND.exception("Can't find enum of code " + code);
    }
}
