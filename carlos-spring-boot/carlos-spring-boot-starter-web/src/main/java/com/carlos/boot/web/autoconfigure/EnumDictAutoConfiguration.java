package com.carlos.boot.web.autoconfigure;

import com.carlos.boot.web.config.WebProperties;
import com.carlos.boot.web.enums.EnumDictService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * 枚举字典自动配置。
 *
 * @author Carlos
 * @since 3.0.0
 */
@Slf4j
@AutoConfiguration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "carlos.web.enum-dict", name = "enabled", havingValue = "true")
public class EnumDictAutoConfiguration {

    private final WebProperties webProperties;

    @Bean
    public EnumDictService enumDictService() {
        return new EnumDictService(webProperties.getEnumDict());
    }
}
