package com.carlos.auth.api.pojo.param;


import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 应用信息 API提供的对象(API Object)
 * </p>
 *
 * @author Carlos
 * @date 2025-3-12 14:00:14
 */
@Data
@Accessors(chain = true)
public class ApiAppClientParam implements Serializable {
    /**
     * 主键
     */
    private Long id;
    /**
     * 应用编号
     */
    private String appKey;
    /**
     * 应用名称
     */
    private String appName;
    /**
     * 应用logo
     */
    private String appLogo;
    /**
     * 应用密钥
     */
    private String appSecret;
    /**
     * 应用密钥到期时间
     */
    private LocalDateTime clientSecretExpiresAt;
    /**
     * 应用发行时间
     */
    private LocalDateTime clientIssuedAt;
    /**
     * 认证方式
     */
    private String authenticationMethods;
    /**
     * grant_type
     */
    private String authorizationGrantTypes;
    /**
     * scopes
     */
    private String scopes;
    /**
     * 重定向地址
     */
    private String redirectUris;
    /**
     * client设置 key:value;
     */
    private String clientSettings;
    /**
     * token设置 key:value;
     */
    private String tokenSettings;
    /**
     * 应用状态
     */
    private String clientStatus;
    /**
     * 创建人
     */
    private Long createBy;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    /**
     * 更新人
     */
    private Long updateBy;
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
