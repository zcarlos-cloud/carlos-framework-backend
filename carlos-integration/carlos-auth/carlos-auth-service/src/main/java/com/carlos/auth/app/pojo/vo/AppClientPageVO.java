package com.carlos.auth.app.pojo.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 应用信息 显示层对象，向页面传输的对象
 * </p>
 *
 * @author Carlos
 * @date 2025-3-12 14:00:14
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AppClientPageVO implements Serializable {
    private static final long serialVersionUID = 1L;
    @Schema(description = "主键")
    private Long id;
    @Schema(description = "应用编号")
    private String appKey;
    @Schema(description = "应用名称")
    private String appName;
    @Schema(description = "应用logo")
    private String appLogo;
    @Schema(description = "应用密钥到期时间")
    private LocalDateTime clientSecretExpiresAt;
    @Schema(description = "应用发行时间")
    private LocalDateTime clientIssuedAt;
    @Schema(description = "认证方式")
    private String authenticationMethods;
    @Schema(description = "grant_type")
    private String authorizationGrantTypes;
    @Schema(description = "scopes")
    private String scopes;
    @Schema(description = "重定向地址")
    private String redirectUris;
    @Schema(description = "应用状态")
    private String clientStatus;
    @Schema(description = "创建人")
    private Long createBy;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "更新人")
    private Long updateBy;
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
