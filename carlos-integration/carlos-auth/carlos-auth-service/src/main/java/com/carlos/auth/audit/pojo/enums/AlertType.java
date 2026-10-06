package com.carlos.auth.audit.pojo.enums;

import com.carlos.auth.api.enums.AuthErrorCode;
import com.carlos.core.enums.AppEnum;
import com.carlos.core.enums.BaseEnum;

/**
 * <p>
 * 告警类型枚举
 * </p>
 *
 * @author Carlos
 * @date 2026-02-26
 */
@AppEnum(code = "alert_type")
public enum AlertType implements BaseEnum<String> {

    /**
     * 异地登录
     */
    UNUSUAL_LOCATION("unusual_location", "异地登录", "检测到用户从非常用地登录"),

    /**
     * 新设备登录
     */
    NEW_DEVICE("new_device", "新设备登录", "检测到用户从新设备登录"),

    /**
     * 暴力破解
     */
    BRUTE_FORCE("brute_force", "暴力破解", "检测到多次登录失败，可能存在暴力破解"),

    /**
     * 非工作时间登录
     */
    OFF_HOURS_LOGIN("off_hours_login", "非工作时间登录", "检测到用户在非工作时间（22:00-06:00）登录"),

    /**
     * 敏感操作
     */
    SENSITIVE_OPERATION("sensitive_operation", "敏感操作", "检测到用户执行敏感操作，建议确认"),

    /**
     * MFA被绕过
     */
    MFA_BYPASSED("mfa_bypassed", "MFA被绕过", "检测到绕过MFA验证的尝试");

    private final String code;
    private final String name;
    private final String description;

    AlertType(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    @Override
    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String getDesc() {
        return description;
    }

    /**
     * 根据code获取枚举
     *
     * @param code 代码
     * @return 枚举值
     */
    public static AlertType fromCode(String code) {
        for (AlertType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw AuthErrorCode.AUTH_ALERT_TYPE_UNKNOWN.exception(code);
    }
}
