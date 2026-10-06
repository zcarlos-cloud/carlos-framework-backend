package com.carlos.boot.translation.cache;

import java.io.Serializable;

/**
 * <p>
 * 缓存 Key 生成器
 * </p>
 *
 * @author carlos
 * @date 2025/01/20
 */
public final class CacheKeys {

    private static final String PREFIX = "framework:translation:";

    private CacheKeys() {
        // 工具类
    }

    /**
     * 用户缓存Key
     *
     * @param id 用户ID
     * @return 缓存Key
     */
    public static String userKey(Serializable id) {
        return PREFIX + "user:" + id;
    }

    /**
     * 字典缓存Key
     *
     * @param type 字典类型
     * @param code 字典编码
     * @return 缓存Key
     */
    public static String dictKey(String type, String code) {
        return PREFIX + "dict:" + type + ":" + code;
    }

    /**
     * 部门缓存Key
     *
     * @param id 部门ID
     * @return 缓存Key
     */
    public static String deptKey(Serializable id) {
        return PREFIX + "dept:" + id;
    }

    /**
     * 区域缓存Key
     *
     * @param code 区域编码
     * @return 缓存Key
     */
    public static String regionKey(String code) {
        return PREFIX + "region:" + code;
    }
}
