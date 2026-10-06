package com.carlos.org.pojo.param;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;


/**
 * <p>
 * 租户 更新参数封装
 * </p>
 * <p>更新租户（租户编码创建后不可修改）</p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
@Data
@Accessors(chain = true)
@Schema(description = "租户更新参数")
public class OrgTenantUpdateParam {

    /**
     * 主键
     */
    @NotNull(message = "租户ID不能为空")
    @Schema(description = "租户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    /**
     * 租户名称
     */
    @NotBlank(message = "租户名称不能为空")
    @Size(max = 128, message = "租户名称长度不能超过128")
    @Schema(description = "租户名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String tenantName;

    /**
     * 联系人
     */
    @Size(max = 64, message = "联系人长度不能超过64")
    @Schema(description = "联系人")
    private String contactName;

    /**
     * 联系电话
     */
    @Size(max = 32, message = "联系电话长度不能超过32")
    @Schema(description = "联系电话")
    private String contactPhone;

    /**
     * 过期时间（NULL 表示永不过期）
     */
    @Schema(description = "过期时间（不传表示永不过期）")
    private LocalDateTime expireTime;

    /**
     * 备注
     */
    @Size(max = 512, message = "备注长度不能超过512")
    @Schema(description = "备注")
    private String remark;

}
