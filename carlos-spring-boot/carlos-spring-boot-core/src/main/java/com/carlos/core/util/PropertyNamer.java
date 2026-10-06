package com.carlos.core.util;

import cn.hutool.core.util.StrUtil;
import com.carlos.core.exception.ComponentException;

/**
 * <p>
 * 属性名称工具类
 * </p>
 *
 * @author carlos
 * @date 2021/12/23 11:11
 */
public class PropertyNamer {

    public static final String GET = "get";
    public static final String SET = "set";
    public static final String IS = "is";

    private PropertyNamer() {
        // Prevent Instantiation of Static Class
    }

    /**
     * 根据方法名获取属性名
     *
     * @param name 方法名
     * @return java.lang.String
     * @author carlos
     * @date 2021/12/23 11:12
     */
    public static String methodToProperty(String name) {
        if (name.startsWith(IS)) {
            name = name.substring(2);
        } else if (name.startsWith(GET) || name.startsWith(SET)) {
            name = name.substring(3);
        } else {
            throw new ComponentException("Error parsing property name '" + name + "'.  Didn't start with 'is', 'get' or 'set'.");
        }
        // 首字母小写
        name = StrUtil.lowerFirst(name);
        return name;
    }

    /**
     * 是否是属性方法
     *
     * @param name 方法名称
     * @return boolean
     * @author carlos
     * @date 2021/12/23 11:14
     */
    public static boolean isProperty(String name) {
        return isGetter(name) || isSetter(name);
    }

    /**
     * 是否是get方法
     *
     * @param name 方法名称
     * @return boolean
     * @author carlos
     * @date 2021/12/23 11:14
     */
    public static boolean isGetter(String name) {
        return (name.startsWith(GET) && name.length() > 3) || (name.startsWith(IS) && name.length() > 2);
    }

    /**
     * 是否是set方法
     *
     * @param name 方法名称
     * @return boolean
     * @author carlos
     * @date 2021/12/23 11:13
     */
    public static boolean isSetter(String name) {
        return name.startsWith(SET) && name.length() > 3;
    }
}
