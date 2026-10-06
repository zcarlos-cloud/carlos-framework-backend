package com.carlos.message.pojo.ao;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 消息状态AO
 * </p>
 *
 * @author Carlos
 * @date 2026/3/11
 */
@Data
@Schema(description = "消息状态")
public class MessageStatusAO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息ID
     */
    @Schema(description = "消息ID")
    private String messageId;

    /**
     * 状态: 0-待发送 1-发送中 2-已发送 3-送达 4-已读 5-失败 6-撤回
     */
    @Schema(description = "状态: 0-待发送 1-发送中 2-已发送 3-送达 4-已读 5-失败 6-撤回")
    private Integer status;

    /**
     * 状态描述
     */
    @Schema(description = "状态描述")
    private String statusDesc;

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
     * 创建时间
     */
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /**
     * 发送时间
     */
    @Schema(description = "发送时间")
    private LocalDateTime sendTime;
}
