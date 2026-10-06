package com.carlos.auth.security.filter;

import com.carlos.auth.login.PublicKeyController;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;

/**
 * 密码传输解密过滤器
 *
 * <p>拦截 Spring Security 表单登录 {@code POST /login}，
 * 当前端提交 SM2 加密密码时，先解密再交给 {@code UsernamePasswordAuthenticationFilter}。</p>
 *
 * <h3>处理逻辑：</h3>
 * <ol>
 *   <li>仅处理 {@code POST /login}</li>
 *   <li>读取 {@code encrypted} 和 {@code keyId} 参数</li>
 *   <li>若 {@code encrypted=true}，使用 Redis 中的临时私钥解密 {@code password}</li>
 *   <li>将 {@code password} 替换为明文后继续过滤器链</li>
 *   <li>解密完成后删除 Redis 私钥（单次使用）</li>
 * </ol>
 *
 * @author carlos
 * @version 1.0.0
 * @since 2026-07-09
 */
@Slf4j
public class PasswordDecryptFilter extends OncePerRequestFilter {

    /**
     * 登录处理 URL
     */
    private static final String LOGIN_URL = "/login";

    /**
     * 密码参数字段名（与 Spring Security formLogin 配置一致）
     */
    private static final String PASSWORD_PARAM = "password";

    /**
     * 加密标识字段名
     */
    private static final String ENCRYPTED_PARAM = "encrypted";

    /**
     * 密钥 ID 字段名
     */
    private static final String KEY_ID_PARAM = "keyId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (!requiresDecryption(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String keyId = request.getParameter(KEY_ID_PARAM);
        String encryptedFlag = request.getParameter(ENCRYPTED_PARAM);
        String cipherPassword = request.getParameter(PASSWORD_PARAM);

        if (!isEncryptedRequest(encryptedFlag, keyId, cipherPassword)) {
            // 未加密请求直接放行（兼容旧模式）
            filterChain.doFilter(request, response);
            return;
        }

        String plainPassword = PublicKeyController.decryptPassword(keyId, cipherPassword);
        if (!StringUtils.hasText(plainPassword)) {
            log.warn("Password decryption failed, keyId={}", keyId);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "密码解密失败，请刷新页面后重试");
            return;
        }

        // 包装请求，将 password 替换为明文
        DecryptedRequestWrapper wrapper = new DecryptedRequestWrapper(request, plainPassword);
        filterChain.doFilter(wrapper, response);
    }

    /**
     * 判断是否是需要解密的登录请求
     */
    private boolean requiresDecryption(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod())
            && LOGIN_URL.equals(request.getRequestURI())
            && isFormContentType(request.getContentType());
    }

    /**
     * 判断是否为表单内容类型
     */
    private boolean isFormContentType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return false;
        }
        return contentType.startsWith(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
            || contentType.startsWith(MediaType.MULTIPART_FORM_DATA_VALUE);
    }

    /**
     * 判断请求是否携带了加密参数
     */
    private boolean isEncryptedRequest(String encryptedFlag, String keyId, String cipherPassword) {
        return Boolean.parseBoolean(encryptedFlag)
            && StringUtils.hasText(keyId)
            && StringUtils.hasText(cipherPassword);
    }

    /**
     * 解密后的请求包装器
     *
     * <p>将 {@code password} 参数替换为明文，其他参数保持不变。</p>
     */
    private static class DecryptedRequestWrapper extends HttpServletRequestWrapper {

        private final Map<String, String[]> parameterMap;

        public DecryptedRequestWrapper(HttpServletRequest request, String plainPassword) {
            super(request);
            this.parameterMap = new HashMap<>(request.getParameterMap());
            this.parameterMap.put(PASSWORD_PARAM, new String[]{plainPassword});
            // 移除加密标识，避免下游业务误判
            this.parameterMap.remove(ENCRYPTED_PARAM);
            this.parameterMap.remove(KEY_ID_PARAM);
        }

        @Override
        public String getParameter(String name) {
            String[] values = parameterMap.get(name);
            return values != null && values.length > 0 ? values[0] : null;
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            return Collections.unmodifiableMap(parameterMap);
        }

        @Override
        public Enumeration<String> getParameterNames() {
            return Collections.enumeration(parameterMap.keySet());
        }

        @Override
        public String[] getParameterValues(String name) {
            return parameterMap.get(name);
        }
    }
}
