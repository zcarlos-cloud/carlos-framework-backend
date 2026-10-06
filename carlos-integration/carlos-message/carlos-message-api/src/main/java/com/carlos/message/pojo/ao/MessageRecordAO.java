package com.carlos.message.pojo.ao;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * <p>
 * 消息记录AO
 * </p>
 *
 * @author Carlos
 * @date 2026/3/11
 */
@Data
@Schema(description = "消息记录")
public class MessageRecordAO implements Serializable {

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
     * 消息标题
     */
    @Schema(description = "消息标题")
    private String messageTitle;

    /**
     * 消息内容
     */
    @Schema(description = "消息内容")
    private String messageContent;

    /**
     * 发送人ID
     */
    @Schema(description = "发送人ID")
    private String senderId;

    /**
     * 发送人名称
     */
    @Schema(description = "发送人名称")
    private String senderName;

    /**
     * 发送系统
     */
    @Schema(description = "发送系统")
    private String senderSystem;

    /**
     * 优先级: 1-紧急 2-高 3-普通 4-低
     */
    @Schema(description = "优先级: 1-紧急 2-高 3-普通 4-低")
    private Integer priority;

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
     * 状态: 0-待发送 1-发送中 2-已发送 3-送达 4-已读 5-失败 6-撤回
     */
    @Schema(description = "状态")
    private Integer status;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /**
     * 扩展信息
     */
    @Schema(description = "扩展信息")
    private Map<String, Object> extras;
}
