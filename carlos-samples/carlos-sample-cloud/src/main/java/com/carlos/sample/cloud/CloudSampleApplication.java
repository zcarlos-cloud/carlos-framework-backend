package com.carlos.sample.cloud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Carlos Spring Cloud 示例应用启动类
 *
 * <p>演示 Spring Cloud Alibaba 微服务治理能力，包括：</p>
 * <ul>
 *     <li>Nacos 服务注册与发现</li>
 *     <li>OpenFeign 声明式 HTTP 客户端</li>
 *     <li>Spring Cloud LoadBalancer 负载均衡</li>
 *     <li>Sentinel 流量控制（可选）</li>
 * </ul>
 *
 * <p>注意：此处<b>不可</b>再声明 {@code @EnableFeignClients}。carlos-spring-cloud-starter 的自动配置
 * {@code com.carlos.cloud.feign.FeignConfig} 已声明 {@code @EnableFeignClients(basePackages = "com.carlos")}，
 * 其扫描范围覆盖本包的 {@code com.carlos.sample.cloud.client}；两者叠加会让同一批 {@code @FeignClient}
 * 接口被注册两次，启动时抛 {@code BeanDefinitionOverrideException}。</p>
 *
 * @author carlos
 * @since 3.0.0
 */
@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = "com.carlos.sample.cloud")
public class CloudSampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(CloudSampleApplication.class, args);
    }

}
