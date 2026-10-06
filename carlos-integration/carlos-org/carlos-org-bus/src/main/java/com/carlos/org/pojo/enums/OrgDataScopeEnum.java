package com.carlos.org.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.carlos.core.enums.AppEnum;
import com.carlos.core.enums.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * 数据权限范围枚举
 * </p>
 *
 * @author carlos
 * @date 2026-03-03
 */
@AppEnum(code = "OrgDataScope")
@Getter
@AllArgsConstructor
public enum OrgDataScopeEnum implements BaseEnum {

    /**
     * 全部数据
     */
    ALL(1, "全部数据"),

    /**
     * 本部门及子部门数据
     */
    DEPT_AND_CHILD(2, "本部门及子部门"),

    /**
     * 仅本部门数据
     */
    DEPT_ONLY(3, "仅本部门"),

    /**
     * 仅本人数据
     */
    SELF_ONLY(4, "仅本人"),

    /**
     * 自定义规则
     */
    CUSTOM(5, "自定义规则");

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

}
