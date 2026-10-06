package com.carlos.boot.web.autoconfigure;

import com.carlos.boot.web.WebConstant;
import com.carlos.boot.web.config.WebProperties;
import com.carlos.boot.web.interceptor.RequestLogInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.lang.NonNull;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Set;

/**
 * HandlerInterceptor 自动配置。
 *
 * @author Carlos
 * @since 3.0.0
 */
@Slf4j
@AutoConfiguration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "carlos.web.interceptor.request-log", name = "enabled", havingValue = "true",
    matchIfMissing = true)
public class InterceptorAutoConfiguration implements WebMvcConfigurer {

    private final WebProperties webProperties;

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        var props = webProperties.getInterceptor().getRequestLog();
        Set<String> includePaths = props.getIncludePaths();
        if (CollectionUtils.isEmpty(includePaths)) {
            includePaths = Set.of(WebConstant.ALL_PATH);
        }

        registry.addInterceptor(new RequestLogInterceptor(props))
            .addPathPatterns(includePaths.toArray(String[]::new))
            .excludePathPatterns(props.getEffectiveExcludePaths().toArray(String[]::new));

        log.debug("RequestLogInterceptor registered: logType={}", props.getLogType());
    }
}
