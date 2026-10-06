package com.carlos.message.pojo.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 消息已读参数
 * </p>
 *
 * @author Carlos
 * @date 2026/5/30
 */
@Data
@Schema(description = "消息已读参数")
public class MessageReadParam implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "接收人ID不能为空")
    @Size(max = 64, message = "接收人ID长度不能超过 64")
    @Schema(description = "接收人ID", required = true)
    private String receiverId;

    @Size(max = 200, message = "消息ID列表单次不能超过 200 条")
    @Schema(description = "消息ID列表（与 messageId 二选一）")
    private List<String> messageIds;

    @Size(max = 64, message = "消息ID长度不能超过 64")
    @Schema(description = "单个消息ID（与 messageIds 二选一）")
    private String messageId;
}
