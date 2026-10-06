package com.carlos.boot.web.autoconfigure;

import com.carlos.boot.web.advice.ResponseWrapAdvice;
import com.carlos.boot.web.config.WebProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * 统一响应包装自动配置。
 *
 * @author Carlos
 * @since 3.0.0
 */
@Slf4j
@AutoConfiguration
@RequiredArgsConstructor
public class ResponseWrapAutoConfiguration {

    private final WebProperties webProperties;

    @Bean
    @ConditionalOnProperty(prefix = "carlos.web.response-wrap", name = "enabled", havingValue = "true",
        matchIfMissing = true)
    public ResponseWrapAdvice responseWrapAdvice() {
        return new ResponseWrapAdvice(webProperties.getResponseWrap());
    }
}
