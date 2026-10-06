package com.carlos.message.pojo.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 消息接收人表 显示层对象，向页面传输的对象
 * </p>
 *
 * @author Carlos
 * @date 2026年3月12日 上午11:17:06
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageReceiverVO implements Serializable {
    private static final long serialVersionUID = 1L;
    @Schema(description = "主键ID")
    private Long id;
    @Schema(description = "消息ID")
    private String messageId;
    @Schema(description = "渠道编码")
    private String channelCode;
    @Schema(description = "接收者ID")
    private String receiverId;
    @Schema(description = "接收人类型: 1-用户 2-部门 3-角色")
    private Integer receiverType;
    @Schema(description = "接收地址")
    private String receiverNumber;
    @Schema(description = "接收者设备标识")
    private String receiverAudience;
    @Schema(description = "渠道返回的消息ID")
    private String channelMessageId;
    @Schema(description = "状态: 0-待发送 1-发送中 2-已发送 3-送达 4-已读 5-失败 6-撤回")
    private Integer status;
    @Schema(description = "失败次数")
    private Integer failCount;
    @Schema(description = "失败原因")
    private String failReason;
    @Schema(description = "定时发送时间")
    private LocalDateTime scheduleTime;
    @Schema(description = "实际发送时间")
    private LocalDateTime sendTime;
    @Schema(description = "送达时间")
    private LocalDateTime deliverTime;
    @Schema(description = "阅读时间")
    private LocalDateTime readTime;
    @Schema(description = "回调URL")
    private String callbackUrl;
    @Schema(description = "扩展信息")
    private String extras;
    @Schema(description = "创建人")
    private Long createBy;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "更新人")
    private Long updateBy;
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
