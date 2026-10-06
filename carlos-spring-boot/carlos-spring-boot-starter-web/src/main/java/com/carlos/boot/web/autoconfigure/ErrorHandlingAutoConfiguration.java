package com.carlos.boot.web.autoconfigure;

import com.carlos.boot.web.error.CustomErrorAttributes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * 错误处理自动配置。
 *
 * @author Carlos
 * @since 3.0.0
 */
@Slf4j
@AutoConfiguration
public class ErrorHandlingAutoConfiguration {

    @Bean
    public CustomErrorAttributes customErrorAttributes() {
        return new CustomErrorAttributes();
    }
}
