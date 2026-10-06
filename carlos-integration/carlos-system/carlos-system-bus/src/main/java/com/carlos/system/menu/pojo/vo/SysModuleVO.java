package com.carlos.system.menu.pojo.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 功能模块 显示层对象
 * </p>
 *
 * @author carlos
 * @date 2026-05-26
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SysModuleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "父级ID")
    private Long parentId;

    @Schema(description = "模块名称")
    private String moduleName;

    @Schema(description = "模块编码")
    private String moduleCode;

    @Schema(description = "排序")
    private Integer orderNum;

    @Schema(description = "状态：true启用 false禁用")
    private Boolean moduleStatus;

}
