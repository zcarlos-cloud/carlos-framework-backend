package com.carlos.job.enums;

import com.carlos.core.exception.BusinessException;
import com.carlos.core.response.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 定时任务调度中心错误码
 * <p>
 * 模块编码：06（调度中心）
 * <p>
 * 错误码格式：A-BB-CC
 * <ul>
 *   <li>A - 错误级别：1客户端错误，2业务错误，3第三方错误，5系统错误</li>
 *   <li>BB - 模块编码：06调度中心</li>
 *   <li>CC - 具体错误序号</li>
 * </ul>
 *
 * @author Carlos
 * @since 3.0.0
 */
@Getter
@RequiredArgsConstructor
public enum JobErrorCode implements ErrorCode {

    // ==================== 客户端错误 (1-06-xx) ====================
    JOB_PARAM_NAME_EMPTY("10601", "任务名称不能为空", 400),
    JOB_PARAM_HANDLER_EMPTY("10602", "任务处理器名称不能为空", 400),
    JOB_PARAM_SERVICE_EMPTY("10603", "目标服务名不能为空", 400),
    JOB_PARAM_CRON_INVALID("10604", "cron 表达式无效", 400),

    // ==================== 业务错误 (2-06-xx) ====================
    JOB_NOT_FOUND("20601", "任务不存在", 404),
    JOB_ALREADY_EXISTS("20602", "任务已存在", 409),
    JOB_HANDLER_NOT_FOUND("20603", "任务处理器不存在", 404),
    JOB_SERVICE_UNAVAILABLE("20604", "目标服务不可用", 503),
    JOB_EXECUTE_FAILED("20605", "任务执行失败", 500),
    JOB_LOG_NOT_FOUND("20611", "任务执行日志不存在", 404),

    // ==================== 系统错误 (5-06-xx) ====================
    JOB_SCHEDULER_ERROR("50601", "调度器内部错误", 500),
    JOB_DISPATCH_ERROR("50602", "任务派发异常", 500);

    private final String code;
    private final String message;
    private final int httpStatus;

    /**
     * 创建业务异常（使用默认消息，保留 httpStatus）
     *
     * @return BusinessException
     */
    public BusinessException exception() {
        return new BusinessException(this);
    }

    /**
     * 创建业务异常（自定义消息，保留 httpStatus）
     *
     * @param message 自定义消息
     * @return BusinessException
     */
    public BusinessException exception(String message) {
        return new BusinessException(this, message);
    }

}
