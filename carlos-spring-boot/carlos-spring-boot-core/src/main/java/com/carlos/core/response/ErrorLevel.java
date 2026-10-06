package com.carlos.core.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 错误级别枚举
 * <p>
 * 错误码的第一位数字表示错误级别：
 * <ul>
 *   <li>0 - 成功</li>
 *   <li>1 - 客户端错误（请求参数问题）</li>
 *   <li>2 - 业务错误（业务规则限制）</li>
 *   <li>3 - 第三方服务错误</li>
 *   <li>5 - 系统错误（内部故障）</li>
 * </ul>
 *
 * @author carlos
 * @since 3.0.0
 */
@Getter
@RequiredArgsConstructor
public enum ErrorLevel {

    /**
     * 成功
     */
    SUCCESS('0', "成功"),

    /**
     * 客户端错误 - 用户请求参数问题
     */
    CLIENT_ERROR('1', "客户端错误"),

    /**
     * 业务错误 - 业务规则限制
     */
    BUSINESS_ERROR('2', "业务错误"),

    /**
     * 第三方服务错误 - 外部服务调用失败
     */
    THIRD_PARTY_ERROR('3', "第三方服务错误"),

    /**
     * 系统错误 - 系统内部故障
     */
    SYSTEM_ERROR('5', "系统错误");

    private final char code;
    private final String description;

    /**
     * 根据代码字符获取错误级别
     *
     * @param code 级别代码字符
     * @return ErrorLevel
     */
    public static ErrorLevel fromCode(char code) {
        for (ErrorLevel level : values()) {
            if (level.code == code) {
                return level;
            }
        }
        return SYSTEM_ERROR;
    }

    /**
     * 根据代码字符串获取错误级别
     *
     * @param codeStr 级别代码字符串
     * @return ErrorLevel
     */
    public static ErrorLevel fromCode(String codeStr) {
        if (codeStr == null || codeStr.isEmpty()) {
            return SYSTEM_ERROR;
        }
        return fromCode(codeStr.charAt(0));
    }

    /**
     * 根据错误码获取错误级别
     * <p>
     * 错误码格式为5位数字字符串，第一位表示级别
     *
     * @param errorCode 错误码，如 "20101"
     * @return ErrorLevel
     */
    public static ErrorLevel fromErrorCode(String errorCode) {
        if (errorCode == null || errorCode.length() < 5) {
            return SYSTEM_ERROR;
        }
        return fromCode(errorCode.charAt(0));
    }

    /**
     * 判断是否为成功级别
     *
     * @return true 表示成功
     */
    public boolean isSuccess() {
        return this == SUCCESS;
    }

    /**
     * 判断是否为客户端错误
     *
     * @return true 表示客户端错误
     */
    public boolean isClientError() {
        return this == CLIENT_ERROR;
    }

    /**
     * 判断是否为业务错误
     *
     * @return true 表示业务错误
     */
    public boolean isBusinessError() {
        return this == BUSINESS_ERROR;
    }

    /**
     * 判断是否为第三方服务错误
     *
     * @return true 表示第三方错误
     */
    public boolean isThirdPartyError() {
        return this == THIRD_PARTY_ERROR;
    }

    /**
     * 判断是否为系统错误
     *
     * @return true 表示系统错误
     */
    public boolean isSystemError() {
        return this == SYSTEM_ERROR;
    }

    /**
     * 是否需要记录堆栈
     * <p>
     * 系统错误和第三方错误通常需要记录堆栈以便排查问题
     *
     * @return true 表示需要记录堆栈
     */
    public boolean shouldLogStackTrace() {
        return this == SYSTEM_ERROR || this == THIRD_PARTY_ERROR;
    }

    /**
     * 是否需要告警
     * <p>
     * 系统错误和第三方错误通常需要触发告警
     *
     * @return true 表示需要告警
     */
    public boolean shouldAlert() {
        return this == SYSTEM_ERROR || this == THIRD_PARTY_ERROR;
    }
}
