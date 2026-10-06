package com.carlos.boot.web.filter;

import cn.hutool.extra.servlet.JakartaServletUtil;
import com.carlos.boot.web.filter.wrapper.CachedBodyRequestWrapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * 请求体缓存 Filter，在 Filter 链最前端将请求体缓存，供后续多次读取。
 *
 * <p>对于 multipart/form-data 请求透传不缓存。</p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@Slf4j
public class RequestBodyCacheFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        // Multipart 请求透传
        if (JakartaServletUtil.isMultipart(httpRequest)) {
            chain.doFilter(request, response);
            return;
        }

        // P2-008 修复：保持原始 Content-Type，不再默认设为 application/json
        String contentType = request.getContentType();

        CachedBodyRequestWrapper wrapper = new CachedBodyRequestWrapper(httpRequest, contentType);
        chain.doFilter(wrapper, response);
    }
}
