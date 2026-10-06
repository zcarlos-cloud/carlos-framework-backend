package com.carlos.auth.app.pojo.param;


import com.carlos.auth.app.pojo.dto.Oauth2ClientSettings;
import com.carlos.auth.app.pojo.dto.Oauth2TokenSettings;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.Set;


/**
 * <p>
 * 应用信息 新增参数封装
 * </p>
 *
 * @author Carlos
 * @date 2025-3-12 14:00:14
 */
@Data
@Accessors(chain = true)
@Schema(name = "应用信息新增参数", description = "应用信息新增参数")
public class AppClientCreateParam {
    @NotBlank(message = "应用名称不能为空")
    @Size(max = 64, message = "应用名称长度不能超过64")
    @Schema(description = "应用名称")
    private String appName;
    @NotBlank(message = "应用logo不能为空")
    @Size(max = 255, message = "应用logo长度不能超过255")
    @Schema(description = "应用logo")
    private String appLogo;
    @Size(max = 255, message = "应用描述长度不能超过255")
    @Schema(description = "应用描述")
    private String appDesc;
    @Schema(description = "应用类型(1内部应用 2外部应用 3移动应用)")
    private Integer appType;
    @Schema(description = "应用分类ID")
    private Long appCategoryId;
    @Size(max = 255, message = "应用首页地址长度不能超过255")
    @Schema(description = "应用首页地址(完整URL)")
    private String homepageUrl;
    @Schema(description = "门户展示排序")
    private Integer displayOrder;
    @Schema(description = "门户可见(0隐藏 1可见)")
    private Boolean portalVisible;
    @Schema(description = "应用密钥到期时间")
    private LocalDateTime clientSecretExpiresAt;
    @Schema(description = "应用发行时间")
    private LocalDateTime clientIssuedAt;
    @NotEmpty(message = "认证方式不能为空")
    @Schema(description = "认证方式")
    private Set<String> authenticationMethods;
    @NotEmpty(message = "grant_type不能为空")
    @Schema(description = "grant_type")
    private Set<String> authorizationGrantTypes;
    @Schema(description = "scopes")
    private Set<String> scopes;
    @Schema(description = "重定向地址")
    private Set<String> redirectUris;
    @Schema(description = "client设置 key:value;")
    private Oauth2ClientSettings clientSettings;
    @Schema(description = "token设置 key:value;")
    private Oauth2TokenSettings tokenSettings;
}
