package com.carlos.org.pojo.enums;

import com.carlos.core.enums.AppEnum;
import com.carlos.core.enums.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * 岗位性质枚举
 * </p>
 *
 * <p>
 * 本枚举是**字典源**：承载字段在 AO/DTO 里仍是 {@code Integer}，JSON 下发数字码，
 * 前端经 {@code /org/app/enums} 按码值取文案/语义色。码值与
 * {@code OrgPosition.positionNature} 保持一致（1 全职 / 2 兼职 / 3 实习 / 4 外包）。
 * 性质不是状态，前端标签统一默认灰，故不声明语义色（默认 MUTED）。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@AppEnum(code = "OrgPositionNature")
@Getter
@AllArgsConstructor
public enum OrgPositionNatureEnum implements BaseEnum<Integer> {

    /**
     * 全职
     */
    FULL_TIME(1, "全职", "Full-time"),

    /**
     * 兼职
     */
    PART_TIME(2, "兼职", "Part-time"),

    /**
     * 实习
     */
    INTERN(3, "实习", "Intern"),

    /**
     * 外包
     */
    OUTSOURCED(4, "外包", "Outsourced");

    private final Integer code;

    private final String desc;

    /** 英文描述：随 /app/enums 下发，前端按当前语言取用 */
    private final String descEn;

    @Override
    public Integer getCode() {
        return code;
    }

    @Override
    public String getDesc() {
        return desc;
    }

}
