package com.carlos.test.convert;

import org.mapstruct.Named;

import java.io.Serializable;

/**
 * <p>
 * 通用转换
 * </p>
 *
 * @author Carlos
 * @date 2023-8-12 11:16:18
 */
@Named("CommonConvert")
public class CommonConvert {

    /**
     * 样例方法
     *
     * @param id source字段绑定的参
     * @return java.lang.String
     * @author Carlos
     * @date 2023-8-12 11:16:18
     */
    @Named("toName")
    public String toName(Serializable id) {
        if (id == null) {
            return null;
        }
        // MenuManager menuManager = SpringUtil.getBean(MenuManager.class);
        // return menuManager.getNameById(menuId);
        return "";
    }

}
