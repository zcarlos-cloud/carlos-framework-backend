package com.carlos.gateway.security;

import cn.hutool.json.JSONUtil;
import com.carlos.core.response.CommonErrorCode;
import com.carlos.gateway.config.GlobalFilterOrder;
import com.carlos.gateway.util.ClientIpUtil;
import com.carlos.gateway.util.LogSanitizer;
import com.carlos.gateway.exception.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.BitSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * Web 应用防火墙（WAF）过滤器 - 性能优化版
 * 防护：SQL 注入、XSS、CSRF、路径遍历、敏感文件访问、命令注入
 * </p>
 * <p>
 * 优化点：
 * 1. 快速字符检查（BitSet），提前过滤安全请求
 * 2. 延迟日志参数计算，减少字符串拼接开销
 * 3. Header 检查优化，使用快速路径
 * </p>
 *
 * @author carlos
 * @date 2026/3/16
 * @updated 2026/4/10 性能优化：BitSet 快速检查
 * @updated 2026/6/6 移除 ThreadLocal Matcher，直接使用 Pattern.matcher()（现代 JVM 中 Matcher 创建开销极低）
 */
@Slf4j
public class WafFilter implements GlobalFilter, Ordered {

    private final WafProperties properties;

    // ========== 正则模式（预编译） ==========
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "('|\"|%27|%22|%25%27|%25%22)|(--|%2D%2D|#|%23)|(/\\*|%2F\\*)|(\\*/|%2A%2F)|" +
            "(union|UNION|%75%6E%69%6F%6E|%55%4E%49%4F%4E)|(select|SELECT|%73%65%6C%65%63%74)|" +
            "(insert|INSERT|%69%6E%73%65%72%74)|(delete|DELETE|%64%65%6C%65%74%65)|" +
            "(update|UPDATE|%75%70%64%61%74%65)|(drop|DROP|%64%72%6F%70)|(exec|EXEC|%65%78%65%63)",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern XSS_PATTERN = Pattern.compile(
        "(<script|SCRIPT|&lt;script|%3Cscript)|(javascript:|JAVASCRIPT:|%6A%61%76%61%73%63%72%69%70%74)|" +
            "(onerror|onload|onclick|onmouseover|ONERROR|ONLOAD)=|(<iframe|IFRAME|&lt;iframe)|" +
            "(alert\\(|confirm\\(|prompt\\()",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile(
        "(\\.\\./|\\.\\.\\\\|%2e%2e%2f|%2e%2e%5c)",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern SENSITIVE_FILE_PATTERN = Pattern.compile(
        "\\.(git|svn|htaccess|env|ini|log|sql|bak|backup|swp|old|dist)(/|$)|" +
            "(WEB-INF|META-INF|web\\.xml|server\\.xml|config\\.xml)|" +
            "/(api-docs|swagger-ui|actuator|health|metrics|prometheus)(/|$)",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern COMMAND_INJECTION_PATTERN = Pattern.compile(
        "(;|\\||&&|`\\$\\(\\(|%3B|%7C|%26%26|%60)|" +
            "(cat|ls|pwd|whoami|id|uname|wget|curl|nc|netcat|bash|sh|cmd|powershell)(\\s|%20)",
        Pattern.CASE_INSENSITIVE
    );

    // ========== 快速检查字符集（BitSet 优化） ==========
    private static final BitSet SQL_DANGEROUS_CHARS = new BitSet();
    private static final BitSet XSS_DANGEROUS_CHARS = new BitSet();
    private static final BitSet CMD_DANGEROUS_CHARS = new BitSet();

    static {
        // SQL 危险字符: ' " % - # * / ;
        "'\"%-#*/;".chars().forEach(SQL_DANGEROUS_CHARS::set);
        // XSS 危险字符: < > " ' % ( )
        "<>\"'%()".chars().forEach(XSS_DANGEROUS_CHARS::set);
        // 命令注入危险字符: ; | & ` $ ( )
        ";|&`$()".chars().forEach(CMD_DANGEROUS_CHARS::set);
    }

    public WafFilter(WafProperties properties) {
        this.properties = properties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!properties.isEnabled()) {
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 1. 检查路径（快速路径）
        if (checkPath(path)) {
            return blockRequest(exchange, "PATH_TRAVERSAL", "suspicious_path",
                "检测到可疑路径访问，请求已被拦截");
        }

        // 2. 检查查询参数
        String query = request.getURI().getQuery();
        if (query != null) {
            // SQL 注入检查
            if (properties.isSqlInjectionProtection() && containsSqlDangerousChars(query)) {
                if (SQL_INJECTION_PATTERN.matcher(query).find()) {
                    if (log.isWarnEnabled()) {
                        log.warn("WAF blocked SQL injection in query: {}",
                            LogSanitizer.truncate(LogSanitizer.sanitizeQuery(query), 100));
                    }
                    return blockRequest(exchange, "SQL_INJECTION", "query_string",
                        "检测到 SQL 注入攻击，请求已被拦截");
                }
            }

            // XSS 检查
            if (properties.isXssProtection() && containsXssDangerousChars(query)) {
                if (XSS_PATTERN.matcher(query).find()) {
                    return blockRequest(exchange, "XSS", "query_string",
                        "检测到 XSS 攻击，请求已被拦截");
                }
            }

            // 命令注入检查
            if (properties.isCommandInjectionProtection() && containsCmdDangerousChars(query)) {
                if (COMMAND_INJECTION_PATTERN.matcher(query).find()) {
                    return blockRequest(exchange, "COMMAND_INJECTION", "query_string",
                        "检测到命令注入攻击，请求已被拦截");
                }
            }
        }

        // 3. 检查请求头
        String headerViolation = checkHeaders(request.getHeaders());
        if (headerViolation != null) {
            return blockRequest(exchange, "MALICIOUS_HEADER", headerViolation,
                "检测到恶意请求头，请求已被拦截");
        }

        // 4. 检查请求体（POST/PUT 请求）
        if (properties.isCheckBody() &&
            (request.getMethod() == org.springframework.http.HttpMethod.POST ||
                request.getMethod() == org.springframework.http.HttpMethod.PUT ||
                request.getMethod() == org.springframework.http.HttpMethod.PATCH)) {
            return checkRequestBody(exchange, chain);
        }

        // 5. CSRF 检查
        if (properties.isCsrfProtection() && isCsrfRequest(request)) {
            if (!checkCsrfToken(request)) {
                return blockRequest(exchange, "CSRF", "invalid_token",
                    "CSRF Token 验证失败，请求已被拦截");
            }
        }

        return chain.filter(exchange);
    }

    /**
     * 快速字符检查 - SQL 危险字符
     */
    private boolean containsSqlDangerousChars(String input) {
        for (int i = 0; i < input.length(); i++) {
            if (SQL_DANGEROUS_CHARS.get(input.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 快速字符检查 - XSS 危险字符
     */
    private boolean containsXssDangerousChars(String input) {
        for (int i = 0; i < input.length(); i++) {
            if (XSS_DANGEROUS_CHARS.get(input.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 快速字符检查 - 命令注入危险字符
     */
    private boolean containsCmdDangerousChars(String input) {
        for (int i = 0; i < input.length(); i++) {
            if (CMD_DANGEROUS_CHARS.get(input.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查路径
     */
    private boolean checkPath(String path) {
        if (properties.isPathTraversalProtection()) {
            Matcher matcher = PATH_TRAVERSAL_PATTERN.matcher(path);
            if (matcher.find()) {
                if (log.isWarnEnabled()) {
                    log.warn("WAF: Path traversal detected: {}", path);
                }
                return true;
            }
        }
        if (properties.isSensitiveFileProtection()) {
            Matcher matcher = SENSITIVE_FILE_PATTERN.matcher(path);
            if (matcher.find()) {
                if (log.isWarnEnabled()) {
                    log.warn("WAF: Sensitive file access detected: {}", path);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * 检查请求头
     * @return 违规类型，如果没有违规返回 null
     */
    private String checkHeaders(HttpHeaders headers) {
        // 快速路径：检查常用 Header
        List<String> userAgent = headers.get(HttpHeaders.USER_AGENT);
        if (userAgent != null && properties.isSqlInjectionProtection()) {
            for (String value : userAgent) {
                if (containsSqlDangerousChars(value) && SQL_INJECTION_PATTERN.matcher(value).find()) {
                    if (log.isWarnEnabled()) {
                        log.warn("WAF: SQL injection in User-Agent: {}",
                            LogSanitizer.truncate(value, 100));
                    }
                    return HttpHeaders.USER_AGENT;
                }
            }
        }

        // 检查所有 Header（防御性检查）
        for (String headerName : headers.headerNames()) {
            // 跳过已检查的 Header
            if (HttpHeaders.USER_AGENT.equalsIgnoreCase(headerName)) {
                continue;
            }
            List<String> values = headers.get(headerName);
            if (values != null) {
                for (String value : values) {
                    if (properties.isSqlInjectionProtection() && containsSqlDangerousChars(value)) {
                        if (SQL_INJECTION_PATTERN.matcher(value).find()) {
                            if (log.isWarnEnabled()) {
                                log.warn("WAF: SQL injection in header {}: {}", headerName,
                                    LogSanitizer.sanitizeHeader(headerName, value));
                            }
                            return headerName;
                        }
                    }
                    if (properties.isXssProtection() && containsXssDangerousChars(value)) {
                        if (XSS_PATTERN.matcher(value).find()) {
                            if (log.isWarnEnabled()) {
                                log.warn("WAF: XSS in header {}: {}", headerName,
                                    LogSanitizer.sanitizeHeader(headerName, value));
                            }
                            return headerName;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * 检查请求体
     */
    private Mono<Void> checkRequestBody(ServerWebExchange exchange, GatewayFilterChain chain) {
        return DataBufferUtils.join(exchange.getRequest().getBody())
            .flatMap(dataBuffer -> {
                try {
                    int length = dataBuffer.readableByteCount();
                    // 限制检查体大小，避免大文件上传导致内存问题
                    if (length > properties.getMaxBodySize()) {
                        DataBufferUtils.release(dataBuffer);
                        return blockRequest(exchange, "PAYLOAD_TOO_LARGE", "request_body",
                            "请求体大小超过限制（" + properties.getMaxBodySize() + " 字节），请求已被拦截");
                    }

                    byte[] bytes = new byte[length];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);

                    String body = new String(bytes, StandardCharsets.UTF_8);

                    // SQL 注入检查
                    if (properties.isSqlInjectionProtection() && containsSqlDangerousChars(body)) {
                        if (SQL_INJECTION_PATTERN.matcher(body).find()) {
                            return blockRequest(exchange, "SQL_INJECTION", "request_body",
                                "检测到 SQL 注入攻击（请求体），请求已被拦截");
                        }
                    }

                    // XSS 检查
                    if (properties.isXssProtection() && containsXssDangerousChars(body)) {
                        if (XSS_PATTERN.matcher(body).find()) {
                            return blockRequest(exchange, "XSS", "request_body",
                                "检测到 XSS 攻击（请求体），请求已被拦截");
                        }
                    }

                    // 命令注入检查
                    if (properties.isCommandInjectionProtection() && containsCmdDangerousChars(body)) {
                        if (COMMAND_INJECTION_PATTERN.matcher(body).find()) {
                            return blockRequest(exchange, "COMMAND_INJECTION", "request_body",
                                "检测到命令注入攻击（请求体），请求已被拦截");
                        }
                    }

                    // 重新包装请求体
                    DataBuffer newBuffer = exchange.getResponse().bufferFactory().wrap(bytes);
                    ServerHttpRequest mutatedRequest = new ServerHttpRequestDecorator(exchange.getRequest()) {
                        @Override
                        public Flux<DataBuffer> getBody() {
                            return Flux.just(newBuffer);
                        }
                    };

                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                } catch (Exception e) {
                    DataBufferUtils.release(dataBuffer);
                    log.error("WAF: Error checking request body", e);
                    return blockRequest(exchange, "WAF_ERROR", "request_body",
                        "请求体安全检查失败，请求已被拦截");
                }
            })
            .onErrorResume(e -> {
                log.error("WAF: Error reading request body", e);
                return blockRequest(exchange, "WAF_ERROR", "request_body_read",
                    "读取请求体失败，请求已被拦截");
            });
    }

    /**
     * 判断是否是 CSRF 风险请求
     */
    private boolean isCsrfRequest(ServerHttpRequest request) {
        String contentType = request.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
        return contentType != null && (
            contentType.contains(MediaType.APPLICATION_FORM_URLENCODED_VALUE) ||
                contentType.contains(MediaType.MULTIPART_FORM_DATA_VALUE)
        );
    }

    /**
     * 检查 CSRF Token
     * <p>
     * 使用 Origin/Referer 精确域名匹配防止 CSRF 攻击。
     * 注意：网关层无会话状态，不验证 X-CSRF-Token（需下游服务实现双重提交 Cookie 模式）。
     * </p>
     */
    private boolean checkCsrfToken(ServerHttpRequest request) {
        String origin = request.getHeaders().getFirst(HttpHeaders.ORIGIN);
        String referer = request.getHeaders().getFirst(HttpHeaders.REFERER);

        // 至少需要一个头
        if (origin == null && referer == null) {
            if (log.isWarnEnabled()) {
                log.warn("WAF: CSRF check failed - missing Origin and Referer");
            }
            return false;
        }

        // 使用精确域名匹配，防止 startsWith 绕过（如 http://localhost:3000.attacker.com）
        if (origin != null && !isAllowedOrigin(origin)) {
            if (log.isWarnEnabled()) {
                log.warn("WAF: CSRF check failed - invalid origin: {}", origin);
            }
            return false;
        }
        if (referer != null && !isAllowedOrigin(referer)) {
            if (log.isWarnEnabled()) {
                log.warn("WAF: CSRF check failed - invalid referer: {}", referer);
            }
            return false;
        }

        return true;
    }

    /**
     * 验证 Origin/Referer 是否来自允许的域名
     * <p>使用精确 host + port 匹配，避免 startsWith 绕过</p>
     */
    private boolean isAllowedOrigin(String url) {
        try {
            String allowed = properties.getAllowedOrigin();
            if (allowed == null || allowed.isBlank()) {
                return false;
            }

            // 确保 URL 格式正确
            String normalizedAllowed = allowed.contains("://") ? allowed : "http://" + allowed;
            String normalizedActual = url.contains("://") ? url : "http://" + url;

            java.net.URI allowedUri = java.net.URI.create(normalizedAllowed);
            java.net.URI actualUri = java.net.URI.create(normalizedActual);

            String allowedHost = allowedUri.getHost();
            String actualHost = actualUri.getHost();
            if (allowedHost == null || !allowedHost.equalsIgnoreCase(actualHost)) {
                return false;
            }

            // 比较端口（处理默认端口）
            int allowedPort = allowedUri.getPort();
            int actualPort = actualUri.getPort();
            if (allowedPort == -1) {
                allowedPort = "https".equalsIgnoreCase(allowedUri.getScheme()) ? 443 : 80;
            }
            if (actualPort == -1) {
                actualPort = "https".equalsIgnoreCase(actualUri.getScheme()) ? 443 : 80;
            }
            return allowedPort == actualPort;
        } catch (Exception e) {
            log.warn("WAF: Failed to parse origin/referer URL: {}", url);
            return false;
        }
    }

    /**
     * 拦截请求，返回统一格式的错误响应
     */
    private Mono<Void> blockRequest(ServerWebExchange exchange, String ruleType, String ruleName, String message) {
        String path = exchange.getRequest().getURI().getPath();
        String clientIp = ClientIpUtil.getClientIp(exchange.getRequest());

        if (log.isWarnEnabled()) {
            log.warn("WAF blocked request from [{}] to [{}]: {} - ruleType={}, ruleName={}",
                clientIp, path, message, ruleType, ruleName);
        }

        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // 构建标准化的错误响应
        ErrorResponse errorResponse = ErrorResponse.builder()
            .status(HttpStatus.FORBIDDEN.value())
            .code(CommonErrorCode.FORBIDDEN.getCode())
            .msg(message)
            .path(path)
            .method(exchange.getRequest().getMethod() != null ? exchange.getRequest().getMethod().name() : "UNKNOWN")
            .extra(java.util.Map.of(
                "ruleType", ruleType,
                "ruleName", ruleName,
                "clientIp", clientIp
            ))
            .build();
        return response.writeWith(Mono.fromSupplier(() -> {
            byte[] bytes = JSONUtil.toJsonStr(errorResponse).getBytes(StandardCharsets.UTF_8);
            return response.bufferFactory().wrap(bytes);
        }));
    }

    @Override
    public int getOrder() {
        // 在统一白名单之后执行，尽早拦截恶意请求
        return GlobalFilterOrder.WAF_FILTER_ORDER;
    }
}
