package com.carlos.audit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * <p>
 * 微服务启动类
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午4:24:06
 */
@EnableDiscoveryClient
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class AuditCloudApplication {

    public static void main(final String[] args) {
        SpringApplication.run(AuditCloudApplication.class, args);
    }
}
