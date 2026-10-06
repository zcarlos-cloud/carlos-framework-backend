package com.carlos.org.pojo.param;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDate;

/**
 * <p>
 * 用户分配岗位参数
 * </p>
 *
 * @author carlos
 * @date 2026-03-03
 */
@Data
@Accessors(chain = true)
@Schema(description = "用户分配岗位参数")
public class OrgUserAssignPositionParam {

    @NotNull(message = "用户id不能为空")
    @Schema(description = "用户ID")
    private Long userId;

    @NotNull(message = "岗位id不能为空")
    @Schema(description = "岗位ID")
    private Long positionId;

    @NotNull(message = "职级id不能为空")
    @Schema(description = "职级ID")
    private Long levelId;

    @NotNull(message = "部门id不能为空")
    @Schema(description = "任职部门ID")
    private Long departmentId;

    @NotNull(message = "是否主岗位不能为空")
    @Schema(description = "是否主岗位：true是，false否")
    private Boolean isMain;

    @Schema(description = "任职日期")
    private LocalDate appointDate;

}
