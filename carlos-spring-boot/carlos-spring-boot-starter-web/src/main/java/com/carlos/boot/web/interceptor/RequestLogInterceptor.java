package com.carlos.boot.web.interceptor;

import com.carlos.boot.web.config.InterceptorProperties;
import com.carlos.boot.web.request.WebRequestContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

/**
 * 全局请求日志拦截器：记录请求/响应日志，管理 ThreadLocal 生命周期。
 *
 * @author Carlos
 * @since 3.0.0
 */
@Slf4j
public class RequestLogInterceptor implements AsyncHandlerInterceptor {

    private static final String START_TIME_ATTR = RequestLogInterceptor.class.getName() + ".startTime";

    private final InterceptorProperties.RequestLogProps properties;

    public RequestLogInterceptor(InterceptorProperties.RequestLogProps properties) {
        this.properties = properties;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        // 记录开始时间
        request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());

        var logType = properties.getLogType();
        if (logType == InterceptorProperties.RequestLogProps.LogType.REQUEST
            || logType == InterceptorProperties.RequestLogProps.LogType.BOTH) {
            log.info("[REQUEST] {} {} from {}",
                request.getMethod(), request.getRequestURI(), request.getRemoteAddr());
        }
        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                @NonNull Object handler, Exception ex) {
        var logType = properties.getLogType();
        if (logType == InterceptorProperties.RequestLogProps.LogType.BOTH) {
            long duration = 0;
            Object startTime = request.getAttribute(START_TIME_ATTR);
            if (startTime instanceof Long startMs) {
                duration = System.currentTimeMillis() - startMs;
            }
            log.info("[RESPONSE] {} {} {} {}ms",
                request.getMethod(), request.getRequestURI(), response.getStatus(), duration);
        }

        // 清理 ThreadLocal
        WebRequestContext.remove();
    }
}
