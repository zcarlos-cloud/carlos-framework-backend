package com.carlos.boot.web.autoconfigure;

import cn.hutool.extra.spring.EnableSpringUtil;
import com.carlos.boot.web.advice.GlobalExceptionHandler;
import com.carlos.boot.web.config.WebProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

/**
 * Web Starter 主自动配置入口，聚合所有子配置。
 *
 * @author Carlos
 * @since 3.0.0
 */
@Slf4j
@AutoConfiguration
@EnableSpringUtil
@EnableConfigurationProperties(WebProperties.class)
@Import({
    // 全局异常处理器：单体应用启动类在 com.carlos 子包，扫描不到 advice 包，需显式导入；
    // 已被组件扫描注册的应用会因同名同类自动去重，不会重复注册
    GlobalExceptionHandler.class,
    FilterAutoConfiguration.class,
    InterceptorAutoConfiguration.class,
    ConverterAutoConfiguration.class,
    ResponseWrapAutoConfiguration.class,
    EnumDictAutoConfiguration.class,
    ErrorHandlingAutoConfiguration.class,
    ResourceScanAutoConfiguration.class,
    WebMvcAutoConfiguration.class,
    ApplicationAutoConfiguration.class,
    RunnerAutoConfiguration.class
})
public class WebAutoConfiguration {

    public WebAutoConfiguration() {
        log.debug("Carlos Web Starter auto-configuration loaded");
    }
}
