package com.carlos.audit.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.carlos.core.enums.AppEnum;
import com.carlos.core.enums.BaseEnum;
import com.carlos.core.enums.EnumColor;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * 审计日志归档状态枚举
 * </p>
 *
 * @author Carlos
 * @date 2026-03-06
 */
@AppEnum(code = "AuditLogArchiveState")
@Getter
@AllArgsConstructor
public enum AuditLogArchiveStateEnum implements BaseEnum {

    /**
     * 归档中
     */
    ARCHIVING(1, "归档中", "Archiving", EnumColor.PRIMARY),

    /**
     * 归档完成
     */
    COMPLETED(2, "归档完成", "Completed", EnumColor.SUCCESS),

    /**
     * 归档失败
     */
    FAILED(3, "归档失败", "Failed", EnumColor.DANGER),

    /**
     * 已删除
     */
    DELETED(4, "已删除", "Deleted", EnumColor.MUTED),

    /**
     * 已恢复
     */
    RESTORED(5, "已恢复", "Restored", EnumColor.WARNING);

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
