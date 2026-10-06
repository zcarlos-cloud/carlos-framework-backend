package com.carlos.message.pojo.entity;


import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 消息记录表 数据源对象
 * </p>
 *
 * @author Carlos
 * @date 2026年3月12日 上午11:17:05
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("message_record")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageRecord extends Model<MessageRecord> implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID, value = "id")
    private Long id;
    /**
     * 消息唯一标识
     */
    @TableField(value = "message_id")
    private String messageId;
    /**
     * 模板编码
     */
    @TableField(value = "template_code")
    private String templateCode;
    /**
     * 模板参数JSON
     */
    @TableField(value = "template_params")
    private String templateParams;
    /**
     * 消息类型编码
     */
    @TableField(value = "message_type")
    private String messageType;
    /**
     * 消息标题
     */
    @TableField(value = "message_title")
    private String messageTitle;
    /**
     * 消息内容
     */
    @TableField(value = "message_content")
    private String messageContent;
    /**
     * 发送人ID
     */
    @TableField(value = "sender_id")
    private String senderId;
    /**
     * 发送人名称
     */
    @TableField(value = "sender_name")
    private String senderName;
    /**
     * 发送系统标识
     */
    @TableField(value = "sender_system")
    private String senderSystem;
    /**
     * 操作反馈类型
     */
    @TableField(value = "feedback_type")
    private String feedbackType;
    /**
     * 操作反馈内容
     */
    @TableField(value = "feedback_content")
    private String feedbackContent;
    /**
     * 优先级: 1-紧急 2-高 3-普通 4-低
     */
    @TableField(value = "priority")
    private Integer priority;
    /**
     * 消息有效期
     */
    @TableField(value = "valid_until")
    private LocalDateTime validUntil;
    /**
     * 总接收人数
     */
    @TableField(value = "total_count")
    private Integer totalCount;
    /**
     * 成功发送数
     */
    @TableField(value = "success_count")
    private Integer successCount;
    /**
     * 失败发送数
     */
    @TableField(value = "fail_count")
    private Integer failCount;
    /**
     * 扩展信息
     */
    @TableField(value = "extras")
    private String extras;
    /**
     * 创建人
     */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private Long createBy;
    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    /**
     * 更新人
     */
    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    private Long updateBy;
    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;

}
