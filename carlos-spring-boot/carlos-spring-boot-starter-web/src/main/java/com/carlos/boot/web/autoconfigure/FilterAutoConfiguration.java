package com.carlos.boot.web.autoconfigure;

import com.carlos.boot.web.config.WebProperties;
import com.carlos.boot.web.filter.RequestBodyCacheFilter;
import com.carlos.boot.web.filter.XssFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

/**
 * Servlet Filter 自动配置。
 *
 * @author Carlos
 * @since 3.0.0
 */
@Slf4j
@AutoConfiguration
@RequiredArgsConstructor
public class FilterAutoConfiguration {

    private final WebProperties webProperties;

    /**
     * 请求体缓存 Filter（解决请求体多次读取问题）。
     */
    @Bean
    @ConditionalOnProperty(prefix = "carlos.web.filter.body-cache", name = "enabled", havingValue = "true",
        matchIfMissing = true)
    public FilterRegistrationBean<RequestBodyCacheFilter> requestBodyCacheFilter() {
        var props = webProperties.getFilter().getBodyCache();
        FilterRegistrationBean<RequestBodyCacheFilter> filter =
            new FilterRegistrationBean<>(new RequestBodyCacheFilter());
        filter.setOrder(props.getOrder());
        filter.setEnabled(props.isEnabled());
        filter.addUrlPatterns(props.getUrlPatterns());
        return filter;
    }

    /**
     * XSS 防护 Filter。
     */
    @Bean
    @ConditionalOnProperty(prefix = "carlos.web.filter.xss", name = "enabled", havingValue = "true")
    public FilterRegistrationBean<XssFilter> xssFilter() {
        var props = webProperties.getFilter().getXss();
        FilterRegistrationBean<XssFilter> filter = new FilterRegistrationBean<>(new XssFilter());
        filter.setOrder(props.getOrder());
        filter.setEnabled(props.isEnabled());
        filter.addUrlPatterns(props.getUrlPatterns());
        return filter;
    }
}
