package com.carlos.auth.util;

import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.event.Level;

/**
 * <p>
 * 敏感数据脱敏工具类
 * </p>
 *
 * <p>用于对日志中的敏感信息进行脱敏处理，防止信息泄露</p>
 *
 * @author Carlos
 * @date 2026-02-26
 */
public class SensitiveDataUtil {

    /**
     * 脱敏密码（全部替换为*）
     *
     * @param password 密码
     * @return 脱敏后的密码
     */
    public static String maskPassword(String password) {
        if (StrUtil.isBlank(password)) {
            return password;
        }
        return DesensitizedUtil.password(password);
    }

    /**
     * 脱敏手机号（保留前3位和后4位，中间用*替代）
     *
     * @param phone 手机号
     * @return 脱敏后的手机号
     */
    public static String maskPhone(String phone) {
        if (StrUtil.isBlank(phone)) {
            return phone;
        }
        return DesensitizedUtil.mobilePhone(phone);
    }

    /**
     * 脱敏邮箱（保留@前的第一个字符和@后的内容）
     *
     * @param email 邮箱
     * @return 脱敏后的邮箱
     */
    public static String maskEmail(String email) {
        if (StrUtil.isBlank(email)) {
            return email;
        }
        return DesensitizedUtil.email(email);
    }

    /**
     * 脱敏身份证号（保留前6位和后4位）
     *
     * @param idCard 身份证号
     * @return 脱敏后的身份证号
     */
    public static String maskIdCard(String idCard) {
        if (StrUtil.isBlank(idCard)) {
            return idCard;
        }
        return DesensitizedUtil.idCardNum(idCard, 6, 4);
    }

    /**
     * 脱敏银行卡号（保留前4位和后4位）
     *
     * @param bankCard 银行卡号
     * @return 脱敏后的银行卡号
     */
    public static String maskBankCard(String bankCard) {
        if (StrUtil.isBlank(bankCard)) {
            return bankCard;
        }
        return DesensitizedUtil.bankCard(bankCard);
    }

    /**
     * 脱敏JWT令牌（保留前10位和后10位）
     *
     * @param token JWT令牌
     * @return 脱敏后的令牌
     */
    public static String maskToken(String token) {
        if (StrUtil.isBlank(token) || token.length() < 30) {
            return token;
        }
        return token.substring(0, 10) + "****" + token.substring(token.length() - 10);
    }

    /**
     * 脱敏API密钥（保留前4位和后4位）
     *
     * @param apiKey API密钥
     * @return 脱敏后的密钥
     */
    public static String maskApiKey(String apiKey) {
        if (StrUtil.isBlank(apiKey) || apiKey.length() < 8) {
            return apiKey;
        }
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }

    /**
     * 脱敏通用字符串（保留前3位和后3位）
     *
     * @param text 文本
     * @return 脱敏后的文本
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

    /**
     * 脱敏地址（保留前6个字符）
     *
     * @param address 地址
     * @return 脱敏后的地址
     */
    public static String maskAddress(String address) {
        if (StrUtil.isBlank(address) || address.length() <= 6) {
            return address;
        }
        return address.substring(0, 6) + "****";
    }

    /**
     * 使用SLF4J记录脱敏日志
     *
     * @param logger 日志记录器
     * @param level 日志级别
     * @param message 消息模板
     * @param params 参数（敏感数据会自动脱敏）
     */
    public static void logWithMask(Logger logger, Level level, String message, Object... params) {
        Object[] maskedParams = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            maskedParams[i] = maskObject(params[i]);
        }

        switch (level) {
            case DEBUG:
                logger.debug(message, maskedParams);
                break;
            case INFO:
                logger.info(message, maskedParams);
                break;
            case WARN:
                logger.warn(message, maskedParams);
                break;
            case ERROR:
                logger.error(message, maskedParams);
                break;
            default:
                logger.trace(message, maskedParams);
        }
    }

    /**
     * 自动识别并脱敏对象
     *
     * @param obj 对象
     * @return 脱敏后的字符串
     */
    private static Object maskObject(Object obj) {
        if (obj == null) {
            return null;
        }

        String str = obj.toString();
        String lowerStr = str.toLowerCase();

        // 自动识别类型并脱敏
        if (lowerStr.contains("password") || lowerStr.contains("pwd")) {
            return maskPassword(str);
        }

        // 中国的手机号（11位，以1开头）
        if (str.matches("^1[3-9]\\d{9}$")) {
            return maskPhone(str);
        }

        // 邮箱格式
        if (str.contains("@") && str.contains(".")) {
            return maskEmail(str);
        }

        // JWT令牌格式（包含.分隔）
        if (str.matches("^[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+$")) {
            return maskToken(str);
        }

        // 身份证号（18位）
        if (str.matches("^\\d{17}[0-9Xx]$")) {
            return maskIdCard(str);
        }

        return str;
    }

    /**
     * 检查是否为敏感字段名
     *
     * @param fieldName 字段名
     * @return true-敏感字段，false-非敏感字段
     */
    public static boolean isSensitiveField(String fieldName) {
        if (StrUtil.isBlank(fieldName)) {
            return false;
        }

        String lowerName = fieldName.toLowerCase();
        String[] sensitiveWords = {
            "password", "pwd", "token", "secret", "key",
            "credential", "auth", "phone", "mobile",
            "email", "idcard", "bankcard", "credit"
        };

        for (String word : sensitiveWords) {
            if (lowerName.contains(word)) {
                return true;
            }
        }

        return false;
    }
}
