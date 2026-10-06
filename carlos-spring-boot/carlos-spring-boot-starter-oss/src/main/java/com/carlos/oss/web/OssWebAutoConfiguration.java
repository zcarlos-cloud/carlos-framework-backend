package com.carlos.oss.web;

import com.carlos.oss.config.OssAutoConfiguration;
import com.carlos.oss.config.OssProperties;
import com.carlos.oss.core.OssTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * <p>
 * OSS Web 自动配置
 * 注册 OssObject 返回值处理器
 * </p>
 *
 * @author carlos
 * @date 2026-03-06
 */
@AutoConfiguration(after = OssAutoConfiguration.class)
@ConditionalOnProperty(prefix = "carlos.oss", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnBean(OssTemplate.class)
public class OssWebAutoConfiguration implements WebMvcConfigurer {

    private final OssTemplate ossTemplate;

    public OssWebAutoConfiguration(OssTemplate ossTemplate) {
        this.ossTemplate = ossTemplate;
    }

    @Override
    public void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> handlers) {
        handlers.add(new OssObjectHandlerMethodReturnValueHandler(ossTemplate));
    }

    /**
     * 注册 OSS Controller
     */
    /**
     * OSS 接口控制器
     * <p>
     * {@code @ConditionalOnMissingBean} 是为了与组件扫描共存：{@link OssController} 上带
     * {@code @RestController}，宿主扫描根较宽（如启动类在 {@code com.carlos}）时会先扫到并注册一份。
     * 自动配置晚于组件扫描，此条件据此让路，避免同名 Bean 冲突
     * （{@code allow-bean-definition-overriding} 默认 false，冲突会让服务直接起不来）。
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "carlos.oss", name = "web.enabled", havingValue = "true", matchIfMissing = true)
    public OssController ossController(OssProperties properties, OssTemplate ossTemplate) {
        return new OssController(properties, ossTemplate);
    }
}
