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
 * 消息模板 数据源对象
 * </p>
 *
 * @author Carlos
 * @date 2026年3月12日 上午11:17:05
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("message_template")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageTemplate extends Model<MessageTemplate> implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID, value = "id")
    private Long id;
    /**
     * 消息类型ID
     */
    @TableField(value = "type_id")
    private Long typeId;
    /**
     * 模板编码
     */
    @TableField(value = "template_code")
    private String templateCode;
    /**
     * 模板名称
     */
    @TableField(value = "template_name")
    private String templateName;
    /**
     * 标题模板
     */
    @TableField(value = "title_template")
    private String titleTemplate;
    /**
     * 模板内容
     */
    @TableField(value = "content_template")
    private String contentTemplate;
    /**
     * 参数定义JSON
     */
    @TableField(value = "param_schema")
    private String paramSchema;
    /**
     * 渠道特殊配置
     */
    @TableField(value = "channel_config")
    private String channelConfig;
    /**
     * 是否启用: 0-禁用 1-启用 2-草稿
     */
    @TableField(value = "is_enabled")
    private Boolean enabled;
    /**
     * 逻辑删除
     */
    @TableLogic
    @TableField(value = "is_deleted")
    private Boolean deleted;
    /**
     * 创建者编号
     */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private Long createBy;
    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    /**
     * 更新者编号
     */
    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    private Long updateBy;
    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;

}
