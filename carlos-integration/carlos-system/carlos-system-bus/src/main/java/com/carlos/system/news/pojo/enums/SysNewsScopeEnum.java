package com.carlos.system.news.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.carlos.core.enums.AppEnum;
import com.carlos.core.enums.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * 公告发布范围枚举
 * </p>
 *
 * @author carlos
 * @since 3.0.0
 */
@AppEnum(code = "SysNewsScope")
@Getter
@AllArgsConstructor
public enum SysNewsScopeEnum implements BaseEnum {

    /**
     * 全员
     */
    ALL(1, "全员", "All Users"),
    /**
     * 指定角色
     */
    ROLE(2, "指定角色", "By Role");


    @EnumValue
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
