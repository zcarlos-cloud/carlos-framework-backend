package com.carlos.org.pojo.param;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;


/**
 * <p>
 * 租户 状态变更参数封装
 * </p>
 * <p>停用/启用租户</p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
@Data
@Accessors(chain = true)
@Schema(description = "租户状态变更参数")
public class OrgTenantChangeStatusParam {

    /**
     * 租户ID
     */
    @NotNull(message = "租户ID不能为空")
    @Schema(description = "租户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    /**
     * 租户状态，1：正常, 2: 停用
     */
    @NotNull(message = "租户状态不能为空")
    @Schema(description = "租户状态，1：正常, 2: 停用", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer tenantStatus;

}
