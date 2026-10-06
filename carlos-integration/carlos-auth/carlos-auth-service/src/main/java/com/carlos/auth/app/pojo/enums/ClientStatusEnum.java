package com.carlos.auth.app.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.carlos.core.enums.AppEnum;
import com.carlos.core.enums.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 *   应用状态枚举
 * </p>
 *
 * @author Carlos
 * @date 2025-03-14 15:32
 */
@AppEnum(code = "ClientState")
@Getter
@AllArgsConstructor
public enum ClientStatusEnum implements BaseEnum {

    /**
     * 开启
     */
    ENABLE(1, "启用"),
    DISABLE(2, "禁用"),
    LOGGED_OUT(3, "已注销"),
    ;


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
