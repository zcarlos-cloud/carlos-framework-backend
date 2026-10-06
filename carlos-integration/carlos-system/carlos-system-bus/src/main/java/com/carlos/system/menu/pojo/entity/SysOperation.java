package com.carlos.system.menu.pojo.entity;


import com.baomidou.mybatisplus.annotation.*;
import com.carlos.system.enums.OperationStatus;
import com.carlos.system.enums.OperationType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 统一操作表（按钮 + API） 数据源对象
 * </p>
 *
 * @author carlos
 * @date 2026-05-27
 */
@Data
@Accessors(chain = true)
@TableName("sys_operation")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SysOperation implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID, value = "id")
    private Long id;

    /**
     * 操作类型：1=按钮, 2=API
     */
    @TableField(value = "op_type")
    private OperationType opType;

    /**
     * 所属菜单ID（按钮必填，API可选）
     */
    @TableField(value = "menu_id")
    private Long menuId;

    /**
     * 操作名称，如"新增用户"
     */
    @TableField(value = "op_name")
    private String opName;

    /**
     * 权限码，如 user:create
     */
    @TableField(value = "perm_code")
    private String permCode;

    /**
     * 图标（仅按钮）
     */
    @TableField(value = "icon")
    private String icon;

    /**
     * 排序
     */
    @TableField(value = "order_num")
    private Integer orderNum;

    /**
     * API路径（仅API）
     */
    @TableField(value = "api_path")
    private String apiPath;

    /**
     * HTTP方法（仅API）
     */
    @TableField(value = "api_method")
    private String apiMethod;

    /**
     * 是否自动扫描生成：0否 1是（仅API）
     */
    @TableField(value = "auto_scan")
    private Boolean autoScan;

    /**
     * 状态：0禁用 1启用
     */
    @TableField(value = "op_status")
    private OperationStatus opStatus;

    /**
     * 逻辑删除
     */
    @TableLogic
    @TableField(value = "is_deleted")
    private Boolean deleted;

    /**
     * 创建人
     */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private Long createBy;

    /**
     * 修改人
     */
    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    private Long updateBy;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 修改时间
     */
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;

}
