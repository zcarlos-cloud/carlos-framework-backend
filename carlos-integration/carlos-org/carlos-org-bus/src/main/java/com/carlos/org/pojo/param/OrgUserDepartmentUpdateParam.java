package com.carlos.org.pojo.param;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * <p>
 * 用户部门 更新参数封装
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@Data
@Accessors(chain = true)
@Schema(description = "用户部门修改参数")
public class OrgUserDepartmentUpdateParam {
    @NotNull(message = "主键不能为空")
    @Schema(description = "主键")
    private Long id;
    @Schema(description = "用户id")
    private Long userId;
    @Schema(description = "部门id")
    private Long deptId;
    @Schema(description = "是否为主部门")
    private Boolean mainDept;
}
