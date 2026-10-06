package com.carlos.org.pojo.enums;

import com.carlos.core.enums.AppEnum;
import com.carlos.core.enums.BaseEnum;
import com.carlos.core.enums.EnumColor;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * 任职变更类型枚举
 * </p>
 *
 * <p>
 * 本枚举是**字典源**：承载字段在 AO/DTO 里仍是 {@code Integer}，JSON 下发数字码，
 * 前端经 {@code /org/app/enums} 按码值取文案/语义色。码值与
 * {@code org_position_history.change_type} 保持一致（1 入职 / 2 晋升 / 3 降职 /
 * 4 转岗 / 5 兼职 / 6 卸任 / 7 离职）。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@AppEnum(code = "OrgPositionChangeType")
@Getter
@AllArgsConstructor
public enum OrgPositionChangeTypeEnum implements BaseEnum<Integer> {

    /**
     * 入职
     */
    ONBOARD(1, "入职", "Onboard", EnumColor.SUCCESS),

    /**
     * 晋升
     */
    PROMOTION(2, "晋升", "Promotion", EnumColor.PRIMARY),

    /**
     * 降职
     */
    DEMOTION(3, "降职", "Demotion", EnumColor.WARNING),

    /**
     * 转岗
     */
    TRANSFER(4, "转岗", "Transfer", EnumColor.PRIMARY),

    /**
     * 兼职
     */
    PART_TIME(5, "兼职", "Part-time", EnumColor.MUTED),

    /**
     * 卸任
     */
    RESIGN(6, "卸任", "Step down", EnumColor.MUTED),

    /**
     * 离职
     */
    LEAVE(7, "离职", "Resignation", EnumColor.DANGER);

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
