package com.carlos.monitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * <p>
 * 监控模块微服务启动类
 * </p>
 *
 * @author Carlos
 * @date 2026年10月1日
 */
@EnableDiscoveryClient
@SpringBootApplication
public class MonitorCloudApplication {

    public static void main(final String[] args) {
        SpringApplication.run(MonitorCloudApplication.class, args);
    }
}
