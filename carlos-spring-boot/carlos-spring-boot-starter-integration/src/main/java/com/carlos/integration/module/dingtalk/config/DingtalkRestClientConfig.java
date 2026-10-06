package com.carlos.integration.module.dingtalk.config;

import com.carlos.integration.module.dingtalk.api.DingtalkApiClient;
import com.carlos.integration.module.dingtalk.service.DingtalkRestClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.time.Duration;

/**
 * <p>
 * 钉钉 RestClient 配置
 * </p>
 *
 * @author Carlos
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@RequiredArgsConstructor
@EnableConfigurationProperties(DingtalkProperties.class)
@ConditionalOnProperty(prefix = "carlos.integration.dingtalk", name = "enabled", havingValue = "true")
public class DingtalkRestClientConfig {

    private final DingtalkProperties properties;

    /**
     * 创建钉钉专用RestClient
     */
    @Bean
    @ConditionalOnMissingBean(name = "dingtalkRestClient")
    public RestClient dingtalkRestClient() {
        log.info("Creating Dingtalk RestClient with baseUrl: {}", properties.getHost());

        // 配置请求工厂（超时设置）
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(30));

        return RestClient.builder()
            .requestFactory(requestFactory)
            .baseUrl(properties.getHost())
            .defaultHeader("Content-Type", "application/json")
            .build();
    }

    /**
     * 创建钉钉 API 客户端代
     */
    @Bean
    @ConditionalOnMissingBean(DingtalkApiClient.class)
    public DingtalkApiClient dingtalkApiClient(RestClient dingtalkRestClient) {
        HttpServiceProxyFactory factory = HttpServiceProxyFactory
            .builderFor(org.springframework.web.client.support.RestClientAdapter.create(dingtalkRestClient))
            .build();

        return factory.createClient(DingtalkApiClient.class);
    }

    /**
     * Token 管理
     */
    @Bean
    @ConditionalOnMissingBean(DingtalkAccessTokenManager.class)
    public DingtalkAccessTokenManager dingtalkAccessTokenManager() {
        return new DingtalkAccessTokenManager(properties);
    }

    /**
     * 钉钉初始化任
     */
    @Bean
    public DingtalkInitWorker dingtalkInitWorker(DingtalkAccessTokenManager tokenManager) {
        return new DingtalkInitWorker(tokenManager, properties.getAccessTokenRefreshCorn());
    }

    /**
     * 钉钉服务（RestClient 实现）
     */
    @Bean
    @ConditionalOnMissingBean(DingtalkRestClientService.class)
    public DingtalkRestClientService dingtalkRestClientService(DingtalkApiClient apiClient,
                                                               DingtalkAccessTokenManager tokenManager) {
        return new DingtalkRestClientService(apiClient, tokenManager, properties);
    }
}
