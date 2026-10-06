package com.carlos.org.pojo.param;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;


/**
 * <p>
 * 租户 初始化管理员参数封装
 * </p>
 * <p>为新租户初始化管理员账号（账号在该租户内唯一）</p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
@Data
@Accessors(chain = true)
@Schema(description = "租户初始化管理员参数")
public class OrgTenantInitAdminParam {

    /**
     * 租户ID
     */
    @NotNull(message = "租户ID不能为空")
    @Schema(description = "租户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long tenantId;

    /**
     * 管理员账号
     */
    @NotBlank(message = "管理员账号不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{4,32}$", message = "管理员账号只能包含字母、数字、下划线，长度4-32")
    @Schema(description = "管理员账号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String account;

    /**
     * 管理员昵称
     */
    @NotBlank(message = "管理员昵称不能为空")
    @Size(max = 64, message = "管理员昵称长度不能超过64")
    @Schema(description = "管理员昵称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nickname;

    /**
     * 初始密码（明文，服务端编码后存储）
     */
    @NotBlank(message = "初始密码不能为空")
    @Size(min = 8, max = 64, message = "初始密码长度8-64位")
    @Schema(description = "初始密码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    /**
     * 手机号
     */
    @Size(max = 32, message = "手机号长度不能超过32")
    @Schema(description = "手机号")
    private String phone;

}
