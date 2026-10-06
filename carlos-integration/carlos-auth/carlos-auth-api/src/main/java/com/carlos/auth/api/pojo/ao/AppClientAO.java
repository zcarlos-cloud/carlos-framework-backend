package com.carlos.auth.api.pojo.ao;


import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 应用信息 API提供的对象(API Object)
 * </p>
 *
 * @author Carlos
 * @date 2025-3-12 14:00:14
 */
@Data
public class AppClientAO implements Serializable {
    private static final long serialVersionUID = 6203103176331087502L;
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
}
