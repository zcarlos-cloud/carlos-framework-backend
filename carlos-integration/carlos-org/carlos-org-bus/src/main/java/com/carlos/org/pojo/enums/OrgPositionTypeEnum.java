package com.carlos.org.pojo.enums;

import com.carlos.core.enums.AppEnum;
import com.carlos.core.enums.BaseEnum;
import com.carlos.core.enums.EnumColor;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * 岗位类别枚举
 * </p>
 *
 * <p>
 * 本枚举是**字典源**：承载字段在 AO/DTO 里仍是 {@code Integer}，JSON 下发数字码，
 * 前端经 {@code /org/app/enums} 按码值取文案/语义色。码值与
 * {@code OrgPosition.positionType} 保持一致（1 标准岗 / 2 虚拟岗 / 3 兼职岗）。
 * 类别不是状态，三种取值沿用同一种主色（与前端的既有展示一致，不做逐值区分）。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@AppEnum(code = "OrgPositionType")
@Getter
@AllArgsConstructor
public enum OrgPositionTypeEnum implements BaseEnum<Integer> {

    /**
     * 标准岗
     */
    STANDARD(1, "标准岗", "Standard", EnumColor.PRIMARY),

    /**
     * 虚拟岗
     */
    VIRTUAL(2, "虚拟岗", "Virtual", EnumColor.PRIMARY),

    /**
     * 兼职岗
     */
    PART_TIME(3, "兼职岗", "Part-time", EnumColor.PRIMARY);

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
