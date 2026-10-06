package com.carlos.monitor.util;

import cn.hutool.core.util.StrUtil;
import com.carlos.json.JsonUtils;
import com.carlos.redis.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.type.TypeReference;

/**
 * <p>
 * 监控模块 Redis 缓存读取工具（类型安全）
 * </p>
 * <p>
 * 背景：框架 RedisTemplate 的 value 序列化器（Jackson 带 default typing +
 * Long 序列化为 String）在反序列化小数字/原生 INCR 计数时会产生
 * Integer/String 与调用点期望的 Long 不一致（ClassCastException）。
 * 本工具收敛全部读取点：数字统一经 Number/String 宽容解析，
 * 对象统一按「已是实例→直转；JSON 字符串→JsonUtils 显式反序列化」处理，
 * 与底层序列化器配置（JACKSON/FASTJSON/KRYO）解耦
 * </p>
 *
 * @author Carlos
 * @date 2026年10月7日
 */
@Slf4j
public final class MonitorCacheUtil {

    private MonitorCacheUtil() {
    }

    /**
     * 读 Long（兼容 Integer/Long/String 三种实际存储形态；解析失败返回 null）
     *
     * @param key 缓存键
     * @return Long 值，不存在或不可解析返回 null
     */
    public static Long getLong(String key) {
        Object value = RedisUtil.getValue(key);
        return toLong(value);
    }

    /**
     * 读 Long 带默认值
     */
    public static long getLong(String key, long defaultValue) {
        Long value = getLong(key);
        return value == null ? defaultValue : value;
    }

    /**
     * 读 Double（兼容 Number/String；解析失败返回 null）
     */
    public static Double getDouble(String key) {
        Object value = RedisUtil.getValue(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text) {
            try {
                return Double.parseDouble(text.trim());
            } catch (NumberFormatException e) {
                log.warn("Redis 数值解析失败, key:{}, value:{}", key, text);
                return null;
            }
        }
        log.warn("Redis 数值类型不符, key:{}, type:{}", key, value.getClass().getName());
        return null;
    }

    /**
     * 读 String（任何非空值按 String.valueOf 返回）
     */
    public static String getString(String key) {
        Object value = RedisUtil.getValue(key);
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 读对象（目标类型）：已是实例直转；JSON 字符串显式反序列化；
     * 其他形态（如 Map，来自不带类型信息的序列化器）先转 JSON 再反序列化。
     * 解析失败返回 null（按缓存未命中处理，调用方回源重建）
     */
    public static <T> T getObject(String key, Class<T> clazz) {
        Object value = RedisUtil.getValue(key);
        if (value == null) {
            return null;
        }
        if (clazz.isInstance(value)) {
            return clazz.cast(value);
        }
        try {
            String json = value instanceof String text ? text : JsonUtils.toJson(value);
            return JsonUtils.fromJson(json, clazz);
        } catch (Exception e) {
            log.warn("Redis 对象反序列化失败（按未命中处理）, key:{}, type:{}, error:{}",
                key, value.getClass().getName(), e.getMessage());
            return null;
        }
    }

    /**
     * 读对象（泛型，如 Paging&lt;InstanceVO&gt;）：语义同 {@link #getObject(String, Class)}
     */
    public static <T> T getObject(String key, TypeReference<T> typeReference) {
        Object value = RedisUtil.getValue(key);
        if (value == null) {
            return null;
        }
        if (value instanceof String text) {
            try {
                return JsonUtils.fromJson(text, typeReference);
            } catch (Exception e) {
                log.warn("Redis 泛型对象反序列化失败（按未命中处理）, key:{}, error:{}", key, e.getMessage());
                return null;
            }
        }
        try {
            // 已是目标形态（带类型信息序列化器还原）或中间形态（Map/List）：
            // 统一经 toJson → fromJson 归一到目标泛型
            return JsonUtils.fromJson(JsonUtils.toJson(value), typeReference);
        } catch (Exception e) {
            log.warn("Redis 泛型对象反序列化失败（按未命中处理）, key:{}, type:{}, error:{}",
                key, value.getClass().getName(), e.getMessage());
            return null;
        }
    }

    /**
     * 写对象（统一为无类型信息的纯 JSON 字符串，与读取侧配对）
     *
     * @param timeoutSeconds 过期时间（秒）
     */
    public static void setObject(String key, Object value, long timeoutSeconds) {
        RedisUtil.setValue(key, JsonUtils.toJson(value), timeoutSeconds);
    }

    private static Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            try {
                return Long.parseLong(text.trim());
            } catch (NumberFormatException e) {
                log.warn("Redis 数值解析失败, value:{}", text);
                return null;
            }
        }
        log.warn("Redis 数值类型不符, type:{}", value.getClass().getName());
        return null;
    }
}
