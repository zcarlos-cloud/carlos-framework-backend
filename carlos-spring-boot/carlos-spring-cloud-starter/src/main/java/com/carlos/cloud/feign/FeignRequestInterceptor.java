package com.carlos.cloud.feign;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.http.Header;
import com.alibaba.ttl.TransmittableThreadLocal;
import com.carlos.core.constant.CoreConstant;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Feign 请求拦截器
 * </p>
 *
 * <p>
 * 负责在微服务调用时传递必要的上下文信息，包括：
 * <ul>
 *   <li>请求头传递（Authorization、X-Request-Id 等）</li>
 *   <li>租户 ID 传递</li>
 *   <li>用户 ID 传递</li>
 *   <li>RPC 调用标记</li>
 * </ul>
 * </p>
 *
 * @author carlos
 * @date 2021/12/7 15:06
 */
@Slf4j
public class FeignRequestInterceptor implements RequestInterceptor {

    private final FeignProperties feignProperties;

    /**
     * 线程本地存储 - 用于非 Web 环境下的上下文传递
     */
    private static final TransmittableThreadLocal<Map<String, String>> CONTEXT_HOLDER = new TransmittableThreadLocal<>();

    public FeignRequestInterceptor(FeignProperties feignProperties) {
        this.feignProperties = feignProperties;
    }

    @Override
    public void apply(RequestTemplate requestTemplate) {
        Map<String, Collection<String>> headers = requestTemplate.headers();

        // 检查是否是内部平台请求
        Collection<String> platformReq = headers.get("Platform-Request");
        if (CollUtil.isNotEmpty(platformReq)) {
            return;
        }

        // 标记该请求为 Feign 请求
        requestTemplate.header(CoreConstant.HEADER_NAME_RPC, CoreConstant.HEADER_RPC_VALUE_FEIGN);

        // 从线程本地存储获取上下文（异步场景）
        Map<String, String> context = CONTEXT_HOLDER.get();
        if (context != null) {
            context.forEach(requestTemplate::header);
        }

        // 从当前 Web 请求获取上下文
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return;
        }

        HttpServletRequest request = attributes.getRequest();
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames == null) {
            return;
        }

        FeignProperties.HeaderProperties headerProps = feignProperties.getHeader();

        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            String value = request.getHeader(name);

            // 跳过忽略的请求头
            if (shouldIgnoreHeader(name, headerProps)) {
                continue;
            }

            // 根据配置选择性传递
            if (shouldPassHeader(name, headerProps)) {
                requestTemplate.header(name, value);
            }
        }
    }

    /**
     * 判断是否应该忽略该请求头
     */
    private boolean shouldIgnoreHeader(String name, FeignProperties.HeaderProperties headerProps) {
        String lowerName = name.toLowerCase();

        // 检查忽略列表
        if (headerProps.getIgnoreHeaders().contains(lowerName)) {
            return true;
        }

        // 跳过 content-length、content-type 等
        return CharSequenceUtil.equalsIgnoreCase(Header.CONTENT_LENGTH.getValue(), name)
            || CharSequenceUtil.equalsIgnoreCase(Header.CONTENT_TYPE.getValue(), name)
            || CharSequenceUtil.equalsIgnoreCase(Header.HOST.getValue(), name)
            || CharSequenceUtil.equalsIgnoreCase(Header.CONNECTION.getValue(), name)
            || CharSequenceUtil.equalsIgnoreCase("X-Forwarded-For", name)
            || CharSequenceUtil.equalsIgnoreCase(Header.REFERER.getValue(), name)
            || CharSequenceUtil.equalsIgnoreCase(Header.ORIGIN.getValue(), name);
    }

    /**
     * 判断是否应该传递该请求头
     */
    private boolean shouldPassHeader(String name, FeignProperties.HeaderProperties headerProps) {
        String lowerName = name.toLowerCase();

        // 如果配置了明确的传递列表，使用配置
        if (!headerProps.getPassHeaders().isEmpty()) {
            return headerProps.getPassHeaders().contains(lowerName);
        }

        // 检查特定请求头
        if (lowerName.equals("authorization") && headerProps.isPassAuthorization()) {
            return true;
        }
        if (lowerName.equals("x-tenant-id") && headerProps.isPassTenantId()) {
            return true;
        }
        if (lowerName.equals("x-user-id") && headerProps.isPassUserId()) {
            return true;
        }
        if (lowerName.equals("x-request-id") && headerProps.isPassRequestId()) {
            return true;
        }

        // 传递所有自定义请求头（以 x- 开头）
        return lowerName.startsWith("x-");
    }

    /**
     * 设置线程本地上下文（用于异步场景）
     */
    public static void setContext(Map<String, String> context) {
        CONTEXT_HOLDER.set(context);
    }

    /**
     * 添加单个上下文值
     */
    public static void setContextValue(String key, String value) {
        Map<String, String> context = CONTEXT_HOLDER.get();
        if (context == null) {
            context = new HashMap<>();
            CONTEXT_HOLDER.set(context);
        }
        context.put(key, value);
    }

    /**
     * 清除线程本地上下文
     */
    public static void clearContext() {
        CONTEXT_HOLDER.remove();
    }

    /**
     * 获取当前上下文
     */
    public static Map<String, String> getContext() {
        return CONTEXT_HOLDER.get();
    }
}
