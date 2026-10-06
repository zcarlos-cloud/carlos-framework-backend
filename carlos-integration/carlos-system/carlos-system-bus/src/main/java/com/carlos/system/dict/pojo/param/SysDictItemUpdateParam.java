package com.carlos.system.dict.pojo.param;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * <p>
 * 系统字典详情 更新参数封装
 * </p>
 *
 * @author carlos
 * @date 2021-11-22 14:49:00
 */
@Data
@Accessors(chain = true)
@Schema(description = "系统字典详情修改参数")
public class SysDictItemUpdateParam {

    @NotNull(message = "主键不能为空")
    @Schema(description = "主键")
    private Long id;

    @Size(max = 100, message = "字典项值长度不能超过100")
    @Schema(description = "字典项值")
    private String itemName;

    @Size(max = 100, message = "字典项code长度不能超过100")
    @Schema(description = "字典项key")
    private String itemCode;

    @Size(max = 500, message = "描述长度不能超过500")
    @Schema(description = "描述")
    private String description;

    @Schema(description = "排序")
    private Integer orderNum;

    @Schema(description = "是否启用")
    private Boolean enable;

}
