package com.carlos.monitor.enums;

import com.carlos.core.exception.BusinessException;
import com.carlos.core.response.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 监控模块错误码
 * <p>
 * 模块编码：13（监控服务）
 * <p>
 * 错误码格式：A-BB-CC
 * <ul>
 *   <li>A - 错误级别：1客户端错误，2业务错误，3第三方错误，5系统错误</li>
 *   <li>BB - 模块编码：13监控服务</li>
 *   <li>CC - 具体错误序号</li>
 * </ul>
 * 对齐《DESIGN-20260930-系统监控模块功能设计》第 6 节
 *
 * @author Carlos
 * @date 2026年10月1日
 */
@Getter
@RequiredArgsConstructor
public enum MonitorErrorCode implements ErrorCode {

    // ==================== 客户端错误 (1-13-xx) ====================
    MONITOR_INSTANCE_NOT_FOUND("11301", "实例不存在或已删除", 404),
    MONITOR_INSTANCE_NAME_EXISTS("11302", "实例名称在当前环境下已存在", 409),
    MONITOR_CONNECT_TEST_FAILED("11303", "连接测试失败", 400),
    MONITOR_INSTANCE_TYPE_UNSUPPORTED("11304", "不支持的实例类型", 400),

    // ==================== 业务错误 (2-13-xx) ====================
    MONITOR_MANAGE_DISABLED("21301", "实例数据管理功能已关闭", 400),
    MONITOR_CONFIRM_TOKEN_INVALID("21302", "高危操作缺少有效确认令牌", 400),
    MONITOR_SQL_BLOCKED("21303", "SQL 被安全策略拦截", 400),
    MONITOR_SQL_WRITE_DISABLED("21304", "写模式未开启，拒绝执行写 SQL", 400),
    MONITOR_SQL_AFFECTED_ROWS_EXCEEDED("21305", "影响行数超出保护阈值，已回滚", 400),
    MONITOR_CONNECTION_UNAVAILABLE("21306", "实例连接不可用", 500),
    MONITOR_REDIS_COMMAND_FORBIDDEN("21307", "Redis 命令不在允许范围", 400),

    // ==================== 第三方错误 (3-13-xx) ====================
    MONITOR_NACOS_DISCOVERY_FAILED("31301", "Nacos 服务发现失败", 500),
    MONITOR_NOTIFY_SEND_FAILED("31302", "carlos-message 通知发送失败", 500),

    // ==================== 系统错误 (5-13-xx) ====================
    MONITOR_CLICKHOUSE_WRITE_FAILED("51301", "ClickHouse 写入失败（已落盘备份）", 500),
    MONITOR_COLLECT_TASK_FAILED("51302", "采集任务执行失败", 500);

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
