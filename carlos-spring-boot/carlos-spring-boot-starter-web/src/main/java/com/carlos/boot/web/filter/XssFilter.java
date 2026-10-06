package com.carlos.boot.web.filter;

import cn.hutool.extra.servlet.JakartaServletUtil;
import com.carlos.boot.web.filter.wrapper.XssRequestWrapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * XSS 跨站脚本攻击防护 Filter。
 *
 * <p>将原始请求包装为 {@link XssRequestWrapper}，对所有输入进行 HTML 转义/XSS 危险模式过滤。</p>
 * <p>对于 multipart/form-data 文件上传请求直接透传，避免提前消费上传流。</p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@Slf4j
public class XssFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        // P2-009 修复：multipart/form-data 请求直接透传，不包装为 XssRequestWrapper
        if (JakartaServletUtil.isMultipart(httpRequest)) {
            chain.doFilter(request, response);
            return;
        }

        chain.doFilter(new XssRequestWrapper(httpRequest), response);
    }
}
