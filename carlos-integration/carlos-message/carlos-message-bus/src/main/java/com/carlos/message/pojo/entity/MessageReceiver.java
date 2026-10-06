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
 * 消息接收人表 数据源对象
 * </p>
 *
 * @author Carlos
 * @date 2026年3月12日 上午11:17:05
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("message_receiver")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageReceiver extends Model<MessageReceiver> implements Serializable {
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
     * 渠道编码
     */
    @TableField(value = "channel_code")
    private String channelCode;
    /**
     * 接收者ID
     */
    @TableField(value = "receiver_id")
    private String receiverId;
    /**
     * 接收人类型: 1-用户 2-部门 3-角色
     */
    @TableField(value = "receiver_type")
    private Integer receiverType;
    /**
     * 接收地址
     */
    @TableField(value = "receiver_number")
    private String receiverNumber;
    /**
     * 接收者设备标识
     */
    @TableField(value = "receiver_audience")
    private String receiverAudience;
    /**
     * 渠道返回的消息ID
     */
    @TableField(value = "channel_message_id")
    private String channelMessageId;
    /**
     * 状态: 0-待发送 1-发送中 2-已发送 3-送达 4-已读 5-失败 6-撤回
     */
    @TableField(value = "status")
    private Integer status;
    /**
     * 失败次数
     */
    @TableField(value = "fail_count")
    private Integer failCount;
    /**
     * 失败原因
     */
    @TableField(value = "fail_reason")
    private String failReason;
    /**
     * 定时发送时间
     */
    @TableField(value = "schedule_time")
    private LocalDateTime scheduleTime;
    /**
     * 实际发送时间
     */
    @TableField(value = "send_time")
    private LocalDateTime sendTime;
    /**
     * 送达时间
     */
    @TableField(value = "deliver_time")
    private LocalDateTime deliverTime;
    /**
     * 阅读时间
     */
    @TableField(value = "read_time")
    private LocalDateTime readTime;
    /**
     * 回调URL
     */
    @TableField(value = "callback_url")
    private String callbackUrl;
    /**
     * 扩展信息
     */
    @TableField(value = "extras")
    private String extras;
    /**
     * 创建人
     */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private Long createBy;
    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    /**
     * 更新人
     */
    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    private Long updateBy;
    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;

}
