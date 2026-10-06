package com.carlos.org.pojo.param;


import com.carlos.core.param.ParamPage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;


/**
 * <p>
 * 用户部门 列表查询参数封装
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户部门列表查询参数")
public class OrgUserDepartmentPageParam extends ParamPage {
    @Schema(description = "用户id")
    private Long userId;
    @Schema(description = "部门id")
    private Long deptId;
    @Schema(description = "是否为主部门")
    private Boolean mainDept;
    @Schema(description = "开始时间")
    private LocalDateTime start;

    @Schema(description = "结束时间")
    private LocalDateTime end;
}
