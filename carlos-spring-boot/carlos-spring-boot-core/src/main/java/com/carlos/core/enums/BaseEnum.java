package com.carlos.core.enums;


/**
 * <p>
 * 枚举类型父接口 若需要枚举字典则必须实现该接口
 *
 * </p>
 *
 * @author carlos
 * @date 2020/4/10 15:41
 */
public interface BaseEnum<T> {

    /**
     * 获取枚举标识
     */
    T getCode();

    /**
     * 获取枚举描述
     */
    String getDesc();

    /**
     * 获取英文描述
     *
     * <p>默认回落中文 {@link #getDesc()}。需要双语的枚举只需加一个 {@code descEn} 字段
     * （Lombok {@code @Getter} 会生成同名 getter，从而覆盖本默认实现）。</p>
     */
    default String getDescEn() {
        return getDesc();
    }

    /**
     * 获取语义色档位
     *
     * <p>默认 {@link EnumColor#MUTED}。颜色随枚举一起下发，前端才不必为每个枚举值维护颜色表。</p>
     */
    default EnumColor getColor() {
        return EnumColor.MUTED;
    }

    /**
     * 获取枚举描述信息
     *
     * @return com.carlos.core.enums.EnumVo
     * @author carlos
     * @date 2021/11/25 14:03
     */
    default EnumInfo getEnumVo() {
        EnumInfo vo = new EnumInfo();
        vo.setCode(getCode());
        vo.setDesc(getDesc());
        vo.setName(this.toString());
        vo.setDescEn(getDescEn());
        vo.setColor(getColor());
        return vo;
    }
}
