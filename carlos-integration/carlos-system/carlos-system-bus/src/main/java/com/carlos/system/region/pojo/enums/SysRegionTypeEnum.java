package com.carlos.system.region.pojo.enums;

import com.carlos.core.enums.AppEnum;
import com.carlos.core.enums.BaseEnum;
import com.carlos.core.enums.EnumColor;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * 行政区域类型枚举
 * </p>
 *
 * <p>
 * 本枚举是**字典源**：承载字段 {@code SysRegion.regionType} 仍是 {@code String}，
 * JSON 下发字符串码，前端经 {@code /sys/app/enums} 按码值取文案/语义色。
 * 码值与库表保持一致（0 村下级 / 1 社区·村 / 2 街道·乡镇 / 3 区县 / 4 市 / 5 省·直辖市）。
 * </p>
 *
 * <p>
 * ⚠️ 两点说明：
 * <ul>
 *   <li>{@code regionType} 是<b>派生字段</b>：{@code SysRegionService#setRegionType}
 *       在新增与修改时按上级区域重新推导，调用方传什么都会被覆盖；</li>
 *   <li>层级无业务语义，颜色仅作视觉区分：按「细 → 粗」递进
 *       （MUTED → SUCCESS → PRIMARY → WARNING → DANGER），最细的两级（0/1）共用 MUTED。
 *       由前端 6 档 antd 预设色收敛到 5 档语义色，展示与原表略有差异。</li>
 * </ul>
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@AppEnum(code = "SysRegionType")
@Getter
@AllArgsConstructor
public enum SysRegionTypeEnum implements BaseEnum<String> {

    /**
     * 村下级
     */
    VILLAGE_SUB("0", "村下级", "Sub-village", EnumColor.MUTED),

    /**
     * 社区·村
     */
    VILLAGE("1", "社区·村", "Community / Village", EnumColor.MUTED),

    /**
     * 街道·乡镇
     */
    STREET("2", "街道·乡镇", "Street / Township", EnumColor.SUCCESS),

    /**
     * 区县
     */
    DISTRICT("3", "区县", "District / County", EnumColor.PRIMARY),

    /**
     * 市
     */
    CITY("4", "市", "City", EnumColor.WARNING),

    /**
     * 省·直辖市
     */
    PROVINCE("5", "省·直辖市", "Province / Municipality", EnumColor.DANGER);

    private final String code;

    private final String desc;

    /** 英文描述：随 /app/enums 下发，前端按当前语言取用 */
    private final String descEn;

    /** 语义色：随 /app/enums 下发，避免前端为每个枚举值维护颜色表 */
    private final EnumColor color;

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getDesc() {
        return desc;
    }

}
