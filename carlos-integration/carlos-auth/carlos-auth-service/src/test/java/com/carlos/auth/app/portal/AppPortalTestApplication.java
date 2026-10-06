package com.carlos.auth.app.portal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * 应用门户测试专用最小 Spring Boot 应用。
 * <ul>
 *   <li>排除所有 manager/impl 子包（依赖 MyBatis Mapper/Redis）</li>
 *   <li>AppPortalTestConfig 提供 Mock 替代</li>
 * </ul>
 */
@SpringBootApplication
@ComponentScan(
        basePackages = "com.carlos.auth",
        excludeFilters = {
                @ComponentScan.Filter(
                        type = FilterType.REGEX,
                        pattern = "com\\.carlos\\.auth\\..*\\.impl\\..*"
                ),
                @ComponentScan.Filter(
                        type = FilterType.REGEX,
                        pattern = "com\\.carlos\\.auth\\.idp\\..*"
                ),
                @ComponentScan.Filter(
                        type = FilterType.REGEX,
                        pattern = "com\\.carlos\\.auth\\.apiimpl\\..*"
                ),
                @ComponentScan.Filter(
                        type = FilterType.REGEX,
                        pattern = "com\\.carlos\\.auth\\.sso\\..*"
                ),
                @ComponentScan.Filter(
                        type = FilterType.REGEX,
                        pattern = "com\\.carlos\\.auth\\.passkey\\..*"
                ),
                @ComponentScan.Filter(
                        type = FilterType.REGEX,
                        pattern = "com\\.carlos\\.auth\\.qrcode\\..*"
                ),
        }
)
public class AppPortalTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(AppPortalTestApplication.class, args);
    }
}
