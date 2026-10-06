package com.carlos.boot.web.autoconfigure;

import cn.hutool.core.map.MapUtil;
import com.carlos.boot.web.config.WebProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.HashMap;
import java.util.Map;

/**
 * Web MVC 基础配置（静态资源映射等）。
 *
 * @author Carlos
 * @since 3.0.0
 */
@Slf4j
@AutoConfiguration
@RequiredArgsConstructor
public class WebMvcAutoConfiguration implements WebMvcConfigurer {

    private final WebProperties webProperties;

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        Map<String, String> resourceHandlers = webProperties.getResourceHandlers();
        if (MapUtil.isEmpty(resourceHandlers)) {
            resourceHandlers = new HashMap<>();
        }
        resourceHandlers.put("/static/**", "classpath:/static/");
        resourceHandlers.forEach((key, value) -> {
            if (!registry.hasMappingForPattern(key)) {
                registry.addResourceHandler(key).addResourceLocations(value);
            }
        });
        log.debug("Static resource handlers configured: {}", resourceHandlers.keySet());
    }
}
