package com.carlos.audit.util;

import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.StrUtil;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.StringNode;
import lombok.extern.slf4j.Slf4j;

/**
 * 敏感数据脱敏工具类
 *
 * <p>对审计日志中 JSON 格式的 payload、数据变更、对象快照等字段进行字段级脱敏</p>
 *
 * @author Carlos
 * @date 2026-06-12
 */
@Slf4j
public class SensitiveDataUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String[] SENSITIVE_FIELDS = {
        "password", "pwd", "token", "secret", "apikey", "api_key",
        "credential", "auth", "phone", "mobile", "email", "idcard",
        "id_card", "bankcard", "bank_card", "credit"
    };

    private SensitiveDataUtil() {
    }

    /**
     * 脱敏 JSON 字符串中的敏感字段值
     *
     * @param json JSON 字符串
     * @return 脱敏后的 JSON 字符串；解析失败时返回原字符串
     */
    public static String maskJson(String json) {
        if (StrUtil.isBlank(json)) {
            return json;
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(json);
            JsonNode masked = maskNode(root);
            return masked.toString();
        } catch (Exception e) {
            log.warn("Mask json failed, return original, error:{}", e.getMessage());
            return json;
        }
    }

    private static JsonNode maskNode(JsonNode node) {
        if (node == null || node.isNull()) {
            return node;
        }
        if (node.isObject()) {
            ObjectNode result = OBJECT_MAPPER.createObjectNode();
            node.properties().forEach(entry -> {
                String fieldName = entry.getKey();
                JsonNode value = entry.getValue();
                if (isSensitiveField(fieldName) && value.isTextual()) {
                    result.set(fieldName, StringNode.valueOf(maskValue(fieldName, value.asText())));
                } else {
                    result.set(fieldName, maskNode(value));
                }
            });
            return result;
        }
        if (node.isArray()) {
            ArrayNode result = OBJECT_MAPPER.createArrayNode();
            for (JsonNode element : node) {
                result.add(maskNode(element));
            }
            return result;
        }
        return node;
    }

    /**
     * 检查字段名是否敏感
     *
     * @param fieldName 字段名
     * @return true-敏感字段
     */
    public static boolean isSensitiveField(String fieldName) {
        if (StrUtil.isBlank(fieldName)) {
            return false;
        }
        String lowerName = fieldName.toLowerCase();
        for (String word : SENSITIVE_FIELDS) {
            if (lowerName.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private static String maskValue(String fieldName, String value) {
        String lowerName = fieldName.toLowerCase();
        if (lowerName.contains("password") || lowerName.contains("pwd")) {
            return maskPassword(value);
        }
        if (lowerName.contains("token")) {
            return maskToken(value);
        }
        if (lowerName.contains("apikey") || lowerName.contains("api_key") || lowerName.contains("secret")) {
            return maskApiKey(value);
        }
        if (lowerName.contains("phone") || lowerName.contains("mobile")) {
            return maskPhone(value);
        }
        if (lowerName.contains("email")) {
            return maskEmail(value);
        }
        if (lowerName.contains("idcard") || lowerName.contains("id_card")) {
            return maskIdCard(value);
        }
        if (lowerName.contains("bankcard") || lowerName.contains("bank_card")) {
            return maskBankCard(value);
        }
        return maskGeneric(value);
    }

    /**
     * 脱敏密码
     */
    public static String maskPassword(String password) {
        if (StrUtil.isBlank(password)) {
            return password;
        }
        return DesensitizedUtil.password(password);
    }

    /**
     * 脱敏手机号
     */
    public static String maskPhone(String phone) {
        if (StrUtil.isBlank(phone)) {
            return phone;
        }
        return DesensitizedUtil.mobilePhone(phone);
    }

    /**
     * 脱敏邮箱
     */
    public static String maskEmail(String email) {
        if (StrUtil.isBlank(email)) {
            return email;
        }
        return DesensitizedUtil.email(email);
    }

    /**
     * 脱敏身份证号
     */
    public static String maskIdCard(String idCard) {
        if (StrUtil.isBlank(idCard)) {
            return idCard;
        }
        return DesensitizedUtil.idCardNum(idCard, 6, 4);
    }

    /**
     * 脱敏银行卡号
     */
    public static String maskBankCard(String bankCard) {
        if (StrUtil.isBlank(bankCard)) {
            return bankCard;
        }
        return DesensitizedUtil.bankCard(bankCard);
    }

    /**
     * 脱敏 JWT 令牌
     */
    public static String maskToken(String token) {
        if (StrUtil.isBlank(token) || token.length() < 30) {
            return token;
        }
        return token.substring(0, 10) + "****" + token.substring(token.length() - 10);
    }

    /**
     * 脱敏 API 密钥
     */
    public static String maskApiKey(String apiKey) {
        if (StrUtil.isBlank(apiKey) || apiKey.length() < 8) {
            return apiKey;
        }
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }

    /**
     * 通用脱敏（保留前3位和后3位）
     */
    public static String maskGeneric(String text) {
        if (StrUtil.isBlank(text) || text.length() < 8) {
            return text;
        }
        if (text.length() <= 10) {
            return text.charAt(0) + "****" + text.charAt(text.length() - 1);
        }
        return text.substring(0, 3) + "****" + text.substring(text.length() - 3);
    }
}
