package com.carlos.system.config;

import cn.hutool.extra.spring.SpringUtil;
import com.carlos.system.menu.manager.SysMenuManager;
import org.mapstruct.Named;

import java.io.Serializable;

/**
 * <p>
 * 通用转换器
 * </p>
 *
 * @author carlos
 * @date 2022-11-8 19:30:24
 */
@Named("CommonConvert")
public class CommonConvert {

    @Named("toMenuName")
    public String toMenuName(final Serializable menuId) {
        if (menuId == null) {
            return null;
        }
        final SysMenuManager menuManager = SpringUtil.getBean(SysMenuManager.class);
        return menuManager.getNameById(menuId);
    }

    @Named("haveChildrenMenu")
    public Boolean haveChildrenMenu(final Serializable menuId) {
        if (menuId == null) {
            return null;
        }
        final SysMenuManager menuManager = SpringUtil.getBean(SysMenuManager.class);
        return menuManager.getSubMenuCount(menuId) > 0L;
    }

}
