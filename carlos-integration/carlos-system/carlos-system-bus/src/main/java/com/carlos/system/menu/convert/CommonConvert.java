package com.carlos.system.menu.convert;

import org.mapstruct.Named;

import java.io.Serializable;

/**
 * <p>
 * 通用转换器
 * </p>
 *
 * @author carlos
 * @date 2023-7-7 14:19:55
 */
@Named("CommonConvert")
public class CommonConvert {

    /**
     * 样例方法
     *
     * @param id source字段绑定的参数
     * @return java.lang.String
     * @author carlos
     * @date 2023-7-7 14:19:55
     */
    @Named("toName")
    public String toName(Serializable id) {
        if (id == null) {
            return null;
        }
        // SysMenuManager menuManager = SpringUtil.getBean(SysMenuManager.class);
        // return menuManager.getNameById(menuId);
        return "";
    }

}
