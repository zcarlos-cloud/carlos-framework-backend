package com.carlos.json;

import com.carlos.json.config.JsonProperties;
import com.carlos.json.config.JsonProperties.JsonEngineType;
import com.carlos.json.core.JsonService;
import com.carlos.json.fastjson.FastjsonJsonService;
import com.carlos.json.gson.GsonJsonService;
import com.carlos.json.jackson.JacksonJsonService;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 * JSON 服务工厂类
 * </p>
 *
 * <p>根据配置创建对应的 JSON 服务实例，支持多引擎切换</p>
 *
 * @author carlos
 * @date 2025/01/15
 */
@Slf4j
public class JsonFactory {

    private static final Map<JsonEngineType, JsonService> SERVICE_CACHE = new ConcurrentHashMap<>();
    private static volatile JsonProperties globalProperties;
    private static volatile ObjectMapper globalObjectMapper;

    /**
     * 获取 JSON 服务实例（使用默认配置）
     *
     * @return JsonService
     */
    public static JsonService getService() {
        return getService(JsonEngineType.JACKSON);
    }

    /**
     * 获取指定引擎的 JSON 服务实例
     *
     * @param engineType 引擎类型
     * @return JsonService
     */
    public static JsonService getService(JsonEngineType engineType) {
        return getService(engineType, null);
    }

    /**
     * 获取指定引擎的 JSON 服务实例（带配置）
     *
     * @param engineType 引擎类型
     * @param properties 配置属性
     * @return JsonService
     */
    public static JsonService getService(JsonEngineType engineType, JsonProperties properties) {
        return SERVICE_CACHE.computeIfAbsent(engineType, type -> createService(type, properties));
    }

    /**
     * 创建 JSON 服务实例
     *
     * @param engineType 引擎类型
     * @param properties 配置属性
     * @return JsonService
     */
    private static JsonService createService(JsonEngineType engineType, JsonProperties properties) {
        log.info("创建 JSON 服务实例，引擎类型: {}", engineType);

        return switch (engineType) {
            case FASTJSON2 -> new FastjsonJsonService(properties);
            case GSON -> new GsonJsonService(properties);
            default -> {
                if (globalObjectMapper != null) {
                    yield new JacksonJsonService(globalObjectMapper, properties);
                }
                yield new JacksonJsonService(new ObjectMapper(), properties);
            }
        };
    }

    /**
     * 根据配置创建 JSON 服务实例
     *
     * @param properties 配置属性
     * @return JsonService
     */
    public static JsonService createFromProperties(JsonProperties properties) {
        if (properties == null) {
            return getService(JsonEngineType.JACKSON);
        }
        return getService(properties.getEngine(), properties);
    }

    /**
     * 设置全局配置
     *
     * @param properties 配置属性
     */
    public static void setGlobalProperties(JsonProperties properties) {
        globalProperties = properties;
        // 清除缓存，使用新配置重新创建
        clearCache();
    }

    /**
     * 设置全局 ObjectMapper（Jackson）
     *
     * @param objectMapper ObjectMapper
     */
    public static void setGlobalObjectMapper(ObjectMapper objectMapper) {
        globalObjectMapper = objectMapper;
    }

    /**
     * 注册 JSON 服务实例（供 Spring 上下文注入使用）
     *
     * <p>当 Spring Boot 创建 JsonService Bean 时，通过此方法注册到工厂，
     * 使静态方法 {@link #getService()} 能够获取到 Spring 管理的实例。</p>
     *
     * @param engineType 引擎类型
     * @param service    JSON 服务实例
     */
    public static void registerService(JsonEngineType engineType, JsonService service) {
        SERVICE_CACHE.put(engineType, service);
        log.debug("Registered JsonService for engine: {}", engineType);
    }

    /**
     * 清除服务缓存
     */
    public static void clearCache() {
        SERVICE_CACHE.clear();
        log.info("JSON 服务缓存已清除");
    }

    /**
     * 获取当前使用的 JSON 引擎名称
     *
     * @return 引擎名称
     */
    public static String getCurrentEngineName() {
        return getService().getEngineName();
    }

    /**
     * 获取 Jackson 服务（便捷方法）
     *
     * @return JacksonJsonService
     */
    public static JacksonJsonService getJacksonService() {
        return (JacksonJsonService) getService(JsonEngineType.JACKSON);
    }

    /**
     * 获取 Fastjson 服务（便捷方法）
     *
     * @return FastjsonJsonService
     */
    public static FastjsonJsonService getFastjsonService() {
        return (FastjsonJsonService) getService(JsonEngineType.FASTJSON2);
    }

    /**
     * 获取 Gson 服务（便捷方法）
     *
     * @return GsonJsonService
     */
    public static GsonJsonService getGsonService() {
        return (GsonJsonService) getService(JsonEngineType.GSON);
    }
}
