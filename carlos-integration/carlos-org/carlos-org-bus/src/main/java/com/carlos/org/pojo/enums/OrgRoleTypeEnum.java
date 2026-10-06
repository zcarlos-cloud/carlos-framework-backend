package com.carlos.org.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.carlos.core.enums.AppEnum;
import com.carlos.core.enums.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * 角色类型枚举
 * </p>
 *
 * @author carlos
 * @date 2026-03-03
 */
@AppEnum(code = "OrgRoleType")
@Getter
@AllArgsConstructor
public enum OrgRoleTypeEnum implements BaseEnum {

    /**
     * 系统角色
     */
    SYSTEM(1, "系统角色"),

    /**
     * 自定义角色
     */
    CUSTOM(2, "自定义角色");

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
