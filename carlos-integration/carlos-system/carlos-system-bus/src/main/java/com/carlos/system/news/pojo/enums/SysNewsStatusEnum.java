package com.carlos.system.news.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.carlos.core.enums.AppEnum;
import com.carlos.core.enums.BaseEnum;
import com.carlos.core.enums.EnumColor;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * 公告状态枚举
 * </p>
 *
 * @author carlos
 * @since 3.0.0
 */
@AppEnum(code = "SysNewsStatus")
@Getter
@AllArgsConstructor
public enum SysNewsStatusEnum implements BaseEnum {

    /**
     * 草稿
     */
    DRAFT(1, "草稿", "Draft", EnumColor.MUTED),
    /**
     * 已发布
     */
    PUBLISHED(2, "已发布", "Published", EnumColor.SUCCESS),
    /**
     * 已下线
     */
    OFFLINE(3, "已下线", "Offline", EnumColor.DANGER);


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
