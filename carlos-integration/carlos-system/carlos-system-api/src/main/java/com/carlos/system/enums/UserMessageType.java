package com.carlos.system.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.carlos.core.enums.AppEnum;
import com.carlos.core.enums.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author wangze
 * @date 2024/3/28 18:00
 */
@AppEnum(code = "UserMessageType")
@Getter
@AllArgsConstructor
public enum UserMessageType implements BaseEnum {
    /**
     * 数据显示
     */
    MESSAGE(5, "系统公告", "message"),
    COMPLAINTS(6, "投诉建议", "complaints"),
    COLLECTION(7, "采集任务", "collection"),
    CHECK(8, "核查任务", "check"),
    APPLY(9, "需求申请", "apply"),
    ;
    @EnumValue
    private final Integer code;
    private final String desc;
    @Getter
    private final String type;

    @Override
    public Integer getCode() {
        return this.code;
    }

    @Override
    public String getDesc() {
        return this.desc;
    }

}
