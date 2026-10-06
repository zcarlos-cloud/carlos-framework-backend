package com.carlos.audit.api.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.carlos.core.enums.AppEnum;
import com.carlos.core.enums.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * 审计日志业务渠道枚举
 * </p>
 *
 * @author Carlos
 * @date 2026-03-06
 */
@AppEnum(code = "AuditLogBizChannel")
@Getter
@AllArgsConstructor
public enum AuditLogBizChannelEnum implements BaseEnum {

    /**
     * Web端
     */
    WEB(1, "Web端", "Web"),

    /**
     * App移动端
     */
    APP(2, "App移动端", "App"),

    /**
     * 小程序
     */
    MINI_PROGRAM(3, "小程序", "Mini Program"),

    /**
     * 开放API
     */
    OPEN_API(4, "开放API", "Open API"),

    /**
     * 桌面客户端
     */
    DESKTOP(5, "桌面客户端", "Desktop"),

    /**
     * H5页面
     */
    H5(6, "H5页面", "H5"),

    /**
     * 系统后台
     */
    BACKGROUND(7, "系统后台", "Background");

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
