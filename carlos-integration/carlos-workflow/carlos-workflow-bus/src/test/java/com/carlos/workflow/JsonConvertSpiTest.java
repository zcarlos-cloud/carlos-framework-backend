package com.carlos.workflow;

import org.dromara.warm.flow.core.json.JsonConvert;
import org.dromara.warm.flow.core.utils.ServiceLoaderUtil;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * <p>
 * JsonConvert SPI 回归测试：验证 warm-flow-plugin-json-v1 的排除在 classpath 层面生效，
 * 引擎只会加载 Jackson 3 的实现，不会出现 jsonConvert 为 null 或 JSON 实现不确定的情况。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
class JsonConvertSpiTest {

    private static final String SPI_RESOURCE = "META-INF/services/" + JsonConvert.class.getName();

    private static final String JACKSON3_PROVIDER = "org.dromara.warm.plugin.json.JsonConvertJackson3";

    /**
     * warm-flow-plugin-json-v1 注册的 5 个实现中对应的 Jackson 2 实现
     */
    private static final String JACKSON2_PROVIDER = "org.dromara.warm.plugin.json.JsonConvertJackson";

    @Test
    void onlyOneSpiDescriptorIsVisible() throws IOException {
        assertThat(spiDescriptors())
            .as("warm-flow-plugin-json-v1 应已被排除，JsonConvert 的 SPI 描述文件只应剩一个")
            .hasSize(1);
    }

    @Test
    void declaredProvidersContainJackson3Only() throws IOException {
        List<String> providers = declaredProviders();

        assertThat(providers).containsExactly(JACKSON3_PROVIDER);
        assertThat(providers).noneMatch(provider -> provider.contains("Snack"));
        assertThat(providers).noneMatch(provider -> provider.contains("FastJson"));
        assertThat(providers).noneMatch(provider -> provider.contains("Gson"));
        assertThat(providers).noneMatch(provider -> provider.endsWith(JACKSON2_PROVIDER));
    }

    @Test
    void everyDeclaredProviderIsLoadable() throws IOException {
        List<String> providers = declaredProviders();
        assertThat(providers).isNotEmpty();

        for (String provider : providers) {
            assertProviderLoadable(provider);
        }
    }

    @Test
    void engineSpiLookupReturnsJackson3InsteadOfNull() {
        JsonConvert jsonConvert = ServiceLoaderUtil.loadFirst(JsonConvert.class);

        assertThat(jsonConvert).isNotNull();
        assertThat(jsonConvert.getClass().getName()).isEqualTo(JACKSON3_PROVIDER);
    }

    @Test
    void jackson2DatabindIsNotOnTheClasspath() {
        assertThatThrownBy(() -> Class.forName("com.fasterxml.jackson.databind.ObjectMapper"))
            .as("Jackson 2 的 ObjectMapper 不应在 classpath 上，它随 warm-flow-plugin-json-v1 一起被排除")
            .isInstanceOf(ClassNotFoundException.class);
    }

    private void assertProviderLoadable(String provider) {
        try {
            Class<?> providerClass = Class.forName(provider);
            assertThat(providerClass).isAssignableTo(JsonConvert.class);
            assertThat(providerClass.getDeclaredConstructor().newInstance()).isInstanceOf(JsonConvert.class);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("SPI 提供者 " + provider + " 无法加载或实例化，引擎加载时会得到 null", e);
        }
    }

    private List<String> declaredProviders() throws IOException {
        List<String> providers = new ArrayList<>();
        for (URL url : spiDescriptors()) {
            try (InputStream input = url.openStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                reader.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .forEach(providers::add);
            }
        }
        return providers;
    }

    private List<URL> spiDescriptors() throws IOException {
        Enumeration<URL> resources = contextClassLoader().getResources(SPI_RESOURCE);
        return Collections.list(resources);
    }

    private ClassLoader contextClassLoader() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        return classLoader == null ? JsonConvertSpiTest.class.getClassLoader() : classLoader;
    }
}
