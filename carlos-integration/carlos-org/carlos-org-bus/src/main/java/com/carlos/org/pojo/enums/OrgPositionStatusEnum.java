package com.carlos.org.pojo.enums;

import com.carlos.core.enums.AppEnum;
import com.carlos.core.enums.BaseEnum;
import com.carlos.core.enums.EnumColor;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * 岗位状态枚举
 * </p>
 *
 * <p>
 * 本枚举是**字典源**：承载字段在 AO/DTO 里仍是 {@code Integer}，JSON 下发数字码，
 * 前端经 {@code /org/app/enums} 按码值取文案/语义色。码值与
 * {@code OrgPosition.positionStatus} 保持一致（0 禁用 / 1 启用 / 2 冻结）。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@AppEnum(code = "OrgPositionStatus")
@Getter
@AllArgsConstructor
public enum OrgPositionStatusEnum implements BaseEnum<Integer> {

    /**
     * 禁用
     */
    DISABLE(0, "禁用", "Disabled", EnumColor.DANGER),

    /**
     * 启用
     */
    ENABLE(1, "启用", "Enabled", EnumColor.SUCCESS),

    /**
     * 冻结
     */
    FROZEN(2, "冻结", "Frozen", EnumColor.WARNING);

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
