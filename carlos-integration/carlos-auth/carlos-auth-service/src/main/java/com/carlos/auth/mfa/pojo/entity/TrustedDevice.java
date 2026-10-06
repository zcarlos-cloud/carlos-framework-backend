package com.carlos.auth.mfa.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 可信设备实体
 * </p>
 *
 * @author Carlos
 * @date 2026-02-26
 */
@Data
@Accessors(chain = true)
@TableName("auth_trusted_device")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TrustedDevice implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 设备ID
     */
    @TableId(type = IdType.AUTO, value = "id")
    private Long id;

    /**
     * 用户ID
     */
    @TableField(value = "user_id")
    private Long userId;

    /**
     * 设备指纹（MD5）
     */
    @TableField(value = "device_fingerprint")
    private String deviceFingerprint;

    /**
     * 设备名称（如：Chrome on Windows）
     */
    @TableField(value = "device_name")
    private String deviceName;

    /**
     * IP地址
     */
    @TableField(value = "ip_address")
    private String ipAddress;

    /**
     * 地理位置
     */
    @TableField(value = "location")
    private String location;

    /**
     * User-Agent
     */
    @TableField(value = "user_agent")
    private String userAgent;

    /**
     * 浏览器名称
     */
    @TableField(value = "browser")
    private String browser;

    /**
     * 是否为移动设备
     */
    @TableField(value = "is_mobile")
    private Boolean isMobile;

    /**
     * 添加为可信时间
     */
    @TableField(value = "trusted_time")
    private LocalDateTime trustedTime;

    /**
     * 最后使用时间
     */
    @TableField(value = "last_used_time")
    private LocalDateTime lastUsedTime;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(value = "deleted")
    @TableLogic
    private Boolean deleted;
}
