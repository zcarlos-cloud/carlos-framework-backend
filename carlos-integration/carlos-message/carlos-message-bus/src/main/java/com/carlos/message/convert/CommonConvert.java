package com.carlos.message.convert;

import org.mapstruct.Named;

import java.io.Serializable;

/**
 * <p>
 * 通用转换器
 * </p>
 *
 * @author Carlos
 * @date 2026年3月12日 上午11:17:05
 */
@Named("CommonConvert")
public class CommonConvert {

    /**
     * 样例方法
     *
     * @param id source字段绑定的参数
     * @return java.lang.String
     * @author Carlos
     * @date 2026年3月12日 上午11:17:05
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
