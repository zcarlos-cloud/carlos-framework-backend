package com.carlos.message.pojo.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 消息发送参数
 * </p>
 *
 * @author Carlos
 * @date 2026/3/11
 */
@Data
@Schema(description = "消息发送参数")
public class MessageSendParam implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 模板编码
     */
    @NotBlank(message = "模板编码不能为空")
    @Schema(description = "模板编码", required = true)
    private String templateCode;

    /**
     * 模板参数
     */
    @Schema(description = "模板参数")
    private Map<String, Object> templateParams;

    /**
     * 接收人列表
     */
    @NotEmpty(message = "接收人不能为空")
    @Schema(description = "接收人列表", required = true)
    private List<ReceiverParam> receivers;

    /**
     * 渠道编码列表（为空则使用模板默认渠道）
     */
    @Schema(description = "渠道编码列表")
    private List<String> channelCodes;

    /**
     * 发送人ID
     */
    @NotBlank(message = "发送人ID不能为空")
    @Schema(description = "发送人ID", required = true)
    private String senderId;

    /**
     * 发送人名称
     */
    @Schema(description = "发送人名称")
    private String senderName;

    /**
     * 发送系统标识
     */
    @NotBlank(message = "发送系统标识不能为空")
    @Schema(description = "发送系统标识", required = true)
    private String senderSystem;

    /**
     * 优先级: 1-紧急 2-高 3-普通 4-低
     */
    @Schema(description = "优先级: 1-紧急 2-高 3-普通 4-低", defaultValue = "3")
    private Integer priority = 3;

    /**
     * 定时发送时间（为空则立即发送）
     */
    @Schema(description = "定时发送时间")
    private LocalDateTime scheduleTime;

    /**
     * 消息有效期
     */
    @Schema(description = "消息有效期")
    private LocalDateTime validUntil;

    /**
     * 回调URL
     */
    @Schema(description = "回调URL")
    private String callbackUrl;

    /**
     * 扩展信息
     */
    @Schema(description = "扩展信息")
    private Map<String, Object> extras;

    /**
     * 接收人参数
     */
    @Data
    @Schema(description = "接收人参数")
    public static class ReceiverParam implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 接收者ID
         */
        @NotBlank(message = "接收者ID不能为空")
        @Schema(description = "接收者ID", required = true)
        private String receiverId;

        /**
         * 接收者类型: 1-用户 2-部门 3-角色
         */
        @Schema(description = "接收者类型: 1-用户 2-部门 3-角色", defaultValue = "1")
        private Integer receiverType = 1;

        /**
         * 接收地址（手机号/邮箱等，为空则根据receiverId查询）
         */
        @Schema(description = "接收地址")
        private String receiverNumber;

        /**
         * 设备标识
         */
        @Schema(description = "设备标识")
        private String receiverAudience;
    }
}
