package com.carlos.message.pojo.param;

import com.carlos.core.param.ParamPage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * <p>
 * 消息分页查询参数
 * </p>
 *
 * @author Carlos
 * @date 2026/3/11
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "消息分页查询参数")
public class MessagePageParam extends ParamPage {

    private static final long serialVersionUID = 1L;

    /**
     * 消息ID
     */
    @Schema(description = "消息ID")
    private String messageId;

    /**
     * 模板编码
     */
    @Schema(description = "模板编码")
    private String templateCode;

    /**
     * 消息类型
     */
    @Schema(description = "消息类型")
    private String messageType;

    /**
     * 发送人ID
     */
    @Schema(description = "发送人ID")
    private String senderId;

    /**
     * 发送系统
     */
    @Schema(description = "发送系统")
    private String senderSystem;

    /**
     * 状态
     */
    @Schema(description = "状态")
    private Integer status;

    /**
     * 开始时间
     */
    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @Schema(description = "结束时间")
    private LocalDateTime endTime;
}
