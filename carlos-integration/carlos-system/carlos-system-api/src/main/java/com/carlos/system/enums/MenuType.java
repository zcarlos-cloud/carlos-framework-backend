package com.carlos.system.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.carlos.core.enums.AppEnum;
import com.carlos.core.enums.BaseEnum;
import com.carlos.core.response.CommonErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 菜单客户端类型（与 sys_menu.client_type 列 VARCHAR 存储值一致）
 *
 * @author Carlos
 * @date 2022/12/22 11:31
 */
@Getter
@AllArgsConstructor
@AppEnum(code = "MenuType")
public enum MenuType implements BaseEnum<String> {

    /**
     * PC端菜单
     */
    PC("PC", "PC端", "PC"),
    /**
     * 移动端菜单
     */
    MOBILE("MOBILE", "移动端", "Mobile"),
    /**
     * 管理端
     */
    MANAGE("MANAGE", "管理端", "Manage"),
    /**
     * 全部端
     */
    ALL("ALL", "全部端", "All"),
    ;

    @EnumValue
    private final String code;

    private final String desc;

    /** 英文描述：随 /app/enums 下发，前端按当前语言取用 */
    private final String descEn;

    public static MenuType ofCode(final String code) {
        final MenuType[] values = MenuType.values();
        for (final MenuType value : values) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        throw CommonErrorCode.NOT_FOUND.exception("Can't find enum of code " + code);
    }
}
