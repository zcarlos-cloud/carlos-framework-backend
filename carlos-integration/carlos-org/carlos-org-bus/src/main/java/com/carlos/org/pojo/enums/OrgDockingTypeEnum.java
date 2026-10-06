package com.carlos.org.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.carlos.core.enums.AppEnum;
import com.carlos.core.enums.BaseEnum;
import com.carlos.core.response.CommonErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;


@AppEnum(code = "OrgDockingType")
@Getter
@AllArgsConstructor
public enum OrgDockingTypeEnum implements BaseEnum {

    /**  */
    USER("user", 0, "用户"),
    ROLE("role", 1, "角色"),
    DEPARTMENT("department", 2, "组织机构"),


    ;

    @EnumValue
    private final String name;

    private final Integer code;

    private final String desc;


    public static OrgDockingTypeEnum codeOf(final String name) {
        final OrgDockingTypeEnum[] values = OrgDockingTypeEnum.values();
        for (final OrgDockingTypeEnum value : values) {
            if (value.getName().equals(name)) {
                return value;
            }
        }
        throw CommonErrorCode.PARAM_TYPE_ERROR.exception("数据类型不合法");
    }


}
