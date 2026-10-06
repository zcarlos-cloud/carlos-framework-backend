package com.carlos.auth.audit.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 安全告警实体
 * </p>
 *
 * @author Carlos
 * @date 2026-02-26
 */
@Data
@Accessors(chain = true)
@TableName("security_alert")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SecurityAlert implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 告警ID
     */
    @TableId(type = IdType.AUTO, value = "id")
    private Long id;

    /**
     * 用户ID
     */
    @TableField(value = "user_id")
    private Long userId;

    /**
     * 用户名
     */
    @TableField(value = "username")
    private String username;

    /**
     * 告警类型
     */
    @TableField(value = "alert_type")
    private String alertType;

    /**
     * 告警级别
     */
    @TableField(value = "severity")
    private String severity;

    /**
     * 告警标题
     */
    @TableField(value = "title")
    private String title;

    /**
     * 告警内容
     */
    @TableField(value = "content")
    private String content;

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
     * 是否已处理
     */
    @TableField(value = "handled")
    private Boolean handled;

    /**
     * 处理时间
     */
    @TableField(value = "handled_time")
    private LocalDateTime handledTime;

    /**
     * 备注
     */
    @TableField(value = "remark")
    private String remark;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
