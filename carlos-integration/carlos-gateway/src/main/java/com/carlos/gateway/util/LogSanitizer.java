package com.carlos.gateway.util;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 日志敏感信息脱敏工具类
 * <p>
 * 对日志中的敏感字段进行掩码处理，防止敏感信息泄露到日志文件中。
 * 支持的敏感字段：password、token、authorization、secret、key、cookie、accessToken、refreshToken 等。
 * </p>
 *
 * @author carlos
 * @date 2026/6/7
 */
public final class LogSanitizer {

    private LogSanitizer() {
        // 工具类，禁止实例化
    }

    // 敏感字段名（大小写不敏感）
    private static final Set<String> SENSITIVE_KEYS = Set.of(
        "password", "passwd", "pwd",
        "token", "access_token", "refresh_token", "accessToken", "refreshToken",
        "authorization", "auth",
        "secret", "secret_key", "secretKey", "api_secret", "apiSecret",
        "key", "api_key", "apiKey", "private_key", "privateKey",
        "cookie", "cookies",
        "credit_card", "creditCard", "card_no", "cardNo",
        "phone", "mobile", "telephone",
        "id_card", "idCard", "idcard", "identity",
        "email"
    );

    // URL query 参数脱敏正则：匹配 key=value 中的敏感 key
    private static final Pattern QUERY_PARAM_PATTERN = Pattern.compile(
        "([?&])(" + String.join("|", SENSITIVE_KEYS) + ")=([^&]*)",
        Pattern.CASE_INSENSITIVE
    );

    // JSON 风格脱敏正则：匹配 "key": "value" 或 "key":"value" 中的敏感 key
    private static final Pattern JSON_PATTERN = Pattern.compile(
        "\"(" + String.join("|", SENSITIVE_KEYS) + ")\"\\s*:\\s*\"([^\"]*)\"",
        Pattern.CASE_INSENSITIVE
    );

    // Header 脱敏正则：匹配 HeaderName: value 中的敏感 Header
    private static final Pattern HEADER_PATTERN = Pattern.compile(
        "^(Authorization|Cookie|X-Auth-Token|X-API-Key|X-Secret-Key|Proxy-Authorization)\\s*:\\s*(.+)$",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * 脱敏 URL 查询字符串中的敏感参数
     *
     * @param query 查询字符串（如 "?password=123&name=test"）
     * @return 脱敏后的查询字符串
     */
    public static String sanitizeQuery(String query) {
        if (query == null || query.isEmpty()) {
            return query;
        }
        Matcher matcher = QUERY_PARAM_PATTERN.matcher(query);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(sb, matcher.group(1) + matcher.group(2) + "=***");
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 脱敏 JSON 字符串中的敏感字段
     *
     * @param json JSON 字符串
     * @return 脱敏后的 JSON 字符串
     */
    public static String sanitizeJson(String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }
        Matcher matcher = JSON_PATTERN.matcher(json);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(sb, "\"" + matcher.group(1) + "\":\"***\"");
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 脱敏 HTTP Header 值
     *
     * @param headerName  Header 名称
     * @param headerValue Header 值
     * @return 脱敏后的 Header 值，如果不是敏感 Header 则返回原值
     */
    public static String sanitizeHeader(String headerName, String headerValue) {
        if (headerName == null || headerValue == null) {
            return headerValue;
        }
        String lowerName = headerName.toLowerCase();
        if (lowerName.contains("authorization") || lowerName.contains("cookie")
            || lowerName.contains("token") || lowerName.contains("secret")
            || lowerName.contains("api-key")) {
            // 保留前 6 位，其余用 *** 替换（如果长度足够）
            if (headerValue.length() > 10) {
                return headerValue.substring(0, 6) + "***";
            }
            return "***";
        }
        return headerValue;
    }

    /**
     * 脱敏通用字符串（替换明显的敏感模式）
     *
     * @param input 原始字符串
     * @return 脱敏后的字符串
     */
    public static String sanitize(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        String result = sanitizeQuery(input);
        result = sanitizeJson(result);
        return result;
    }

    /**
     * 截断长字符串（用于日志）
     *
     * @param input 原始字符串
     * @param max   最大长度
     * @return 截断后的字符串
     */
    public static String truncate(String input, int max) {
        if (input == null || input.length() <= max) {
            return input;
        }
        return input.substring(0, max) + "...";
    }
}
