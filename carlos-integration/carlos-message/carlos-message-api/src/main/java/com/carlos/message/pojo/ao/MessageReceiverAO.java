package com.carlos.message.pojo.ao;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 消息接收人AO
 * </p>
 *
 * @author Carlos
 * @date 2026/3/11
 */
@Data
@Schema(description = "消息接收人")
public class MessageReceiverAO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息ID
     */
    @Schema(description = "消息ID")
    private String messageId;

    /**
     * 渠道编码
     */
    @Schema(description = "渠道编码")
    private String channelCode;

    /**
     * 接收者ID
     */
    @Schema(description = "接收者ID")
    private String receiverId;

    /**
     * 接收者类型: 1-用户 2-部门 3-角色
     */
    @Schema(description = "接收者类型: 1-用户 2-部门 3-角色")
    private Integer receiverType;

    /**
     * 接收地址
     */
    @Schema(description = "接收地址")
    private String receiverNumber;

    /**
     * 状态: 0-待发送 1-发送中 2-已发送 3-送达 4-已读 5-失败 6-撤回
     */
    @Schema(description = "状态: 0-待发送 1-发送中 2-已发送 3-送达 4-已读 5-失败 6-撤回")
    private Integer status;

    /**
     * 失败次数
     */
    @Schema(description = "失败次数")
    private Integer failCount;

    /**
     * 失败原因
     */
    @Schema(description = "失败原因")
    private String failReason;

    /**
     * 发送时间
     */
    @Schema(description = "发送时间")
    private LocalDateTime sendTime;

    /**
     * 送达时间
     */
    @Schema(description = "送达时间")
    private LocalDateTime deliverTime;

    /**
     * 阅读时间
     */
    @Schema(description = "阅读时间")
    private LocalDateTime readTime;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
