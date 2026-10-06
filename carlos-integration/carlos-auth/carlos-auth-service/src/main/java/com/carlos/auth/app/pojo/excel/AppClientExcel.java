package com.carlos.auth.app.pojo.excel;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 部门 导入导出数据对象
 * </p>
 */
@Data
public class AppClientExcel implements Serializable {

    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "应用编号(非必填)")
    private String appKey;
    @ExcelProperty(value = "应用名称(必填)")
    private String appName;
    @ExcelProperty(value = "应用logo(非必填)")
    private String appLogo;
    @ExcelProperty(value = "应用密钥(非必填)")
    private String appSecret;
    @ExcelProperty(value = "应用密钥到期时间(非必填)")
    private LocalDateTime clientSecretExpiresAt;
    @ExcelProperty(value = "应用发行时间(非必填)")
    private LocalDateTime clientIssuedAt;
    @ExcelProperty(value = "认证方式")
    private String authenticationMethods;
    @ExcelProperty(value = "grant_type(非必填)")
    private String authorizationGrantTypes;
    @ExcelProperty(value = "scopes(非必填)")
    private String scopes;
    @ExcelProperty(value = "重定向地址(非必填)")
    private String redirectUris;
    @ExcelProperty(value = "错误信息")
    private String error;

}
