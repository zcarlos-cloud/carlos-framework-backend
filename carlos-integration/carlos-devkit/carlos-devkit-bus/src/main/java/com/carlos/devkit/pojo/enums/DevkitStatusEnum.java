package com.carlos.devkit.pojo.enums;

import com.carlos.core.enums.AppEnum;
import com.carlos.core.enums.BaseEnum;
import com.carlos.core.enums.EnumColor;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * 启用状态枚举（{@code dev_template_group.status} / {@code dev_template.status} /
 * {@code dev_datasource.status}）
 * </p>
 * <p>
 * 承载字段为 {@code Integer}（库表 SMALLINT），JSON 下发数字码。
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@AppEnum(code = "DevkitStatus")
@Getter
@AllArgsConstructor
public enum DevkitStatusEnum implements BaseEnum<Integer> {

    /**
     * 启用
     */
    ENABLED(1, "启用", "Enabled", EnumColor.SUCCESS),

    /**
     * 停用
     */
    DISABLED(0, "停用", "Disabled", EnumColor.MUTED);

    private final Integer code;

    private final String desc;

    private final String descEn;

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
