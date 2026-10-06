package com.carlos.message.enums;

import com.carlos.core.exception.BusinessException;
import com.carlos.core.response.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 消息服务模块错误码
 * <p>
 * 模块编码：05（消息服务）
 * <p>
 * 错误码格式：A-BB-CC
 * <ul>
 *   <li>A - 错误级别：1客户端错误，2业务错误，3第三方错误，5系统错误</li>
 *   <li>BB - 模块编码：05消息服务</li>
 *   <li>CC - 具体错误序号</li>
 * </ul>
 *
 * @author carlos
 * @since 3.0.0
 */
@Getter
@RequiredArgsConstructor
public enum MessageErrorCode implements ErrorCode {

    // ==================== 客户端错误 (1-05-xx) ====================
    MSG_PARAM_RECEIVER_EMPTY("10501", "消息接收人不能为空", 400),
    MSG_PARAM_CONTENT_EMPTY("10502", "消息内容不能为空", 400),
    MSG_PARAM_TEMPLATE_CODE_EMPTY("10503", "消息模板编码不能为空", 400),
    MSG_PARAM_CHANNEL_INVALID("10504", "消息渠道无效", 400),
    MSG_PARAM_SEND_TIME_INVALID("10505", "定时发送时间无效", 400),

    // ==================== 业务错误 (2-05-xx) ====================
    // 消息模板相关
    MSG_TEMPLATE_NOT_FOUND("20501", "消息模板不存在", 404),
    MSG_TEMPLATE_DISABLED("20502", "消息模板已禁用", 400),
    MSG_TEMPLATE_PARAM_INVALID("20503", "消息模板参数无效", 400),
    MSG_TEMPLATE_ALREADY_EXISTS("20504", "消息模板编码已存在", 409),

    // 消息发送相关
    MSG_SEND_FAILED("20511", "消息发送失败", 500),
    MSG_SEND_RATE_LIMIT("20512", "消息发送频率超限", 429),
    MSG_SEND_CHANNEL_DISABLED("20513", "消息发送渠道已禁用", 400),
    MSG_SEND_CHANNEL_NOT_SUPPORT("20514", "不支持的消息发送渠道", 400),

    // 消息记录相关
    MSG_RECORD_NOT_FOUND("20521", "消息记录不存在", 404),
    MSG_RECORD_STATUS_INVALID("20522", "消息状态无效", 400),
    MSG_RECORD_ALREADY_READ("20523", "消息已读，不能重复操作", 409),

    // 消息渠道相关
    MSG_CHANNEL_NOT_FOUND("20531", "消息渠道不存在", 404),
    MSG_CHANNEL_CONFIG_INVALID("20532", "消息渠道配置无效", 400),
    MSG_CHANNEL_BALANCE_INSUFFICIENT("20533", "消息渠道余额不足", 400),

    // ==================== 第三方错误 (3-05-xx) ====================
    MSG_SMS_SEND_ERROR("30501", "短信发送服务异常", 500),
    MSG_SMS_TEMPLATE_AUDIT_FAILED("30502", "短信模板审核失败", 400),
    MSG_EMAIL_SEND_ERROR("30503", "邮件发送服务异常", 500),
    MSG_PUSH_SEND_ERROR("30504", "推送服务异常", 500),
    MSG_WECHAT_SEND_ERROR("30505", "微信消息发送失败", 500),
    MSG_DINGTALK_SEND_ERROR("30506", "钉钉消息发送失败", 500),
    MSG_THIRD_PARTY_RATE_LIMIT("30507", "第三方服务限流", 429),
    MSG_THIRD_PARTY_TIMEOUT("30508", "第三方服务超时", 504),

    // ==================== 系统错误 (5-05-xx) ====================
    MSG_SYSTEM_ERROR("50501", "消息系统内部错误", 500),
    MSG_QUEUE_ERROR("50502", "消息队列异常", 500),
    MSG_STORAGE_ERROR("50503", "消息存储异常", 500),
    MSG_SERIALIZE_ERROR("50504", "消息序列化失败", 500);

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
