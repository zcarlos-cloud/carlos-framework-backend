package com.carlos.json.jackson;

import com.carlos.json.config.JsonProperties;
import com.carlos.json.core.JsonService;
import com.carlos.json.exception.JsonParseException;
import com.carlos.json.jackson.filter.EmptyStringPropertyFilter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.ser.std.SimpleFilterProvider;
import tools.jackson.databind.ser.std.ToStringSerializer;
import tools.jackson.databind.type.CollectionType;
import tools.jackson.databind.type.MapType;

import java.io.InputStream;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * Jackson 实现的 JSON 服务
 * </p>
 *
 * @author carlos
 * @date 2025/01/15
 */
@Slf4j
public class JacksonJsonService implements JsonService {

    private final ObjectMapper objectMapper;
    private final ObjectMapper prettyObjectMapper;
    private final ObjectMapper withNullsObjectMapper;

    public JacksonJsonService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.prettyObjectMapper = objectMapper.rebuild()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build();
        this.withNullsObjectMapper = objectMapper.rebuild()
            .changeDefaultPropertyInclusion(v -> JsonInclude.Value.ALL_ALWAYS)
            .build();
    }

    public JacksonJsonService(ObjectMapper objectMapper, JsonProperties properties) {
        this.objectMapper = configureMapper(objectMapper, properties);
        this.prettyObjectMapper = this.objectMapper.rebuild()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build();
        this.withNullsObjectMapper = this.objectMapper.rebuild()
            .changeDefaultPropertyInclusion(v -> JsonInclude.Value.ALL_ALWAYS)
            .build();
    }

    @SuppressWarnings("unchecked")
    private ObjectMapper configureMapper(ObjectMapper mapper, JsonProperties properties) {
        if (properties == null) {
            return mapper;
        }
        var builder = mapper.rebuild();
        // 应用配置
        if (properties.getSerialization().isIndentOutput()) {
            builder.enable(SerializationFeature.INDENT_OUTPUT);
        }
        if (properties.getSerialization().isFailOnEmptyBeans()) {
            builder.enable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        } else {
            builder.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        }
        if (properties.isIgnoreNull()) {
            builder.changeDefaultPropertyInclusion(v ->
                JsonInclude.Value.construct(
                    JsonInclude.Include.NON_NULL,
                    JsonInclude.Include.ALWAYS));
        }
        if (properties.getDeserialization().isFailOnUnknownProperties()) {
            builder.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        } else {
            builder.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        }
        if (properties.isIgnoreEmptyString()) {
            SimpleFilterProvider filterProvider = new SimpleFilterProvider();
            filterProvider.addFilter(EmptyStringPropertyFilter.FILTER_NAME, new EmptyStringPropertyFilter());
            builder.filterProvider(filterProvider);
        }

        // Long/BigInteger 转 String（解决前端精度丢失）
        if (properties.getSerialization().isLongToString()) {
            SimpleModule longModule = new SimpleModule("LongToStringModule");
            longModule.addSerializer(BigInteger.class, ToStringSerializer.instance);
            longModule.addSerializer(Long.class, ToStringSerializer.instance);
            longModule.addSerializer(Long.TYPE, ToStringSerializer.instance);
            builder.addModule(longModule);
            log.debug("Enabled: Long/BigInteger to String serializers");
        }

        return builder.build();
    }

    @Override
    public String toJson(Object object) {
        if (object == null) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JacksonException e) {
            throw new JsonParseException("Jackson 序列化失败", e);
        }
    }

    @Override
    public String toJsonPretty(Object object) {
        if (object == null) {
            return "{}";
        }
        try {
            return prettyObjectMapper.writeValueAsString(object);
        } catch (JacksonException e) {
            throw new JsonParseException("Jackson 格式化序列化失败", e);
        }
    }

    @Override
    public byte[] toJsonBytes(Object object) {
        if (object == null) {
            return new byte[0];
        }
        try {
            return objectMapper.writeValueAsBytes(object);
        } catch (JacksonException e) {
            throw new JsonParseException("Jackson 序列化为字节数组失败", e);
        }
    }

    @Override
    public String toJsonWithNulls(Object object) {
        if (object == null) {
            return "{}";
        }
        try {
            return withNullsObjectMapper.writeValueAsString(object);
        } catch (JacksonException e) {
            throw new JsonParseException("Jackson 序列化（含 null）失败", e);
        }
    }

    @Override
    public <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JacksonException e) {
            throw new JsonParseException("Jackson 反序列化失败", e);
        }
    }

    @Override
    public <T> T fromJson(String json, TypeReference<T> typeReference) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (JacksonException e) {
            throw new JsonParseException("Jackson 反序列化失败", e);
        }
    }

    @Override
    public <T> T fromJson(byte[] bytes, Class<T> clazz) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            return objectMapper.readValue(bytes, clazz);
        } catch (JacksonException e) {
            throw new JsonParseException("Jackson 从字节数组反序列化失败", e);
        }
    }

    @Override
    public <T> T fromJson(byte[] bytes, TypeReference<T> typeReference) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            return objectMapper.readValue(bytes, typeReference);
        } catch (JacksonException e) {
            throw new JsonParseException("Jackson 从字节数组反序列化失败", e);
        }
    }

    @Override
    public <T> T fromJson(InputStream inputStream, Class<T> clazz) {
        if (inputStream == null) {
            return null;
        }
        try {
            return objectMapper.readValue(inputStream, clazz);
        } catch (JacksonException e) {
            throw new JsonParseException("Jackson 从输入流反序列化失败", e);
        }
    }

    @Override
    public <T> List<T> toList(String json, Class<T> elementClass) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            CollectionType listType = objectMapper.getTypeFactory()
                .constructCollectionType(List.class, elementClass);
            return objectMapper.readValue(json, listType);
        } catch (JacksonException e) {
            throw new JsonParseException("Jackson 反序列化为 List 失败", e);
        }
    }

    @Override
    public Map<String, Object> toMap(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (JacksonException e) {
            throw new JsonParseException("Jackson 反序列化为 Map 失败", e);
        }
    }

    @Override
    public <V> Map<String, V> toMap(String json, Class<V> valueClass) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            MapType mapType = objectMapper.getTypeFactory()
                .constructMapType(Map.class, String.class, valueClass);
            return objectMapper.readValue(json, mapType);
        } catch (JacksonException e) {
            throw new JsonParseException("Jackson 反序列化为 Map 失败", e);
        }
    }

    @Override
    public Map<String, Object> objectToMap(Object object) {
        if (object == null) {
            return null;
        }
        return objectMapper.convertValue(object, new TypeReference<Map<String, Object>>() {
        });
    }

    @Override
    public <T> T mapToObject(Map<String, Object> map, Class<T> clazz) {
        if (map == null) {
            return null;
        }
        return objectMapper.convertValue(map, clazz);
    }

    @Override
    public <T> T convertValue(Object fromValue, Class<T> toValueType) {
        if (fromValue == null) {
            return null;
        }
        return objectMapper.convertValue(fromValue, toValueType);
    }

    @Override
    public <T> T convertValue(Object fromValue, TypeReference<T> toValueTypeRef) {
        if (fromValue == null) {
            return null;
        }
        return objectMapper.convertValue(fromValue, toValueTypeRef);
    }

    @Override
    public boolean isValidJson(String json) {
        if (json == null || json.isEmpty()) {
            return false;
        }
        try {
            objectMapper.readTree(json);
            return true;
        } catch (JacksonException e) {
            return false;
        }
    }

    @Override
    public boolean canSerialize(Object value) {
        if (value == null) {
            return true;
        }
        try {
            objectMapper.writeValueAsString(value);
            return true;
        } catch (JacksonException e) {
            return false;
        }
    }

    @Override
    public Object extract(String json, String path) {
        if (json == null || json.isEmpty() || path == null || path.isEmpty()) {
            return null;
        }
        try {
            return JsonPath.read(json, path);
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
            JsonNode node1 = objectMapper.readTree(json1);
            JsonNode node2 = objectMapper.readTree(json2);

            if (!node1.isObject() || !node2.isObject()) {
                throw new JsonParseException("只能合并 JSON 对象");
            }

            ObjectNode result = ((ObjectNode) node1).deepCopy();
            for (Map.Entry<String, JsonNode> field : node2.properties()) {
                result.set(field.getKey(), field.getValue());
            }
            return objectMapper.writeValueAsString(result);
        } catch (JacksonException e) {
            throw new JsonParseException("合并 JSON 失败", e);
        }
    }

    @Override
    public Object getEngine() {
        return objectMapper;
    }

    @Override
    public String getEngineName() {
        return "Jackson";
    }

    /**
     * 获取 ObjectMapper 实例
     *
     * @return ObjectMapper
     */
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    /**
     * 创建 ObjectNode
     *
     * @return ObjectNode
     */
    public ObjectNode createObjectNode() {
        return objectMapper.createObjectNode();
    }

    /**
     * 创建 ArrayNode
     *
     * @return ArrayNode
     */
    public ArrayNode createArrayNode() {
        return objectMapper.createArrayNode();
    }
}
