package com.carlos.message.pojo.param;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;


/**
 * <p>
 * 消息接收人表 新增参数封装
 * </p>
 *
 * @author Carlos
 * @date 2026年3月12日 上午11:17:05
 */
@Data
@Accessors(chain = true)
@Schema(description = "消息接收人表新增参数")
public class MessageReceiverCreateParam {
    @NotBlank(message = "消息ID不能为空")
    @Size(max = 64, message = "消息ID长度不能超过 64")
    @Schema(description = "消息ID")
    private String messageId;
    @NotBlank(message = "渠道编码不能为空")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_.-]{1,63}$", message = "渠道编码须以字母开头，仅允许字母、数字、下划线、中划线、点")
    @Size(max = 32, message = "渠道编码长度不能超过 32")
    @Schema(description = "渠道编码")
    private String channelCode;
    @NotBlank(message = "接收者ID不能为空")
    @Size(max = 64, message = "接收者ID长度不能超过 64")
    @Schema(description = "接收者ID")
    private String receiverId;
    @Min(value = 1, message = "接收人类型取值 1-3")
    @Max(value = 3, message = "接收人类型取值 1-3")
    @Schema(description = "接收人类型: 1-用户 2-部门 3-角色")
    private Integer receiverType;
    @Size(max = 128, message = "接收地址长度不能超过 128")
    @Schema(description = "接收地址")
    private String receiverNumber;
    @Size(max = 64, message = "接收者设备标识长度不能超过 64")
    @Schema(description = "接收者设备标识")
    private String receiverAudience;
    @Size(max = 64, message = "渠道返回的消息ID长度不能超过 64")
    @Schema(description = "渠道返回的消息ID")
    private String channelMessageId;
    @NotNull(message = "状态: 0-待发送 1-发送中 2-已发送 3-送达 4-已读 5-失败 6-撤回不能为空")
    @Min(value = 0, message = "状态取值 0-6")
    @Max(value = 6, message = "状态取值 0-6")
    @Schema(description = "状态: 0-待发送 1-发送中 2-已发送 3-送达 4-已读 5-失败 6-撤回")
    private Integer status;
    @Min(value = 0, message = "失败次数不能为负")
    @Schema(description = "失败次数")
    private Integer failCount;
    @Size(max = 512, message = "失败原因长度不能超过 512")
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
    @Size(max = 512, message = "回调URL长度不能超过 512")
    @Schema(description = "回调URL")
    private String callbackUrl;
    @Size(max = 2000, message = "扩展信息长度不能超过 2000")
    @Schema(description = "扩展信息")
    private String extras;
}
