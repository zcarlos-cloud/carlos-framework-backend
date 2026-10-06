package com.carlos.devkit.engine.support;

import cn.hutool.core.util.StrUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * 命名转换工具（表名 → 类名 / 属性名）
 * </p>
 * <p>
 * 迁移自 carlos-tools 原 {@code com.carlos.fx.utils.NameUtil}，逻辑保持逐字一致，
 * 以保证既有 107 个模板的产物不发生命名漂移。
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
public final class NameUtil {

    private static final Pattern PATTERN = Pattern.compile("([A-Za-z\\d]+)(_)?");

    private NameUtil() {
    }

    /**
     * 下划线转驼峰
     *
     * @param line       名字
     * @param smallCamel 是否使用小驼峰
     * @return 驼峰名
     */
    public static String getCamelName(final String line, final boolean smallCamel) {
        if (line == null || line.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        Matcher matcher = PATTERN.matcher(line);
        while (matcher.find()) {
            String word = matcher.group();
            sb.append(smallCamel && matcher.start() == 0
                ? Character.toLowerCase(word.charAt(0))
                : Character.toUpperCase(word.charAt(0)));
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
     * @return 类名前缀（大驼峰）
     */
    public static String getClassNamePrefix(final String tableName, final boolean usePrefix) {
        StringBuilder className = new StringBuilder();
        String[] nameItems = tableName.split("_");
        if (nameItems.length == 1) {
            return className.append(nameItems[0].substring(0, 1).toUpperCase())
                .append(nameItems[0].substring(1)).toString();
        }
        for (int i = 0, len = nameItems.length; i < len; i++) {
            if (!usePrefix && i == 0) {
                continue;
            }
            String nameItem = nameItems[i];
            className.append(nameItem.substring(0, 1).toUpperCase()).append(nameItem.substring(1));
        }
        return className.toString();
    }

    /**
     * 类名 → 属性调用名（首字母小写）
     *
     * @param classPrefix 类名前缀
     * @return 属性前缀
     */
    public static String getClassPropertyNamePrefix(final String classPrefix) {
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
     * @param propertyName 属性名
     * @return 首字母大写后的属性名
     */
    public static String getPropertyNameUp(final String propertyName) {
        char[] chars = propertyName.toCharArray();
        chars[0] -= 32;
        return String.valueOf(chars);
    }

    /**
     * 下划线转小驼峰
     *
     * @param str 下划线名
     * @return 小驼峰名
     */
    public static String underLineToCamel(final String str) {
        return getCamelName(str, true);
    }

    /**
     * 去除前缀并转小驼峰
     *
     * @param str 含前缀名
     * @return 小驼峰名
     */
    public static String delPrefixToCamel(final String str) {
        int index = str.indexOf('_');
        if (index == -1) {
            return str;
        }
        return getCamelName(str.substring(index + 1), true);
    }

    /**
     * 去除第一个下划线前缀
     *
     * @param str 含前缀名
     * @return 去前缀名
     */
    public static String delPrefix(final String str) {
        int index = str.indexOf('_');
        if (index == -1) {
            return str;
        }
        return str.substring(index + 1);
    }

    /**
     * 全大写转小驼峰
     *
     * @param str 全大写名
     * @return 小驼峰名
     */
    public static String upperToLowerAndCamel(final String str) {
        return getCamelName(str.toLowerCase(), true);
    }
}
