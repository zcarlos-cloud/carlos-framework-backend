package com.carlos.system.i18n.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 国际化消息 数据源对象
 * </p>
 *
 * @author carlos
 * @date 2026-05-26
 */
@Data
@Accessors(chain = true)
@TableName("sys_i18n_message")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SysI18nMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID, value = "id")
    private Long id;

    /**
     * 语言标识
     */
    @TableField(value = "lang")
    private String lang;

    /**
     * 所属模块ID
     */
    @TableField(value = "module_id")
    private Long moduleId;

    /**
     * 消息键
     */
    @TableField(value = "msg_key")
    private String msgKey;

    /**
     * 消息值
     */
    @TableField(value = "msg_value")
    private String msgValue;

    /**
     * 描述说明
     */
    @TableField(value = "description", updateStrategy = FieldStrategy.ALWAYS)
    private String description;

    /**
     * 状态：true启用 false禁用
     */
    @TableField(value = "msg_status", updateStrategy = FieldStrategy.ALWAYS)
    private Boolean msgStatus;

    /**
     * 创建者
     */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private Long createBy;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新者
     */
    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    private Long updateBy;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;

}
