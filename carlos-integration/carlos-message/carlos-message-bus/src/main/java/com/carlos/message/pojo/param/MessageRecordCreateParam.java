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
 * 消息记录表 新增参数封装
 * </p>
 *
 * @author Carlos
 * @date 2026年3月12日 上午11:17:05
 */
@Data
@Accessors(chain = true)
@Schema(description = "消息记录表新增参数")
public class MessageRecordCreateParam {
    @NotBlank(message = "消息唯一标识不能为空")
    @Size(max = 64, message = "消息唯一标识长度不能超过 64")
    @Schema(description = "消息唯一标识")
    private String messageId;
    @NotBlank(message = "模板编码不能为空")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_.-]{1,63}$", message = "模板编码须以字母开头，仅允许字母、数字、下划线、中划线、点")
    @Size(max = 32, message = "模板编码长度不能超过 32")
    @Schema(description = "模板编码")
    private String templateCode;
    @Size(max = 2000, message = "模板参数JSON长度不能超过 2000")
    @Schema(description = "模板参数JSON")
    private String templateParams;
    @NotBlank(message = "消息类型编码不能为空")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_.-]{1,63}$", message = "消息类型编码须以字母开头，仅允许字母、数字、下划线、中划线、点")
    @Size(max = 32, message = "消息类型编码长度不能超过 32")
    @Schema(description = "消息类型编码")
    private String messageType;
    @Size(max = 255, message = "消息标题长度不能超过 255")
    @Schema(description = "消息标题")
    private String messageTitle;
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 2000, message = "消息内容长度不能超过 2000")
    @Schema(description = "消息内容")
    private String messageContent;
    @NotBlank(message = "发送人ID不能为空")
    @Size(max = 32, message = "发送人ID长度不能超过 32")
    @Schema(description = "发送人ID")
    private String senderId;
    @Size(max = 64, message = "发送人名称长度不能超过 64")
    @Schema(description = "发送人名称")
    private String senderName;
    @NotBlank(message = "发送系统标识不能为空")
    @Size(max = 50, message = "发送系统标识长度不能超过 50")
    @Schema(description = "发送系统标识")
    private String senderSystem;
    @Size(max = 16, message = "操作反馈类型长度不能超过 16")
    @Schema(description = "操作反馈类型")
    private String feedbackType;
    @Size(max = 255, message = "操作反馈内容长度不能超过 255")
    @Schema(description = "操作反馈内容")
    private String feedbackContent;
    @NotNull(message = "优先级: 1-紧急 2-高 3-普通 4-低不能为空")
    @Min(value = 1, message = "优先级取值 1-4")
    @Max(value = 4, message = "优先级取值 1-4")
    @Schema(description = "优先级: 1-紧急 2-高 3-普通 4-低")
    private Integer priority;
    @Schema(description = "消息有效期")
    private LocalDateTime validUntil;
    @Min(value = 0, message = "总接收人数不能为负")
    @Schema(description = "总接收人数")
    private Integer totalCount;
    @Min(value = 0, message = "成功发送数不能为负")
    @Schema(description = "成功发送数")
    private Integer successCount;
    @Min(value = 0, message = "失败发送数不能为负")
    @Schema(description = "失败发送数")
    private Integer failCount;
    @Size(max = 2000, message = "扩展信息长度不能超过 2000")
    @Schema(description = "扩展信息")
    private String extras;
}
