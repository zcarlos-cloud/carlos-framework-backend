package com.carlos.auth.passkey.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * Passkey/WebAuthn 凭据实体
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@Data
@Accessors(chain = true)
@TableName("auth_passkey_credential")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PasskeyCredential implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO, value = "id")
    private Long id;

    /**
     * 用户ID
     */
    @TableField(value = "user_id")
    private Long userId;

    /**
     * 凭据ID（二进制）
     */
    @TableField(value = "credential_id")
    private byte[] credentialId;

    /**
     * 凭据ID（base64url，用于查询）
     */
    @TableField(value = "credential_id_b64")
    private String credentialIdB64;

    /**
     * COSE 格式公钥
     */
    @TableField(value = "public_key_cose")
    private byte[] publicKeyCose;

    /**
     * 签名计数器
     */
    @TableField(value = "sign_count")
    private Long signCount;

    /**
     * RP ID
     */
    @TableField(value = "rp_id")
    private String rpId;

    /**
     * 用户句柄（二进制）
     */
    @TableField(value = "user_handle")
    private byte[] userHandle;

    /**
     * 用户句柄（base64url）
     */
    @TableField(value = "user_handle_b64")
    private String userHandleB64;

    /**
     * 认证器 AAGUID
     */
    @TableField(value = "aaguid")
    private byte[] aaguid;

    /**
     * 证明类型
     */
    @TableField(value = "attestation_type")
    private String attestationType;

    /**
     * 设备名称
     */
    @TableField(value = "device_name")
    private String deviceName;

    /**
     * 传输方式 JSON 数组
     */
    @TableField(value = "transports")
    private String transports;

    /**
     * 是否可发现凭据：0-否 1-是
     */
    @TableField(value = "is_discoverable")
    private Integer discoverable;

    /**
     * 是否可备份：0-否 1-是
     */
    @TableField(value = "is_backup_eligible")
    private Integer backupEligible;

    /**
     * 当前是否已备份：0-否 1-是
     */
    @TableField(value = "is_backup_state")
    private Integer backupState;

    /**
     * 最后使用时间
     */
    @TableField(value = "last_used_time")
    private LocalDateTime lastUsedTime;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;

}
