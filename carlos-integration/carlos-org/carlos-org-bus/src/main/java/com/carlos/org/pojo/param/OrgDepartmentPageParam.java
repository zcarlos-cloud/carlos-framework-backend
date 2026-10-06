package com.carlos.org.pojo.param;


import com.carlos.core.param.ParamPage;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;


/**
 * <p>
 * 部门分页查询参数
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OrgDepartmentPageParam extends ParamPage {

    /**
     * 父部门id
     */
    @Schema(description = "父部门id")
    private Serializable parentId;

    /**
     * 部门名称
     */
    @Size(max = 64, message = "部门名称长度不能超过64")
    @Schema(description = "部门名称")
    private String deptName;

    /**
     * 状态，0：禁用，1：启用
     */
    @Schema(description = "状态，0：禁用，1：启用")
    private Integer deptStatus;

}
