package com.carlos.auth.app.pojo.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 应用门户 数据传输对象
 *
 * @author Carlos
 * @date 2026-07-02
 */
@Data
@Accessors(chain = true)
public class AppPortalAppDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 应用ID */
    private Long id;

    /** 应用编号 */
    private String appKey;

    /** 应用名称 */
    private String appName;

    /** 应用Logo */
    private String appLogo;

    /** 应用描述 */
    private String appDesc;

    /** 应用类型(1内部应用 2外部应用 3移动应用) */
    private Integer appType;

    /** 应用首页地址 */
    private String homepageUrl;

    /** 展示排序 */
    private Integer displayOrder;

    /** 分类ID */
    private Long categoryId;
}
