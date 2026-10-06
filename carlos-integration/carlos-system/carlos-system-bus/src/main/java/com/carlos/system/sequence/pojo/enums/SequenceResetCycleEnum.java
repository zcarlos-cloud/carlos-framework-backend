package com.carlos.system.sequence.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.carlos.core.enums.AppEnum;
import com.carlos.core.enums.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * 序列号重置周期枚举
 * </p>
 *
 * <p>决定 Redis 计数键的过期时间与日期段切换时的计数归零行为。</p>
 *
 * @author carlos
 * @since 3.0.0
 */
@AppEnum(code = "SequenceResetCycle")
@Getter
@AllArgsConstructor
public enum SequenceResetCycleEnum implements BaseEnum {

    /**
     * 永不重置（无日期段的规则必须用此项，否则流水号会跨周期重复）
     */
    NEVER(1, "永不重置"),
    /**
     * 按日重置
     */
    DAILY(2, "按日重置"),
    /**
     * 按月重置
     */
    MONTHLY(3, "按月重置"),
    /**
     * 按年重置
     */
    YEARLY(4, "按年重置");


    @EnumValue
    private final Integer code;

    private final String desc;


    @Override
    public Integer getCode() {
        return code;
    }

    @Override
    public String getDesc() {
        return desc;
    }

    /**
     * 按编码解析重置周期
     *
     * @param code 枚举编码
     * @return 枚举，无匹配返回 null
     */
    public static SequenceResetCycleEnum of(Integer code) {
        if (code == null) {
            return null;
        }
        for (SequenceResetCycleEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
