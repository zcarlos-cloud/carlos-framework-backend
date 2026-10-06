package com.carlos.auth.audit.pojo.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 安全告警 DTO
 * </p>
 *
 * <p>服务层与数据层之间传输的对象</p>
 *
 * @author Carlos
 * @date 2026-04-08
 */
@Data
@Accessors(chain = true)
public class SecurityAlertDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 告警ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 告警类型
     */
    private String alertType;

    /**
     * 告警级别
     */
    private String severity;

    /**
     * 告警标题
     */
    private String title;

    /**
     * 告警内容
     */
    private String content;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * 地理位置
     */
    private String location;

    /**
     * User-Agent
     */
    private String userAgent;

    /**
     * 是否已处理
     */
    private Boolean handled;

    /**
     * 处理时间
     */
    private LocalDateTime handledTime;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
