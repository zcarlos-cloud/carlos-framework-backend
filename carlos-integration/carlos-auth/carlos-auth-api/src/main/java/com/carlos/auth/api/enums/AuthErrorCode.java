package com.carlos.auth.api.enums;

import com.carlos.core.exception.BusinessException;
import com.carlos.core.response.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 认证授权模块错误码
 * <p>
 * 模块编码：02（认证授权服务）
 * <p>
 * 错误码格式：A-BB-CC
 * <ul>
 *   <li>A - 错误级别：1客户端错误，2业务错误，3第三方错误，5系统错误</li>
 *   <li>BB - 模块编码：02认证授权</li>
 *   <li>CC - 具体错误序号</li>
 * </ul>
 *
 * @author carlos
 * @since 3.0.0
 */
@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    // ==================== 客户端错误 (1-02-xx) ====================
    AUTH_PARAM_CLIENT_ID_MISSING("10201", "客户端ID不能为空", 400),
    AUTH_PARAM_CLIENT_SECRET_MISSING("10202", "客户端密钥不能为空", 400),
    AUTH_PARAM_GRANT_TYPE_INVALID("10203", "授权类型无效", 400),
    AUTH_PARAM_REDIRECT_URI_INVALID("10204", "重定向URI无效", 400),
    AUTH_PARAM_SCOPE_INVALID("10205", "授权范围无效", 400),
    AUTH_PARAM_CODE_INVALID("10206", "授权码无效或已过期", 400),
    AUTH_PARAM_ID_MISSING("10207", "ID不能为空", 400),
    AUTH_PARAM_USERNAME_MISSING("10208", "用户名不能为空", 400),
    AUTH_PARAM_PASSWORD_MISSING("10209", "密码不能为空", 400),
    AUTH_PARAM_PHONE_MISSING("10210", "手机号不能为空", 400),
    AUTH_PARAM_EMAIL_MISSING("10211", "邮箱不能为空", 400),
    AUTH_PARAM_VERIFICATION_CODE_MISSING("10212", "验证码不能为空", 400),
    AUTH_PARAM_INVALID("10213", "请求参数无效", 400),

    // ==================== 业务错误 (2-02-xx) ====================
    // 客户端相关 (20201-20220)
    AUTH_CLIENT_NOT_FOUND("20201", "应用客户端不存在", 404),
    AUTH_CLIENT_DISABLED("20202", "应用客户端已被禁用", 403),
    AUTH_CLIENT_SECRET_ERROR("20203", "客户端密钥错误", 401),
    AUTH_CLIENT_GRANT_TYPE_NOT_SUPPORTED("20204", "该客户端不支持此授权类型", 400),
    AUTH_CLIENT_SCOPE_NOT_SUPPORTED("20205", "该客户端不支持此授权范围", 403),
    AUTH_CLIENT_NAME_EXISTS("20206", "应用名称已存在", 400),
    AUTH_CLIENT_ADD_FAILED("20207", "添加应用客户端失败", 500),
    AUTH_CLIENT_UPDATE_FAILED("20208", "更新应用客户端失败", 500),
    AUTH_CLIENT_DELETE_FAILED("20209", "删除应用客户端失败", 500),
    AUTH_CLIENT_EXPORT_FAILED("20210", "导出应用客户端失败", 500),
    AUTH_CLIENT_IMPORT_FAILED("20211", "导入应用客户端失败", 500),

    // Token 相关 (20211-20230)
    AUTH_TOKEN_EXPIRED("20211", "访问令牌已过期", 401),
    AUTH_TOKEN_INVALID("20212", "访问令牌无效", 401),
    AUTH_TOKEN_REVOKED("20213", "访问令牌已被吊销", 401),
    AUTH_REFRESH_TOKEN_EXPIRED("20214", "刷新令牌已过期", 401),
    AUTH_REFRESH_TOKEN_INVALID("20215", "刷新令牌无效", 401),
    AUTH_TOKEN_GENERATE_FAILED("20216", "令牌生成失败", 500),
    AUTH_TOKEN_PARSE_FAILED("20217", "令牌解析失败", 500),
    AUTH_TOKEN_REFRESH_FAILED("20218", "令牌刷新失败", 400),

    // 用户认证相关 (20231-20250)
    AUTH_USER_AUTH_FAILED("20231", "用户认证失败", 401),
    AUTH_USER_CREDENTIALS_EXPIRED("20232", "用户凭证已过期", 401),
    AUTH_USER_ACCOUNT_LOCKED("20233", "用户账号已被锁定", 403),
    AUTH_USER_ACCOUNT_DISABLED("20234", "用户账号已被禁用", 403),
    AUTH_USER_ACCOUNT_EXPIRED("20235", "用户账号已过期", 403),
    AUTH_USER_NOT_FOUND("20236", "用户不存在", 404),
    AUTH_USER_PASSWORD_ERROR("20237", "用户名或密码错误", 401),
    AUTH_USER_PHONE_NOT_FOUND("20238", "手机号对应的用户不存在", 404),
    AUTH_USER_EMAIL_NOT_FOUND("20239", "邮箱对应的用户不存在", 404),

    // 验证码相关 (20251-20260)
    AUTH_VERIFICATION_CODE_ERROR("20251", "验证码错误", 400),
    AUTH_VERIFICATION_CODE_EXPIRED("20252", "验证码已过期", 400),
    AUTH_VERIFICATION_CODE_SEND_FAILED("20253", "验证码发送失败", 500),
    AUTH_VERIFICATION_CODE_TOO_FREQUENT("20254", "验证码发送过于频繁，请稍后再试", 429),

    // 授权相关 (20261-20280)
    AUTH_ACCESS_DENIED("20261", "访问被拒绝，权限不足", 403),
    AUTH_INSUFFICIENT_SCOPE("20262", "授权范围不足", 403),
    AUTH_RESOURCE_ACCESS_DENIED("20263", "资源访问被拒绝", 403),

    // 授权码相关 (20281-20290)
    AUTH_CODE_EXPIRED("20281", "授权码已过期", 400),
    AUTH_CODE_INVALID("20282", "授权码无效", 400),
    AUTH_CODE_REUSED("20283", "授权码已被使用", 400),

    // 扫码登录相关 (20291-20300)
    AUTH_QR_CODE_EXPIRED("20291", "二维码已过期", 400),
    AUTH_QR_CODE_INVALID("20292", "二维码无效", 400),
    AUTH_QR_CODE_ALREADY_SCANNED("20293", "二维码已被扫描", 400),
    AUTH_QR_CODE_ALREADY_CONFIRMED("20294", "二维码已被确认", 400),
    AUTH_QR_CODE_SERVICE_NOT_CONFIGURED("20295", "扫码认证服务未配置", 500),

    // 社交登录相关 (20301-20310)
    AUTH_SOCIAL_LOGIN_FAILED("20301", "第三方登录失败", 400),
    AUTH_SOCIAL_LOGIN_SERVICE_NOT_CONFIGURED("20302", "第三方登录服务未配置", 500),
    AUTH_SOCIAL_LOGIN_INFO_INVALID("20303", "第三方登录信息格式错误", 400),
    AUTH_SOCIAL_ACCOUNT_NOT_BOUND("20304", "社交账号未绑定", 400),

    // MFA 相关 (20311-20320)
    AUTH_MFA_NOT_ENABLED("20311", "多因素认证未启用", 400),
    AUTH_MFA_CODE_ERROR("20312", "多因素认证验证码错误", 400),
    AUTH_MFA_CODE_EXPIRED("20313", "多因素认证验证码已过期", 400),
    AUTH_MFA_SETUP_FAILED("20314", "多因素认证设置失败", 500),

    // SSO 相关 (20321-20330)
    AUTH_SSO_TOKEN_EXCHANGE_DISABLED("20321", "SSO Token 交换未启用", 403),
    AUTH_SSO_TOKEN_EXCHANGE_FAILED("20322", "SSO Token 交换失败", 400),
    AUTH_SSO_TARGET_CLIENT_NOT_FOUND("20323", "目标客户端不存在", 404),
    AUTH_SSO_GLOBAL_LOGOUT_DISABLED("20324", "全局登出未启用", 403),
    AUTH_SSO_EXCHANGE_RATE_LIMIT("20325", "Token 交换过于频繁，请稍后再试", 429),

    // ==================== 第三方错误 (3-02-xx) ====================
    AUTH_IDP_ERROR("30201", "身份提供者服务异常", 500),
    AUTH_LDAP_CONNECTION_ERROR("30202", "LDAP 连接失败", 500),
    AUTH_SAML_ERROR("30203", "SAML 认证失败", 500),
    AUTH_OAUTH2_PROVIDER_ERROR("30204", "OAuth2 认证服务异常", 500),
    AUTH_SMS_SERVICE_ERROR("30205", "短信服务异常", 500),
    AUTH_EMAIL_SERVICE_ERROR("30206", "邮件服务异常", 500),

    // ==================== 系统错误 (5-02-xx) ====================
    AUTH_SYSTEM_ERROR("50201", "认证系统内部错误", 500),
    AUTH_KEY_STORE_ERROR("50202", "密钥存储异常", 500),
    AUTH_ENCRYPTION_ERROR("50203", "加密操作失败", 500),
    AUTH_DECRYPTION_ERROR("50204", "解密操作失败", 500),
    AUTH_DATABASE_ERROR("50205", "数据库操作失败", 500),
    AUTH_CACHE_ERROR("50206", "缓存操作失败", 500),
    AUTH_AUDIT_LOG_ERROR("50207", "审计日志记录失败", 500),

    // MFA / QR 码相关 (50211-50220)
    AUTH_MFA_QR_PARAM_INVALID("50211", "用户名和密钥不能为空", 400),

    // 密钥库相关 (50221-50230)
    AUTH_KEY_STORE_ALIAS_NOT_FOUND("50221", "密钥库中未找到别名对应的私钥", 500),
    AUTH_KEY_SIZE_INVALID("50222", "密钥长度无效，必须为 2048 或 4096", 400),
    AUTH_RSA_KEY_GENERATE_FAILED("50223", "RSA 密钥对生成失败", 500),

    // OAuth2 编解码相关 (50231-50240)
    AUTH_OAUTH2_SERIALIZE_FAILED("50231", "OAuth2 授权信息序列化失败", 500),
    AUTH_OAUTH2_DESERIALIZE_FAILED("50232", "OAuth2 授权信息反序列化失败", 500),

    // 安全告警相关 (50241-50250)
    AUTH_ALERT_TYPE_UNKNOWN("50241", "未知的告警类型", 400),

    /** 在线用户/会话管理 */
    AUTH_ONLINE_SESSION_NOT_FOUND("50251", "会话不存在或已下线", 404),
    AUTH_KICK_PARAM_INVALID("50252", "请指定要踢下线的会话或用户", 400),
    AUTH_LOGIN_LOG_QUERY_FAILED("50253", "登录日志查询失败", 500),
    AUTH_LOGIN_LOG_NOT_FOUND("50254", "登录日志不存在", 404);

    private final String code;
    private final String message;
    private final int httpStatus;

    /**
     * 创建业务异常（使用默认消息）
     *
     * @return BusinessException
     */
    @Override
    public BusinessException exception() {
        return new BusinessException(this);
    }

    /**
     * 创建业务异常（自定义消息）
     *
     * @param customMessage 自定义消息
     * @return BusinessException
     */
    @Override
    public BusinessException exception(String customMessage) {
        return new BusinessException(this, customMessage);
    }

    /**
     * 创建业务异常（格式化消息）
     *
     * @param format 格式字符串
     * @param args   参数
     * @return BusinessException
     */
    @Override
    public BusinessException exception(String format, Object... args) {
        return new BusinessException(this, String.format(format, args));
    }
}
