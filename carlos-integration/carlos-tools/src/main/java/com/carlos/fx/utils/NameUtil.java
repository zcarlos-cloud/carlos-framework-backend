package com.carlos.fx.utils;

import cn.hutool.core.util.StrUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 通用工具类
 *
 * @author Carlos
 * @date 2019/5/9 11:58
 * @ModificationHistory Who  When  What ---------     -------------   --------------------------------------
 */
public class NameUtil {

    private final static Pattern pattern = Pattern.compile("([A-Za-z\\d]+)(_)?");


    /**
     * 下划线转驼峰
     *
     * @param line       名字
     * @param smallCamel 是否使用小驼峰
     * @author Carlos
     * @date 2019/5/9 11:57
     */
    public static String getCamelName(String line, boolean smallCamel) {
        if (line == null || "".equals(line)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        Matcher matcher = pattern.matcher(line);
        while (matcher.find()) {
            String word = matcher.group();
            sb.append(smallCamel && matcher.start() == 0 ? Character.toLowerCase(word.charAt(0)) : Character.toUpperCase(word.charAt(0)));
            int index = word.lastIndexOf('_');
            if (index > 0) {
                sb.append(word, 1, index);
            } else {
                sb.append(word.substring(1));
            }
        }
        return sb.toString();
    }

    /**
     * 根据表名获取类名前缀
     *
     * @param tableName 表名
     * @param usePrefix 是否使用表名前缀
     * @author Carlos
     * @date 2019/12/30 10:06
     */
    public static String getClassNamePrefix(String tableName, boolean usePrefix) {
        StringBuilder className = new StringBuilder();
        String[] nameItems = tableName.split("_");
        if (nameItems.length == 1) {
            return className.append(nameItems[0].substring(0, 1).toUpperCase()).append(nameItems[0].substring(1)).toString();
        }
        String nameItem;
        for (int i = 0, len = nameItems.length; i < len; i++) {
            if (!usePrefix && i == 0) {
                continue;
            }
            nameItem = nameItems[i];
            className.append(nameItem.substring(0, 1).toUpperCase()).append(nameItem.substring(1));
        }
        return className.toString();
    }

    /**
     * 类名的属性调用名称
     *
     * @author Carlos
     * @date 2019/12/30 14:30
     */
    public static String getClassPropertyNamePrefix(String classPrefix) {
        if (StrUtil.isBlank(classPrefix)) {
            return classPrefix;
        }
        char[] chars = classPrefix.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }

    /**
     * 属性名首字母大写
     *
     * @author Carlos
     * @date 2019/12/30 14:58
     */
    public static String getPropertyNameUp(String propertyName) {
        char[] chars = propertyName.toCharArray();
        chars[0] -= 32;
        return String.valueOf(chars);
    }

    /**
     * 下划线转驼峰
     *
     * @author Carlos
     * @date 2020/1/6 13:10
     */
    public static String underLineToCamel(String str) {
        return getCamelName(str, true);
    }

    /**
     * 去除前缀并且转小驼峰
     *
     * @author Carlos
     * @date 2020/1/6 13:15
     */
    public static String delPrefixToCamel(String str) {
        int index = str.indexOf("_");
        if (index == -1) {
            return str;
        }
        String name = str.substring(index + 1);
        return getCamelName(name, true);
    }

    /**
     * 去除前缀
     *
     * @author Carlos
     * @date 2020/1/6 13:16
     */
    public static String delPrefix(String str) {
        int index = str.indexOf("_");
        if (index == -1) {
            return str;
        }
        return str.substring(index + 1);
    }

    /**
     * 小写转大写并且转驼峰
     *
     * @author Carlos
     * @date 2020/1/6 13:17
     */
    public static String upperToLowerAndCamel(String str) {
        return getCamelName(str.toLowerCase(), true);
    }

    /**
     * 根据列名判断是否是乐观锁字段
     *
     * @param columnName 列名
     * @author Carlos
     * @date 2020/1/8 17:13
     */
    public static boolean isVersionField(String columnName) {
        if ("fd_version".equalsIgnoreCase(columnName)) {
            return true;
        }
        if ("version".equalsIgnoreCase(columnName)) {
            return true;
        }
        return false;
    }
}
