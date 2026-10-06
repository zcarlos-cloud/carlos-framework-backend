package com.carlos.org.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.carlos.core.enums.AppEnum;
import com.carlos.core.enums.BaseEnum;
import com.carlos.core.enums.EnumColor;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * 租户状态枚举
 * </p>
 *
 * @author carlos
 * @date 2026-09-22
 */
@AppEnum(code = "OrgTenantState")
@Getter
@AllArgsConstructor
public enum OrgTenantStateEnum implements BaseEnum {

    /**
     * 正常
     */
    NORMAL(1, "正常", "Normal", EnumColor.SUCCESS),

    /**
     * 停用
     */
    DISABLE(2, "停用", "Disabled", EnumColor.DANGER);

    @EnumValue
    private final Integer code;

    private final String desc;

    /** 英文描述：随 /app/enums 下发，前端按当前语言取用 */
    private final String descEn;

    /** 语义色：随 /app/enums 下发，避免前端为每个枚举值维护颜色表 */
    private final EnumColor color;

    @Override
    public Integer getCode() {
        return code;
    }

    @Override
    public String getDesc() {
        return desc;
    }

}
