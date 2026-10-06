package com.carlos.audit.api.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.carlos.core.enums.AppEnum;
import com.carlos.core.enums.BaseEnum;
import com.carlos.core.enums.EnumColor;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * 审计日志大类枚举
 * </p>
 *
 * @author Carlos
 * @date 2026-03-06
 */
@AppEnum(code = "AuditLogCategory")
@Getter
@AllArgsConstructor
public enum AuditLogCategoryEnum implements BaseEnum {

    /**
     * 安全类
     */
    SECURITY(1, "安全类", "Security", EnumColor.DANGER),

    /**
     * 业务类
     */
    BUSINESS(2, "业务类", "Business", EnumColor.PRIMARY),

    /**
     * 系统类
     */
    SYSTEM(3, "系统类", "System", EnumColor.WARNING),

    /**
     * 审计类
     */
    AUDIT(4, "审计类", "Audit", EnumColor.SUCCESS);

    @EnumValue
    private final Integer code;

    private final String desc;

    /** 英文描述：随 /app/enums 下发，前端按当前语言取用 */
    private final String descEn;

    /** 语义色：随 /app/enums 下发，避免前端为每个枚举值维护颜色表 */
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
