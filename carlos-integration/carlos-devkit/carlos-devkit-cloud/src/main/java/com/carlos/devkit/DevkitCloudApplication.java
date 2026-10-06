package com.carlos.devkit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * <p>
 * 开发工具平台微服务启动类
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@EnableDiscoveryClient
@SpringBootApplication
public class DevkitCloudApplication {

    public static void main(final String[] args) {
        SpringApplication.run(DevkitCloudApplication.class, args);
    }
}
