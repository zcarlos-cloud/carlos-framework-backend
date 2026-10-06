package com.carlos.core.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 通用错误码定义
 * <p>
 * 错误码格式：5位数字字符串 A-BB-CC
 * <ul>
 *   <li>A - 错误级别：0成功，1客户端错误，2业务错误，3第三方错误，5系统错误</li>
 *   <li>BB - 模块编码：00通用，01用户，02认证，03订单，04支付，05消息，06文件，07工作流，08数据权限，09系统管理</li>
 *   <li>CC - 具体错误序号</li>
 * </ul>
 *
 * @author carlos
 * @since 3.0.0
 */
@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {

    // ==================== 成功响应 (0-00-xx) ====================
    SUCCESS("00000", "操作成功", 200),

    // ==================== 客户端错误 (1-xx-xx) ====================
    // 通用客户端错误 (1-00-xx)
    BAD_REQUEST("10001", "请求参数错误", 400),
    UNAUTHORIZED("A0001", "未授权，请先登录", 401),
    FORBIDDEN("A0002", "禁止访问，权限不足", 403),
    NOT_FOUND("10004", "请求的资源不存在", 404),
    METHOD_NOT_ALLOWED("10005", "请求方法不支持", 405),
    REQUEST_TIMEOUT("10006", "请求超时", 408),
    TOO_MANY_REQUESTS("10007", "请求过于频繁", 429),
    PAYLOAD_TOO_LARGE("10008", "请求实体过大", 413),

    // 参数校验错误 (1-01-xx)
    PARAM_VALIDATION_ERROR("10101", "参数校验失败", 400),
    PARAM_MISSING("10102", "缺少必要参数", 400),
    PARAM_TYPE_ERROR("10103", "参数类型错误", 400),
    PARAM_FORMAT_ERROR("10104", "参数格式错误", 400),
    PARAM_OUT_OF_RANGE("10105", "参数超出范围", 400),

    // ==================== 业务错误 (2-xx-xx) ====================
    // 通用业务错误 (2-00-xx)
    BUSINESS_ERROR("20001", "业务处理异常", 400),

    // 用户模块错误 (2-01-xx)
    USER_NOT_FOUND("20101", "用户不存在", 404),
    USER_PASSWORD_ERROR("20102", "用户名或密码错误", 400),
    USER_ACCOUNT_LOCKED("20103", "账号已被锁定", 403),
    USER_ACCOUNT_EXPIRED("20104", "账号已过期", 403),
    USER_ALREADY_EXISTS("20105", "用户已存在", 409),
    USER_ORIGINAL_PASSWORD_ERROR("20106", "原密码错误", 400),
    USER_NOT_ACTIVATED("20107", "用户未激活", 403),
    USER_DISABLED("20108", "用户已被禁用", 403),

    // 认证授权错误 (2-02-xx)
    AUTH_TOKEN_EXPIRED("A0004", "登录凭证已过期", 401),
    AUTH_TOKEN_INVALID("20202", "登录凭证无效", 401),
    AUTH_ACCESS_DENIED("20203", "没有访问权限", 403),
    AUTH_VERIFICATION_FAILED("20204", "验证码错误或已过期", 400),
    AUTH_DEVICE_RESTRICTED("20205", "登录设备受限", 403),
    AUTH_CONCURRENT_LOGIN_LIMIT("20206", "并发登录超出限制", 403),
    AUTH_TOKEN_REVOKED("20207", "Token 已被吊销", 401),

    // 订单服务错误 (2-03-xx)
    ORDER_NOT_FOUND("20301", "订单不存在", 404),
    ORDER_STATUS_INVALID("20302", "订单状态不正确", 400),
    ORDER_ALREADY_PAID("20303", "订单已支付", 409),
    ORDER_INSUFFICIENT_STOCK("20304", "商品库存不足", 400),
    ORDER_PAYMENT_TIMEOUT("20305", "支付超时，订单已关闭", 400),

    // 支付服务错误 (2-04-xx)
    PAYMENT_FAILED("20401", "支付失败", 400),
    PAYMENT_INSUFFICIENT_BALANCE("20402", "余额不足", 400),
    PAYMENT_METHOD_NOT_SUPPORTED("20403", "不支持的支付方式", 400),

    // 消息服务错误 (2-05-xx)
    MESSAGE_SEND_FAILED("20501", "消息发送失败", 500),
    MESSAGE_TEMPLATE_NOT_FOUND("20502", "消息模板不存在", 400),

    // 文件服务错误 (2-06-xx)
    FILE_UPLOAD_ERROR("20601", "文件上传失败", 500),
    FILE_TYPE_NOT_ALLOWED("20602", "文件类型不允许", 400),
    FILE_SIZE_EXCEEDED("20603", "文件大小超出限制", 400),
    FILE_NOT_FOUND("20604", "文件不存在", 404),
    FILE_DOWNLOAD_ERROR("20605", "文件下载失败", 500),

    // 工作流错误 (2-07-xx)
    WORKFLOW_DEFINITION_NOT_FOUND("20701", "流程定义不存在", 404),
    WORKFLOW_INSTANCE_NOT_FOUND("20702", "流程实例不存在", 404),
    WORKFLOW_TASK_NOT_FOUND("20703", "任务不存在", 404),
    WORKFLOW_OPERATION_NOT_ALLOWED("20704", "当前操作不允许", 400),

    // 数据权限错误 (2-08-xx)
    DATA_ACCESS_DENIED("20801", "无权访问该数据", 403),
    DATA_SCOPE_NOT_FOUND("20802", "数据范围配置不存在", 404),

    // 系统管理错误 (2-09-xx)
    DICT_NOT_FOUND("20901", "字典数据不存在", 404),
    CONFIG_NOT_FOUND("20902", "配置项不存在", 404),

    // ==================== 第三方服务错误 (3-xx-xx) ====================
    // 通用第三方错误 (3-00-xx)
    THIRD_PARTY_ERROR("30001", "第三方服务异常", 500),
    THIRD_PARTY_TIMEOUT("30002", "第三方服务超时", 504),

    // 短信服务错误 (3-05-xx)
    SMS_SEND_ERROR("30501", "短信发送失败", 500),
    SMS_RATE_LIMIT("30502", "短信发送过于频繁", 429),
    SMS_TEMPLATE_NOT_FOUND("30503", "短信模板不存在", 400),

    // OSS/文件存储错误 (3-06-xx)
    OSS_UPLOAD_ERROR("30601", "文件存储上传失败", 500),
    OSS_DOWNLOAD_ERROR("30602", "文件存储下载失败", 500),

    // ==================== 系统错误 (5-xx-xx) ====================
    // 通用系统错误 (5-00-xx)
    INTERNAL_ERROR("50001", "系统内部错误", 500),
    DATABASE_ERROR("50002", "数据库操作异常", 500),
    CACHE_ERROR("50003", "缓存服务异常", 500),
    SERVICE_CALL_ERROR("50004", "服务调用异常", 503),
    SERVICE_UNAVAILABLE("50005", "系统繁忙，请稍后重试", 503),
    CONFIG_LOAD_ERROR("50006", "配置加载失败", 500),
    ENCRYPTION_ERROR("50007", "加密操作异常", 500),
    SERIALIZATION_ERROR("50008", "序列化异常", 500),
    SERVER_TIMEOUT("50009", "服务请求超时", 504),
    ;

    private final String code;
    private final String message;
    private final int httpStatus;
}
