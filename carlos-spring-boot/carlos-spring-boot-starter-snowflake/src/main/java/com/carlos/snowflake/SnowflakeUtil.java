package com.carlos.snowflake;


import cn.hutool.core.lang.Snowflake;

/**
 * <p>
 * 雪花算法工具类
 * </p>
 *
 * @author carlos
 * @date 2021/10/2 0:13
 */
public class SnowflakeUtil {

    private static Snowflake snowflake;

    /**
     * 初始化雪花算法工具类
     * <p>
     * 由 {@link SnowflakeConfig} 在自动配置时调用
     * </p>
     *
     * @param snowflake Snowflake 实例
     */
    public static void init(Snowflake snowflake) {
        SnowflakeUtil.snowflake = snowflake;
    }

    /**
     * 获取长整型的id
     *
     * @return long
     * @author carlos
     * @date 2020/10/26 0:00
     */
    public static long longId() {
        return snowflake.nextId();
    }

    /**
     * 获取字符串id
     *
     * @return java.lang.String
     * @author carlos
     * @date 2020/10/26 0:00
     */
    public static String strId() {
        return snowflake.nextIdStr();
    }

}
