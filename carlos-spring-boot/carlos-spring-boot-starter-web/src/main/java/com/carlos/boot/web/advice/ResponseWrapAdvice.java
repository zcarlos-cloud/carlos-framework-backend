package com.carlos.boot.web.advice;

import com.carlos.boot.web.annotation.NoWrap;
import com.carlos.boot.web.config.ResponseWrapProperties;
import com.carlos.core.response.Result;
import com.carlos.json.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 统一响应包装 Advice。
 *
 * <p>在 Controller 执行 return 之后、response 返回给客户端之前，自动将非 {@link Result} 类型的返回值
 * 包装为 {@link Result#success(Object)}。支持通过以下方式跳过包装：</p>
 * <ul>
 *   <li>{@link NoWrap} 注解 — 类级别或方法级别</li>
 *   <li>配置项 {@code carlos.web.response-wrap.exclude-paths} — Ant 风格路径匹配</li>
 *   <li>配置项 {@code carlos.web.response-wrap.exclude-packages} — 包名前缀匹配</li>
 * </ul>
 *
 * <p><strong>注意：</strong>必须标注 {@link ControllerAdvice}。Spring Framework 7 起
 * {@code RequestMappingHandlerAdapter} 仅通过 {@code ControllerAdviceBean.findAnnotatedBeans()}
 * 收集 {@link ResponseBodyAdvice}，未标注该注解的 Advice Bean 不会被注册，包装会静默失效。</p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@Slf4j
@RequiredArgsConstructor
@ControllerAdvice
public class ResponseWrapAdvice implements ResponseBodyAdvice<Object> {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    /** 默认排除的框架包（Swagger/OpenAPI/Actuator） */
    private static final Set<String> DEFAULT_EXCLUDE_PACKAGES = Set.of(
        "org.springdoc",
        "io.swagger",
        "springfox",
        "org.openapitools",
        "org.springframework.boot.actuate"
    );

    private final ResponseWrapProperties properties;

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        if (!properties.isEnabled()) {
            return false;
        }

        if (returnType == null) {
            return false;
        }

        Class<?> declaringClass = returnType.getDeclaringClass();
        String className = declaringClass.getName();

        // 过滤框架端点（可配置 + 默认排除）
        Set<String> excludePkgs = new HashSet<>(DEFAULT_EXCLUDE_PACKAGES);
        List<String> configuredPkgs = properties.getExcludePackages();
        if (configuredPkgs != null) {
            excludePkgs.addAll(configuredPkgs);
        }
        for (String pkg : excludePkgs) {
            if (className.startsWith(pkg)) {
                return false;
            }
        }

        // 仅处理 @RestController
        if (declaringClass.getAnnotation(RestController.class) == null) {
            return false;
        }

        // @NoWrap 跳过包装
        if (declaringClass.isAnnotationPresent(NoWrap.class)
            || (returnType.getMethod() != null && returnType.getMethod().isAnnotationPresent(NoWrap.class))) {
            return false;
        }

        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        // 排除路径检查
        String path = request.getURI().getPath();
        List<String> excludePaths = properties.getExcludePaths();
        if (excludePaths != null) {
            for (String excludePath : excludePaths) {
                if (PATH_MATCHER.match(excludePath, path)) {
                    return body;
                }
            }
        }

        // 已经是包装类型则跳过
        if (body instanceof Result) {
            return body;
        }

        // String 类型特殊处理
        if (body instanceof String strBody) {
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            try {
                return JsonUtils.toJson(Result.success(strBody));
            } catch (Exception e) {
                log.error("包装 String 响应失败", e);
                return body;
            }
        }

        return Result.success(body);
    }
}
