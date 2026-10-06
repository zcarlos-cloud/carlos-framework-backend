package com.carlos.boot.web.filter.wrapper;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.http.MediaType;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * XSS 请求 Wrapper，对 HTTP 请求输入进行安全转义/过滤。
 *
 * <p>处理策略：</p>
 * <ul>
 *   <li>JSON body — 移除已知 XSS 危险模式（保留 JSON 结构）</li>
 *   <li>Form 参数 / Query String — HTML 实体转义</li>
 *   <li>Header — 移除 CR/LF 防止 HTTP 响应拆分</li>
 *   <li>非 JSON 请求体（XML、纯文本、二进制等）— 原样透传，避免破坏业务数据</li>
 * </ul>
 *
 * @author Carlos
 * @since 3.0.0
 */
public class XssRequestWrapper extends HttpServletRequestWrapper {

    private static final Pattern[] XSS_PATTERNS = {
        Pattern.compile("<script[^>]*>[\\s\\S]*?</script>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("on\\w+\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<iframe[^>]*>[\\s\\S]*?</iframe>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<object[^>]*>[\\s\\S]*?</object>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<embed[^>]*>", Pattern.CASE_INSENSITIVE),
    };

    private final byte[] filteredBody;

    public XssRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);
        this.filteredBody = filterBody(request);
    }

    // ==================== 参数过滤 ====================

    @Override
    public String getQueryString() {
        return cleanXss(super.getQueryString());
    }

    @Override
    public String getParameter(String name) {
        String value = super.getParameter(name);
        return cleanXss(value);
    }

    @Override
    public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(name);
        if (ArrayUtils.isEmpty(values)) {
            return values;
        }
        ArrayList<String> escaped = new ArrayList<>(values.length);
        for (String value : values) {
            escaped.add(cleanXss(value));
        }
        return escaped.toArray(String[]::new);
    }

    // ==================== Header 过滤 ====================

    @Override
    public String getHeader(String name) {
        String value = super.getHeader(name);
        return cleanHeader(value);
    }

    // ==================== Attribute 过滤 ====================

    @Override
    public Object getAttribute(String name) {
        Object value = super.getAttribute(name);
        if (value instanceof String str && StrUtil.isNotBlank(str)) {
            return cleanXss(str);
        }
        return value;
    }

    // ==================== InputStream / Reader ====================

    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream bais = new ByteArrayInputStream(filteredBody);
        return new ServletInputStream() {
            @Override
            public int read() {
                return bais.read();
            }

            @Override
            public boolean isFinished() {
                return bais.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener listener) {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }

    // ==================== 内部方法 ====================

    private byte[] filterBody(HttpServletRequest request) throws IOException {
        byte[] rawBody = request.getInputStream().readAllBytes();
        if (rawBody.length == 0) {
            return rawBody;
        }

        String contentType = request.getContentType();
        if (contentType == null || !isJsonContentType(contentType)) {
            // P0-003/P2-010 修复：非 JSON 请求体原样透传，避免破坏 XML/纯文本/二进制数据
            return rawBody;
        }

        // JSON 请求体：过滤 XSS 危险模式
        String bodyStr = new String(rawBody, StandardCharsets.UTF_8);
        String filtered = filterXssPatterns(bodyStr);
        return filtered.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 判断 Content-Type 是否为 JSON（忽略大小写，兼容 application/*+json）
     */
    private boolean isJsonContentType(String contentType) {
        try {
            MediaType mediaType = MediaType.parseMediaType(contentType);
            return MediaType.APPLICATION_JSON.includes(mediaType)
                || mediaType.getSubtype().endsWith("+json");
        } catch (Exception e) {
            // 解析失败时回退到大小写不敏感的包含判断
            String lower = contentType.toLowerCase();
            return lower.contains("application/json") || lower.contains("+json");
        }
    }

    private String filterXssPatterns(String value) {
        if (StrUtil.isBlank(value)) {
            return value;
        }
        String result = value;
        for (Pattern pattern : XSS_PATTERNS) {
            result = pattern.matcher(result).replaceAll("[XSS_FILTERED]");
        }
        return result;
    }

    private String cleanXss(String value) {
        if (value == null) {
            return null;
        }
        return StringEscapeUtils.escapeHtml4(value);
    }

    /**
     * Header 值过滤：移除 CR/LF 防止 HTTP 响应拆分攻击。
     */
    private String cleanHeader(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("\r", "").replace("\n", "");
    }
}
