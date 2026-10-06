package com.carlos.message.pojo.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 消息创建参数
 * </p>
 *
 * @author Carlos
 * @date 2026/3/11
 */
@Data
@Schema(description = "消息创建参数")
public class MessageCreateParam implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "模板编码不能为空")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_.-]{1,63}$", message = "模板编码须以字母开头，仅允许字母、数字、下划线、中划线、点")
    @Size(max = 32, message = "模板编码长度不能超过 32")
    @Schema(description = "模板编码", required = true)
    private String templateCode;

    @Schema(description = "模板参数")
    private Map<String, Object> templateParams;

    // @Valid 不可省：没有它，元素类型 ReceiverParam 上的 @NotBlank 一处都不会触发
    // （@NotEmpty 只校验列表本身非空，不级联到元素），于是「接收者ID不能为空」是条死规则
    @NotEmpty(message = "接收人不能为空")
    @Valid
    @Schema(description = "接收人列表", required = true)
    private List<ReceiverParam> receivers;

    @Schema(description = "渠道编码列表")
    private List<String> channelCodes;

    @NotBlank(message = "发送人ID不能为空")
    @Size(max = 32, message = "发送人ID长度不能超过 32")
    @Schema(description = "发送人ID", required = true)
    private String senderId;

    @Size(max = 64, message = "发送人名称长度不能超过 64")
    @Schema(description = "发送人名称")
    private String senderName;

    @NotBlank(message = "发送系统标识不能为空")
    @Size(max = 50, message = "发送系统标识长度不能超过 50")
    @Schema(description = "发送系统标识", required = true)
    private String senderSystem;

    @Min(value = 1, message = "优先级取值 1-4")
    @Max(value = 4, message = "优先级取值 1-4")
    @Schema(description = "优先级: 1-紧急 2-高 3-普通 4-低", defaultValue = "3")
    private Integer priority = 3;

    @Schema(description = "定时发送时间")
    private LocalDateTime scheduleTime;

    @Size(max = 512, message = "回调URL长度不能超过 512")
    @Schema(description = "回调URL")
    private String callbackUrl;

    @Data
    @Schema(description = "接收人参数")
    public static class ReceiverParam implements Serializable {

        private static final long serialVersionUID = 1L;

        @NotBlank(message = "接收者ID不能为空")
        @Size(max = 64, message = "接收者ID长度不能超过 64")
        @Schema(description = "接收者ID", required = true)
        private String receiverId;

        @Min(value = 1, message = "接收者类型取值 1-3")
        @Max(value = 3, message = "接收者类型取值 1-3")
        @Schema(description = "接收者类型: 1-用户 2-部门 3-角色", defaultValue = "1")
        private Integer receiverType = 1;

        @Size(max = 128, message = "接收地址长度不能超过 128")
        @Schema(description = "接收地址")
        private String receiverNumber;
    }
}
