package com.carlos.audit.api.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.carlos.core.enums.AppEnum;
import com.carlos.core.enums.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * 审计日志认证方式枚举
 * </p>
 *
 * @author Carlos
 * @date 2026-03-06
 */
@AppEnum(code = "AuditLogAuthType")
@Getter
@AllArgsConstructor
public enum AuditLogAuthTypeEnum implements BaseEnum {

    /**
     * 密码认证
     */
    PASSWORD(1, "密码认证", "Password"),

    /**
     * 短信验证码
     */
    SMS(2, "短信验证码", "SMS Code"),

    /**
     * OAuth2认证
     */
    OAUTH2(3, "OAuth2认证", "OAuth2"),

    /**
     * LDAP认证
     */
    LDAP(4, "LDAP认证", "LDAP"),

    /**
     * 证书认证
     */
    CERT(5, "证书认证", "Certificate"),

    /**
     * 扫码登录
     */
    QR_CODE(6, "扫码登录", "QR Code"),

    /**
     * 人脸识别
     */
    FACE(7, "人脸识别", "Face"),

    /**
     * 指纹认证
     */
    FINGERPRINT(8, "指纹认证", "Fingerprint");

    @EnumValue
    private final Integer code;

    private final String desc;

    /** 英文描述：随 /app/enums 下发，前端按当前语言取用 */
    private final String descEn;

    @Override
    public Integer getCode() {
        return code;
    }

    @Override
    public String getDesc() {
        return desc;
    }
}
