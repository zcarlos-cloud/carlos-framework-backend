package com.carlos.core.util;

import com.carlos.core.enums.BaseEnum;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 * 枚举工具类
 * </p>
 *
 * @author carlos
 * @date 2026/04/03
 */
@Slf4j
public class EnumUtil {

    /**
     * 枚举缓存：key = 枚举类全名，value = 枚举数组
     */
    private static final Map<String, BaseEnum<?>[]> ENUM_CACHE = new ConcurrentHashMap<>();

    /**
     * Code 映射缓存：key = 枚举类全名 + ":" + code，value = 枚举实例
     */
    private static final Map<String, BaseEnum<?>> CODE_MAPPING_CACHE = new ConcurrentHashMap<>();

    /**
     * 根据 code 获取枚举实例
     *
     * @param enumClass 枚举类型
     * @param code      枚举 code
     * @return 匹配的枚举实例，未找到返回 null
     */
    @SuppressWarnings("unchecked")
    public static <T extends BaseEnum<?>> T getByCode(Class<T> enumClass, Object code) {
        if (enumClass == null || code == null) {
            return null;
        }

        // 构建缓存 key
        String cacheKey = enumClass.getName() + ":" + code.toString();

        // 尝试从缓存获取
        return (T) CODE_MAPPING_CACHE.computeIfAbsent(cacheKey, key -> findEnumByCode(enumClass, code));
    }

    /**
     * 查找枚举（实际执行逻辑）
     *
     * @param enumClass 枚举类型
     * @param code      枚举 code
     * @return 匹配的枚举实例，未找到返回 null
     */
    private static <T extends BaseEnum<?>> T findEnumByCode(Class<T> enumClass, Object code) {
        try {
            // 获取或加载枚举数组
            @SuppressWarnings("unchecked")
            T[] enums = (T[]) getEnums(enumClass);

            // 遍历匹配
            for (T enumItem : enums) {
                Object enumCode = enumItem.getCode();

                // 处理不同类型的 code 比较
                if (compareCodes(enumCode, code)) {
                    log.debug("Found enum: {} for code: {}", enumItem, code);
                    return enumItem;
                }
            }

            log.debug("Enum not found for code: {} in class: {}", code, enumClass.getSimpleName());
            return null;

        } catch (Exception e) {
            log.error("Error getting enum by code: {} in class: {}", code, enumClass.getSimpleName(), e);
            return null;
        }
    }

    /**
     * 获取枚举数组（带缓存）
     *
     * @param enumClass 枚举类型
     * @return 枚举数组
     */
    private static <T extends BaseEnum<?>> BaseEnum<?>[] getEnums(Class<T> enumClass) {
        return ENUM_CACHE.computeIfAbsent(enumClass.getName(), key -> {
            BaseEnum<?>[] enums = enumClass.getEnumConstants();
            if (enums == null || enums.length == 0) {
                log.warn("No enum constants found for class: {}", enumClass.getName());
                return new BaseEnum[0];
            }
            log.debug("Loaded {} enum constants for: {}", enums.length, enumClass.getSimpleName());
            return enums;
        });
    }

    /**
     * 比较两个 code 是否相等（支持多种类型）
     *
     * @param enumCode 枚举中的 code
     * @param targetCode 目标 code
     * @return 是否相等
     */
    private static boolean compareCodes(Object enumCode, Object targetCode) {
        if (enumCode == null) {
            return targetCode == null;
        }

        // 如果类型相同，直接比较
        if (enumCode.getClass().equals(targetCode.getClass())) {
            return enumCode.equals(targetCode);
        }

        // 类型不同，尝试转换为字符串比较
        return enumCode.toString().equals(targetCode.toString());
    }

    /**
     * 清除枚举缓存
     * 当枚举定义发生变化时调用此方法
     */
    public static void clearCache() {
        ENUM_CACHE.clear();
        CODE_MAPPING_CACHE.clear();
        log.info("Enum cache cleared");
    }

    /**
     * 清除指定枚举类型的缓存
     *
     * @param enumClass 枚举类型
     */
    public static void clearCache(Class<? extends BaseEnum<?>> enumClass) {
        if (enumClass != null) {
            String prefix = enumClass.getName();
            ENUM_CACHE.remove(prefix);
            CODE_MAPPING_CACHE.entrySet().removeIf(entry -> entry.getKey().startsWith(prefix + ":"));
            log.debug("Enum cache cleared for: {}", enumClass.getSimpleName());
        }
    }
}
