package com.carlos.message.pojo.entity;


import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 消息发送日志 数据源对象
 * </p>
 *
 * @author Carlos
 * @date 2026年3月12日 上午11:17:05
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("message_send_log")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageSendLog extends Model<MessageSendLog> implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID, value = "id")
    private Long id;
    /**
     * 消息ID
     */
    @TableField(value = "message_id")
    private String messageId;
    /**
     * 接收人记录ID
     */
    @TableField(value = "receiver_id")
    private Long receiverId;
    /**
     * 渠道编码
     */
    @TableField(value = "channel_code")
    private String channelCode;
    /**
     * 请求参数
     */
    @TableField(value = "request_param")
    private String requestParam;
    /**
     * 响应数据
     */
    @TableField(value = "response_data")
    private String responseData;
    /**
     * 是否成功: 0-失败 1-成功
     */
    @TableField(value = "is_success")
    private Boolean success;
    /**
     * 错误码
     */
    @TableField(value = "error_code")
    private String errorCode;
    /**
     * 错误信息
     */
    @TableField(value = "error_message")
    private String errorMessage;
    /**
     * 耗时(ms)
     */
    @TableField(value = "cost_time")
    private Integer costTime;
    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

}
