package com.carlos.message.pojo.ao;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 发送结果AO
 * </p>
 *
 * @author Carlos
 * @date 2026/3/11
 */
@Data
@Schema(description = "发送结果")
public class SendResultAO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息ID
     */
    @Schema(description = "消息ID")
    private String messageId;

    /**
     * 是否成功
     */
    @Schema(description = "是否成功")
    private Boolean success;

    /**
     * 总接收人数
     */
    @Schema(description = "总接收人数")
    private Integer totalCount;

    /**
     * 成功发送数
     */
    @Schema(description = "成功发送数")
    private Integer successCount;

    /**
     * 失败发送数
     */
    @Schema(description = "失败发送数")
    private Integer failCount;

    /**
     * 发送时间
     */
    @Schema(description = "发送时间")
    private LocalDateTime sendTime;

    /**
     * 错误信息（失败时）
     */
    @Schema(description = "错误信息")
    private String errorMsg;
}
