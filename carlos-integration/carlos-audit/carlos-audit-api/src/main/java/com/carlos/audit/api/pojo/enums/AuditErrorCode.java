package com.carlos.audit.api.pojo.enums;

import com.carlos.core.exception.BusinessException;
import com.carlos.core.response.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 审计模块错误码
 * <p>
 * 模块编码：11（审计服务）
 * <p>
 * 错误码格式：A-BB-CC
 * <ul>
 *   <li>A - 错误级别：1客户端错误，2业务错误，3第三方错误，5系统错误</li>
 *   <li>BB - 模块编码：11审计服务</li>
 *   <li>CC - 具体错误序号</li>
 * </ul>
 *
 * @author carlos
 * @since 3.0.0
 */
@Getter
@RequiredArgsConstructor
public enum AuditErrorCode implements ErrorCode {

    // ==================== 业务错误 (2-11-xx) ====================
    // 审计日志相关
    AUDIT_LOG_NOT_FOUND("21101", "审计日志不存在", 404),
    AUDIT_LOG_ALREADY_EXISTS("21102", "审计日志已存在", 409),
    AUDIT_LOG_SAVE_FAILED("21103", "审计日志保存失败", 500),

    // 审计配置相关
    AUDIT_CONFIG_NOT_FOUND("21111", "审计配置不存在", 404),
    AUDIT_CONFIG_INVALID("21112", "审计配置无效", 400),

    // 审计查询相关
    AUDIT_QUERY_PARAM_INVALID("21121", "审计查询参数无效", 400),
    AUDIT_QUERY_TIME_RANGE_INVALID("21122", "审计查询时间范围无效", 400),
    AUDIT_QUERY_NO_PERMISSION("21123", "无权查询该审计数据", 403),
    AUDIT_QUERY_EXPORT_LIMIT_EXCEEDED("21124", "导出数据量超出上限，请缩小查询范围", 400),
    AUDIT_QUERY_EXPORT_FAILED("21125", "审计日志导出失败", 500),

    // 审计归档相关
    AUDIT_ARCHIVE_FAILED("21131", "审计日志归档失败", 500),
    AUDIT_ARCHIVE_NOT_FOUND("21132", "审计归档记录不存在", 404),

    // ==================== 第三方错误 (3-11-xx) ====================
    AUDIT_STORAGE_ERROR("31101", "审计存储服务异常", 500),

    // ==================== 系统错误 (5-11-xx) ====================
    AUDIT_SYSTEM_ERROR("51101", "审计系统内部错误", 500);

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
