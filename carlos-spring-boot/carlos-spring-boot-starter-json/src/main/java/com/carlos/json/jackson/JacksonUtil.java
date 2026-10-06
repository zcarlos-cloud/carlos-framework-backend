package com.carlos.json.jackson;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.carlos.core.exception.ComponentException;
import com.carlos.util.function.CheckedConsumer;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.TreeNode;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.type.CollectionLikeType;
import tools.jackson.databind.type.MapType;

import java.io.InputStream;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.*;

/**
 * <p>
 * Jackson 序列化工具类
 * </p>
 *
 * <p><strong>注意：</strong>业务代码请优先使用 {@link com.carlos.json.JsonUtils}，
 * 该类提供统一的 JSON 操作接口，支持引擎切换（Jackson/Fastjson/Gson）。</p>
 *
 * <p>此类保留以下 Jackson 特有功能：</p>
 * <ul>
 *   <li>JsonNode 操作（readTree, createObjectNode, createArrayNode）</li>
 *   <li>JavaType 构造（getMapType, getListType, getParametricType）</li>
 *   <li>Tree 模型转换（treeToValue, valueToTree）</li>
 *   <li>Hutool JSON trim（jsonTrim, jsonTrimArray）</li>
 * </ul>
 *
 * @author carlos
 * @date 2020/4/16 9:21
 * @see com.carlos.json.JsonUtils
 */
@Slf4j
public class JacksonUtil {

    /**
     * 清空JSONArray 值前后空格
     *
     * @param array 参数0
     * @return JSONArray
     * @author carlos
     * @date 2022/1/27 10:26
     */
    public static JSONArray jsonTrimArray(JSONArray array) {
        if (array.size() > 0) {
            for (int i = 0; i < array.size(); i++) {
                Object object = array.get(i);
                if (object != null) {
                    switch (object) {
                        case String s -> array.set(i, s.trim());
                        case JSONObject jsonObject -> jsonTrim(jsonObject);
                        case JSONArray jsonArray -> jsonTrimArray(jsonArray);
                        default -> {
                        }
                    }
                }
            }
        }
        return array;
    }

    /**
     * 去除value的空格
     *
     * @param jsonObject 参数0
     * @return JSONObject
     * @author carlos
     * @date 2022/1/27 10:26
     */
    public static JSONObject jsonTrim(JSONObject jsonObject) {
        for (Map.Entry<String, Object> next : jsonObject.entrySet()) {
            Object value = next.getValue();
            if (value != null) {
                switch (value) {
                    case String s ->
                        // 清空值前后空格
                        jsonObject.set(next.getKey(), s.trim());
                    case JSONObject jo -> jsonTrim(jo);
                    case JSONArray ja -> jsonTrimArray(ja);
                    default -> {
                    }
                }
            }
        }

        return jsonObject;
    }

    /**
     * 将json字符串转成 JsonNode
     *
     * @param jsonString jsonString
     * @return jsonString json字符串
     */
    public static JsonNode readTree(String jsonString) {
        Objects.requireNonNull(jsonString, "jsonString is null");
        return getInstance().readTree(jsonString);
    }

    /**
     * 将json字符串转成 JsonNode
     *
     * @param in InputStream
     * @return jsonString json字符串
     */
    public static JsonNode readTree(InputStream in) {
        Objects.requireNonNull(in, "InputStream in is null");
        return getInstance().readTree(in);
    }

    /**
     * 将json字符串转成 JsonNode
     *
     * @param content content
     * @return jsonString json字符串
     */
    public static JsonNode readTree(byte[] content) {
        Objects.requireNonNull(content, "byte[] content is null");
        return getInstance().readTree(content);
    }

    /**
     * 将json字符串转成 JsonNode
     *
     * @param jsonParser JsonParser
     * @return jsonString json字符串
     */
    public static JsonNode readTree(JsonParser jsonParser) {
        Objects.requireNonNull(jsonParser, "jsonParser is null");
        return getInstance().readTree(jsonParser);
    }

    /**
     * 将json反序列化成对象
     *
     * @param content  bytes
     * @param javaType JavaType
     * @param <T>      T 泛型标记
     * @return Bean
     */
    @Nullable
    public static <T> T readValue(@Nullable byte[] content, JavaType javaType) {
        if (ObjUtil.isEmpty(content)) {
            return null;
        }
        return getInstance().readValue(content, javaType);
    }

    /**
     * 将json反序列化成对象
     *
     * @param jsonString jsonString
     * @param javaType   JavaType
     * @param <T>        T 泛型标记
     * @return Bean
     */
    @Nullable
    public static <T> T readValue(@Nullable String jsonString, JavaType javaType) {
        if (StringUtils.isEmpty(jsonString)) {
            return null;
        }
        return getInstance().readValue(jsonString, javaType);
    }

    /**
     * 将json反序列化成对象
     *
     * @param in       InputStream
     * @param javaType JavaType
     * @param <T>      T 泛型标记
     * @return Bean
     */
    @Nullable
    public static <T> T readValue(@Nullable InputStream in, JavaType javaType) {
        if (in == null) {
            return null;
        }
        return getInstance().readValue(in, javaType);
    }

    /**
     * 封装 map type，keyClass String
     *
     * @param valueClass value 类型
     * @return MapType
     */
    public static MapType getMapType(Class<?> valueClass) {
        return getMapType(String.class, valueClass);
    }

    /**
     * 封装 map type
     *
     * @param keyClass   key 类型
     * @param valueClass value 类型
     * @return MapType
     */
    public static MapType getMapType(Class<?> keyClass, Class<?> valueClass) {
        return getInstance().getTypeFactory().constructMapType(Map.class, keyClass, valueClass);
    }

    /**
     * 封装 map type
     *
     * @param elementClass 集合值类型
     * @return CollectionLikeType
     */
    public static CollectionLikeType getListType(Class<?> elementClass) {
        return getInstance().getTypeFactory().constructCollectionLikeType(List.class, elementClass);
    }

    /**
     * 封装参数化类型
     *
     * <p>
     * 例如： Map.class, String.class, String.class 对应 Map[String, String]
     * </p>
     *
     * @param parametrized     泛型参数化
     * @param parameterClasses 泛型参数类型
     * @return JavaType
     */
    public static JavaType getParametricType(Class<?> parametrized, Class<?>... parameterClasses) {
        return getInstance().getTypeFactory().constructParametricType(parametrized, parameterClasses);
    }

    /**
     * jackson 的类型转换
     *
     * @param fromValue   来源对象
     * @param toValueType 转换的类型
     * @param <T>         泛型标记
     * @return 转换结果
     */
    public static <T> T convertValue(Object fromValue, JavaType toValueType) {
        return getInstance().convertValue(fromValue, toValueType);
    }


    /**
     * tree 转对象
     *
     * @param treeNode  TreeNode
     * @param valueType valueType
     * @param <T>       泛型标记
     * @return 转换结果
     */
    public static <T> T treeToValue(TreeNode treeNode, Class<T> valueType) {
        try {
            return getInstance().treeToValue(treeNode, valueType);
        } catch (JacksonException e) {
            throw new ComponentException(e);
        }
    }

    /**
     * 对象转 tree
     *
     * @param fromValue fromValue
     * @param <T>       泛型标记
     * @return 转换结果
     */
    public static <T extends JsonNode> T valueToTree(@Nullable Object fromValue) {
        return getInstance().valueToTree(fromValue);
    }

    /**
     * 判断是否可以序列化
     *
     * @param value 对象
     * @return 是否可以序列化
     * @deprecated 请使用 {@link com.carlos.json.JsonUtils#canSerialize(Object)}
     */
    @Deprecated(since = "3.0.0", forRemoval = true)
    public static boolean canSerialize(@Nullable Object value) {
        if (value == null) {
            return true;
        }
        try {
            getInstance().writeValueAsString(value);
            return true;
        } catch (JacksonException e) {
            return false;
        }
    }

    /**
     * 判断是否可以反序列化
     *
     * @param type JavaType
     * @return 是否可以反序列化
     */
    public static boolean canDeserialize(JavaType type) {
        try {
            getInstance().readValue("null", type);
            return true;
        } catch (JacksonException e) {
            return false;
        }
    }


    /**
     * 检验 json 格式
     *
     * @param consumer ObjectMapper consumer
     * @return 是否成功
     */
    public static boolean isValidJson(CheckedConsumer<ObjectMapper> consumer) {
        ObjectMapper mapper = getInstance().rebuild()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
            .build();
        try {
            consumer.accept(mapper);
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    /**
     * 创建 ObjectNode
     *
     * @return ObjectNode
     */
    public static ObjectNode createObjectNode() {
        return getInstance().createObjectNode();
    }

    /**
     * 创建 ArrayNode
     *
     * @return ArrayNode
     */
    public static ArrayNode createArrayNode() {
        return getInstance().createArrayNode();
    }

    /**
     * 获取 ObjectMapper 实例
     *
     * @return ObjectMapper
     */
    public static ObjectMapper getInstance() {
        return JacksonHolder.INSTANCE;
    }

    private static class JacksonHolder {

        private static final ObjectMapper INSTANCE = createMapper();

        private static ObjectMapper createMapper() {
            Locale china = Locale.CHINA;
            return JsonMapper.builder(jsonFactory())
                .defaultLocale(china)
                .defaultTimeZone(TimeZone.getTimeZone(ZoneId.systemDefault()))
                .defaultDateFormat(new DateFormat() {
                    private final ThreadLocal<SimpleDateFormat> delegate =
                        ThreadLocal.withInitial(() -> new SimpleDateFormat(DatePattern.NORM_DATETIME_PATTERN, china));

                    @Override
                    public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition fieldPosition) {
                        return delegate.get().format(date, toAppendTo, fieldPosition);
                    }

                    @Override
                    public Date parse(String source, ParsePosition pos) {
                        return delegate.get().parse(source, pos);
                    }
                })
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .changeDefaultPropertyInclusion(v -> JsonInclude.Value.ALL_NON_NULL)
                .findAndAddModules()
                .build();
        }

        private static JsonFactory jsonFactory() {
            return JsonFactory.builder()
                // 可解析反斜杠引用的所有字符
                .enable(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER)
                // 允许JSON字符串包含非引号控制字符（值小于32的ASCII字符，包含制表符和换行符）
                .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)
                // 允许单引号
                .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
                .build();
        }

    }
}
