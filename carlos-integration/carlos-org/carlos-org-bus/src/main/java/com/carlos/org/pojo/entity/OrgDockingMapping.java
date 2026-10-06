package com.carlos.org.pojo.entity;


import com.baomidou.mybatisplus.annotation.*;
import com.carlos.org.pojo.enums.OrgDockingTypeEnum;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 用户信息对接关联表 数据源对象
 * </p>
 *
 * @author Carlos
 * @date 2025-2-27 15:41:32
 */
@Data
@Accessors(chain = true)
@TableName("org_docking_mapping")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrgDockingMapping implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID, value = "id")
    private Long id;
    /**
     * 系统数据id
     */
    @TableField(value = "system_id")
    private Long systemId;
    /**
     * 目标系统id
     */
    @TableField(value = "target_id")
    private Long targetId;
    /**
     * 目标系统标识
     */
    @TableField(value = "target_code")
    private String targetCode;
    /**
     * 对接类型
     */
    @TableField(value = "docking_type")
    private OrgDockingTypeEnum dockingType;
    /**
     * 逻辑删除，0：未删除，1：已删除
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
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

}
