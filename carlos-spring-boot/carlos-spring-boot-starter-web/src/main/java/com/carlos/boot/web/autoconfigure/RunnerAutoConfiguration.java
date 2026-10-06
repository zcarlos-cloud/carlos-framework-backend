package com.carlos.boot.web.autoconfigure;

import com.carlos.boot.web.runner.StartupBannerRunner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * 应用启动器自动配置。
 *
 * @author Carlos
 * @since 3.0.0
 */
@Slf4j
@AutoConfiguration
public class RunnerAutoConfiguration {

    @Bean
    public ApplicationRunner startupBannerRunner(Environment environment) {
        return new StartupBannerRunner(environment);
    }
}
