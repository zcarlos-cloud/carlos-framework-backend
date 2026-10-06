package com.carlos.system.dict.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.carlos.core.enums.AppEnum;
import com.carlos.core.enums.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * 字典类型枚举
 * </p>
 *
 * @author carlos
 * @date 2021/11/17 23:54
 */
@AppEnum(code = "DictType")
@Getter
@AllArgsConstructor
public enum DictTypeEnum implements BaseEnum {

    /**
     * 开启
     */
    NUMBER(1, "数值类型"),
    STRING(2, "字符串类型");


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
