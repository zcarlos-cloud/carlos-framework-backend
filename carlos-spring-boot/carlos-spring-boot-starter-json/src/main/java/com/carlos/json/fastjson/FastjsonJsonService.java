package com.carlos.json.fastjson;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.filter.ValueFilter;
import com.carlos.json.config.JsonProperties;
import com.carlos.json.core.JsonService;
import com.carlos.json.exception.JsonParseException;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * Fastjson2 实现的 JSON 服务
 * </p>
 *
 * @author carlos
 * @date 2025/01/15
 */
@Slf4j
public class FastjsonJsonService implements JsonService {

    private final JsonProperties properties;
    private JSONWriter.Feature[] writerFeatures;
    private com.alibaba.fastjson2.JSONReader.Feature[] readerFeatures;
    private final ValueFilter emptyStringFilter;

    public FastjsonJsonService() {
        this(null);
    }

    public FastjsonJsonService(JsonProperties properties) {
        this.properties = properties;
        this.emptyStringFilter = createEmptyStringFilter();
        configureFeatures();
    }

    private ValueFilter createEmptyStringFilter() {
        if (properties != null && properties.isIgnoreEmptyString()) {
            return (object, name, value) -> {
                if (value instanceof String str && str.isEmpty()) {
                    return null; // 返回 null 会导致该字段被忽略
                }
                return value;
            };
        }
        return null;
    }

    private void configureFeatures() {
        // 配置序列化特性
        boolean indentOutput = properties != null && properties.getSerialization() != null
            && properties.getSerialization().isIndentOutput();
        if (indentOutput) {
            writerFeatures = new JSONWriter.Feature[]{
                JSONWriter.Feature.PrettyFormat,
                JSONWriter.Feature.WriteLongAsString
            };
        } else {
            writerFeatures = new JSONWriter.Feature[]{
                JSONWriter.Feature.WriteLongAsString
            };
        }

        // 配置反序列化特性
        readerFeatures = new com.alibaba.fastjson2.JSONReader.Feature[]{
            com.alibaba.fastjson2.JSONReader.Feature.FieldBased,
            com.alibaba.fastjson2.JSONReader.Feature.IgnoreNullPropertyValue
        };

        // 安全加固：全局禁用 autoType，防止反序列化漏洞
        JSON.config(com.alibaba.fastjson2.JSONReader.Feature.SupportAutoType, false);
    }

    @Override
    public String toJson(Object object) {
        if (object == null) {
            return "{}";
        }
        if (emptyStringFilter != null) {
            return JSON.toJSONString(object, emptyStringFilter, writerFeatures);
        }
        return JSON.toJSONString(object, writerFeatures);
    }

    @Override
    public String toJsonPretty(Object object) {
        if (object == null) {
            return "{}";
        }
        return JSON.toJSONString(object,
            JSONWriter.Feature.PrettyFormat,
            JSONWriter.Feature.WriteLongAsString);
    }

    @Override
    public byte[] toJsonBytes(Object object) {
        if (object == null) {
            return new byte[0];
        }
        return JSON.toJSONBytes(object, writerFeatures);
    }

    @Override
    public String toJsonWithNulls(Object object) {
        if (object == null) {
            return "{}";
        }
        return JSON.toJSONString(object,
            JSONWriter.Feature.WriteNulls,
            JSONWriter.Feature.WriteLongAsString);
    }

    @Override
    public <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return JSON.parseObject(json, clazz, readerFeatures);
        } catch (Exception e) {
            throw new JsonParseException("Fastjson 反序列化失败", e);
        }
    }

    @Override
    public <T> T fromJson(String json, tools.jackson.core.type.TypeReference<T> typeReference) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            // 获取 Jackson TypeReference 中的 Type，使用 Fastjson 的 parseObject
            Type type = typeReference.getType();
            return JSON.parseObject(json, type, readerFeatures);
        } catch (Exception e) {
            throw new JsonParseException("Fastjson 反序列化失败", e);
        }
    }

    @Override
    public <T> T fromJson(byte[] bytes, Class<T> clazz) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            return JSON.parseObject(bytes, clazz, readerFeatures);
        } catch (Exception e) {
            throw new JsonParseException("Fastjson 从字节数组反序列化失败", e);
        }
    }

    @Override
    public <T> T fromJson(byte[] bytes, tools.jackson.core.type.TypeReference<T> typeReference) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            // Fastjson2 不支持 bytes + TypeReference 的重载，需要先转为字符串
            String str = new String(bytes, StandardCharsets.UTF_8);
            return fromJson(str, typeReference);
        } catch (Exception e) {
            throw new JsonParseException("Fastjson 从字节数组反序列化失败", e);
        }
    }

    @Override
    public <T> T fromJson(InputStream inputStream, Class<T> clazz) {
        if (inputStream == null) {
            return null;
        }
        try {
            return JSON.parseObject(inputStream, StandardCharsets.UTF_8, clazz, readerFeatures);
        } catch (Exception e) {
            throw new JsonParseException("Fastjson 从输入流反序列化失败", e);
        }
    }

    @Override
    public <T> List<T> toList(String json, Class<T> elementClass) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return JSON.parseArray(json, elementClass);
        } catch (Exception e) {
            throw new JsonParseException("Fastjson 反序列化为 List 失败", e);
        }
    }

    @Override
    public Map<String, Object> toMap(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return JSON.parseObject(json, new com.alibaba.fastjson2.TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            throw new JsonParseException("Fastjson 反序列化为 Map 失败", e);
        }
    }

    @Override
    public <V> Map<String, V> toMap(String json, Class<V> valueClass) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return JSON.parseObject(json, new com.alibaba.fastjson2.TypeReference<Map<String, V>>() {
            });
        } catch (Exception e) {
            throw new JsonParseException("Fastjson 反序列化为 Map 失败", e);
        }
    }

    @Override
    public Map<String, Object> objectToMap(Object object) {
        if (object == null) {
            return null;
        }
        String json = JSON.toJSONString(object);
        return JSON.parseObject(json);
    }

    @Override
    public <T> T mapToObject(Map<String, Object> map, Class<T> clazz) {
        if (map == null) {
            return null;
        }
        String json = JSON.toJSONString(map);
        return JSON.parseObject(json, clazz);
    }

    @Override
    public <T> T convertValue(Object fromValue, Class<T> toValueType) {
        if (fromValue == null) {
            return null;
        }
        String json = JSON.toJSONString(fromValue);
        return JSON.parseObject(json, toValueType);
    }

    @Override
    public <T> T convertValue(Object fromValue, tools.jackson.core.type.TypeReference<T> toValueTypeRef) {
        if (fromValue == null) {
            return null;
        }
        String json = JSON.toJSONString(fromValue);
        // 获取 Jackson TypeReference 中的 Type
        Type type = toValueTypeRef.getType();
        return JSON.parseObject(json, type);
    }

    @Override
    public boolean isValidJson(String json) {
        if (json == null || json.isEmpty()) {
            return false;
        }
        try {
            Object obj = JSON.parse(json);
            return obj != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean canSerialize(Object value) {
        if (value == null) {
            return true;
        }
        try {
            JSON.toJSONString(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Object extract(String json, String path) {
        if (json == null || json.isEmpty() || path == null || path.isEmpty()) {
            return null;
        }
        try {
            // 将 Fastjson 路径转换为 JsonPath 格式
            String jsonPath = convertToJsonPath(path);
            return JsonPath.read(json, jsonPath);
        } catch (PathNotFoundException e) {
            log.warn("JSON path not found: {}", path);
            return null;
        }
    }

    @Override
    public <T> T extract(String json, String path, Class<T> clazz) {
        Object result = extract(json, path);
        if (result == null) {
            return null;
        }
        return convertValue(result, clazz);
    }

    @Override
    public String merge(String json1, String json2) {
        if (json1 == null || json1.isEmpty()) {
            return json2;
        }
        if (json2 == null || json2.isEmpty()) {
            return json1;
        }
        try {
            JSONObject obj1 = JSON.parseObject(json1);
            JSONObject obj2 = JSON.parseObject(json2);

            // 合并 obj2 到 obj1
            for (String key : obj2.keySet()) {
                obj1.put(key, obj2.get(key));
            }
            return obj1.toJSONString();
        } catch (Exception e) {
            throw new JsonParseException("合并 JSON 失败", e);
        }
    }

    @Override
    public Object getEngine() {
        return JSON.class;
    }

    @Override
    public String getEngineName() {
        return "Fastjson2";
    }

    /**
     * 将路径转换为 JsonPath 格式
     */
    private String convertToJsonPath(String path) {
        if (path.startsWith("$")) {
            return path;
        }
        // 支持点分隔的路径：user.address.city -> $.user.address.city
        return "$" + (path.startsWith(".") ? path : "." + path);
    }

    /**
     * 创建 JSONObject
     *
     * @return JSONObject
     */
    public JSONObject createJSONObject() {
        return new JSONObject();
    }

    /**
     * 创建 JSONArray
     *
     * @return JSONArray
     */
    public JSONArray createJSONArray() {
        return new JSONArray();
    }

    /**
     * 解析为 JSONObject
     *
     * @param json JSON 字符串
     * @return JSONObject
     */
    public JSONObject parseObject(String json) {
        return JSON.parseObject(json);
    }

    /**
     * 解析为 JSONArray
     *
     * @param json JSON 字符串
     * @return JSONArray
     */
    public JSONArray parseArray(String json) {
        return JSON.parseArray(json);
    }
}
