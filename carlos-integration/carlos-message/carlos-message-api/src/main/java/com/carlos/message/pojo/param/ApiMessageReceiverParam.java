package com.carlos.message.pojo.param;


import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 消息接收人表 API提供的对象(API Object)
 * </p>
 *
 * @author Carlos
 * @date 2026年3月12日 上午11:17:05
 */
@Data
@Accessors(chain = true)
public class ApiMessageReceiverParam implements Serializable {
    /** 主键ID */
    private Long id;
    /** 消息ID */
    private String messageId;
    /** 渠道编码 */
    private String channelCode;
    /** 接收者ID */
    private String receiverId;
    /** 接收人类型: 1-用户 2-部门 3-角色 */
    private Integer receiverType;
    /** 接收地址 */
    private String receiverNumber;
    /** 接收者设备标识 */
    private String receiverAudience;
    /** 渠道返回的消息ID */
    private String channelMessageId;
    /** 状态: 0-待发送 1-发送中 2-已发送 3-送达 4-已读 5-失败 6-撤回 */
    private Integer status;
    /** 失败次数 */
    private Integer failCount;
    /** 失败原因 */
    private String failReason;
    /** 定时发送时间 */
    private LocalDateTime scheduleTime;
    /** 实际发送时间 */
    private LocalDateTime sendTime;
    /** 送达时间 */
    private LocalDateTime deliverTime;
    /** 阅读时间 */
    private LocalDateTime readTime;
    /** 回调URL */
    private String callbackUrl;
    /** 扩展信息 */
    private String extras;
    /** 创建人 */
    private Long createBy;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 更新人 */
    private Long updateBy;
    /** 更新时间 */
    private LocalDateTime updateTime;
}
