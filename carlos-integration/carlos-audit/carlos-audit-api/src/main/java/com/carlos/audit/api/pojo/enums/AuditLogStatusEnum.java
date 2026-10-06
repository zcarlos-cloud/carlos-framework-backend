package com.carlos.audit.api.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.carlos.core.enums.AppEnum;
import com.carlos.core.enums.BaseEnum;
import com.carlos.core.enums.EnumColor;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * 审计日志状态枚举
 * </p>
 *
 * @author Carlos
 * @date 2026-03-06
 */
@AppEnum(code = "AuditLogState")
@Getter
@AllArgsConstructor
public enum AuditLogStatusEnum implements BaseEnum {

    /**
     * 成功
     */
    SUCCESS(1, "成功", "Success", EnumColor.SUCCESS),

    /**
     * 失败
     */
    FAIL(2, "失败", "Failed", EnumColor.DANGER),

    /**
     * 处理中
     */
    PENDING(3, "处理中", "Pending", EnumColor.MUTED),

    /**
     * 超时
     */
    TIMEOUT(4, "超时", "Timeout", EnumColor.WARNING),

    /**
     * 部分成功
     */
    PARTIAL_SUCCESS(5, "部分成功", "Partial Success", EnumColor.PRIMARY);

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
