package com.carlos.monitor.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * <p>
 * 系统监控自动配置类
 * 注册 MonitorProperties 配置属性；采集器 SPI、sink、调度等组件由包扫描装配
 * （各组件自带 @ConditionalOnProperty 条件化开关）；
 * @EnableAspectJAutoProxy 支撑 @MonitorAudit 审计切面
 * </p>
 *
 * @author Carlos
 * @date 2026年10月1日
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableConfigurationProperties(MonitorProperties.class)
@ConditionalOnProperty(prefix = "carlos.monitor", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MonitorAutoConfiguration {

    /**
     * GET 请求枚举参数（@RequestParam/路径变量）按字典 code 绑定，与 Jackson 的 @JsonCreator 行为对齐
     */
    @Bean
    public BaseEnumConverterFactory baseEnumConverterFactory() {
        return new BaseEnumConverterFactory();
    }

}
