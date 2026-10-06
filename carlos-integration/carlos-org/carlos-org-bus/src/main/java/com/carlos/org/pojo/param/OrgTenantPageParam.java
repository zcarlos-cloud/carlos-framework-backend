package com.carlos.org.pojo.param;


import com.carlos.core.param.ParamPage;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;


/**
 * <p>
 * 租户分页查询参数
 * </p>
 * <p>租户列表</p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OrgTenantPageParam extends ParamPage {

    /**
     * 租户编码
     */
    @Size(max = 32, message = "租户编码长度不能超过32")
    @Schema(description = "租户编码")
    private String tenantCode;

    /**
     * 租户名称
     */
    @Size(max = 128, message = "租户名称长度不能超过128")
    @Schema(description = "租户名称")
    private String tenantName;

    /**
     * 租户状态，1：正常, 2: 停用
     */
    @Schema(description = "租户状态，1：正常, 2: 停用")
    private Integer tenantStatus;

}
