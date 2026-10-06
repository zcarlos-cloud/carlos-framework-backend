package com.carlos.boot.web.autoconfigure;

import com.carlos.boot.web.config.WebProperties;
import com.carlos.boot.web.controller.ApplicationController;
import com.carlos.boot.web.enums.EnumDictService;
import com.carlos.boot.web.request.WebRequestContext;
import com.carlos.boot.web.resource.ResourceScanService;
import com.carlos.core.interfaces.ApplicationExtend;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * 应用信息自动配置。
 *
 * @author Carlos
 * @since 3.0.0
 */
@Slf4j
@AutoConfiguration
@RequiredArgsConstructor
public class ApplicationAutoConfiguration {

    private final WebProperties webProperties;
    private final ObjectProvider<ApplicationExtend> applicationExtendProvider;

    @PostConstruct
    public void initWebContext() {
        ApplicationExtend extend = applicationExtendProvider.getIfAvailable();
        WebRequestContext.init(extend);
        log.debug("WebRequestContext initialized: extend={}", extend != null ? extend.getClass().getSimpleName() : "null");
    }

    @Bean
    @ConditionalOnMissingBean
    public ApplicationController applicationController(@Nullable EnumDictService enumDictService, @Nullable ResourceScanService resourceScanService) {
        return new ApplicationController(webProperties, enumDictService, resourceScanService);
    }
}
