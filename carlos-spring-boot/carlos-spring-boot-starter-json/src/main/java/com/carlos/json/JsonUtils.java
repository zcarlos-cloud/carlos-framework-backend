package com.carlos.json;

import com.carlos.json.config.JsonProperties;
import com.carlos.json.config.JsonProperties.JsonEngineType;
import com.carlos.json.core.JsonService;
import lombok.experimental.UtilityClass;
import tools.jackson.core.type.TypeReference;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * JSON 工具类
 * </p>
 *
 * <p>提供统一的静态方法访问 JSON 序列化/反序列化功能</p>
 *
 * @author carlos
 * @date 2025/01/15
 */
@UtilityClass
public class JsonUtils {

    // =========================== 默认服务获取 ===========================

    /**
     * 获取默认的 JSON 服务
     *
     * @return JsonService
     */
    private static JsonService getService() {
        return JsonFactory.getService();
    }

    // =========================== 序列化方法 ===========================

    /**
     * 将对象序列化为 JSON 字符串
     *
     * @param object 待序列化对象
     * @return JSON 字符串
     */
    public static String toJson(Object object) {
        return getService().toJson(object);
    }

    /**
     * 将对象序列化为格式化的 JSON 字符串
     *
     * @param object 待序列化对象
     * @return 格式化的 JSON 字符串
     */
    public static String toJsonPretty(Object object) {
        return getService().toJsonPretty(object);
    }

    /**
     * 将对象序列化为 JSON 字节数组
     *
     * @param object 待序列化对象
     * @return JSON 字节数组
     */
    public static byte[] toJsonBytes(Object object) {
        return getService().toJsonBytes(object);
    }

    /**
     * 将对象序列化为 JSON 字符串，包含 null 值
     *
     * @param object 待序列化对象
     * @return JSON 字符串
     */
    public static String toJsonWithNulls(Object object) {
        return getService().toJsonWithNulls(object);
    }

    // =========================== 反序列化方法 ===========================

    /**
     * 将 JSON 字符串反序列化为对象
     *
     * @param json  JSON 字符串
     * @param clazz 目标类型
     * @param <T>   泛型
     * @return 反序列化后的对象
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        return getService().fromJson(json, clazz);
    }

    /**
     * 将 JSON 字符串反序列化为对象（支持泛型）
     *
     * @param json          JSON 字符串
     * @param typeReference 类型引用
     * @param <T>           泛型
     * @return 反序列化后的对象
     */
    public static <T> T fromJson(String json, TypeReference<T> typeReference) {
        return getService().fromJson(json, typeReference);
    }

    /**
     * 从字节数组反序列化对象
     *
     * @param bytes JSON 字节数组
     * @param clazz 目标类型
     * @param <T>   泛型
     * @return 反序列化后的对象
     */
    public static <T> T fromJson(byte[] bytes, Class<T> clazz) {
        return getService().fromJson(bytes, clazz);
    }

    /**
     * 从字节数组反序列化对象（支持泛型）
     *
     * @param bytes         JSON 字节数组
     * @param typeReference 类型引用
     * @param <T>           泛型
     * @return 反序列化后的对象
     */
    public static <T> T fromJson(byte[] bytes, TypeReference<T> typeReference) {
        return getService().fromJson(bytes, typeReference);
    }

    /**
     * 从输入流反序列化对象
     *
     * @param inputStream 输入流
     * @param clazz       目标类型
     * @param <T>         泛型
     * @return 反序列化后的对象
     */
    public static <T> T fromJson(InputStream inputStream, Class<T> clazz) {
        return getService().fromJson(inputStream, clazz);
    }

    // =========================== 集合处理方法 ===========================

    /**
     * 将 JSON 字符串反序列化为 List
     *
     * @param json         JSON 字符串
     * @param elementClass 元素类型
     * @param <T>          泛型
     * @return List 集合
     */
    public static <T> List<T> toList(String json, Class<T> elementClass) {
        return getService().toList(json, elementClass);
    }

    /**
     * 将 JSON 字符串反序列化为 Map
     *
     * @param json JSON 字符串
     * @return Map 对象
     */
    public static Map<String, Object> toMap(String json) {
        return getService().toMap(json);
    }

    /**
     * 将 JSON 字符串反序列化为指定类型的 Map
     *
     * @param json       JSON 字符串
     * @param valueClass 值类型
     * @param <V>        泛型
     * @return Map 对象
     */
    public static <V> Map<String, V> toMap(String json, Class<V> valueClass) {
        return getService().toMap(json, valueClass);
    }

    /**
     * 将对象转换为 Map
     *
     * @param object 待转换对象
     * @return Map 对象
     */
    public static Map<String, Object> objectToMap(Object object) {
        return getService().objectToMap(object);
    }

    /**
     * 将 Map 转换为对象
     *
     * @param map   Map 对象
     * @param clazz 目标类型
     * @param <T>   泛型
     * @return 转换后的对象
     */
    public static <T> T mapToObject(Map<String, Object> map, Class<T> clazz) {
        return getService().mapToObject(map, clazz);
    }

    // =========================== 类型转换方法 ===========================

    /**
     * 对象类型转换
     *
     * @param fromValue   源对象
     * @param toValueType 目标类型
     * @param <T>         泛型
     * @return 转换后的对象
     */
    public static <T> T convertValue(Object fromValue, Class<T> toValueType) {
        return getService().convertValue(fromValue, toValueType);
    }

    /**
     * 对象类型转换（支持泛型）
     *
     * @param fromValue      源对象
     * @param toValueTypeRef 目标类型引用
     * @param <T>            泛型
     * @return 转换后的对象
     */
    public static <T> T convertValue(Object fromValue, TypeReference<T> toValueTypeRef) {
        return getService().convertValue(fromValue, toValueTypeRef);
    }

    // =========================== 验证和检查方法 ===========================

    /**
     * 检查字符串是否为有效的 JSON
     *
     * @param json JSON 字符串
     * @return true 如果有效
     */
    public static boolean isValidJson(String json) {
        return getService().isValidJson(json);
    }

    /**
     * 检查对象是否可序列化
     *
     * @param value 待检查对象
     * @return true 如果可序列化
     */
    public static boolean canSerialize(Object value) {
        return getService().canSerialize(value);
    }

    // =========================== 节点操作方法 ===========================

    /**
     * 从 JSON 字符串中提取指定路径的值
     *
     * @param json JSON 字符串
     * @param path 路径表达式（如：user.address.city）
     * @return 提取的值
     */
    public static Object extract(String json, String path) {
        return getService().extract(json, path);
    }

    /**
     * 从 JSON 字符串中提取指定路径的值并转换为指定类型
     *
     * @param json  JSON 字符串
     * @param path  路径表达式
     * @param clazz 目标类型
     * @param <T>   泛型
     * @return 提取的值
     */
    public static <T> T extract(String json, String path, Class<T> clazz) {
        return getService().extract(json, path, clazz);
    }

    /**
     * 合并两个 JSON 对象
     *
     * @param json1 第一个 JSON 字符串
     * @param json2 第二个 JSON 字符串
     * @return 合并后的 JSON 字符串
     */
    public static String merge(String json1, String json2) {
        return getService().merge(json1, json2);
    }

    // =========================== 引擎相关方法 ===========================

    /**
     * 设置使用的 JSON 引擎
     *
     * @param engineType 引擎类型
     */
    public static void setEngine(JsonEngineType engineType) {
        JsonFactory.getService(engineType);
    }

    /**
     * 设置全局配置
     *
     * @param properties 配置属性
     */
    public static void setProperties(JsonProperties properties) {
        JsonFactory.setGlobalProperties(properties);
    }

    /**
     * 获取当前使用的 JSON 引擎名称
     *
     * @return 引擎名称
     */
    public static String getEngineName() {
        return getService().getEngineName();
    }

    /**
     * 获取底层 JSON 引擎实例
     *
     * @return 引擎实例
     */
    public static Object getEngine() {
        return getService().getEngine();
    }

    // =========================== 便捷方法 ===========================

    /**
     * 深拷贝对象
     *
     * @param source 源对象
     * @param <T>    泛型
     * @return 深拷贝后的对象
     */
    @SuppressWarnings("unchecked")
    public static <T> T deepCopy(T source) {
        if (source == null) {
            return null;
        }
        String json = toJson(source);
        return (T) fromJson(json, source.getClass());
    }

    /**
     * 深拷贝对象（支持泛型）
     *
     * @param source      源对象
     * @param targetClass 目标类型
     * @param <T>         泛型
     * @return 深拷贝后的对象
     */
    public static <T> T deepCopy(Object source, Class<T> targetClass) {
        if (source == null) {
            return null;
        }
        String json = toJson(source);
        return fromJson(json, targetClass);
    }

    /**
     * 比较两个对象是否相等（通过 JSON 序列化后比较）
     *
     * @param obj1 对象1
     * @param obj2 对象2
     * @return true 如果相等
     */
    public static boolean equals(Object obj1, Object obj2) {
        if (obj1 == obj2) {
            return true;
        }
        if (obj1 == null || obj2 == null) {
            return false;
        }
        return toJson(obj1).equals(toJson(obj2));
    }

    /**
     * 美化 JSON 字符串
     *
     * @param json 原始 JSON 字符串
     * @return 格式化后的 JSON 字符串
     */
    public static String prettify(String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }
        Object obj = fromJson(json, Object.class);
        return toJsonPretty(obj);
    }

    /**
     * 压缩 JSON 字符串（去除多余空格）
     *
     * @param json 原始 JSON 字符串
     * @return 压缩后的 JSON 字符串
     */
    public static String compact(String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }
        Object obj = fromJson(json, Object.class);
        return toJson(obj);
    }
}
