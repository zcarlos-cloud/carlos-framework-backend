package com.carlos.boot.web.filter.wrapper;

import cn.hutool.core.io.IoUtil;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.http.MediaType;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * 请求体缓存 Wrapper，解决 HttpServletRequest 中 InputStream 只能读取一次的问题。
 *
 * <p>这是三个旧 Wrapper（GlobalHttpServletRequestWrapper、GlobalBodyHttpServletRequestWrapper、
 * GlobalParamHttpServletRequestWrapper）的统一替代。</p>
 *
 * <p>内容类型处理策略：</p>
 * <ul>
 *   <li>application/json → 缓存原始 body，支持多次读取</li>
 *   <li>application/x-www-form-urlencoded → 缓存参数 Map</li>
 *   <li>multipart/form-data → 透传，不缓存（避免内存问题）</li>
 * </ul>
 *
 * @author Carlos
 * @since 3.0.0
 */
public class CachedBodyRequestWrapper extends HttpServletRequestWrapper {

    private final byte[] cachedBody;
    private final Map<String, String[]> cachedParams;
    private final String contentType;

    /**
     * 构造请求体缓存 Wrapper。
     *
     * @param request    原始请求
     * @param contentType 请求内容类型
     */
    public CachedBodyRequestWrapper(HttpServletRequest request, String contentType) throws IOException {
        super(request);
        this.contentType = contentType;

        if (isJsonRequest()) {
            // JSON 请求：缓存 body 字节
            this.cachedBody = IoUtil.readBytes(request.getInputStream());
            this.cachedParams = new HashMap<>(request.getParameterMap());
        } else if (isFormRequest()) {
            // Form 表单请求：缓存参数
            this.cachedBody = new byte[0];
            this.cachedParams = new HashMap<>(request.getParameterMap());
        } else {
            // 其他类型：透传
            this.cachedBody = IoUtil.readBytes(request.getInputStream());
            this.cachedParams = new HashMap<>(request.getParameterMap());
        }
    }

    // ==================== InputStream / Reader ====================

    @Override
    public ServletInputStream getInputStream() {
        if (isMultipartRequest()) {
            try {
                return super.getInputStream();
            } catch (IOException e) {
                throw new IllegalStateException("Failed to get original input stream", e);
            }
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(cachedBody);
        return new CachedServletInputStream(bais);
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }

    // ==================== 参数 ====================

    @Override
    public String getParameter(String name) {
        String[] values = cachedParams.get(name);
        if (values == null || values.length == 0) {
            return null;
        }
        return values[0];
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return Collections.unmodifiableMap(cachedParams);
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(cachedParams.keySet());
    }

    @Override
    public String[] getParameterValues(String name) {
        return cachedParams.get(name);
    }

    // ==================== 工具方法 ====================

    /**
     * 获取缓存的请求体内容（字符串）。
     */
    public String getBodyAsString() {
        return new String(cachedBody, StandardCharsets.UTF_8);
    }

    /**
     * 获取缓存的请求体内容（字节）。
     */
    public byte[] getBodyBytes() {
        return cachedBody.clone();
    }

    private boolean isJsonRequest() {
        return contentType != null && contentType.contains(MediaType.APPLICATION_JSON_VALUE);
    }

    private boolean isFormRequest() {
        return contentType != null &&
            (contentType.contains(MediaType.APPLICATION_FORM_URLENCODED_VALUE) ||
                contentType.contains(MediaType.MULTIPART_FORM_DATA_VALUE));
    }

    private boolean isMultipartRequest() {
        return contentType != null && contentType.contains(MediaType.MULTIPART_FORM_DATA_VALUE);
    }

    // ==================== 内部类 ====================

    private static class CachedServletInputStream extends ServletInputStream {

        private final ByteArrayInputStream source;

        CachedServletInputStream(ByteArrayInputStream source) {
            this.source = source;
        }

        @Override
        public int read() {
            return source.read();
        }

        @Override
        public boolean isFinished() {
            return source.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener listener) {
            throw new UnsupportedOperationException();
        }
    }
}
