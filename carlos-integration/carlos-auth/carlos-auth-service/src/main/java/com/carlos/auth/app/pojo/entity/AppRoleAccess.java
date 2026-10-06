package com.carlos.auth.app.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 应用角色访问控制
 *
 * @author Carlos
 * @date 2026-07-02
 */
@Data
@Accessors(chain = true)
@TableName("app_role_access")
public class AppRoleAccess implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID, value = "id")
    private Long id;

    /** 应用ID */
    @TableField(value = "app_id")
    private Long appId;

    /** 角色编码 */
    @TableField(value = "role_code")
    private String roleCode;

    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
