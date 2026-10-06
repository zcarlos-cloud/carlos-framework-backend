package com.carlos.json.core;

import tools.jackson.core.type.TypeReference;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * JSON 服务接口 - 统一的 JSON 操作抽象
 * </p>
 *
 * <p>提供与底层 JSON 引擎无关的统一操作接口，支持 Jackson、Fastjson2、Gson 等多种实现</p>
 *
 * @author carlos
 * @date 2025/01/15
 */
public interface JsonService {

    // =========================== 序列化方法 ===========================

    /**
     * 将对象序列化为 JSON 字符串
     *
     * @param object 待序列化对象
     * @return JSON 字符串
     */
    String toJson(Object object);

    /**
     * 将对象序列化为格式化的 JSON 字符串
     *
     * @param object 待序列化对象
     * @return 格式化的 JSON 字符串
     */
    String toJsonPretty(Object object);

    /**
     * 将对象序列化为 JSON 字节数组
     *
     * @param object 待序列化对象
     * @return JSON 字节数组
     */
    byte[] toJsonBytes(Object object);

    /**
     * 将对象序列化为 JSON 字符串，包含 null 值
     *
     * @param object 待序列化对象
     * @return JSON 字符串
     */
    String toJsonWithNulls(Object object);

    // =========================== 反序列化方法 ===========================

    /**
     * 将 JSON 字符串反序列化为对象
     *
     * @param json  JSON 字符串
     * @param clazz 目标类型
     * @param <T>   泛型
     * @return 反序列化后的对象
     */
    <T> T fromJson(String json, Class<T> clazz);

    /**
     * 将 JSON 字符串反序列化为对象（支持泛型）
     *
     * @param json          JSON 字符串
     * @param typeReference 类型引用
     * @param <T>           泛型
     * @return 反序列化后的对象
     */
    <T> T fromJson(String json, TypeReference<T> typeReference);

    /**
     * 从字节数组反序列化对象
     *
     * @param bytes JSON 字节数组
     * @param clazz 目标类型
     * @param <T>   泛型
     * @return 反序列化后的对象
     */
    <T> T fromJson(byte[] bytes, Class<T> clazz);

    /**
     * 从字节数组反序列化对象（支持泛型）
     *
     * @param bytes         JSON 字节数组
     * @param typeReference 类型引用
     * @param <T>           泛型
     * @return 反序列化后的对象
     */
    <T> T fromJson(byte[] bytes, TypeReference<T> typeReference);

    /**
     * 从输入流反序列化对象
     *
     * @param inputStream 输入流
     * @param clazz       目标类型
     * @param <T>         泛型
     * @return 反序列化后的对象
     */
    <T> T fromJson(InputStream inputStream, Class<T> clazz);

    // =========================== 集合处理方法 ===========================

    /**
     * 将 JSON 字符串反序列化为 List
     *
     * @param json        JSON 字符串
     * @param elementClass 元素类型
     * @param <T>         泛型
     * @return List 集合
     */
    <T> List<T> toList(String json, Class<T> elementClass);

    /**
     * 将 JSON 字符串反序列化为 Map
     *
     * @param json JSON 字符串
     * @return Map 对象
     */
    Map<String, Object> toMap(String json);

    /**
     * 将 JSON 字符串反序列化为指定类型的 Map
     *
     * @param json       JSON 字符串
     * @param valueClass 值类型
     * @param <V>        泛型
     * @return Map 对象
     */
    <V> Map<String, V> toMap(String json, Class<V> valueClass);

    /**
     * 将对象转换为 Map
     *
     * @param object 待转换对象
     * @return Map 对象
     */
    Map<String, Object> objectToMap(Object object);

    /**
     * 将 Map 转换为对象
     *
     * @param map   Map 对象
     * @param clazz 目标类型
     * @param <T>   泛型
     * @return 转换后的对象
     */
    <T> T mapToObject(Map<String, Object> map, Class<T> clazz);

    // =========================== 类型转换方法 ===========================

    /**
     * 对象类型转换
     *
     * @param fromValue   源对象
     * @param toValueType 目标类型
     * @param <T>         泛型
     * @return 转换后的对象
     */
    <T> T convertValue(Object fromValue, Class<T> toValueType);

    /**
     * 对象类型转换（支持泛型）
     *
     * @param fromValue      源对象
     * @param toValueTypeRef 目标类型引用
     * @param <T>            泛型
     * @return 转换后的对象
     */
    <T> T convertValue(Object fromValue, TypeReference<T> toValueTypeRef);

    // =========================== 验证和检查方法 ===========================

    /**
     * 检查字符串是否为有效的 JSON
     *
     * @param json JSON 字符串
     * @return true 如果有效
     */
    boolean isValidJson(String json);

    /**
     * 检查对象是否可序列化
     *
     * @param value 待检查对象
     * @return true 如果可序列化
     */
    boolean canSerialize(Object value);

    // =========================== 节点操作方法 ===========================

    /**
     * 从 JSON 字符串中提取指定路径的值
     *
     * @param json JSON 字符串
     * @param path 路径表达式（如：user.address.city）
     * @return 提取的值
     */
    Object extract(String json, String path);

    /**
     * 从 JSON 字符串中提取指定路径的值并转换为指定类型
     *
     * @param json  JSON 字符串
     * @param path  路径表达式
     * @param clazz 目标类型
     * @param <T>   泛型
     * @return 提取的值
     */
    <T> T extract(String json, String path, Class<T> clazz);

    /**
     * 合并两个 JSON 对象
     *
     * @param json1 第一个 JSON 字符串
     * @param json2 第二个 JSON 字符串
     * @return 合并后的 JSON 字符串
     */
    String merge(String json1, String json2);

    // =========================== 引擎相关方法 ===========================

    /**
     * 获取底层 JSON 引擎实例
     *
     * @return 引擎实例（Jackson 返回 ObjectMapper，Fastjson 返回 JSON 等）
     */
    Object getEngine();

    /**
     * 获取当前使用的 JSON 引擎名称
     *
     * @return 引擎名称
     */
    String getEngineName();
}
