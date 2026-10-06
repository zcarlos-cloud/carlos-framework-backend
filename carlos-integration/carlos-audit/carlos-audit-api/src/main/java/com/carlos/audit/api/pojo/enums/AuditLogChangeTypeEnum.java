package com.carlos.audit.api.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.carlos.core.enums.AppEnum;
import com.carlos.core.enums.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * 审计日志变更类型枚举
 * </p>
 *
 * @author Carlos
 * @date 2026-03-06
 */
@AppEnum(code = "AuditLogChangeType")
@Getter
@AllArgsConstructor
public enum AuditLogChangeTypeEnum implements BaseEnum {

    /**
     * 新增
     */
    ADDED(1, "新增"),

    /**
     * 修改
     */
    MODIFIED(2, "修改"),

    /**
     * 删除
     */
    REMOVED(3, "删除");

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
