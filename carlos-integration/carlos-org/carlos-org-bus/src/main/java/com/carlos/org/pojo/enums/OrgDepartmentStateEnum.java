package com.carlos.org.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.carlos.core.enums.AppEnum;
import com.carlos.core.enums.BaseEnum;
import com.carlos.core.enums.EnumColor;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * 部门状态枚举
 * </p>
 *
 * @author carlos
 * @date 2026-03-03
 */
@AppEnum(code = "OrgDepartmentState")
@Getter
@AllArgsConstructor
public enum OrgDepartmentStateEnum implements BaseEnum {

    /**
     * 禁用
     */
    DISABLE(0, "禁用", "Disabled", EnumColor.DANGER),

    /**
     * 启用
     */
    ENABLE(1, "启用", "Enabled", EnumColor.SUCCESS);

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
