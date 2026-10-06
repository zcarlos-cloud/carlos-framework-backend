package com.carlos.auth.mfa.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * MFA备用恢复码实体
 * </p>
 *
 * @author Carlos
 * @date 2026-02-26
 */
@Data
@Accessors(chain = true)
@TableName("auth_mfa_recovery_code")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MfaRecoveryCode implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 恢复码ID
     */
    @TableId(type = IdType.AUTO, value = "id")
    private Long id;

    /**
     * 用户ID
     */
    @TableField(value = "user_id")
    private Long userId;

    /**
     * 恢复码
     */
    @TableField(value = "code")
    private String code;

    /**
     * 是否已使用
     */
    @TableField(value = "used")
    private Boolean used;

    /**
     * 使用时间
     */
    @TableField(value = "used_time")
    private LocalDateTime usedTime;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
