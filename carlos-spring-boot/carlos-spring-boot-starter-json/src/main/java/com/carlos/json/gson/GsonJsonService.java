package com.carlos.json.gson;

import com.carlos.json.config.JsonProperties;
import com.carlos.json.core.JsonService;
import com.carlos.json.exception.JsonParseException;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.type.TypeReference;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * Gson 实现的 JSON 服务
 * </p>
 *
 * @author carlos
 * @date 2025/01/15
 */
@Slf4j
public class GsonJsonService implements JsonService {

    private final Gson gson;
    private final Gson prettyGson;
    private final Gson withNullsGson;

    public GsonJsonService() {
        this(null);
    }

    public GsonJsonService(JsonProperties properties) {
        this.gson = createGson(properties, false, false);
        this.prettyGson = createGson(properties, true, false);
        this.withNullsGson = createGson(properties, false, true);
    }

    private Gson createGson(JsonProperties properties, boolean prettyPrint, boolean serializeNulls) {
        GsonBuilder builder = new GsonBuilder();

        // 基本配置
        if (serializeNulls) {
            builder.serializeNulls();
        }
        if (prettyPrint) {
            builder.setPrettyPrinting();
        }

        // 应用自定义配置
        if (properties != null) {
            // 日期格式
            if (properties.getDateFormat() != null) {
                builder.setDateFormat(properties.getDateFormat());
            }

            // Long 转 String
            if (properties.getSerialization() != null && properties.getSerialization().isLongToString()) {
                builder.setLongSerializationPolicy(LongSerializationPolicy.STRING);
            }

            // 复杂 Map key 序列化
            builder.enableComplexMapKeySerialization();

            // 排除内部类
            builder.disableInnerClassSerialization();

            // 字段命名策略
            if (properties.getFieldMapping() != null) {
                String namingStrategy = properties.getFieldMapping().getPropertyNamingStrategy();
                if ("SNAKE_CASE".equals(namingStrategy)) {
                    builder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
                } else if ("UPPER_CAMEL_CASE".equals(namingStrategy)) {
                    builder.setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE);
                } else if ("KEBAB_CASE".equals(namingStrategy)) {
                    builder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES);
                }
            }

            // 忽略空字符串
            if (properties.isIgnoreEmptyString()) {
                builder.registerTypeAdapter(String.class, new JsonSerializer<String>() {
                    @Override
                    public JsonElement serialize(String src, java.lang.reflect.Type typeOfSrc, JsonSerializationContext context) {
                        if (src == null || src.isEmpty()) {
                            return null; // 返回 null 会跳过该字段
                        }
                        return new JsonPrimitive(src);
                    }
                });
            }
        }

        // 容错处理
        builder.disableHtmlEscaping();

        return builder.create();
    }

    @Override
    public String toJson(Object object) {
        if (object == null) {
            return "{}";
        }
        return gson.toJson(object);
    }

    @Override
    public String toJsonPretty(Object object) {
        if (object == null) {
            return "{}";
        }
        return prettyGson.toJson(object);
    }

    @Override
    public byte[] toJsonBytes(Object object) {
        if (object == null) {
            return new byte[0];
        }
        String json = gson.toJson(object);
        return json.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String toJsonWithNulls(Object object) {
        if (object == null) {
            return "{}";
        }
        return withNullsGson.toJson(object);
    }

    @Override
    public <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return gson.fromJson(json, clazz);
        } catch (JsonParseException e) {
            throw new com.carlos.json.exception.JsonParseException("Gson 反序列化失败", e);
        }
    }

    @Override
    public <T> T fromJson(String json, TypeReference<T> typeReference) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            Type type = new TypeToken<T>() {
            }.getType();
            return gson.fromJson(json, type);
        } catch (JsonParseException e) {
            throw new com.carlos.json.exception.JsonParseException("Gson 反序列化失败", e);
        }
    }

    @Override
    public <T> T fromJson(byte[] bytes, Class<T> clazz) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        String json = new String(bytes, StandardCharsets.UTF_8);
        return fromJson(json, clazz);
    }

    @Override
    public <T> T fromJson(byte[] bytes, TypeReference<T> typeReference) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        String json = new String(bytes, StandardCharsets.UTF_8);
        return fromJson(json, typeReference);
    }

    @Override
    public <T> T fromJson(InputStream inputStream, Class<T> clazz) {
        if (inputStream == null) {
            return null;
        }
        try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            return gson.fromJson(reader, clazz);
        } catch (Exception e) {
            throw new com.carlos.json.exception.JsonParseException("Gson 从输入流反序列化失败", e);
        }
    }

    @Override
    public <T> List<T> toList(String json, Class<T> elementClass) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            Type listType = TypeToken.getParameterized(List.class, elementClass).getType();
            return gson.fromJson(json, listType);
        } catch (JsonParseException e) {
            throw new com.carlos.json.exception.JsonParseException("Gson 反序列化为 List 失败", e);
        }
    }

    @Override
    public Map<String, Object> toMap(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            Type mapType = new TypeToken<Map<String, Object>>() {
            }.getType();
            return gson.fromJson(json, mapType);
        } catch (JsonParseException e) {
            throw new com.carlos.json.exception.JsonParseException("Gson 反序列化为 Map 失败", e);
        }
    }

    @Override
    public <V> Map<String, V> toMap(String json, Class<V> valueClass) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            Type mapType = TypeToken.getParameterized(Map.class, String.class, valueClass).getType();
            return gson.fromJson(json, mapType);
        } catch (JsonParseException e) {
            throw new com.carlos.json.exception.JsonParseException("Gson 反序列化为 Map 失败", e);
        }
    }

    @Override
    public Map<String, Object> objectToMap(Object object) {
        if (object == null) {
            return null;
        }
        String json = gson.toJson(object);
        return toMap(json);
    }

    @Override
    public <T> T mapToObject(Map<String, Object> map, Class<T> clazz) {
        if (map == null) {
            return null;
        }
        String json = gson.toJson(map);
        return fromJson(json, clazz);
    }

    @Override
    public <T> T convertValue(Object fromValue, Class<T> toValueType) {
        if (fromValue == null) {
            return null;
        }
        String json = gson.toJson(fromValue);
        return gson.fromJson(json, toValueType);
    }

    @Override
    public <T> T convertValue(Object fromValue, TypeReference<T> toValueTypeRef) {
        if (fromValue == null) {
            return null;
        }
        String json = gson.toJson(fromValue);
        Type type = new TypeToken<T>() {
        }.getType();
        return gson.fromJson(json, type);
    }

    @Override
    public boolean isValidJson(String json) {
        if (json == null || json.isEmpty()) {
            return false;
        }
        try {
            JsonElement element = JsonParser.parseString(json);
            return element != null && !element.isJsonNull();
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
            gson.toJson(value);
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
            JsonObject obj1 = JsonParser.parseString(json1).getAsJsonObject();
            JsonObject obj2 = JsonParser.parseString(json2).getAsJsonObject();

            // 合并 obj2 到 obj1
            for (Map.Entry<String, JsonElement> entry : obj2.entrySet()) {
                obj1.add(entry.getKey(), entry.getValue());
            }
            return gson.toJson(obj1);
        } catch (Exception e) {
            throw new com.carlos.json.exception.JsonParseException("合并 JSON 失败", e);
        }
    }

    @Override
    public Object getEngine() {
        return gson;
    }

    @Override
    public String getEngineName() {
        return "Gson";
    }

    /**
     * 将路径转换为 JsonPath 格式
     */
    private String convertToJsonPath(String path) {
        if (path.startsWith("$")) {
            return path;
        }
        return "$" + (path.startsWith(".") ? path : "." + path);
    }

    /**
     * 创建 JsonObject
     *
     * @return JsonObject
     */
    public JsonObject createJsonObject() {
        return new JsonObject();
    }

    /**
     * 创建 JsonArray
     *
     * @return JsonArray
     */
    public JsonArray createJsonArray() {
        return new JsonArray();
    }

    /**
     * 解析为 JsonObject
     *
     * @param json JSON 字符串
     * @return JsonObject
     */
    public JsonObject parseObject(String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }

    /**
     * 解析为 JsonArray
     *
     * @param json JSON 字符串
     * @return JsonArray
     */
    public JsonArray parseArray(String json) {
        return JsonParser.parseString(json).getAsJsonArray();
    }

    /**
     * 获取 Gson 实例
     *
     * @return Gson
     */
    public Gson getGson() {
        return gson;
    }
}
