package com.carlos.system.news.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.carlos.core.enums.AppEnum;
import com.carlos.core.enums.BaseEnum;
import com.carlos.core.enums.EnumColor;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * 公告类型枚举
 * </p>
 *
 * @author carlos
 * @since 3.0.0
 */
@AppEnum(code = "SysNewsType")
@Getter
@AllArgsConstructor
public enum SysNewsTypeEnum implements BaseEnum {

    /**
     * 公告
     */
    ANNOUNCEMENT(1, "公告", "Announcement", EnumColor.PRIMARY),
    /**
     * 通知
     */
    NOTICE(2, "通知", "Notice", EnumColor.WARNING),
    /**
     * 更新日志
     */
    CHANGELOG(3, "更新日志", "Changelog", EnumColor.MUTED);


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
