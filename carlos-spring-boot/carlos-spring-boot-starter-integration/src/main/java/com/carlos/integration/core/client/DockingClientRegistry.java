package com.carlos.integration.core.client;

import com.carlos.integration.common.exception.DockingException;
import com.carlos.integration.core.config.DockingRestClientProperties;
import com.carlos.integration.core.interceptor.BasicAuthInterceptor;
import com.carlos.integration.core.interceptor.DockingLoggingInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 * 第三方对接客户端注册中心
 * 负责动态创建和管理 RestClient 客户端实
 * </p>
 *
 * @author Carlos
 * @since 1.0.0
 */
@Slf4j
public class DockingClientRegistry implements BeanFactoryAware {

    private BeanDefinitionRegistry beanRegistry;
    private final BeanFactory beanFactory;
    private final DockingRestClientProperties properties;

    /**
     * 存储已注册的客户端实
     */
    private final Map<String, Object> clientInstances = new ConcurrentHashMap<>();

    /**
     * 存储 RestClient 实例
     */
    private final Map<String, RestClient> restClients = new ConcurrentHashMap<>();

    public DockingClientRegistry(DockingRestClientProperties properties, BeanFactory beanFactory) {
        this.properties = properties;
        this.beanFactory = beanFactory;
        if (beanFactory instanceof BeanDefinitionRegistry registry) {
            this.beanRegistry = registry;
        }
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        if (beanFactory instanceof BeanDefinitionRegistry registry) {
            this.beanRegistry = registry;
        }
    }

    /**
     * 注册预定义接口客户端
     *
     * @param clientName      客户端名
     * @param interfaceClass  接口
     * @param config          客户端配
     * @param <T>             接口类型
     * @return 客户端代理实
     */
    public <T> T registerPredefinedClient(String clientName, Class<T> interfaceClass,
                                          DockingRestClientProperties.ClientConfig config) {
        log.info("Registering predefined client: {}, interface: {}", clientName, interfaceClass.getName());

        // 创建独立RestClient 实例
        RestClient restClient = createRestClient(config);
        restClients.put(clientName, restClient);

        // 创建 HTTP 服务代理
        HttpServiceProxyFactory factory = HttpServiceProxyFactory
            .builderFor(org.springframework.web.client.support.RestClientAdapter.create(restClient))
            .build();

        T client = factory.createClient(interfaceClass);
        clientInstances.put(clientName, client);

        // 注册Spring 容器（如果可用）
        if (beanRegistry != null) {
            registerToSpring(clientName, client, interfaceClass);
        }

        log.info("Successfully registered client: {}", clientName);
        return client;
    }

    /**
     * 注册动态客户端（基于配置生成）
     *
     * @param clientName 客户端名
     * @param config     客户端配
     * @return 动态客户端实例
     */
    public DynamicHttpClient registerDynamicClient(String clientName,
                                                   DockingRestClientProperties.ClientConfig config) {
        log.info("Registering dynamic client: {}", clientName);

        // 创建独立RestClient 实例
        RestClient restClient = createRestClient(config);
        restClients.put(clientName, restClient);

        // 创建 CGLIB 代理
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(DynamicHttpClient.class);
        enhancer.setCallback(new DynamicClientInterceptor(restClient, config));

        DynamicHttpClient client = (DynamicHttpClient) enhancer.create();
        clientInstances.put(clientName, client);

        // 注册Spring 容器（如果可用）
        if (beanRegistry != null) {
            registerToSpring(clientName, client, DynamicHttpClient.class);
        }

        log.info("Successfully registered dynamic client: {}", clientName);
        return client;
    }

    /**
     * 获取已注册的客户
     *
     * @param clientName 客户端名
     * @param type       客户端类
     * @param <T>        类型参数
     * @return 客户端实
     */
    @SuppressWarnings("unchecked")
    public <T> T getClient(String clientName, Class<T> type) {
        Object client = clientInstances.get(clientName);
        if (client == null) {
            throw new DockingException("Client not found: " + clientName);
        }
        if (!type.isInstance(client)) {
            throw new DockingException("Client type mismatch. Expected: " + type.getName()
                + ", Actual: " + client.getClass().getName());
        }
        return (T) client;
    }

    /**
     * 获取动态客户端
     *
     * @param clientName 客户端名
     * @return 动态客户端实例
     */
    public DynamicHttpClient getDynamicClient(String clientName) {
        return getClient(clientName, DynamicHttpClient.class);
    }

    /**
     * 检查客户端是否存在
     *
     * @param clientName 客户端名
     * @return 是否存在
     */
    public boolean hasClient(String clientName) {
        return clientInstances.containsKey(clientName);
    }

    /**
     * 注销客户
     *
     * @param clientName 客户端名
     */
    public void unregisterClient(String clientName) {
        clientInstances.remove(clientName);
        restClients.remove(clientName);
        log.info("Unregistered client: {}", clientName);
    }

    /**
     * 创建 RestClient 实例
     */
    private RestClient createRestClient(DockingRestClientProperties.ClientConfig config) {
        RestClient.Builder builder = RestClient.builder();

        // 基础 URL
        if (config.getBaseUrl() != null) {
            builder.baseUrl(config.getBaseUrl());
        }

        // 请求工厂（配置超时）
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        Duration connectTimeout = config.getConnectTimeout() != null
            ? config.getConnectTimeout()
            : properties.getDefaults().getConnectTimeout();
        Duration readTimeout = config.getReadTimeout() != null
            ? config.getReadTimeout()
            : properties.getDefaults().getReadTimeout();

        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        builder.requestFactory(factory);

        // 默认请求
        Map<String, String> headers = properties.getDefaults().getHeaders();
        if (headers != null) {
            headers.forEach(builder::defaultHeader);
        }
        if (config.getHeaders() != null) {
            config.getHeaders().forEach(builder::defaultHeader);
        }

        // 拦截
        List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();

        // 认证拦截
        if (config.getAuth() != null) {
            ClientHttpRequestInterceptor authInterceptor = createAuthInterceptor(config.getAuth());
            if (authInterceptor != null) {
                interceptors.add(authInterceptor);
            }
        }

        // 自定义拦截器
        if (config.getInterceptors() != null) {
            for (String interceptorClass : config.getInterceptors()) {
                try {
                    Class<?> clazz = Class.forName(interceptorClass);
                    ClientHttpRequestInterceptor interceptor =
                        (ClientHttpRequestInterceptor) clazz.getDeclaredConstructor().newInstance();
                    interceptors.add(interceptor);
                } catch (Exception e) {
                    log.warn("Failed to create interceptor: {}", interceptorClass, e);
                }
            }
        }

        // 日志拦截器（默认添加
        interceptors.add(new DockingLoggingInterceptor());

        builder.requestInterceptors(list -> list.addAll(interceptors));

        return builder.build();
    }

    /**
     * 创建认证拦截
     */
    private ClientHttpRequestInterceptor createAuthInterceptor(
        DockingRestClientProperties.AuthConfig authConfig) {
        return switch (authConfig.getType()) {
            case "basic" -> new BasicAuthInterceptor(authConfig.getUsername(), authConfig.getPassword());
            case "bearer" -> (request, body, execution) -> {
                request.getHeaders().setBearerAuth(authConfig.getToken());
                return execution.execute(request, body);
            };
            case "apikey" -> (request, body, execution) -> {
                if ("header".equalsIgnoreCase(authConfig.getApiKeyIn())) {
                    request.getHeaders().add(authConfig.getApiKeyName(), authConfig.getApiKeyValue());
                } else if ("query".equalsIgnoreCase(authConfig.getApiKeyIn())) {
                    // URI 中添query 参数需要特殊处
                    // 这里简化处理，实际使用时可能需要更复杂的逻辑
                }
                return execution.execute(request, body);
            };
            case "custom" -> {
                try {
                    Class<?> clazz = Class.forName(authConfig.getCustomInterceptor());
                    yield (ClientHttpRequestInterceptor) clazz.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    log.error("Failed to create custom auth interceptor", e);
                    yield null;
                }
            }
            default -> null;
        };
    }

    /**
     * 注册Spring 容器
     */
    private <T> void registerToSpring(String name, T instance, Class<T> type) {
        if (beanRegistry == null) {
            return;
        }

        BeanDefinition beanDefinition = BeanDefinitionBuilder
            .genericBeanDefinition(type, () -> instance)
            .setScope(ConfigurableBeanFactory.SCOPE_SINGLETON)
            .getBeanDefinition();

        beanRegistry.registerBeanDefinition(name, beanDefinition);
        log.debug("Registered bean to Spring context: {}", name);
    }

    /**
     * 获取 RestClient 实例（用于编程式调用
     */
    public RestClient getRestClient(String clientName) {
        RestClient client = restClients.get(clientName);
        if (client == null) {
            throw new DockingException("RestClient not found: " + clientName);
        }
        return client;
    }

    /**
     * 动态客户端方法拦截
     */
    private static class DynamicClientInterceptor implements MethodInterceptor {

        private final RestClient restClient;
        private final DockingRestClientProperties.ClientConfig config;

        public DynamicClientInterceptor(RestClient restClient,
                                        DockingRestClientProperties.ClientConfig config) {
            this.restClient = restClient;
            this.config = config;
        }

        @Override
        public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
            String methodName = method.getName();

            // 处理 execute 方法
            if ("execute".equals(methodName) && args.length >= 2) {
                String endpoint = (String) args[0];
                Map<String, Object> params = (Map<String, Object>) args[1];
                return executeRequest(endpoint, params, method.getReturnType());
            }

            // 处理 request 方法
            if ("request".equals(methodName) && args.length == 1) {
                String endpoint = (String) args[0];
                return new RequestBuilder(restClient, endpoint);
            }

            // 其他方法
            return proxy.invokeSuper(obj, args);
        }

        private Object executeRequest(String endpoint, Map<String, Object> params, Class<?> returnType) {
            // 简化实现，实际应根据配置解HTTP 方法、路径等
            return restClient.post()
                .uri(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .body(params)
                .retrieve()
                .body(returnType);
        }
    }
}
