package com.carlos.system.configration.pojo.entity;


import com.baomidou.mybatisplus.annotation.*;
import com.carlos.system.enums.ConfigValueType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 系统配置 数据源对象
 * </p>
 *
 * @author carlos
 * @date 2022-11-3 13:47:54
 */
@Data
@Accessors(chain = true)
@TableName("sys_config")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SysConfig implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 参数Id
     */
    @TableId(type = IdType.ASSIGN_ID, value = "id")
    private Long id;
    /**
     * 参数名称
     */
    @TableField(value = "config_name")
    private String configName;
    /**
     * 参数编码
     */
    @TableField(value = "config_code")
    private String configCode;
    /**
     * 参数键值
     */
    @TableField(value = "config_value")
    private String configValue;
    /**
     * 值类型
     */
    @TableField(value = "value_type")
    private ConfigValueType valueType;
    /**
     * 状态
     */
    @TableField(value = "config_status")
    private Boolean configStatus;
    /**
     * 备注
     */
    @TableField(value = "remark")
    private String remark;
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
